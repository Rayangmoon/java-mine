# 自选股 Watchlist 后端设计规格

## 概述

一个简单的自选股管理后端应用，支持增删改查，行情接入新浪 API。目标是让前端开发者学习 Java 后端核心知识：Controller → Service → Mapper 三层架构、MyBatis Plus、Docker 部署。

## 决策记录

| 维度 | 决策 | 原因 |
|------|------|------|
| 用户模型 | 先单用户，数据模型不预留 user_id | 保持简单，聚焦核心知识 |
| 分页 | 不需要，全量返回 | 自选股数量有限 |
| 行情策略 | 每次请求实时调新浪 API 拼接 | 最简单直接 |
| 行情数据源 | 新浪 hq.sinajs.cn，挂了再换 | 学习为主 |
| 项目结构 | 单模块 Maven | 降低学习门槛 |
| 部署 | Docker Compose（app + mysql 两个容器） | 简单可复现 |

## 技术栈

- JDK 8 + Spring Boot 2.7.x + Maven
- MyBatis Plus 3.5.x
- MySQL 8.0
- RestTemplate（调用新浪行情 API）
- Docker + Docker Compose

## 项目包结构

```
src/main/java/com/watchlist/
├── controller/          ← 接收请求、返回响应
│   └── StockController.java
├── service/             ← 处理业务规则
│   ├── StockService.java
│   └── impl/
│       └── StockServiceImpl.java
├── mapper/              ← 操作数据库
│   └── StockMapper.java
├── entity/              ← 映射数据库表
│   └── Stock.java
├── dto/                 ← 接口入参/出参定义
│   ├── StockAddRequest.java
│   ├── StockUpdateRequest.java
│   └── StockQuoteVO.java
├── common/              ← 统一响应格式、异常处理
│   └── Result.java
└── config/              ← 配置类
    └── AppConfig.java
```

## 数据模型

### stock_watchlist 表

```sql
CREATE TABLE stock_watchlist (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code  VARCHAR(6)   NOT NULL COMMENT '股票代码，如 600519',
    stock_name  VARCHAR(64)  NOT NULL COMMENT '股票名称',
    notes       VARCHAR(256) DEFAULT '' COMMENT '备注',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_code (stock_code)
);
```

股票代码只存 6 位数字，调用新浪 API 时根据首位数字自动推导交易所前缀：
- `6` 开头 → `sh`（上海，含科创板 688xxx）
- `0` 或 `3` 开头 → `sz`（深圳）
- `4` 或 `8` 开头 → `bj`（北交所）
- 其他首位数字 → 抛出参数异常，拒绝添加

参数校验层额外限制：stockCode 首位必须为 `0/3/4/6/8` 之一。

## API 设计

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 接口列表

#### 1. GET /api/stocks — 查询自选股列表（附带实时行情）

- 入参：无
- 出参：`Result<List<StockQuoteVO>>`
- 逻辑：查数据库获取所有自选股 → 批量调新浪 API 获取行情 → 拼接返回
- 降级：若新浪 API 调用失败（超时/不可用），仍返回自选股列表，行情字段置为 null

#### 2. POST /api/stocks — 添加自选股

- 入参：`StockAddRequest { stockCode, notes? }`
- 出参：`Result<Stock>`
- 逻辑：根据 stockCode 调用新浪 API 自动获取 stockName，无需用户手动填写
- 校验：
  - stockCode 格式匹配 `^\d{6}$`，且首位为 `0/3/4/6/8`
  - 调新浪 API 验证代码有效（能查到行情）
  - 不能重复添加（Service 层检查 + 数据库 UNIQUE 兜底）

#### 3. PUT /api/stocks/{id} — 修改备注

- 入参：`StockUpdateRequest { notes }`
- 出参：`Result<Stock>`
- 校验：id 必须存在

#### 4. DELETE /api/stocks/{id} — 删除自选股

- 入参：路径参数 id
- 出参：`Result<Void>`
- 校验：id 必须存在

#### 5. GET /api/quotes?codes=600519,000001 — 批量获取实时行情

- 入参：query 参数 codes（逗号分隔的股票代码）
- 出参：`Result<List<StockQuoteVO>>`
- 逻辑：拼接前缀 → 调新浪 API → 解析返回

### StockQuoteVO 结构

```json
{
  "stockCode": "600519",
  "stockName": "贵州茅台",
  "currentPrice": 1688.00,
  "todayOpen": 1685.00,
  "yesterdayClose": 1682.00,
  "high": 1695.00,
  "low": 1680.00,
  "volume": 12345678,          // Long 类型，成交量可能很大
  "notes": "长期持有",
  "id": 1
}
```

> GET /api/stocks 返回时包含 id 和 notes（来自数据库）；GET /api/quotes 返回时 id 和 notes 为 null（纯行情查询）。

## 新浪行情 API 对接

请求格式：`https://hq.sinajs.cn/list=sh600519,sz000001`

响应格式（GBK 编码）：
```
var hq_str_sh600519="贵州茅台,1688.00,1682.00,1690.00,1695.00,1680.00,...";
```

逗号分隔的字段中，关键字段位置：
- 0: 股票名称
- 1: 今日开盘价
- 2: 昨日收盘价
- 3: 当前价格
- 4: 今日最高价
- 5: 今日最低价
- 8: 成交量

需要在 RestTemplate 配置中处理 GBK 编码。

## 部署架构

### Docker Compose（两个独立容器）

```
容器 1: app (Spring Boot)  ──内部网络──▶  容器 2: mysql (MySQL 8.0)
         ↕ 端口 8080                              端口 3306 仅内部网络
      宿主机 8080                              volume 持久化

> MySQL 端口不映射到宿主机，仅 app 容器通过 Docker 内部网络访问，避免数据库暴露。
```

### 配置分离

- `application.yml` — 本地开发，连接本地/Docker MySQL
- `application-docker.yml` — Docker 部署，连接容器内 MySQL（hostname 为 docker-compose 服务名 `mysql`）

### 数据库初始化

- `sql/init.sql` 存放建表脚本
- Docker Compose 中挂载到 MySQL 的 `/docker-entrypoint-initdb.d/`，首次启动自动执行

### 部署步骤

1. 本地 `mvn package` 打出 JAR 包
2. Dockerfile 基于 JDK 8 镜像，复制 JAR 进去
3. `docker-compose up -d` 启动
4. 阿里云安全组放行 8080 端口
5. 通过 `http://公网IP:8080/api/stocks` 访问

## 本地开发环境

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 8 (1.8) | Spring Boot 2.7.x 运行环境 |
| Maven | 3.8.x+ | 包管理，类似 npm |
| IDEA | Community 版 | Java IDE |
| Docker Desktop | 最新版 | macOS 上运行 Docker |
| MySQL | 8.0 | 本地开发用 Docker 容器起 |

## 参数校验

- stock_code：正则 `^\d{6}$`，且首位为 `0/3/4/6/8`
- 重复添加：Service 层查询 + 数据库 UNIQUE 约束双重保障
- id 存在性：修改和删除前检查

## 错误处理

使用全局异常处理器（`@RestControllerAdvice`）统一捕获异常，返回标准 Result 格式：
- 参数校验失败 → code 400
- 资源不存在 → code 404
- 重复添加 → code 409
- 外部 API 调用失败 → code 502
- 服务器内部错误 → code 500
