# 自选股 Watchlist 后端项目 — 从零到部署全过程文档

> 本文档记录了一个前端开发者从零搭建 Java 后端项目并部署上线的完整过程。
> 涉及的后端/Java/部署知识点会用 `📌 知识点` 标注。

---

## 一、项目规划与设计

### 1.1 确定业务场景

选定「自选股 Watchlist」作为学习载体，覆盖后端核心知识：

- REST API 设计（增删改查）
- 三层架构（Controller → Service → Mapper）
- 外部 HTTP 调用（新浪行情 API）
- 数据库操作（MySQL + MyBatis Plus）
- 容器化部署（Docker Compose）

### 1.2 技术选型


| 技术              | 作用            | 前端类比               |
| --------------- | ------------- | ------------------ |
| JDK 8           | Java 运行环境     | Node.js            |
| Maven           | 依赖管理 + 构建工具   | npm/pnpm           |
| Spring Boot 2.7 | Web 框架        | Express/Koa        |
| MyBatis Plus    | ORM 框架（操作数据库） | Prisma/Sequelize   |
| MySQL 8.0       | 关系型数据库        | MongoDB/PostgreSQL |
| Docker Compose  | 容器编排部署        | -                  |


> 📌 **知识点：Spring Boot**
> Spring Boot 是 Java 生态最流行的 Web 框架，它通过"约定优于配置"的理念，大幅简化了项目搭建。类似前端的 `create-vue` 脚手架，内置了大量默认配置。

### 1.3 三层架构设计

```
前端请求 → Controller（路由层） → Service（业务逻辑层） → Mapper（数据访问层） → MySQL
```


| 层          | 职责                         | 前端类比                 |
| ---------- | -------------------------- | -------------------- |
| Controller | 接收 HTTP 请求、参数校验、返回响应       | Express 的路由处理函数      |
| Service    | 业务逻辑处理、调用多个 Mapper 或外部 API | Vuex/Pinia 的 actions |
| Mapper     | 数据库 CRUD 操作                | axios 调后端 API（方向相反）  |


> 📌 **知识点：为什么要分三层？**
>
> - **单一职责**：每层只做一件事，好维护
> - **可测试性**：可以 mock 掉下层来单独测试上层
> - **可替换性**：换数据库只改 Mapper 层，不影响 Controller 和 Service

---

## 二、环境搭建

### 2.1 安装 JDK 8

```bash
# macOS 用 Homebrew 安装（实际从清华镜像下载的 pkg）
brew install --cask temurin@8

# 验证
java -version
# → openjdk version "1.8.0_482"
```

> 📌 **知识点：JDK vs JRE**
>
> - JDK = Java Development Kit，开发用（含编译器）
> - JRE = Java Runtime Environment，运行用
> - 开发时装 JDK，Docker 部署时只需 JRE（更小）

### 2.2 安装 Maven

```bash
brew install maven

# 验证
mvn -version
# → Apache Maven 3.9.15
```

> 📌 **知识点：Maven 核心概念**
>
> - `pom.xml` = 类似 `package.json`，定义项目依赖和构建配置
> - `mvn compile` = 编译代码（类似 `tsc`）
> - `mvn test` = 运行测试（类似 `npm test`）
> - `mvn package` = 打包成 JAR（类似 `npm run build`）
> - `~/.m2/repository/` = 本地依赖缓存（类似 `node_modules` 但全局共享）

### 2.3 Podman（容器工具）

本机使用 Podman 代替 Docker（命令兼容，用法一致）。

```bash
podman machine start   # 启动虚拟机
podman run ...         # 运行容器（等同 docker run）
```

---

## 三、项目初始化

### 3.1 创建目录结构

```
stock-watchlist/
├── pom.xml                          ← Maven 配置（依赖声明）
├── sql/init.sql                     ← 数据库建表脚本
├── Dockerfile                       ← 容器镜像定义
├── docker-compose.yml               ← 容器编排
├── src/main/java/com/watchlist/     ← Java 源码
│   ├── StockWatchlistApplication.java  ← 启动入口
│   ├── config/                      ← 配置类
│   ├── common/                      ← 通用工具（Result、异常处理）
│   ├── entity/                      ← 数据库表映射
│   ├── dto/                         ← 请求/响应数据结构
│   ├── mapper/                      ← 数据库操作接口
│   ├── service/                     ← 业务逻辑
│   └── controller/                  ← REST API 入口
├── src/main/resources/              ← 配置文件
│   ├── application.yml              ← 本地开发配置
│   └── application-docker.yml       ← Docker 部署配置
└── src/test/                        ← 测试代码
```

> 📌 **知识点：Java 包命名规范**
> `com.watchlist.controller` 这种格式叫 package（包），类似前端的文件夹路径。按"倒置域名 + 功能模块"命名，保证唯一。

### 3.2 pom.xml 核心依赖

```xml
<!-- Web 框架 -->
spring-boot-starter-web

<!-- 参数校验（@NotBlank、@Pattern 等注解） -->
spring-boot-starter-validation

<!-- MyBatis Plus ORM -->
mybatis-plus-boot-starter

<!-- MySQL 驱动 -->
mysql-connector-java

<!-- Lombok（自动生成 getter/setter） -->
lombok

<!-- 测试框架 -->
spring-boot-starter-test
h2  <!-- 内存数据库，测试用 -->
```

> 📌 **知识点：Spring Boot Starter**
> `spring-boot-starter-xxx` 是 Spring Boot 的"套餐"，一个 starter 包含了某个功能所需的所有依赖。类似前端的 `@vue/cli-plugin-xxx`。

---

## 四、数据库设计

### 4.1 建表 SQL

```sql
CREATE TABLE stock_watchlist (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code  VARCHAR(6)   NOT NULL,
    stock_name  VARCHAR(64)  NOT NULL,
    notes       VARCHAR(256) DEFAULT '',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_code (stock_code)
);
```

> 📌 **知识点：数据库关键概念**
>
> - `PRIMARY KEY` = 主键，每行数据的唯一标识
> - `AUTO_INCREMENT` = 自增，不用手动指定 ID
> - `UNIQUE KEY` = 唯一索引，防止重复（类似 Set 去重）
> - `ON UPDATE CURRENT_TIMESTAMP` = 更新时自动刷新时间戳

### 4.2 多环境配置


| 文件                       | 用途        | 数据库地址                  |
| ------------------------ | --------- | ---------------------- |
| `application.yml`        | 本地开发      | `localhost:3306`       |
| `application-docker.yml` | Docker 部署 | `mysql:3306`（容器内部 DNS） |
| `application-test.yml`   | 单元测试      | H2 内存数据库               |


> 📌 **知识点：Spring Profile**
> 通过 `--spring.profiles.active=docker` 切换配置文件，类似前端的 `.env.development` 和 `.env.production`。

---

## 五、代码实现

### 5.1 Entity — 数据库表映射

```java
@Data                           // Lombok：自动生成 getter/setter
@TableName("stock_watchlist")   // 告诉 ORM 对应哪张表
public class Stock {
    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;
    private String stockCode;    // 自动映射 stock_code 列
    private String stockName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

> 📌 **知识点：ORM（对象关系映射）**
> 把数据库表的每一行映射为 Java 对象。`stock_code` 列自动对应 `stockCode` 字段（下划线转驼峰）。类似前端 TypeScript 的 interface 映射 API 返回值。

### 5.2 Mapper — 数据库操作

```java
public interface StockMapper extends BaseMapper<Stock> {
    // 继承 BaseMapper 即可获得：
    // insert()、selectById()、updateById()、deleteById()
    // selectList()、selectOne() 等方法
    // 不需要写一行 SQL！
}
```

> 📌 **知识点：MyBatis Plus BaseMapper**
> 类似前端用了 Prisma 后不用手写 SQL。继承 `BaseMapper<T>` 即自动获得所有基础 CRUD 方法。复杂查询用 `QueryWrapper` 构建条件。

### 5.3 Service — 业务逻辑

```java
@Service
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;
    private final SinaStockApi sinaStockApi;

    // 构造器注入（依赖注入）
    public StockServiceImpl(StockMapper stockMapper, SinaStockApi sinaStockApi) {
        this.stockMapper = stockMapper;
        this.sinaStockApi = sinaStockApi;
    }

    @Override
    public Stock addStock(StockAddRequest request) {
        // 1. 检查重复
        // 2. 调新浪 API 获取股票名称
        // 3. 插入数据库
        // 4. 返回结果
    }
}
```

> 📌 **知识点：依赖注入（DI）**
> Spring 自动创建对象实例并"注入"到需要它的地方。类似前端的 `provide/inject` 或 React Context。你不用手动 `new StockMapper()`，框架帮你管理。

> 📌 **知识点：接口 + 实现分离**
> `StockService`（接口）定义方法签名，`StockServiceImpl`（实现）写具体逻辑。好处：测试时可以 mock 接口，未来可以替换实现。

### 5.4 Controller — REST API

```java
@RestController         // 标记为 REST 控制器
@RequestMapping("/api") // URL 前缀
public class StockController {

    @GetMapping("/stocks")           // GET /api/stocks
    @PostMapping("/stocks")          // POST /api/stocks
    @PutMapping("/stocks/{id}")      // PUT /api/stocks/123
    @DeleteMapping("/stocks/{id}")   // DELETE /api/stocks/123
}
```

> 📌 **知识点：Spring MVC 注解**
>
> - `@RestController` = 这个类处理 HTTP 请求并返回 JSON
> - `@GetMapping`/`@PostMapping` = 路由映射，类似 Express 的 `router.get()`
> - `@PathVariable` = 从 URL 取参数（`/stocks/:id` 中的 `:id`）
> - `@RequestBody` = 从请求体解析 JSON（类似 `req.body`）
> - `@RequestParam` = 从 query string 取参数（类似 `req.query`）
> - `@Valid` = 触发参数校验

### 5.5 通用响应与异常处理

```java
// 统一响应格式
Result.success(data)  → { "code": 200, "message": "success", "data": ... }
Result.error(400, "参数错误") → { "code": 400, "message": "参数错误", "data": null }

// 全局异常处理器 — 类似前端的 axios 响应拦截器
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<?> handle(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
}
```

> 📌 **知识点：@RestControllerAdvice**
> 全局异常拦截器，任何 Controller 抛出的异常都会被捕获并转换为统一格式。避免每个接口都写 try-catch。

### 5.6 新浪行情 API 对接

```java
// 用 RestTemplate 调外部 HTTP（类似前端的 axios/fetch）
String url = "https://hq.sinajs.cn/list=sh600519";
ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

// 响应是 GBK 编码，需要手动转换
String body = new String(bytes, Charset.forName("GBK"));
```

> 📌 **知识点：RestTemplate**
> Spring 内置的 HTTP 客户端，类似前端的 `axios`。用于在后端调用其他服务的 API。
> 注意：新浪 API 返回 GBK 编码（不是 UTF-8），需要特殊处理。

---

## 六、测试

### 6.1 测试分层


| 测试类                   | 测试什么       | 技术                         |
| --------------------- | ---------- | -------------------------- |
| `StockMapperTest`     | 数据库 CRUD   | H2 内存数据库 + @SpringBootTest |
| `SinaStockApiTest`    | API 响应解析逻辑 | 纯单元测试（不启动 Spring）          |
| `StockServiceTest`    | 业务逻辑       | Mockito mock 依赖            |
| `StockControllerTest` | HTTP 接口    | MockMvc 模拟 HTTP 请求         |


### 6.2 运行测试

```bash
# 运行全部测试
mvn test
# → Tests run: 20, Failures: 0, Errors: 0 ✅

# 运行单个测试类
mvn test -Dtest=StockServiceTest
```

> 📌 **知识点：Mock 测试**
>
> - `@Mock` = 创建一个假的依赖对象（类似 `vi.mock()`）
> - `@InjectMocks` = 把假依赖注入到被测对象中
> - `when(...).thenReturn(...)` = 指定假对象的返回值
> - `@WebMvcTest` = 只启动 Controller 层，mock 掉 Service（快速、轻量）

---

## 七、本地端到端验证

### 7.1 启动 MySQL 容器

```bash
podman run -d --name watchlist-mysql \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -p 3306:3306 \
  -v ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql \
  mysql:8.0
```

> 📌 **知识点：Docker 容器**
>
> - `-d` = 后台运行
> - `-e` = 设置环境变量
> - `-p 3306:3306` = 端口映射（宿主机:容器）
> - `-v` = 挂载文件（把本机的 init.sql 映射到容器内，首次启动自动执行）

### 7.2 启动应用

```bash
mvn spring-boot:run
# → Started StockWatchlistApplication in 2.072 seconds
```

### 7.3 用 curl 验证接口

```bash
# 搜索
curl "http://localhost:8080/api/stocks/search?keyword=600519"

# 添加
curl -X POST "http://localhost:8080/api/stocks" \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"600519","notes":"长期持有"}'

# 列表（含实时行情）
curl "http://localhost:8080/api/stocks"

# 修改备注
curl -X PUT "http://localhost:8080/api/stocks/1" \
  -H "Content-Type: application/json" \
  -d '{"notes":"核心持仓"}'

# 删除
curl -X DELETE "http://localhost:8080/api/stocks/1"

# 批量行情
curl "http://localhost:8080/api/quotes?codes=600519,000001"
```

---

## 八、Docker 容器化

### 8.1 Dockerfile

```dockerfile
FROM eclipse-temurin:8-jre    # 基础镜像（只含 JRE，体积小）
WORKDIR /app
COPY target/stock-watchlist-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> 📌 **知识点：Dockerfile**
> 定义如何构建一个容器镜像。类似"安装说明书"：基于什么系统 → 复制什么文件 → 暴露什么端口 → 运行什么命令。

### 8.2 docker-compose.yml

```yaml
services:
  app:
    build: .
    ports: ["8080:8080"]
    depends_on:
      mysql:
        condition: service_healthy   # 等 MySQL 健康后再启动 app
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: stock_watchlist
    volumes:
      - mysql_data:/var/lib/mysql           # 数据持久化
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      retries: 10
```

> 📌 **知识点：Docker Compose**
> 一个 YAML 文件定义多个容器的编排关系。`docker compose up -d` 一条命令启动所有服务。
>
> - `depends_on + condition: service_healthy` = 确保 MySQL 就绪后再启动应用
> - `volumes` = 数据持久化，容器删了数据还在
> - 容器之间通过服务名（如 `mysql`）互相访问，Docker 内部自带 DNS

---

## 九、部署到阿里云

### 9.1 服务器环境

- 阿里云 ECS，公网 IP：`39.103.59.111`
- 操作系统：Linux
- 已安装 Docker 29.2.1

### 9.2 部署步骤

```bash
# 1. 本地打包
mvn clean package -DskipTests
# → 生成 target/stock-watchlist-1.0.0.jar

# 2. 配置 SSH 免密登录
ssh-copy-id root@39.103.59.111

# 3. 在服务器创建目录
ssh root@39.103.59.111 "mkdir -p /opt/stock-watchlist/{target,sql}"

# 4. 上传文件
scp Dockerfile docker-compose.yml root@39.103.59.111:/opt/stock-watchlist/
scp sql/init.sql root@39.103.59.111:/opt/stock-watchlist/sql/
scp target/stock-watchlist-1.0.0.jar root@39.103.59.111:/opt/stock-watchlist/target/

# 5. 在服务器启动
ssh root@39.103.59.111 "cd /opt/stock-watchlist && docker compose up -d --build"

# 6. 验证
curl http://39.103.59.111:8080/api/stocks
```

### 9.3 遇到的问题与解决


| 问题                                  | 原因                                        | 解决方案                                                        |
| ----------------------------------- | ----------------------------------------- | ----------------------------------------------------------- |
| Docker 拉镜像 429 限流                   | Docker Hub 国内限制                           | 换镜像加速器 + 改用 `eclipse-temurin` 镜像                            |
| SSH Permission denied               | 服务器默认只允许公钥认证                              | 通过阿里云控制台开启密码登录后 `ssh-copy-id`                               |
| JDBC 报 UnsupportedEncodingException | `characterEncoding=utf8mb4` 不是 Java 合法编码名 | 改为 `characterEncoding=UTF-8`                                |
| Controller 测试报 MockBean 找不到         | import 路径错误                               | 正确路径是 `org.springframework.boot.test.mock.mockito.MockBean` |
| @WebMvcTest 报 sqlSessionFactory 缺失  | `@MapperScan` 在主类上，WebMvcTest 也会加载它       | 把 `@MapperScan` 移到独立配置类                                     |


### 9.4 阿里云安全组配置

> 📌 **知识点：安全组**
> 阿里云的"防火墙"。默认只开放 22（SSH）和 443（HTTPS）端口。新服务需要手动放行端口。

操作路径：ECS 实例 → 安全组 → 入方向 → 添加规则：

- 协议：TCP
- 端口：8080
- 授权对象：0.0.0.0/0（所有 IP）

---

## 十、项目最终成果

### 访问地址

```
http://39.103.59.111:8080
```

### 接口总览


| 方法     | 路径                              | 说明           |
| ------ | ------------------------------- | ------------ |
| GET    | /api/stocks                     | 自选股列表（含实时行情） |
| GET    | /api/stocks/search?keyword=xxx  | 搜索股票         |
| POST   | /api/stocks                     | 添加自选股        |
| PUT    | /api/stocks/{id}                | 修改备注         |
| DELETE | /api/stocks/{id}                | 删除自选股        |
| GET    | /api/quotes?codes=600519,000001 | 批量实时行情       |


### 测试覆盖

- 20 个测试用例全部通过
- 覆盖 Mapper / Service / Controller / API 解析 四个层级

---

## 附录：关键命令速查


| 操作                | 命令                                                                                 |
| ----------------- | ---------------------------------------------------------------------------------- |
| 编译                | `mvn compile`                                                                      |
| 运行测试              | `mvn test`                                                                         |
| 打包 JAR            | `mvn clean package -DskipTests`                                                    |
| 本地启动              | `mvn spring-boot:run`                                                              |
| 启动 MySQL 容器       | `podman run -d --name mysql -e MYSQL_ROOT_PASSWORD=root123 -p 3306:3306 mysql:8.0` |
| Docker Compose 启动 | `docker compose up -d --build`                                                     |
| 查看容器日志            | `docker compose logs app`                                                          |
| 停止所有容器            | `docker compose down`                                                              |
| SSH 到服务器          | `ssh root@39.103.59.111`                                                           |
| 上传文件到服务器          | `scp 本地文件 root@39.103.59.111:/远程路径/`                                               |


