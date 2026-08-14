-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- dict_seed.sql  基础字典种子数据
-- 通用枚举：gender / yes_no / account_status / common_status / client_level / agent_level 等
-- 生成依据：docs/02 §3.1.1 system_dict_common
-- 业务字典（按 17 域区分）在各域开发时增量补充到 system_dict_business
-- =====================================================================

-- 性别
INSERT INTO `system_dict_common`
  (`dict_type`, `dict_code`, `dict_name`, `dict_value`, `level`, `sort_order`, `status`, `is_default`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('gender', 'unknown', '未知', '0', 1, 0, 1, 1, '性别-未知', NOW(), NOW(), 'system', 'system', 0),
  ('gender', 'male', '男', '1', 1, 1, 1, 0, '性别-男', NOW(), NOW(), 'system', 'system', 0),
  ('gender', 'female', '女', '2', 1, 2, 1, 0, '性别-女', NOW(), NOW(), 'system', 'system', 0),

  -- 通用启用/禁用状态
  ('common_status', 'disabled', '禁用', '0', 1, 0, 1, 0, '通用状态-禁用', NOW(), NOW(), 'system', 'system', 0),
  ('common_status', 'enabled', '启用', '1', 1, 1, 1, 1, '通用状态-启用', NOW(), NOW(), 'system', 'system', 0),

  -- 账号状态
  ('account_status', 'locked', '锁定', '0', 1, 0, 1, 0, '账号状态-锁定', NOW(), NOW(), 'system', 'system', 0),
  ('account_status', 'normal', '正常', '1', 1, 1, 1, 1, '账号状态-正常', NOW(), NOW(), 'system', 'system', 0),
  ('account_status', 'disabled', '禁用', '2', 1, 2, 1, 0, '账号状态-禁用', NOW(), NOW(), 'system', 'system', 0),

  -- 业务记录状态（通用 3 态：禁用/正常/冻结）
  ('biz_status', 'disabled', '禁用', '0', 1, 0, 1, 0, '业务状态-禁用', NOW(), NOW(), 'system', 'system', 0),
  ('biz_status', 'normal', '正常', '1', 1, 1, 1, 1, '业务状态-正常', NOW(), NOW(), 'system', 'system', 0),
  ('biz_status', 'frozen', '冻结', '2', 1, 2, 1, 0, '业务状态-冻结', NOW(), NOW(), 'system', 'system', 0),

  -- 是否（布尔）
  ('yes_no', 'no', '否', '0', 1, 0, 1, 1, '是否-否', NOW(), NOW(), 'system', 'system', 0),
  ('yes_no', 'yes', '是', '1', 1, 1, 1, 0, '是否-是', NOW(), NOW(), 'system', 'system', 0),

  -- 客户等级
  ('client_level', 'normal', '普通', '1', 1, 0, 1, 1, '客户等级-普通', NOW(), NOW(), 'system', 'system', 0),
  ('client_level', 'vip', 'VIP', '2', 1, 1, 1, 0, '客户等级-VIP', NOW(), NOW(), 'system', 'system', 0),
  ('client_level', 'svip', 'SVIP', '3', 1, 2, 1, 0, '客户等级-SVIP', NOW(), NOW(), 'system', 'system', 0),

  -- 代理人等级
  ('agent_level', 'normal', '普通', '1', 1, 0, 1, 1, '代理人等级-普通', NOW(), NOW(), 'system', 'system', 0),
  ('agent_level', 'silver', '银牌', '2', 1, 1, 1, 0, '代理人等级-银牌', NOW(), NOW(), 'system', 'system', 0),
  ('agent_level', 'gold', '金牌', '3', 1, 2, 1, 0, '代理人等级-金牌', NOW(), NOW(), 'system', 'system', 0),
  ('agent_level', 'diamond', '钻石', '4', 1, 3, 1, 0, '代理人等级-钻石', NOW(), NOW(), 'system', 'system', 0),

  -- 渠道类型
  ('channel_type', 'insurance', '保险公司', '1', 1, 0, 1, 1, '渠道类型-保险公司', NOW(), NOW(), 'system', 'system', 0),
  ('channel_type', 'bank', '银行', '2', 1, 1, 1, 0, '渠道类型-银行', NOW(), NOW(), 'system', 'system', 0),
  ('channel_type', 'agent_org', '代理机构', '3', 1, 2, 1, 0, '渠道类型-代理机构', NOW(), NOW(), 'system', 'system', 0),
  ('channel_type', 'other', '其他', '4', 1, 3, 1, 0, '渠道类型-其他', NOW(), NOW(), 'system', 'system', 0),

  -- 权益状态（8 态，对齐 EQUITY_SM）
  ('equity_status', 'stock', '库存中', '0', 1, 0, 1, 1, '权益状态-库存中', NOW(), NOW(), 'system', 'system', 0),
  ('equity_status', 'outbound', '已出库', '1', 1, 1, 1, 0, '权益状态-已出库', NOW(), NOW(), 'system', 'system', 0),
  ('equity_status', 'activated', '已激活', '2', 1, 2, 1, 0, '权益状态-已激活', NOW(), NOW(), 'system', 'system', 0),
  ('equity_status', 'in_use', '使用中', '3', 1, 3, 1, 0, '权益状态-使用中', NOW(), NOW(), 'system', 'system', 0),
  ('equity_status', 'completed', '已完成', '4', 1, 4, 1, 0, '权益状态-已完成', NOW(), NOW(), 'system', 'system', 0),
  ('equity_status', 'expired', '已过期', '5', 1, 5, 1, 0, '权益状态-已过期', NOW(), NOW(), 'system', 'system', 0),
  ('equity_status', 'void', '已作废', '6', 1, 6, 1, 0, '权益状态-已作废', NOW(), NOW(), 'system', 'system', 0),
  ('equity_status', 'changing_holder', '更换权益人中', '7', 1, 7, 1, 0, '权益状态-更换权益人中', NOW(), NOW(), 'system', 'system', 0),

  -- 订单状态（8 态，对齐 ORDER_SM）
  ('order_status', 'pending_pay', '待支付', '0', 1, 0, 1, 1, '订单状态-待支付', NOW(), NOW(), 'system', 'system', 0),
  ('order_status', 'paid', '已支付', '1', 1, 1, 1, 0, '订单状态-已支付', NOW(), NOW(), 'system', 'system', 0),
  ('order_status', 'processing', '处理中', '2', 1, 2, 1, 0, '订单状态-处理中', NOW(), NOW(), 'system', 'system', 0),
  ('order_status', 'completed', '已完成', '3', 1, 3, 1, 0, '订单状态-已完成', NOW(), NOW(), 'system', 'system', 0),
  ('order_status', 'cancelled', '已取消', '4', 1, 4, 1, 0, '订单状态-已取消', NOW(), NOW(), 'system', 'system', 0),
  ('order_status', 'refunding', '退款中', '5', 1, 5, 1, 0, '订单状态-退款中', NOW(), NOW(), 'system', 'system', 0),
  ('order_status', 'refunded', '已退款', '6', 1, 6, 1, 0, '订单状态-已退款', NOW(), NOW(), 'system', 'system', 0),
  ('order_status', 'error', '异常', '7', 1, 7, 1, 0, '订单状态-异常', NOW(), NOW(), 'system', 'system', 0),

  -- 机构状态（4 态，对齐 PARK_SM）
  ('park_status', 'pending_audit', '待审核', '0', 1, 0, 1, 1, '机构状态-待审核', NOW(), NOW(), 'system', 'system', 0),
  ('park_status', 'online', '已上线', '1', 1, 1, 1, 0, '机构状态-已上线', NOW(), NOW(), 'system', 'system', 0),
  ('park_status', 'offline', '已下架', '2', 1, 2, 1, 0, '机构状态-已下架', NOW(), NOW(), 'system', 'system', 0),
  ('park_status', 'suspended', '暂停营业', '3', 1, 3, 1, 0, '机构状态-暂停营业', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
