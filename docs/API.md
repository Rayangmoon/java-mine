# 自选股 Watchlist API 文档

## 基础信息

| 项目 | 值 |
|------|-----|
| Base URL | `http://39.103.59.111:8080` |
| 协议 | HTTP |
| 数据格式 | JSON |
| 编码 | UTF-8 |

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": ...
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 404 | 资源不存在 |
| 409 | 重复添加 |
| 500 | 服务器内部错误 |
| 502 | 外部行情 API 不可用 |

---

## 接口列表

### 1. 查询自选股列表（含实时行情）

```
GET /api/stocks
```

**请求参数：** 无

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "stockCode": "600519",
      "stockName": "贵州茅台",
      "currentPrice": 1375.00,
      "todayOpen": 1365.10,
      "yesterdayClose": 1384.79,
      "high": 1379.00,
      "low": 1360.05,
      "volume": 4780604,
      "notes": "长期持有"
    }
  ]
}
```

**说明：** 行情字段（currentPrice、todayOpen 等）来自实时接口，若行情服务不可用则为 `null`。

---

### 2. 搜索股票

```
GET /api/stocks/search?keyword={keyword}
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 是 | 股票名称（模糊匹配）或 6 位代码（精确匹配） |

**请求示例：**

```
GET /api/stocks/search?keyword=茅台
GET /api/stocks/search?keyword=600519
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "stockCode": "600519",
      "stockName": "贵州茅台"
    }
  ]
}
```

---

### 3. 添加自选股

```
POST /api/stocks
Content-Type: application/json
```

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| stockCode | string | 是 | 6 位股票代码，首位为 0/3/4/6/8 |
| notes | string | 否 | 备注信息，最长 256 字符 |

**请求示例：**

```json
{
  "stockCode": "600519",
  "notes": "长期持有"
}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "stockCode": "600519",
    "stockName": "贵州茅台",
    "notes": "长期持有",
    "createdAt": "2026-05-06T09:01:21",
    "updatedAt": "2026-05-06T09:01:21"
  }
}
```

**错误情况：**

| code | 场景 |
|------|------|
| 400 | 股票代码格式不正确 / 无效的股票代码 |
| 409 | 该股票已在自选列表中 |

---

### 4. 修改备注

```
PUT /api/stocks/{id}
Content-Type: application/json
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | number | 自选股 ID |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| notes | string | 否 | 新的备注内容，最长 256 字符 |

**请求示例：**

```json
{
  "notes": "核心持仓"
}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "stockCode": "600519",
    "stockName": "贵州茅台",
    "notes": "核心持仓",
    "createdAt": "2026-05-06T09:01:21",
    "updatedAt": "2026-05-06T09:05:33"
  }
}
```

**错误情况：**

| code | 场景 |
|------|------|
| 404 | 自选股不存在 |

---

### 5. 删除自选股

```
DELETE /api/stocks/{id}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | number | 自选股 ID |

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误情况：**

| code | 场景 |
|------|------|
| 404 | 自选股不存在 |

---

### 6. 批量获取实时行情

```
GET /api/quotes?codes={codes}
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| codes | string | 是 | 逗号分隔的股票代码，如 `600519,000001` |

**请求示例：**

```
GET /api/quotes?codes=600519,000001
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": null,
      "stockCode": "600519",
      "stockName": "贵州茅台",
      "currentPrice": 1375.00,
      "todayOpen": 1365.10,
      "yesterdayClose": 1384.79,
      "high": 1379.00,
      "low": 1360.05,
      "volume": 4780604,
      "notes": null
    },
    {
      "id": null,
      "stockCode": "000001",
      "stockName": "平安银行",
      "currentPrice": 11.36,
      "todayOpen": 11.50,
      "yesterdayClose": 11.49,
      "high": 11.50,
      "low": 11.30,
      "volume": 121638755,
      "notes": null
    }
  ]
}
```

**说明：** 此接口不走数据库，`id` 和 `notes` 始终为 `null`。

---

## 典型使用流程

```
搜索股票 → 用户选择 → 添加到自选 → 查看列表（含行情）
```

1. 调用 `GET /api/stocks/search?keyword=银行` 获取候选列表
2. 用户选中一只，取其 `stockCode`
3. 调用 `POST /api/stocks` 添加
4. 调用 `GET /api/stocks` 查看自选列表（附带实时行情）

## 股票代码规则

- 固定 6 位数字
- 首位决定交易所：`6` → 上海，`0/3` → 深圳，`4/8` → 北交所
- 示例：`600519`（贵州茅台）、`000001`（平安银行）、`300750`（宁德时代）
