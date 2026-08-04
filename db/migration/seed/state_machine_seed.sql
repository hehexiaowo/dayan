-- =====================================================================
-- state_machine_seed.sql  状态机配置种子数据
-- 4 个状态机：EQUITY_SM(8态) / ORDER_SM(8态) / SERVICE_SESSION_SM(7态) / PARK_SM(4态)
-- 生成依据：规格 §7.3 + docs/02 §3.1.4 system_state_machine 表结构
-- 字段映射：from_state/to_state ↔ StateRule.fromStatus/toStatus，event_code ↔ event
-- =====================================================================

-- ============================================
-- EQUITY_SM 权益状态机（8 态）
-- 0=库存中 1=已出库 2=已激活 3=使用中 4=已完成 5=已过期 6=已作废 7=更换权益人中
-- ============================================
INSERT INTO `system_state_machine`
  (`machine_code`, `machine_name`, `biz_type`, `from_state`, `from_state_name`, `to_state`, `to_state_name`, `event_code`, `event_name`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('EQUITY_SM', '权益状态机', 'equity', 0, '库存中', 1, '已出库', 'outbound', '出库', 10, 1, '批次出库至渠道/代理人', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 1, '已出库', 2, '已激活', 'activate', '激活', 20, 1, '客户激活权益', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 2, '已激活', 3, '使用中', 'start_service', '发起服务', 30, 1, '权益关联服务会话', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 3, '使用中', 2, '已激活', 'end_service', '服务结束', 31, 1, '服务完成权益恢复', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 3, '使用中', 4, '已完成', 'complete', '权益完成', 40, 1, '权益全部使用完成', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 0, '库存中', 5, '已过期', 'shelf_expire', '上架过期', 50, 1, '上架有效期过期', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 1, '已出库', 5, '已过期', 'expire', '过期', 51, 1, '有效期过期', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 2, '已激活', 5, '已过期', 'expire', '过期', 52, 1, '激活后有效期过期', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 0, '库存中', 6, '已作废', 'void', '作废', 60, 1, '库存作废', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 1, '已出库', 6, '已作废', 'void', '作废', 61, 1, '已出库作废', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 2, '已激活', 7, '更换权益人中', 'change_holder', '更换权益人', 70, 1, '发起更换权益人', NOW(), NOW(), 'system', 'system', 0),
  ('EQUITY_SM', '权益状态机', 'equity', 7, '更换权益人中', 2, '已激活', 'change_done', '更换完成', 71, 1, '更换权益人完成', NOW(), NOW(), 'system', 'system', 0);

-- ============================================
-- ORDER_SM 订单状态机（8 态）
-- 0=待支付 1=已支付 2=处理中 3=已完成 4=已取消 5=退款中 6=已退款 7=异常
-- ============================================
INSERT INTO `system_state_machine`
  (`machine_code`, `machine_name`, `biz_type`, `from_state`, `from_state_name`, `to_state`, `to_state_name`, `event_code`, `event_name`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('ORDER_SM', '订单状态机', 'order', 0, '待支付', 1, '已支付', 'pay', '支付成功', 10, 1, '支付回调确认', NOW(), NOW(), 'system', 'system', 0),
  ('ORDER_SM', '订单状态机', 'order', 0, '待支付', 4, '已取消', 'cancel', '取消', 11, 1, '用户取消或超时自动取消', NOW(), NOW(), 'system', 'system', 0),
  ('ORDER_SM', '订单状态机', 'order', 1, '已支付', 2, '处理中', 'process', '开始处理', 20, 1, '进入业务处理', NOW(), NOW(), 'system', 'system', 0),
  ('ORDER_SM', '订单状态机', 'order', 2, '处理中', 3, '已完成', 'complete', '完成', 30, 1, '业务履约完成', NOW(), NOW(), 'system', 'system', 0),
  ('ORDER_SM', '订单状态机', 'order', 1, '已支付', 5, '退款中', 'refund_apply', '申请退款', 40, 1, '用户申请退款', NOW(), NOW(), 'system', 'system', 0),
  ('ORDER_SM', '订单状态机', 'order', 2, '处理中', 5, '退款中', 'refund_apply', '申请退款', 41, 1, '处理中申请退款', NOW(), NOW(), 'system', 'system', 0),
  ('ORDER_SM', '订单状态机', 'order', 5, '退款中', 6, '已退款', 'refund_done', '退款完成', 50, 1, '退款到账', NOW(), NOW(), 'system', 'system', 0),
  ('ORDER_SM', '订单状态机', 'order', 5, '退款中', 1, '已支付', 'refund_reject', '退款驳回', 51, 1, '退款申请被驳回', NOW(), NOW(), 'system', 'system', 0),
  ('ORDER_SM', '订单状态机', 'order', 1, '已支付', 7, '异常', 'error', '异常', 90, 1, '支付后业务异常', NOW(), NOW(), 'system', 'system', 0),
  ('ORDER_SM', '订单状态机', 'order', 2, '处理中', 7, '异常', 'error', '异常', 91, 1, '处理过程异常', NOW(), NOW(), 'system', 'system', 0);

-- ============================================
-- SERVICE_SESSION_SM 服务会话状态机（7 态）
-- 1=待分配 2=待收集 3=方案中 4=安排中 5=服务中 6=已完成 7=已取消
-- 子状态（sub_status）在业务层单独管理：normal/hold/urgent/reassign/refund_review/refund_done/interrupted
-- ============================================
INSERT INTO `system_state_machine`
  (`machine_code`, `machine_name`, `biz_type`, `from_state`, `from_state_name`, `to_state`, `to_state_name`, `event_code`, `event_name`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('SERVICE_SESSION_SM', '服务会话状态机', 'service', 1, '待分配', 2, '待收集', 'assign_butler', '分配管家', 10, 1, '权益激活后创建会话，分配管家', NOW(), NOW(), 'system', 'system', 0),
  ('SERVICE_SESSION_SM', '服务会话状态机', 'service', 2, '待收集', 3, '方案中', 'submit_demand', '提交需求', 20, 1, '需求收集完成进入方案定制', NOW(), NOW(), 'system', 'system', 0),
  ('SERVICE_SESSION_SM', '服务会话状态机', 'service', 3, '方案中', 4, '安排中', 'confirm_solution', '确认方案', 30, 1, '方案确认进入全程安排', NOW(), NOW(), 'system', 'system', 0),
  ('SERVICE_SESSION_SM', '服务会话状态机', 'service', 3, '方案中', 2, '待收集', 'reject_solution', '驳回方案', 31, 1, '方案被驳回回到需求收集', NOW(), NOW(), 'system', 'system', 0),
  ('SERVICE_SESSION_SM', '服务会话状态机', 'service', 4, '安排中', 5, '服务中', 'start_service', '开始服务', 40, 1, '安排确认后开始服务', NOW(), NOW(), 'system', 'system', 0),
  ('SERVICE_SESSION_SM', '服务会话状态机', 'service', 5, '服务中', 6, '已完成', 'finish', '完成服务', 50, 1, '服务履约完成', NOW(), NOW(), 'system', 'system', 0),
  ('SERVICE_SESSION_SM', '服务会话状态机', 'service', 1, '待分配', 7, '已取消', 'cancel', '取消', 91, 1, '会话取消', NOW(), NOW(), 'system', 'system', 0),
  ('SERVICE_SESSION_SM', '服务会话状态机', 'service', 2, '待收集', 7, '已取消', 'cancel', '取消', 92, 1, '会话取消', NOW(), NOW(), 'system', 'system', 0),
  ('SERVICE_SESSION_SM', '服务会话状态机', 'service', 5, '服务中', 7, '已取消', 'cancel', '取消', 93, 1, '服务中取消（需走退款审核）', NOW(), NOW(), 'system', 'system', 0);

-- ============================================
-- PARK_SM 养老机构状态机（4 态）
-- 0=待审核 1=已上线 2=已下架 3=暂停营业
-- ============================================
INSERT INTO `system_state_machine`
  (`machine_code`, `machine_name`, `biz_type`, `from_state`, `from_state_name`, `to_state`, `to_state_name`, `event_code`, `event_name`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('PARK_SM', '机构状态机', 'park', 0, '待审核', 1, '已上线', 'approve', '审核通过上线', 10, 1, '审核通过并上线', NOW(), NOW(), 'system', 'system', 0),
  ('PARK_SM', '机构状态机', 'park', 1, '已上线', 2, '已下架', 'offline', '下架', 20, 1, '机构主动下架或被下架', NOW(), NOW(), 'system', 'system', 0),
  ('PARK_SM', '机构状态机', 'park', 2, '已下架', 1, '已上线', 'online', '重新上线', 21, 1, '下架后重新上线', NOW(), NOW(), 'system', 'system', 0),
  ('PARK_SM', '机构状态机', 'park', 1, '已上线', 3, '暂停营业', 'suspend', '暂停营业', 30, 1, '临时暂停营业', NOW(), NOW(), 'system', 'system', 0),
  ('PARK_SM', '机构状态机', 'park', 3, '暂停营业', 1, '已上线', 'resume', '恢复营业', 31, 1, '暂停后恢复营业', NOW(), NOW(), 'system', 'system', 0);
