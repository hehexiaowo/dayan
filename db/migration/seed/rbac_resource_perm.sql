SET NAMES utf8mb4;
-- =====================================================================
-- rbac_resource_perm.sql  资源管理域菜单补充 + 按钮级权限补齐
--
-- 内容：
--   1. 补齐 content / course / supplier 三域全部按钮级权限码（rbac_permission_seed.sql
--      仅覆盖了 park/scene/service/channel/goods 五域，这三域此前未播种，非超管无法授权）。
--
-- 历史变更：「内容分类」菜单与 content:category:* 权限随 41 号迁移字典化后由
--   「系统管理 → 字典管理」承载，已于 44 号迁移下线，本文件不再播种。
--   「课程讲师」独立菜单随 67 号迁移下线（讲师管理并入课程管理页抽屉，
--   course:lecturer:* 接口权限仍保留，本文件继续播种）。
--
-- 幂等：system_menu 用 ON DUPLICATE KEY UPDATE；organ_permission 用 ON DUPLICATE KEY
--   UPDATE id=id。现有库可重复 source。超管（is_admin=1）走通配 "*" 不受影响。
-- =====================================================================

-- ============================================================
-- 一、content 域权限（info / media / record-read / record-share）
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- content:info（含 submit/audit/publish/offline 审核流）
  ('content:info:list',    '内容列表',   'content:info', 3, '/admin-api/content/info/page',      'GET',    100, 1, '内容主信息', NOW(), NOW(), 'system', 'system', 0),
  ('content:info:query',   '内容详情',   'content:info', 3, '/admin-api/content/info/*',         'GET',    101, 1, '内容主信息', NOW(), NOW(), 'system', 'system', 0),
  ('content:info:create',  '新增内容',   'content:info', 3, '/admin-api/content/info',           'POST',   102, 1, '内容主信息', NOW(), NOW(), 'system', 'system', 0),
  ('content:info:update',  '修改内容',   'content:info', 3, '/admin-api/content/info',           'PUT',    103, 1, '内容主信息', NOW(), NOW(), 'system', 'system', 0),
  ('content:info:delete',  '删除内容',   'content:info', 3, '/admin-api/content/info/*',         'DELETE', 104, 1, '内容主信息', NOW(), NOW(), 'system', 'system', 0),
  ('content:info:submit',  '提交审核',   'content:info', 3, '/admin-api/content/info/submit',    'POST',   105, 1, '内容审核流', NOW(), NOW(), 'system', 'system', 0),
  ('content:info:audit',   '内容审核',   'content:info', 3, '/admin-api/content/info/audit',     'POST',   106, 1, '内容审核流', NOW(), NOW(), 'system', 'system', 0),
  ('content:info:publish', '内容发布',   'content:info', 3, '/admin-api/content/info/publish',   'POST',   107, 1, '内容审核流', NOW(), NOW(), 'system', 'system', 0),
  ('content:info:offline', '内容下线',   'content:info', 3, '/admin-api/content/info/offline',   'POST',   108, 1, '内容审核流', NOW(), NOW(), 'system', 'system', 0),
  -- content:media
  ('content:media:list',   '媒体列表', 'content:media', 3, '/admin-api/content/media/page', 'GET',    120, 1, '内容媒体', NOW(), NOW(), 'system', 'system', 0),
  ('content:media:query',  '媒体详情', 'content:media', 3, '/admin-api/content/media/*',    'GET',    121, 1, '内容媒体', NOW(), NOW(), 'system', 'system', 0),
  ('content:media:create', '新增媒体', 'content:media', 3, '/admin-api/content/media',      'POST',   122, 1, '内容媒体', NOW(), NOW(), 'system', 'system', 0),
  ('content:media:update', '修改媒体', 'content:media', 3, '/admin-api/content/media',      'PUT',    123, 1, '内容媒体', NOW(), NOW(), 'system', 'system', 0),
  ('content:media:delete', '删除媒体', 'content:media', 3, '/admin-api/content/media/*',    'DELETE', 124, 1, '内容媒体', NOW(), NOW(), 'system', 'system', 0),
  -- content:record-read（无 update/query，有 stats）
  ('content:record-read:list',   '阅读记录列表', 'content:record-read', 3, '/admin-api/content/record-read/page',  'GET',    130, 1, '阅读记录', NOW(), NOW(), 'system', 'system', 0),
  ('content:record-read:create', '新增阅读记录', 'content:record-read', 3, '/admin-api/content/record-read',      'POST',   131, 1, '阅读记录', NOW(), NOW(), 'system', 'system', 0),
  ('content:record-read:delete', '删除阅读记录', 'content:record-read', 3, '/admin-api/content/record-read/*',    'DELETE', 132, 1, '阅读记录', NOW(), NOW(), 'system', 'system', 0),
  ('content:record-read:stats',  '阅读统计',     'content:record-read', 3, '/admin-api/content/record-read/stats','GET',    133, 1, '阅读记录', NOW(), NOW(), 'system', 'system', 0),
  -- content:record-share
  ('content:record-share:list',   '分享记录列表', 'content:record-share', 3, '/admin-api/content/record-share/page', 'GET',    140, 1, '分享记录', NOW(), NOW(), 'system', 'system', 0),
  ('content:record-share:query',  '分享记录详情', 'content:record-share', 3, '/admin-api/content/record-share/*',    'GET',    141, 1, '分享记录', NOW(), NOW(), 'system', 'system', 0),
  ('content:record-share:create', '新增分享记录', 'content:record-share', 3, '/admin-api/content/record-share',      'POST',   142, 1, '分享记录', NOW(), NOW(), 'system', 'system', 0),
  ('content:record-share:update', '修改分享记录', 'content:record-share', 3, '/admin-api/content/record-share',      'PUT',    143, 1, '分享记录', NOW(), NOW(), 'system', 'system', 0),
  ('content:record-share:delete', '删除分享记录', 'content:record-share', 3, '/admin-api/content/record-share/*',    'DELETE', 144, 1, '分享记录', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ============================================================
-- 三、course 域权限（info / lecturer / record-learn）
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- course:info（含 publish/offline 上下架）
  ('course:info:list',    '课程列表', 'course:info', 3, '/admin-api/course/info/page',      'GET',    200, 1, '课程主信息', NOW(), NOW(), 'system', 'system', 0),
  ('course:info:query',   '课程详情', 'course:info', 3, '/admin-api/course/info/*',         'GET',    201, 1, '课程主信息', NOW(), NOW(), 'system', 'system', 0),
  ('course:info:create',  '新增课程', 'course:info', 3, '/admin-api/course/info',           'POST',   202, 1, '课程主信息', NOW(), NOW(), 'system', 'system', 0),
  ('course:info:update',  '修改课程', 'course:info', 3, '/admin-api/course/info/*',         'PUT',    203, 1, '课程主信息', NOW(), NOW(), 'system', 'system', 0),
  ('course:info:delete',  '删除课程', 'course:info', 3, '/admin-api/course/info/*',         'DELETE', 204, 1, '课程主信息', NOW(), NOW(), 'system', 'system', 0),
  ('course:info:publish', '课程上架', 'course:info', 3, '/admin-api/course/info/*/publish', 'PUT',    205, 1, '课程上下架', NOW(), NOW(), 'system', 'system', 0),
  ('course:info:offline', '课程下架', 'course:info', 3, '/admin-api/course/info/*/offline', 'PUT',    206, 1, '课程上下架', NOW(), NOW(), 'system', 'system', 0),
  -- course:lecturer
  ('course:lecturer:list',   '讲师列表', 'course:lecturer', 3, '/admin-api/course/lecturer/page', 'GET',    210, 1, '课程讲师', NOW(), NOW(), 'system', 'system', 0),
  ('course:lecturer:query',  '讲师详情', 'course:lecturer', 3, '/admin-api/course/lecturer/*',     'GET',    211, 1, '课程讲师', NOW(), NOW(), 'system', 'system', 0),
  ('course:lecturer:create', '新增讲师', 'course:lecturer', 3, '/admin-api/course/lecturer',       'POST',   212, 1, '课程讲师', NOW(), NOW(), 'system', 'system', 0),
  ('course:lecturer:update', '修改讲师', 'course:lecturer', 3, '/admin-api/course/lecturer/*',     'PUT',    213, 1, '课程讲师', NOW(), NOW(), 'system', 'system', 0),
  ('course:lecturer:delete', '删除讲师', 'course:lecturer', 3, '/admin-api/course/lecturer/*',     'DELETE', 214, 1, '课程讲师', NOW(), NOW(), 'system', 'system', 0),
  -- course:record-learn
  ('course:record-learn:list',   '学习记录列表', 'course:record-learn', 3, '/admin-api/course/record-learn/page', 'GET',    220, 1, '学习记录', NOW(), NOW(), 'system', 'system', 0),
  ('course:record-learn:query',  '学习记录详情', 'course:record-learn', 3, '/admin-api/course/record-learn/*',    'GET',    221, 1, '学习记录', NOW(), NOW(), 'system', 'system', 0),
  ('course:record-learn:create', '新增学习记录', 'course:record-learn', 3, '/admin-api/course/record-learn',      'POST',   222, 1, '学习记录', NOW(), NOW(), 'system', 'system', 0),
  ('course:record-learn:update', '修改学习记录', 'course:record-learn', 3, '/admin-api/course/record-learn/*',    'PUT',    223, 1, '学习记录', NOW(), NOW(), 'system', 'system', 0),
  ('course:record-learn:delete', '删除学习记录', 'course:record-learn', 3, '/admin-api/course/record-learn/*',    'DELETE', 224, 1, '学习记录', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ============================================================
-- 四、supplier 域权限（info / contact / contract / evaluation / account / role / permission / open-platform）
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- supplier:info（含 audit 审核流）
  ('supplier:info:list',   '供应商列表', 'supplier:info', 3, '/admin-api/supplier/info/page', 'GET',    300, 1, '供应商主信息', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:info:query',  '供应商详情', 'supplier:info', 3, '/admin-api/supplier/info/*',    'GET',    301, 1, '供应商主信息', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:info:create', '新增供应商', 'supplier:info', 3, '/admin-api/supplier/info',      'POST',   302, 1, '供应商主信息', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:info:update', '修改供应商', 'supplier:info', 3, '/admin-api/supplier/info',      'PUT',    303, 1, '供应商主信息', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:info:delete', '删除供应商', 'supplier:info', 3, '/admin-api/supplier/info/*',    'DELETE', 304, 1, '供应商主信息', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:info:audit',  '供应商审核', 'supplier:info', 3, '/admin-api/supplier/info/audit','POST',   305, 1, '供应商审核流', NOW(), NOW(), 'system', 'system', 0),
  -- supplier:contact
  ('supplier:contact:list',   '联系人列表', 'supplier:contact', 3, '/admin-api/supplier/contact/page', 'GET',    310, 1, '供应商联系人', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:contact:create', '新增联系人', 'supplier:contact', 3, '/admin-api/supplier/contact',       'POST',   311, 1, '供应商联系人', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:contact:update', '修改联系人', 'supplier:contact', 3, '/admin-api/supplier/contact',       'PUT',    312, 1, '供应商联系人', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:contact:delete', '删除联系人', 'supplier:contact', 3, '/admin-api/supplier/contact/*',     'DELETE', 313, 1, '供应商联系人', NOW(), NOW(), 'system', 'system', 0),
  -- supplier:contract
  ('supplier:contract:list',   '合同列表', 'supplier:contract', 3, '/admin-api/supplier/contract/page', 'GET',    320, 1, '供应商合同', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:contract:create', '新增合同', 'supplier:contract', 3, '/admin-api/supplier/contract',       'POST',   321, 1, '供应商合同', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:contract:update', '修改合同', 'supplier:contract', 3, '/admin-api/supplier/contract',       'PUT',    322, 1, '供应商合同', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:contract:delete', '删除合同', 'supplier:contract', 3, '/admin-api/supplier/contract/*',     'DELETE', 323, 1, '供应商合同', NOW(), NOW(), 'system', 'system', 0),
  -- supplier:evaluation
  ('supplier:evaluation:list',   '评价列表', 'supplier:evaluation', 3, '/admin-api/supplier/evaluation/page', 'GET',    330, 1, '供应商评价', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:evaluation:create', '新增评价', 'supplier:evaluation', 3, '/admin-api/supplier/evaluation',       'POST',   331, 1, '供应商评价', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:evaluation:update', '修改评价', 'supplier:evaluation', 3, '/admin-api/supplier/evaluation',       'PUT',    332, 1, '供应商评价', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:evaluation:delete', '删除评价', 'supplier:evaluation', 3, '/admin-api/supplier/evaluation/*',     'DELETE', 333, 1, '供应商评价', NOW(), NOW(), 'system', 'system', 0),
  -- supplier:account（含 reset/assign）
  ('supplier:account:list',   '账户列表',     'supplier:account', 3, '/admin-api/supplier/account/page',          'GET',    340, 1, '供应商账户', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:account:create', '新增账户',     'supplier:account', 3, '/admin-api/supplier/account',              'POST',   341, 1, '供应商账户', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:account:update', '修改账户',     'supplier:account', 3, '/admin-api/supplier/account',              'PUT',    342, 1, '供应商账户', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:account:delete', '删除账户',     'supplier:account', 3, '/admin-api/supplier/account/*',            'DELETE', 343, 1, '供应商账户', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:account:reset',  '重置账户密码', 'supplier:account', 3, '/admin-api/supplier/account/*/reset-password', 'PUT', 344, 1, '供应商账户', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:account:assign', '分配账户角色', 'supplier:account', 3, '/admin-api/supplier/account-role',        'PUT',    345, 1, '供应商账户', NOW(), NOW(), 'system', 'system', 0),
  -- supplier:role（含 assign 权限授权）
  ('supplier:role:list',   '角色列表',     'supplier:role', 3, '/admin-api/supplier/role/page',       'GET',    350, 1, '供应商角色', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:role:create', '新增角色',     'supplier:role', 3, '/admin-api/supplier/role',            'POST',   351, 1, '供应商角色', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:role:update', '修改角色',     'supplier:role', 3, '/admin-api/supplier/role',            'PUT',    352, 1, '供应商角色', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:role:delete', '删除角色',     'supplier:role', 3, '/admin-api/supplier/role/*',          'DELETE', 353, 1, '供应商角色', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:role:assign', '角色权限授权', 'supplier:role', 3, '/admin-api/supplier/role/permissions','PUT',    354, 1, '供应商角色', NOW(), NOW(), 'system', 'system', 0),
  -- supplier:permission
  ('supplier:permission:list',   '权限列表', 'supplier:permission', 3, '/admin-api/supplier/permission/page', 'GET',    360, 1, '供应商权限', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:permission:create', '新增权限', 'supplier:permission', 3, '/admin-api/supplier/permission',       'POST',   361, 1, '供应商权限', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:permission:update', '修改权限', 'supplier:permission', 3, '/admin-api/supplier/permission',       'PUT',    362, 1, '供应商权限', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:permission:delete', '删除权限', 'supplier:permission', 3, '/admin-api/supplier/permission/*',     'DELETE', 363, 1, '供应商权限', NOW(), NOW(), 'system', 'system', 0),
  -- supplier:open-platform
  ('supplier:open-platform:list',   '开放平台列表', 'supplier:open-platform', 3, '/admin-api/supplier/open-platform/page', 'GET',    370, 1, '供应商开放平台', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:open-platform:create', '新增开放平台', 'supplier:open-platform', 3, '/admin-api/supplier/open-platform',       'POST',   371, 1, '供应商开放平台', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:open-platform:update', '修改开放平台', 'supplier:open-platform', 3, '/admin-api/supplier/open-platform',       'PUT',    372, 1, '供应商开放平台', NOW(), NOW(), 'system', 'system', 0),
  ('supplier:open-platform:delete', '删除开放平台', 'supplier:open-platform', 3, '/admin-api/supplier/open-platform/*',     'DELETE', 373, 1, '供应商开放平台', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ============================================================
-- 五、tool 域：获客工具菜单 + 按钮级权限（42_tool_domain.sql 配套）
-- ============================================================
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`, `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_resource_tool', '获客工具', 'admin_resource', 2, '/resource/tool', 'resource/tool/index', 'tool:info:list', 'MagicStick', 8, 1, 'admin', 1, '获客工具管理', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `component` = VALUES(`component`), `permission_code` = VALUES(`permission_code`), `path` = VALUES(`path`);

INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('tool:info:list',   '工具列表', 'tool:info', 3, '/admin-api/tool/info/page', 'GET',    400, 1, '获客工具', NOW(), NOW(), 'system', 'system', 0),
  ('tool:info:query',  '工具详情', 'tool:info', 3, '/admin-api/tool/info/*',    'GET',    401, 1, '获客工具', NOW(), NOW(), 'system', 'system', 0),
  ('tool:info:create', '新增工具', 'tool:info', 3, '/admin-api/tool/info',      'POST',   402, 1, '获客工具', NOW(), NOW(), 'system', 'system', 0),
  ('tool:info:update', '修改工具', 'tool:info', 3, '/admin-api/tool/info/*',    'PUT',    403, 1, '获客工具', NOW(), NOW(), 'system', 'system', 0),
  ('tool:info:delete', '删除工具', 'tool:info', 3, '/admin-api/tool/info/*',    'DELETE', 404, 1, '获客工具', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ============================================================
-- 六、lead 域：线索记录菜单 + 只读权限（43_lead_domain.sql 配套；原名线索池，59 迁移更名）
-- ============================================================
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`, `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_channel_lead', '线索记录', 'admin_channel', 2, '/channel/lead', 'channel/lead/index', 'lead:info:list', 'Aim', 5, 1, 'admin', 1, '访客线索记录（分享追踪自动建档，只读）', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `component` = VALUES(`component`), `permission_code` = VALUES(`permission_code`), `path` = VALUES(`path`);

INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('lead:info:list',  '线索列表', 'lead:info', 3, '/admin-api/lead/info/page', 'GET', 410, 1, '访客线索池', NOW(), NOW(), 'system', 'system', 0),
  ('lead:info:query', '线索详情', 'lead:info', 3, '/admin-api/lead/info/*',   'GET', 411, 1, '访客线索池', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
