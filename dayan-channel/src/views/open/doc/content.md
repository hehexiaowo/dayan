# 开放平台接口文档

> 本文档描述大雁养老综合平台对外开放的 API 接口规范。

## 认证方式

所有接口（除 token 获取）需在请求头携带以下认证信息：

| 请求头 | 说明 |
|---|---|
| X-App-Key | 平台分配的应用标识 |
| X-Timestamp | 请求时间戳（毫秒） |
| X-Nonce | 随机字符串（防重放） |
| X-Sign | 签名：HMAC-SHA256(appSecret, appKey + timestamp + nonce + body) |

## 接口清单

### 1. 获取 Token

`POST /open-api/v1/token`

传入 appKey + 签名，返回访问 token（有效期 2 小时）。

### 2. 内容查询

`GET /open-api/v1/contents` — 查询已配置的内容列表

### 3. 场景查询

`GET /open-api/v1/scenes` — 查询已配置的场景列表

### 4. 机构查询

`GET /open-api/v1/parks` — 查询养老机构信息

### 5. 权益查询

`GET /open-api/v1/equities/{equityCode}` — 查询权益详情

### 6. 权益激活回调

`POST /open-api/v1/equities/activate-callback` — 权益激活后回调通知

### 7. 订单查询

`GET /open-api/v1/orders/{orderCode}` — 查询订单状态

### 8. Webhook 注册

`POST /open-api/v1/webhooks/register` — 注册 webhook 回调地址

### 9. Webhook 日志

`GET /open-api/v1/webhooks/logs` — 查询 webhook 推送日志

## 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { },
  "timestamp": 1786149639969,
  "traceId": "917a1dd4b06d48f4b3d97f75ae6b8a48"
}
```

## 错误码

| code | 含义 |
|---|---|
| 0 | 成功 |
| 10000 | 参数校验异常 |
| 10300 | 资源不存在 |
| 10401 | 未认证 |
| 10403 | 无权限 |
| 10500 | 系统内部异常 |

---

> ⚠️ 接口当前建设中，预计 v2 上线。正式接入请联系运营。
