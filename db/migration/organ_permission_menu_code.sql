-- organ_permission_menu_code.sql
-- 角色授权弹窗重构：organ_permission 加 menu_code 归属列 + 数据迁移
-- 规格：.superpowers/specs/2026-08-13-role-grant-menu-tree-design.md

-- 1. 加列 + 索引
ALTER TABLE organ_permission
  ADD COLUMN menu_code VARCHAR(50) NULL COMMENT '归属菜单(system_menu.menu_code)，NULL=未分组(其他权限组)' AFTER parent_code;
ALTER TABLE organ_permission ADD INDEX idx_menu_code (menu_code);

-- 2. type=3 接口权限归属映射（205 条）
UPDATE organ_permission SET menu_code='admin_resource_park'
 WHERE permission_type=3 AND parent_code IN ('park:info','park:adviser','park:care-type','park:display-block','park:facility','park:food-type','park:media-file','park:media-image','park:media-video','park:media-vr','park:periphery','park:pricing','park:room-type','park:score','park:service-item');
UPDATE organ_permission SET menu_code='admin_goods_list'
 WHERE permission_type=3 AND parent_code IN ('goods:info','goods:sku-course','goods:sku-equity','goods:sku-scene','goods:sku-sojourn');
UPDATE organ_permission SET menu_code='admin_resource_scene'
 WHERE permission_type=3 AND parent_code IN ('scene:info','scene:item','scene:item-price','scene:resource','scene:schedule');
UPDATE organ_permission SET menu_code='admin_service_session'
 WHERE permission_type=3 AND parent_code IN ('service:session','service:evaluation','service:visit-record','service:equity-arrange','service:equity-demand','service:equity-followup','service:equity-solution');
UPDATE organ_permission SET menu_code='admin_channel_info'
 WHERE permission_type=3 AND parent_code IN ('channel:info','channel:account','channel:config','channel:openplatform','channel:permission','channel:role');

-- 3. type=1 菜单权限改造为 type=3 并归属（10 条；实为菜单页 list 接口权限，不转则挂不上授权树）
UPDATE organ_permission SET permission_type=3, menu_code='admin_service_butler'    WHERE permission_code='butler:info:list';
UPDATE organ_permission SET permission_type=3, menu_code='admin_channel_client'    WHERE permission_code='client:info:list';
UPDATE organ_permission SET permission_type=3, menu_code='admin_equity_batch'      WHERE permission_code='equity:batch:list';
UPDATE organ_permission SET permission_type=3, menu_code='admin_equity_depot'      WHERE permission_code='equity:depot:list';
UPDATE organ_permission SET permission_type=3, menu_code='admin_finance_bill'      WHERE permission_code='finance:bill:list';
UPDATE organ_permission SET permission_type=3, menu_code='admin_finance_flow'      WHERE permission_code='finance:flow:list';
UPDATE organ_permission SET permission_type=3, menu_code='admin_goods_list'        WHERE permission_code='goods:info:list';
UPDATE organ_permission SET permission_type=3, menu_code='admin_order_equity'      WHERE permission_code='order:equity:list';
UPDATE organ_permission SET permission_type=3, menu_code='admin_order_scene'       WHERE permission_code='order:scene:list';
-- 权益模板功能已废除：仅转类型，不归属任何菜单（弹窗「其他权限」组展示）
UPDATE organ_permission SET permission_type=3, menu_code=NULL                      WHERE permission_code='equity:template:list';

-- 4. type=2 按钮权限归属（5 条，服务项目）
UPDATE organ_permission SET menu_code='admin_goods_service-item'
 WHERE permission_type=2 AND permission_code LIKE 'goods:service-item:%';

-- 5. 补齐 14 个缺失的菜单级权限码（菜单有 permission_code 但 organ_permission 无此行，
--    否则非超管配了菜单调列表接口也被 403；path/method 仅信息展示用，留 NULL）
INSERT INTO organ_permission
  (permission_code, permission_name, parent_code, permission_type, path, method, sort_order, status, remark, menu_code, created_at, updated_at, creator, updater, deleted)
VALUES
  ('organ:account:list',    '账号列表',     'organ:account',    3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_basic_account',        NOW(), NOW(), 'system', 'system', 0),
  ('organ:role:list',       '角色列表',     'organ:role',       3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_basic_role',           NOW(), NOW(), 'system', 'system', 0),
  ('organ:info:list',       '组织列表',     'organ:info',       3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_basic_organ',          NOW(), NOW(), 'system', 'system', 0),
  ('system:menu:list',      '菜单列表',     'system:menu',      3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_basic_menu',           NOW(), NOW(), 'system', 'system', 0),
  ('system:dict:list',      '字典列表',     'system:dict',      3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_system_dict',          NOW(), NOW(), 'system', 'system', 0),
  ('system:sm:list',        '状态规则列表', 'system:sm',        3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_system_state_machine', NOW(), NOW(), 'system', 'system', 0),
  ('system:config:list',    '配置列表',     'system:config',    3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_system_config',        NOW(), NOW(), 'system', 'system', 0),
  ('system:log:list',       '日志列表',     'system:log',       3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_system_log',           NOW(), NOW(), 'system', 'system', 0),
  ('agent:info:list',       '队伍列表',     'agent:info',       3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_channel_agent',        NOW(), NOW(), 'system', 'system', 0),
  ('distributor:info:list', '分销列表',     'distributor:info', 3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_channel_distributor',  NOW(), NOW(), 'system', 'system', 0),
  ('finance:invoice:list',  '发票列表',     'finance:invoice',  3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_finance_invoice',      NOW(), NOW(), 'system', 'system', 0),
  ('content:info:list',     '内容列表',     'content:info',     3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_resource_content',     NOW(), NOW(), 'system', 'system', 0),
  ('course:info:list',      '课程列表',     'course:info',      3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_resource_course',      NOW(), NOW(), 'system', 'system', 0),
  ('supplier:info:list',    '供应列表',     'supplier:info',    3, NULL, NULL, 10, 1, '迁移补齐菜单级权限', 'admin_resource_supplier',    NOW(), NOW(), 'system', 'system', 0);
