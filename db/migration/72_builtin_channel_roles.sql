-- ============================================================
-- 72 渠道内置标准角色全局化
--
-- 背景：内置角色（人管/业管/组训/财务）此前按渠道各复制一份（role_type=1），
-- 改为全渠道共用一套：channel_role 中 channel_code='GLOBAL' 的行即内置角色，
-- 不可编辑/删除；渠道自定义角色（role_type=2）仍按 channel_code 关联。
--
-- 迁移内容：
--   1. 删除所有旧的内置角色（role_type=1，按渠道复制版）
--   2. 插入全局内置角色 4 条（channel_code='GLOBAL'）
--
-- 注意：如已通过"创建渠道自动播种"产生过内置角色，先执行第 1 步清理再插入。
-- ============================================================

-- 1. 清理旧的内置角色（按渠道复制版）
DELETE FROM channel_role WHERE role_type = 1;

-- 2. 全渠道共用内置角色（channel_code='GLOBAL'，role_type=1 系统预置）
INSERT INTO channel_role (channel_code, role_code, role_name, role_type, description, status, sort_order, creator, updater) VALUES
('GLOBAL', 'CR00001', '人管', 1, '人事管理：人员档案与代理人管理（内置标准角色，全渠道共用）', 1, 1, 'system', 'system'),
('GLOBAL', 'CR00002', '业管', 1, '业务管理：客户、订单与权益业务（内置标准角色，全渠道共用）', 1, 2, 'system', 'system'),
('GLOBAL', 'CR00003', '组训', 1, '组织培训：培训课程与活动组织（内置标准角色，全渠道共用）', 1, 3, 'system', 'system'),
('GLOBAL', 'CR00004', '财务', 1, '财务管理：结算、发票与收付款（内置标准角色，全渠道共用）', 1, 4, 'system', 'system');
