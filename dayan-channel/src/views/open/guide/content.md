# 开放平台接入指南

## 接入流程

### 步骤一：申请对接

联系大雁养老运营团队，提交接入申请（说明对接场景、预期调用量、回调地址）。

### 步骤二：获取凭证

运营审核通过后，在「应用管理」页面可查看本渠道的对接配置：

- **AppKey**：应用唯一标识
- **AppSecret**：签名密钥（仅创建时可见，妥善保管）

### 步骤三：配置白名单与回调

在申请表中提供以下信息，由运营配置：

- **IP 白名单**：调用方服务器出口 IP（多个用逗号分隔）
- **回调地址**：webhook 通知接收地址（HTTPS）

### 步骤四：获取 Token

使用 AppKey + 签名换取访问 Token：

```
POST /open-api/v1/token
Headers:
  X-App-Key: {your_app_key}
  X-Timestamp: {current_millis}
  X-Nonce: {random_string}
  X-Sign: HMAC-SHA256({app_secret}, app_key + timestamp + nonce + body)
```

返回 Token，后续接口携带 `Authorization: Bearer {token}`。

### 步骤五：发起调用

以查询内容为例：

```
GET /open-api/v1/contents?current=1&size=10
Headers:
  Authorization: Bearer {token}
```

## 签名算法示例（伪代码）

```
signature = HMAC_SHA256(app_secret, app_key + timestamp + nonce + request_body)
```

> 注意：request_body 为空时传空字符串。timestamp 与服务器时间偏差超过 5 分钟将拒绝请求。

---

> ⚠️ 本指南为预览版，正式接入请联系运营获取最新文档与测试环境凭证。
