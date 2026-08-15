SET NAMES utf8mb4;
-- =====================================================================
-- 57_message_admin.sql  消息管理板块补齐（system_message_template / system_message）
-- 01 迁移建表后仅有实体，管理链路（后端接口/菜单/权限/前端页面）缺失。
-- 1. 系统管理下新增两个菜单：消息模板 + 发送记录
-- 2. 按钮级权限：system:msg-tpl:* / system:msg-record:*
-- 3. 常用消息模板种子 6 条（短信验证码/站内信通知/APP推送，开箱即有参照）
-- 幂等：菜单 ODKU 空操作守卫；权限/模板 ODKU updated_at；模板种子按
--   template_code 唯一键防重，后台人工修改不会被覆盖。
-- =====================================================================

-- ============================================================
-- 一、菜单（挂 admin_system 下）
-- ============================================================
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`, `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_system_message_template', '消息模板', 'admin_system', 2, '/system/message-template', 'system/message/template/index', 'system:msg-tpl:list', 'Message', 6, 1, 'admin', 1, '消息模板管理（短信/站内信/推送/邮件）', NOW(), NOW(), 'system', 'system', 0),
  ('admin_system_message_record', '发送记录', 'admin_system', 2, '/system/message-record', 'system/message/record/index', 'system:msg-record:list', 'ChatDotRound', 7, 1, 'admin', 1, '消息发送记录审计（只读）', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `menu_code` = `menu_code`;

-- ============================================================
-- 二、按钮级权限
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('system:msg-tpl:list',   '消息模板列表', 'system:msg-tpl', 3, '/admin-api/message-templates',     'GET',    220, 1, '消息模板', NOW(), NOW(), 'system', 'system', 0),
  ('system:msg-tpl:create', '新增消息模板', 'system:msg-tpl', 3, '/admin-api/message-templates',     'POST',   221, 1, '消息模板', NOW(), NOW(), 'system', 'system', 0),
  ('system:msg-tpl:update', '修改消息模板', 'system:msg-tpl', 3, '/admin-api/message-templates/*',   'PUT',    222, 1, '消息模板', NOW(), NOW(), 'system', 'system', 0),
  ('system:msg-tpl:delete', '删除消息模板', 'system:msg-tpl', 3, '/admin-api/message-templates/*',   'DELETE', 223, 1, '消息模板', NOW(), NOW(), 'system', 'system', 0),
  ('system:msg-record:list',  '发送记录列表', 'system:msg-record', 3, '/admin-api/messages',     'GET',  224, 1, '消息发送记录', NOW(), NOW(), 'system', 'system', 0),
  ('system:msg-record:query', '发送记录详情', 'system:msg-record', 3, '/admin-api/messages/*',   'GET',  225, 1, '消息发送记录', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ============================================================
-- 三、常用消息模板种子（业务发送链路接入前先备好参照模板）
-- 渠道：1=短信 2=站内信 3=APP推送 6=邮件；标题仅站内信/推送/邮件必填
-- ============================================================
INSERT INTO `system_message_template`
  (`template_code`, `template_name`, `biz_type`, `channel_type`, `title`, `content`, `variables`, `fallback_channel_type`, `channel_code`, `status`, `sort_order`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('sms_register_code', '注册验证码', 'register', 1, NULL,
   '您正在注册大雁养老账号，验证码${code}，${expireMinutes}分钟内有效。为保障账号安全，请勿向他人泄露。',
   '[{"name":"code","label":"验证码"},{"name":"expireMinutes","label":"有效分钟数"}]',
   NULL, NULL, 1, 1, '注册流程短信验证码', NOW(), NOW(), 'system', 'system', 0),
  ('sms_login_code', '登录验证码', 'login', 1, NULL,
   '您正在登录大雁养老，验证码${code}，${expireMinutes}分钟内有效。若非本人操作，请及时修改密码。',
   '[{"name":"code","label":"验证码"},{"name":"expireMinutes","label":"有效分钟数"}]',
   NULL, NULL, 1, 2, '登录流程短信验证码', NOW(), NOW(), 'system', 'system', 0),
  ('inapp_register_welcome', '注册欢迎通知', 'register', 2, '欢迎加入大雁养老',
   '亲爱的${nickname}，欢迎加入大雁养老！您可以浏览旅居基地、预约参观体验，祝您生活愉快。',
   '[{"name":"nickname","label":"用户昵称"}]',
   NULL, NULL, 1, 3, '注册成功站内信欢迎语', NOW(), NOW(), 'system', 'system', 0),
  ('inapp_order_paid', '订单支付成功通知', 'order', 2, '订单支付成功',
   '您购买的「${goodsName}」已支付成功，订单号${orderCode}，可在订单中心查看详情。',
   '[{"name":"goodsName","label":"商品名称"},{"name":"orderCode","label":"订单编号"}]',
   NULL, NULL, 1, 4, '支付成功站内信', NOW(), NOW(), 'system', 'system', 0),
  ('inapp_refund_result', '退款结果通知', 'refund', 2, '退款处理结果',
   '您的订单${orderCode}退款申请已${result}，退款金额${amount}元将于1-3个工作日原路退回。',
   '[{"name":"orderCode","label":"订单编号"},{"name":"result","label":"处理结果"},{"name":"amount","label":"退款金额"}]',
   NULL, NULL, 1, 5, '退款结果站内信', NOW(), NOW(), 'system', 'system', 0),
  ('push_activity_start', '活动开始提醒', 'activity', 3, '活动提醒',
   '您关注的「${activityName}」将于${startTime}开始，记得准时参与哦～',
   '[{"name":"activityName","label":"活动名称"},{"name":"startTime","label":"开始时间"}]',
   2, NULL, 1, 6, '活动开始前推送（降级站内信）', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
