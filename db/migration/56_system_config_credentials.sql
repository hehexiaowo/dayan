SET NAMES utf8mb4;
-- =====================================================================
-- 56_system_config_credentials.sql  系统核心凭据收口入 system_config
--
-- 背景：外部平台核心账号 key 此前散落多处——
--   ① OSS/MinIO：admin/channel 走 yml 环境变量、agent/client 靠 Java 默认值，
--      四个 starter 来源分裂，且改凭据必须改配置重启；
--   ② 天地图 Key：agent 前端硬编码两处（utils/map.ts 在用、config/map.ts 死代码）；
--   ③ 短信平台：仅 Mock 实现，真实凭据无槽位；
--   ④ 支付平台：无网关集成，无槽位。
-- 本文件将四组凭据登记入 system_config（唯一键 config_group+config_key）：
--   map     天地图（前端暴露型，公开）——agent H5 改运行时拉取
--           /agent-api/v1/config/map-key（DB → dayan.map.tianditu-key → 内置兜底）；
--   oss     对象存储——后端 DbStorageCredentialProvider 逐键优先本表、
--           缺失回退 MINIO_* 环境变量，60s 快照缓存热生效；
--   sms     短信平台（预留槽位，接入阿里云后由真实实现消费）；
--   payment 支付渠道（预留槽位，接入微信/支付宝后消费）。
-- 敏感值约定：is_secret=1 的项接口响应统一脱敏 ******，更新留空即保持原值；
--             种子值为当前开发环境实际值，生产部署后务必在系统配置页改掉。
-- 幂等：ODKU 空操作（重放不覆盖管理员已改值）。
-- =====================================================================

INSERT INTO `system_config`
  (`config_group`, `config_key`, `config_value`, `value_type`, `env`, `scope`,
   `config_name`, `description`, `is_secret`, `is_runtime`, `sort_order`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- ---------- map 天地图 ----------
  ('map', 'map.tianditu-key', '1ea38bada071978da6b6cfd68c464450', 'string', 'prod', 'global',
   '天地图前端 Key', '天地图开放平台浏览器端 Key（前端暴露型，随瓦片 URL 明文出现，官方设计如此，非敏感）。agent H5 经 /agent-api/v1/config/map-key 运行时拉取，改此值即全端生效', 0, 1, 10,
   NOW(), NOW(), 'system', 'system', 0),

  -- ---------- oss 对象存储（MinIO 兼容 S3）----------
  ('oss', 'oss.endpoint', 'http://localhost:9000', 'string', 'prod', 'global',
   'OSS 服务地址', 'MinIO/S3 endpoint。系统配置页修改后各服务最迟 60s 热生效；本表缺失该键时回退环境变量 MINIO_ENDPOINT', 0, 1, 10,
   NOW(), NOW(), 'system', 'system', 0),
  ('oss', 'oss.access-key', 'dayan', 'string', 'prod', 'global',
   'OSS AccessKey', '对象存储访问密钥 ID（敏感，列表脱敏展示，编辑留空保持不变）。本表缺失时回退环境变量 MINIO_ACCESS_KEY', 1, 1, 20,
   NOW(), NOW(), 'system', 'system', 0),
  ('oss', 'oss.secret-key', 'dayan12345', 'string', 'prod', 'global',
   'OSS SecretKey', '对象存储访问私钥（敏感，编辑留空保持不变）。本表缺失时回退环境变量 MINIO_SECRET_KEY', 1, 1, 30,
   NOW(), NOW(), 'system', 'system', 0),
  ('oss', 'oss.bucket', 'dayan-public', 'string', 'prod', 'global',
   'OSS 桶名', '公开读桶名。本表缺失时回退环境变量 MINIO_BUCKET', 0, 1, 40,
   NOW(), NOW(), 'system', 'system', 0),
  ('oss', 'oss.public-base-url', '', 'string', 'prod', 'global',
   'OSS 公网基地址', 'C 端可访问的公网地址（如 https://files.example.com），留空回退 endpoint 直链（通常仅内网可用）。对应环境变量 MINIO_PUBLIC_BASE_URL', 0, 1, 50,
   NOW(), NOW(), 'system', 'system', 0),

  -- ---------- sms 短信平台（预留槽位）----------
  ('sms', 'sms.provider', 'mock', 'string', 'prod', 'global',
   '短信实现开关', '当前短信实现：mock=开发态（日志打印验证码）。接入阿里云后改为 aliyun，由真实实现类读取下列键', 0, 1, 10,
   NOW(), NOW(), 'system', 'system', 0),
  ('sms', 'sms.aliyun-access-key', '', 'string', 'prod', 'global',
   '阿里云短信 AccessKey', '预留：阿里云短信访问密钥 ID（接入时由 AliyunSmsServiceImpl 消费）', 1, 1, 20,
   NOW(), NOW(), 'system', 'system', 0),
  ('sms', 'sms.aliyun-secret-key', '', 'string', 'prod', 'global',
   '阿里云短信 SecretKey', '预留：阿里云短信访问私钥', 1, 1, 30,
   NOW(), NOW(), 'system', 'system', 0),
  ('sms', 'sms.aliyun-sign-name', '', 'string', 'prod', 'global',
   '阿里云短信签名', '预留：短信签名（如「大雁养老」）', 0, 1, 40,
   NOW(), NOW(), 'system', 'system', 0),
  ('sms', 'sms.aliyun-template-code', '', 'string', 'prod', 'global',
   '阿里云短信模板号', '预留：验证码模板 CODE（SMS_xxxxx）', 0, 1, 50,
   NOW(), NOW(), 'system', 'system', 0),

  -- ---------- payment 支付渠道（预留槽位）----------
  ('payment', 'payment.wxpay-app-id', '', 'string', 'prod', 'global',
   '微信支付 AppID', '预留：小程序/公众号 AppID（接入微信支付时消费）', 0, 1, 10,
   NOW(), NOW(), 'system', 'system', 0),
  ('payment', 'payment.wxpay-mch-id', '', 'string', 'prod', 'global',
   '微信支付商户号', '预留：微信支付商户号 mchid', 0, 1, 20,
   NOW(), NOW(), 'system', 'system', 0),
  ('payment', 'payment.wxpay-cert-serial', '', 'string', 'prod', 'global',
   '微信支付证书序列号', '预留：商户 API 证书序列号', 0, 1, 30,
   NOW(), NOW(), 'system', 'system', 0),
  ('payment', 'payment.wxpay-api-v3-key', '', 'string', 'prod', 'global',
   '微信支付 APIv3 密钥', '预留：APIv3 密钥（敏感）', 1, 1, 40,
   NOW(), NOW(), 'system', 'system', 0),
  ('payment', 'payment.wxpay-private-key', '', 'string', 'prod', 'global',
   '微信支付商户私钥', '预留：apiclient_key.pem 内容（敏感）', 1, 1, 50,
   NOW(), NOW(), 'system', 'system', 0),
  ('payment', 'payment.alipay-app-id', '', 'string', 'prod', 'global',
   '支付宝应用 AppID', '预留：支付宝开放平台应用 ID', 0, 1, 60,
   NOW(), NOW(), 'system', 'system', 0),
  ('payment', 'payment.alipay-gateway-url', '', 'string', 'prod', 'global',
   '支付宝网关', '预留：正式 https://openapi.alipay.com/gateway.do，沙箱用沙箱网关', 0, 1, 70,
   NOW(), NOW(), 'system', 'system', 0),
  ('payment', 'payment.alipay-private-key', '', 'string', 'prod', 'global',
   '支付宝应用私钥', '预留：应用私钥（敏感）', 1, 1, 80,
   NOW(), NOW(), 'system', 'system', 0),
  ('payment', 'payment.alipay-public-key', '', 'string', 'prod', 'global',
   '支付宝公钥', '预留：支付宝公钥（验签用，敏感）', 1, 1, 90,
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
