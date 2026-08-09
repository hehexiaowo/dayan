SET NAMES utf8mb4;
-- =====================================================================
-- rbac_permission_seed.sql  RBAC 业务权限初始化种子
--
-- 补齐五大业务域（park / scene / service / channel / goods）所有 admin
-- Controller 的完整权限码，覆盖 CRUD + 状态机/业务链特殊动作。
-- 对应后端 @SaCheckPermission 注解的实际权限码，让非超管角色（如 demo 的
-- ROLE_OPERATOR）能执行完整业务操作，而非仅 :list。
--
-- 依赖：rbac_demo_seed.sql 已建立 ROLE_OPERATOR 角色 + organ_role_permission_ship
--   仅含 10 个 *:list 演示权限。本文件在此基础上补全 CRUD 动作权限。
--   admin_seed.sql 的超管账号（is_admin=1）不受影响（返回通配权限）。
--
-- 幂等：全部 INSERT ... ON DUPLICATE KEY UPDATE id=id，现有库可重复 source。
--   已存在的 :list 演示权限会被 UPDATE 覆盖为正式 remark/分类，权限码本身不变。
--
-- 权限码命名规范：{domain}:{resource}:{action}
--   action 常规：list / query / create / update / delete
--   action 业务：transition / accept / confirm / shelf / submit / audit / assign / reset / save 等
--   permission_type：3=接口（本文件全部为后端接口鉴权权限）
-- =====================================================================

-- ============================================================
-- 一、Park 域（76 个权限码，15 资源 + info 状态机）
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`,
   `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- park:info（主机构，含 transition 状态机）
  ('park:info:list',        '机构列表',     'park:info', 3, '/admin-api/park/info', 'GET',     100, 1, '机构主信息', NOW(), NOW(), 'system', 'system', 0),
  ('park:info:query',       '机构详情',     'park:info', 3, '/admin-api/park/info/*', 'GET',    101, 1, '机构主信息', NOW(), NOW(), 'system', 'system', 0),
  ('park:info:create',      '新增机构',     'park:info', 3, '/admin-api/park/info', 'POST',    102, 1, '机构主信息', NOW(), NOW(), 'system', 'system', 0),
  ('park:info:update',      '修改机构',     'park:info', 3, '/admin-api/park/info/*', 'PUT',    103, 1, '机构主信息', NOW(), NOW(), 'system', 'system', 0),
  ('park:info:delete',      '删除机构',     'park:info', 3, '/admin-api/park/info/*', 'DELETE', 104, 1, '机构主信息', NOW(), NOW(), 'system', 'system', 0),
  ('park:info:transition',  '机构状态流转', 'park:info', 3, '/admin-api/park/info/transition', 'POST', 105, 1, '审批/上下线/暂停/恢复', NOW(), NOW(), 'system', 'system', 0),
  -- park:room-type（房型）
  ('park:room-type:list',   '房型列表', 'park:room-type', 3, '/admin-api/park/room-type', 'GET',     110, 1, '机构房型', NOW(), NOW(), 'system', 'system', 0),
  ('park:room-type:query',  '房型详情', 'park:room-type', 3, '/admin-api/park/room-type/*', 'GET',    111, 1, '机构房型', NOW(), NOW(), 'system', 'system', 0),
  ('park:room-type:create', '新增房型', 'park:room-type', 3, '/admin-api/park/room-type', 'POST',    112, 1, '机构房型', NOW(), NOW(), 'system', 'system', 0),
  ('park:room-type:update', '修改房型', 'park:room-type', 3, '/admin-api/park/room-type/*', 'PUT',    113, 1, '机构房型', NOW(), NOW(), 'system', 'system', 0),
  ('park:room-type:delete', '删除房型', 'park:room-type', 3, '/admin-api/park/room-type/*', 'DELETE', 114, 1, '机构房型', NOW(), NOW(), 'system', 'system', 0),
  -- park:pricing（统一定价，合并原 room/care/food/facility/service 5 个 price 权限组）
  ('park:pricing:list',   '定价列表',     'park:pricing', 3, '/admin-api/park/pricing/page',          'GET',     120, 1, '机构定价', NOW(), NOW(), 'system', 'system', 0),
  ('park:pricing:list',   '定价列表(按关联)', 'park:pricing', 3, '/admin-api/park/pricing/list',     'GET',     121, 1, '机构定价', NOW(), NOW(), 'system', 'system', 0),
  ('park:pricing:list',   '定价列表(按费类)', 'park:pricing', 3, '/admin-api/park/pricing/charge-type/*', 'GET', 122, 1, '机构定价', NOW(), NOW(), 'system', 'system', 0),
  ('park:pricing:query',  '定价详情',     'park:pricing', 3, '/admin-api/park/pricing/*',             'GET',     123, 1, '机构定价', NOW(), NOW(), 'system', 'system', 0),
  ('park:pricing:create', '新增定价',     'park:pricing', 3, '/admin-api/park/pricing',               'POST',    124, 1, '机构定价', NOW(), NOW(), 'system', 'system', 0),
  ('park:pricing:update', '修改定价',     'park:pricing', 3, '/admin-api/park/pricing/*',             'PUT',     125, 1, '机构定价', NOW(), NOW(), 'system', 'system', 0),
  ('park:pricing:delete', '删除定价',     'park:pricing', 3, '/admin-api/park/pricing/*',             'DELETE',  126, 1, '机构定价', NOW(), NOW(), 'system', 'system', 0),
  -- park:care-type（照护类型）
  ('park:care-type:list',   '照护类型列表', 'park:care-type', 3, '/admin-api/park/care-type', 'GET',     130, 1, '机构照护类型', NOW(), NOW(), 'system', 'system', 0),
  ('park:care-type:query',  '照护类型详情', 'park:care-type', 3, '/admin-api/park/care-type/*', 'GET',    131, 1, '机构照护类型', NOW(), NOW(), 'system', 'system', 0),
  ('park:care-type:create', '新增照护类型', 'park:care-type', 3, '/admin-api/park/care-type', 'POST',    132, 1, '机构照护类型', NOW(), NOW(), 'system', 'system', 0),
  ('park:care-type:update', '修改照护类型', 'park:care-type', 3, '/admin-api/park/care-type/*', 'PUT',    133, 1, '机构照护类型', NOW(), NOW(), 'system', 'system', 0),
  ('park:care-type:delete', '删除照护类型', 'park:care-type', 3, '/admin-api/park/care-type/*', 'DELETE', 134, 1, '机构照护类型', NOW(), NOW(), 'system', 'system', 0),
  -- park:care-price（已合并入 park:pricing，charge_type=2）
  -- park:food-type（餐饮类型）
  ('park:food-type:list',   '餐饮类型列表', 'park:food-type', 3, '/admin-api/park/food-type', 'GET',     150, 1, '机构餐饮类型', NOW(), NOW(), 'system', 'system', 0),
  ('park:food-type:query',  '餐饮类型详情', 'park:food-type', 3, '/admin-api/park/food-type/*', 'GET',    151, 1, '机构餐饮类型', NOW(), NOW(), 'system', 'system', 0),
  ('park:food-type:create', '新增餐饮类型', 'park:food-type', 3, '/admin-api/park/food-type', 'POST',    152, 1, '机构餐饮类型', NOW(), NOW(), 'system', 'system', 0),
  ('park:food-type:update', '修改餐饮类型', 'park:food-type', 3, '/admin-api/park/food-type/*', 'PUT',    153, 1, '机构餐饮类型', NOW(), NOW(), 'system', 'system', 0),
  ('park:food-type:delete', '删除餐饮类型', 'park:food-type', 3, '/admin-api/park/food-type/*', 'DELETE', 154, 1, '机构餐饮类型', NOW(), NOW(), 'system', 'system', 0),
  -- park:food-price（已合并入 park:pricing，charge_type=3）
  -- park:service-item（服务项）
  ('park:service-item:list',   '服务项列表', 'park:service-item', 3, '/admin-api/park/service-item', 'GET',     170, 1, '机构服务项', NOW(), NOW(), 'system', 'system', 0),
  ('park:service-item:query',  '服务项详情', 'park:service-item', 3, '/admin-api/park/service-item/*', 'GET',    171, 1, '机构服务项', NOW(), NOW(), 'system', 'system', 0),
  ('park:service-item:create', '新增服务项', 'park:service-item', 3, '/admin-api/park/service-item', 'POST',    172, 1, '机构服务项', NOW(), NOW(), 'system', 'system', 0),
  ('park:service-item:update', '修改服务项', 'park:service-item', 3, '/admin-api/park/service-item/*', 'PUT',    173, 1, '机构服务项', NOW(), NOW(), 'system', 'system', 0),
  ('park:service-item:delete', '删除服务项', 'park:service-item', 3, '/admin-api/park/service-item/*', 'DELETE', 174, 1, '机构服务项', NOW(), NOW(), 'system', 'system', 0),
  -- park:facility（设施）
  ('park:facility:list',   '设施列表', 'park:facility', 3, '/admin-api/park/facility', 'GET',     180, 1, '机构设施', NOW(), NOW(), 'system', 'system', 0),
  ('park:facility:query',  '设施详情', 'park:facility', 3, '/admin-api/park/facility/*', 'GET',    181, 1, '机构设施', NOW(), NOW(), 'system', 'system', 0),
  ('park:facility:create', '新增设施', 'park:facility', 3, '/admin-api/park/facility', 'POST',    182, 1, '机构设施', NOW(), NOW(), 'system', 'system', 0),
  ('park:facility:update', '修改设施', 'park:facility', 3, '/admin-api/park/facility/*', 'PUT',    183, 1, '机构设施', NOW(), NOW(), 'system', 'system', 0),
  ('park:facility:delete', '删除设施', 'park:facility', 3, '/admin-api/park/facility/*', 'DELETE', 184, 1, '机构设施', NOW(), NOW(), 'system', 'system', 0),
  -- park:adviser（顾问）
  ('park:adviser:list',   '顾问列表', 'park:adviser', 3, '/admin-api/park/adviser', 'GET',     190, 1, '机构顾问', NOW(), NOW(), 'system', 'system', 0),
  ('park:adviser:query',  '顾问详情', 'park:adviser', 3, '/admin-api/park/adviser/*', 'GET',    191, 1, '机构顾问', NOW(), NOW(), 'system', 'system', 0),
  ('park:adviser:create', '新增顾问', 'park:adviser', 3, '/admin-api/park/adviser', 'POST',    192, 1, '机构顾问', NOW(), NOW(), 'system', 'system', 0),
  ('park:adviser:update', '修改顾问', 'park:adviser', 3, '/admin-api/park/adviser/*', 'PUT',    193, 1, '机构顾问', NOW(), NOW(), 'system', 'system', 0),
  ('park:adviser:delete', '删除顾问', 'park:adviser', 3, '/admin-api/park/adviser/*', 'DELETE', 194, 1, '机构顾问', NOW(), NOW(), 'system', 'system', 0),
  -- park:periphery（周边信息）
  ('park:periphery:list',   '周边信息列表', 'park:periphery', 3, '/admin-api/park/periphery', 'GET',     200, 1, '机构周边信息', NOW(), NOW(), 'system', 'system', 0),
  ('park:periphery:query',  '周边信息详情', 'park:periphery', 3, '/admin-api/park/periphery/*', 'GET',    201, 1, '机构周边信息', NOW(), NOW(), 'system', 'system', 0),
  ('park:periphery:create', '新增周边信息', 'park:periphery', 3, '/admin-api/park/periphery', 'POST',    202, 1, '机构周边信息', NOW(), NOW(), 'system', 'system', 0),
  ('park:periphery:update', '修改周边信息', 'park:periphery', 3, '/admin-api/park/periphery/*', 'PUT',    203, 1, '机构周边信息', NOW(), NOW(), 'system', 'system', 0),
  ('park:periphery:delete', '删除周边信息', 'park:periphery', 3, '/admin-api/park/periphery/*', 'DELETE', 204, 1, '机构周边信息', NOW(), NOW(), 'system', 'system', 0),
  -- park:asset（机构素材库，合并原 media-image/media-video/media-vr/media-file）
  ('park:asset:list',   '素材列表', 'park:asset', 3, '/admin-api/park/asset', 'GET',     210, 1, '机构素材库', NOW(), NOW(), 'system', 'system', 0),
  ('park:asset:query',  '素材详情', 'park:asset', 3, '/admin-api/park/asset/*', 'GET',    211, 1, '机构素材库', NOW(), NOW(), 'system', 'system', 0),
  ('park:asset:create', '新增素材', 'park:asset', 3, '/admin-api/park/asset', 'POST',    212, 1, '机构素材库', NOW(), NOW(), 'system', 'system', 0),
  ('park:asset:update', '修改素材', 'park:asset', 3, '/admin-api/park/asset/*', 'PUT',    213, 1, '机构素材库', NOW(), NOW(), 'system', 'system', 0),
  ('park:asset:delete', '删除素材', 'park:asset', 3, '/admin-api/park/asset/*', 'DELETE', 214, 1, '机构素材库', NOW(), NOW(), 'system', 'system', 0),
  -- park:facility-price（已合并入 park:pricing，charge_type=5）
  -- park:service-price（已合并入 park:pricing，charge_type=6）
  -- park:display-block（展示板块）
  ('park:display-block:list',   '展示板块列表', 'park:display-block', 3, '/admin-api/park/display-block', 'GET',     270, 1, '机构展示板块', NOW(), NOW(), 'system', 'system', 0),
  ('park:display-block:query',  '展示板块详情', 'park:display-block', 3, '/admin-api/park/display-block/*', 'GET',    271, 1, '机构展示板块', NOW(), NOW(), 'system', 'system', 0),
  ('park:display-block:create', '新增展示板块', 'park:display-block', 3, '/admin-api/park/display-block', 'POST',    272, 1, '机构展示板块', NOW(), NOW(), 'system', 'system', 0),
  ('park:display-block:update', '修改展示板块', 'park:display-block', 3, '/admin-api/park/display-block/*', 'PUT',    273, 1, '机构展示板块', NOW(), NOW(), 'system', 'system', 0),
  ('park:display-block:delete', '删除展示板块', 'park:display-block', 3, '/admin-api/park/display-block/*', 'DELETE', 274, 1, '机构展示板块', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ============================================================
-- 二、Scene 域（31 个权限码，info 含 6 个状态机动作）
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`,
   `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- scene:info（主场景，含提交审核/审核/上下架/满期 6 个状态机动作）
  ('scene:info:list',     '场景列表',     'scene:info', 3, '/admin-api/scene/info', 'GET',     300, 1, '场景主信息', NOW(), NOW(), 'system', 'system', 0),
  ('scene:info:query',    '场景详情',     'scene:info', 3, '/admin-api/scene/info/*', 'GET',    301, 1, '场景主信息', NOW(), NOW(), 'system', 'system', 0),
  ('scene:info:create',   '新增场景',     'scene:info', 3, '/admin-api/scene/info', 'POST',    302, 1, '场景主信息', NOW(), NOW(), 'system', 'system', 0),
  ('scene:info:update',   '修改场景',     'scene:info', 3, '/admin-api/scene/info/*', 'PUT',    303, 1, '场景主信息', NOW(), NOW(), 'system', 'system', 0),
  ('scene:info:delete',   '删除场景',     'scene:info', 3, '/admin-api/scene/info/*', 'DELETE', 304, 1, '场景主信息', NOW(), NOW(), 'system', 'system', 0),
  ('scene:info:submit',   '场景提交审核', 'scene:info', 3, '/admin-api/scene/info/submit', 'POST', 305, 1, '场景状态机', NOW(), NOW(), 'system', 'system', 0),
  ('scene:info:audit',    '场景审核',     'scene:info', 3, '/admin-api/scene/info/audit', 'POST',   306, 1, '场景状态机', NOW(), NOW(), 'system', 'system', 0),
  ('scene:info:shelf',    '场景上架',     'scene:info', 3, '/admin-api/scene/info/shelf', 'POST',   307, 1, '场景状态机', NOW(), NOW(), 'system', 'system', 0),
  ('scene:info:unshelf',  '场景下架',     'scene:info', 3, '/admin-api/scene/info/unshelf', 'POST', 308, 1, '场景状态机', NOW(), NOW(), 'system', 'system', 0),
  ('scene:info:reshelf',  '场景重新上架', 'scene:info', 3, '/admin-api/scene/info/reshelf', 'POST', 309, 1, '场景状态机', NOW(), NOW(), 'system', 'system', 0),
  ('scene:info:full',     '场景满期',     'scene:info', 3, '/admin-api/scene/info/full', 'POST',    310, 1, '场景状态机', NOW(), NOW(), 'system', 'system', 0),
  -- scene:item（项目明细）
  ('scene:item:list',   '项目明细列表', 'scene:item', 3, '/admin-api/scene/item', 'GET',     311, 1, '场景项目明细', NOW(), NOW(), 'system', 'system', 0),
  ('scene:item:query',  '项目明细详情', 'scene:item', 3, '/admin-api/scene/item/*', 'GET',    312, 1, '场景项目明细', NOW(), NOW(), 'system', 'system', 0),
  ('scene:item:create', '新增项目明细', 'scene:item', 3, '/admin-api/scene/item', 'POST',    313, 1, '场景项目明细', NOW(), NOW(), 'system', 'system', 0),
  ('scene:item:update', '修改项目明细', 'scene:item', 3, '/admin-api/scene/item/*', 'PUT',    314, 1, '场景项目明细', NOW(), NOW(), 'system', 'system', 0),
  ('scene:item:delete', '删除项目明细', 'scene:item', 3, '/admin-api/scene/item/*', 'DELETE', 315, 1, '场景项目明细', NOW(), NOW(), 'system', 'system', 0),
  -- scene:item-price（价格档位）
  ('scene:item-price:list',   '价格档位列表', 'scene:item-price', 3, '/admin-api/scene/item-price', 'GET',     320, 1, '场景价格档位', NOW(), NOW(), 'system', 'system', 0),
  ('scene:item-price:query',  '价格档位详情', 'scene:item-price', 3, '/admin-api/scene/item-price/*', 'GET',    321, 1, '场景价格档位', NOW(), NOW(), 'system', 'system', 0),
  ('scene:item-price:create', '新增价格档位', 'scene:item-price', 3, '/admin-api/scene/item-price', 'POST',    322, 1, '场景价格档位', NOW(), NOW(), 'system', 'system', 0),
  ('scene:item-price:update', '修改价格档位', 'scene:item-price', 3, '/admin-api/scene/item-price/*', 'PUT',    323, 1, '场景价格档位', NOW(), NOW(), 'system', 'system', 0),
  ('scene:item-price:delete', '删除价格档位', 'scene:item-price', 3, '/admin-api/scene/item-price/*', 'DELETE', 324, 1, '场景价格档位', NOW(), NOW(), 'system', 'system', 0),
  -- scene:schedule（活动日程）
  ('scene:schedule:list',   '活动日程列表', 'scene:schedule', 3, '/admin-api/scene/schedule', 'GET',     330, 1, '场景活动日程', NOW(), NOW(), 'system', 'system', 0),
  ('scene:schedule:query',  '活动日程详情', 'scene:schedule', 3, '/admin-api/scene/schedule/*', 'GET',    331, 1, '场景活动日程', NOW(), NOW(), 'system', 'system', 0),
  ('scene:schedule:create', '新增活动日程', 'scene:schedule', 3, '/admin-api/scene/schedule', 'POST',    332, 1, '场景活动日程', NOW(), NOW(), 'system', 'system', 0),
  ('scene:schedule:update', '修改活动日程', 'scene:schedule', 3, '/admin-api/scene/schedule/*', 'PUT',    333, 1, '场景活动日程', NOW(), NOW(), 'system', 'system', 0),
  ('scene:schedule:delete', '删除活动日程', 'scene:schedule', 3, '/admin-api/scene/schedule/*', 'DELETE', 334, 1, '场景活动日程', NOW(), NOW(), 'system', 'system', 0),
  -- scene:resource（所需资源）
  ('scene:resource:list',   '所需资源列表', 'scene:resource', 3, '/admin-api/scene/resource', 'GET',     340, 1, '场景所需资源', NOW(), NOW(), 'system', 'system', 0),
  ('scene:resource:query',  '所需资源详情', 'scene:resource', 3, '/admin-api/scene/resource/*', 'GET',    341, 1, '场景所需资源', NOW(), NOW(), 'system', 'system', 0),
  ('scene:resource:create', '新增所需资源', 'scene:resource', 3, '/admin-api/scene/resource', 'POST',    342, 1, '场景所需资源', NOW(), NOW(), 'system', 'system', 0),
  ('scene:resource:update', '修改所需资源', 'scene:resource', 3, '/admin-api/scene/resource/*', 'PUT',    343, 1, '场景所需资源', NOW(), NOW(), 'system', 'system', 0),
  ('scene:resource:delete', '删除所需资源', 'scene:resource', 3, '/admin-api/scene/resource/*', 'DELETE', 344, 1, '场景所需资源', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ============================================================
-- 三、Service 域（46 个权限码，session 含 8 状态机动作 + arrange confirm + solution accept）
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`,
   `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- service:session（会话主表，含分配管家/提交需求/确认方案/驳回/开始服务/完成/取消/通用流转/子状态 9 个动作）
  ('service:session:list',            '会话列表',     'service:session', 3, '/admin-api/service/session', 'GET',     400, 1, '服务会话', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:query',           '会话详情',     'service:session', 3, '/admin-api/service/session/*', 'GET',    401, 1, '服务会话', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:create',          '新增会话',     'service:session', 3, '/admin-api/service/session', 'POST',    402, 1, '服务会话', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:update',          '修改会话',     'service:session', 3, '/admin-api/service/session/*', 'PUT',    403, 1, '服务会话', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:delete',          '删除会话',     'service:session', 3, '/admin-api/service/session/*', 'DELETE', 404, 1, '服务会话', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:assign-butler',   '分配管家',     'service:session', 3, '/admin-api/service/session/assign-butler', 'POST', 405, 1, '会话状态机 1→2', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:submit-demand',   '提交需求',     'service:session', 3, '/admin-api/service/session/submit-demand', 'POST', 406, 1, '会话状态机 2→3', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:confirm-solution','确认方案',     'service:session', 3, '/admin-api/service/session/confirm-solution', 'POST', 407, 1, '会话状态机 3→4', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:reject-solution', '驳回方案',     'service:session', 3, '/admin-api/service/session/reject-solution', 'POST', 408, 1, '会话状态机 3→2', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:start-service',   '开始服务',     'service:session', 3, '/admin-api/service/session/start-service', 'POST', 409, 1, '会话状态机 4→5', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:finish',          '完成服务',     'service:session', 3, '/admin-api/service/session/finish', 'POST', 410, 1, '会话状态机 5→6', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:cancel',          '取消会话',     'service:session', 3, '/admin-api/service/session/cancel', 'POST', 411, 1, '会话状态机 →7', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:transition',      '会话通用流转', 'service:session', 3, '/admin-api/service/session/transition', 'POST', 412, 1, '会话状态机通用事件', NOW(), NOW(), 'system', 'system', 0),
  ('service:session:sub-status',      '会话子状态',   'service:session', 3, '/admin-api/service/session/sub-status', 'PUT', 413, 1, '会话子状态独立更新', NOW(), NOW(), 'system', 'system', 0),
  -- service:evaluation（评价，1:1 一会话一评价）
  ('service:evaluation:list',   '评价列表', 'service:evaluation', 3, '/admin-api/service/evaluation', 'GET',     420, 1, '服务评价', NOW(), NOW(), 'system', 'system', 0),
  ('service:evaluation:query',  '评价详情', 'service:evaluation', 3, '/admin-api/service/evaluation/*', 'GET',    421, 1, '服务评价', NOW(), NOW(), 'system', 'system', 0),
  ('service:evaluation:create', '新增评价', 'service:evaluation', 3, '/admin-api/service/evaluation', 'POST',    422, 1, '服务评价（一会话一评价）', NOW(), NOW(), 'system', 'system', 0),
  ('service:evaluation:update', '修改评价', 'service:evaluation', 3, '/admin-api/service/evaluation/*', 'PUT',    423, 1, '服务评价（含回复）', NOW(), NOW(), 'system', 'system', 0),
  ('service:evaluation:delete', '删除评价', 'service:evaluation', 3, '/admin-api/service/evaluation/*', 'DELETE', 424, 1, '服务评价', NOW(), NOW(), 'system', 'system', 0),
  -- service:equity-demand（权益需求）
  ('service:equity-demand:list',   '需求列表', 'service:equity-demand', 3, '/admin-api/service/demand', 'GET',     430, 1, '权益需求', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-demand:query',  '需求详情', 'service:equity-demand', 3, '/admin-api/service/demand/*', 'GET',    431, 1, '权益需求', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-demand:create', '新增需求', 'service:equity-demand', 3, '/admin-api/service/demand', 'POST',    432, 1, '权益需求', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-demand:update', '修改需求', 'service:equity-demand', 3, '/admin-api/service/demand/*', 'PUT',    433, 1, '权益需求', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-demand:delete', '删除需求', 'service:equity-demand', 3, '/admin-api/service/demand/*', 'DELETE', 434, 1, '权益需求', NOW(), NOW(), 'system', 'system', 0),
  -- service:equity-solution（权益方案，含 accept 标记）
  ('service:equity-solution:list',   '方案列表',     'service:equity-solution', 3, '/admin-api/service/solution', 'GET',     440, 1, '权益方案', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-solution:query',  '方案详情',     'service:equity-solution', 3, '/admin-api/service/solution/*', 'GET',    441, 1, '权益方案', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-solution:create', '新增方案',     'service:equity-solution', 3, '/admin-api/service/solution', 'POST',    442, 1, '权益方案', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-solution:update', '修改方案',     'service:equity-solution', 3, '/admin-api/service/solution/*', 'PUT',    443, 1, '权益方案', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-solution:delete', '删除方案',     'service:equity-solution', 3, '/admin-api/service/solution/*', 'DELETE', 444, 1, '权益方案', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-solution:accept', '方案接受标记', 'service:equity-solution', 3, '/admin-api/service/solution/accept', 'POST', 445, 1, '业务链：确认方案前提', NOW(), NOW(), 'system', 'system', 0),
  -- service:equity-arrange（全程安排，含 confirm 确认）
  ('service:equity-arrange:list',    '安排列表',   'service:equity-arrange', 3, '/admin-api/service/arrange', 'GET',     450, 1, '全程安排', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-arrange:query',   '安排详情',   'service:equity-arrange', 3, '/admin-api/service/arrange/*', 'GET',    451, 1, '全程安排', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-arrange:create',  '新增安排',   'service:equity-arrange', 3, '/admin-api/service/arrange', 'POST',    452, 1, '全程安排', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-arrange:update',  '修改安排',   'service:equity-arrange', 3, '/admin-api/service/arrange/*', 'PUT',    453, 1, '全程安排', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-arrange:delete',  '删除安排',   'service:equity-arrange', 3, '/admin-api/service/arrange/*', 'DELETE', 454, 1, '全程安排', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-arrange:confirm', '确认安排',   'service:equity-arrange', 3, '/admin-api/service/arrange/confirm', 'POST', 455, 1, '业务链：开始服务前提', NOW(), NOW(), 'system', 'system', 0),
  -- service:equity-followup（回访品控）
  ('service:equity-followup:list',   '回访列表', 'service:equity-followup', 3, '/admin-api/service/followup', 'GET',     460, 1, '回访品控', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-followup:query',  '回访详情', 'service:equity-followup', 3, '/admin-api/service/followup/*', 'GET',    461, 1, '回访品控', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-followup:create', '新增回访', 'service:equity-followup', 3, '/admin-api/service/followup', 'POST',    462, 1, '回访品控', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-followup:update', '修改回访', 'service:equity-followup', 3, '/admin-api/service/followup/*', 'PUT',    463, 1, '回访品控', NOW(), NOW(), 'system', 'system', 0),
  ('service:equity-followup:delete', '删除回访', 'service:equity-followup', 3, '/admin-api/service/followup/*', 'DELETE', 464, 1, '回访品控', NOW(), NOW(), 'system', 'system', 0),
  -- service:visit-record（上门记录，独立实体非会话子表）
  ('service:visit-record:list',   '上门记录列表', 'service:visit-record', 3, '/admin-api/service/visit-record', 'GET',     470, 1, '上门记录', NOW(), NOW(), 'system', 'system', 0),
  ('service:visit-record:query',  '上门记录详情', 'service:visit-record', 3, '/admin-api/service/visit-record/*', 'GET',    471, 1, '上门记录', NOW(), NOW(), 'system', 'system', 0),
  ('service:visit-record:create', '新增上门记录', 'service:visit-record', 3, '/admin-api/service/visit-record', 'POST',    472, 1, '上门记录', NOW(), NOW(), 'system', 'system', 0),
  ('service:visit-record:update', '修改上门记录', 'service:visit-record', 3, '/admin-api/service/visit-record/*', 'PUT',    473, 1, '上门记录', NOW(), NOW(), 'system', 'system', 0),
  ('service:visit-record:delete', '删除上门记录', 'service:visit-record', 3, '/admin-api/service/visit-record/*', 'DELETE', 474, 1, '上门记录', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ============================================================
-- 四、Channel 域（30 个权限码，含 reset/assign/save 特殊动作）
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`,
   `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- channel:info（渠道主信息）
  ('channel:info:list',   '渠道列表', 'channel:info', 3, '/admin-api/channels', 'GET',     500, 1, '渠道主信息', NOW(), NOW(), 'system', 'system', 0),
  ('channel:info:query',  '渠道详情', 'channel:info', 3, '/admin-api/channels/*', 'GET',    501, 1, '渠道主信息', NOW(), NOW(), 'system', 'system', 0),
  ('channel:info:create', '新增渠道', 'channel:info', 3, '/admin-api/channels', 'POST',    502, 1, '渠道主信息', NOW(), NOW(), 'system', 'system', 0),
  ('channel:info:update', '修改渠道', 'channel:info', 3, '/admin-api/channels/*', 'PUT',    503, 1, '渠道主信息', NOW(), NOW(), 'system', 'system', 0),
  ('channel:info:delete', '删除渠道', 'channel:info', 3, '/admin-api/channels/*', 'DELETE', 504, 1, '渠道主信息', NOW(), NOW(), 'system', 'system', 0),
  -- channel:account（渠道账户，含 reset 重置密码 + assign 分配角色）
  ('channel:account:list',   '账户列表',     'channel:account', 3, '/admin-api/channel-accounts', 'GET',     510, 1, '渠道账户', NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:query',  '账户详情',     'channel:account', 3, '/admin-api/channel-accounts/*', 'GET',    511, 1, '渠道账户', NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:create', '新增账户',     'channel:account', 3, '/admin-api/channel-accounts', 'POST',    512, 1, '渠道账户', NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:update', '修改账户',     'channel:account', 3, '/admin-api/channel-accounts/*', 'PUT',    513, 1, '渠道账户', NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:delete', '删除账户',     'channel:account', 3, '/admin-api/channel-accounts/*', 'DELETE', 514, 1, '渠道账户', NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:reset',  '重置账户密码', 'channel:account', 3, '/admin-api/channel-accounts/*/reset-password', 'PUT', 515, 1, '渠道账户：重置 dayan@123', NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:assign', '分配账户角色', 'channel:account', 3, '/admin-api/channel-account-roles', 'PUT', 516, 1, '渠道账户：角色分配/查询', NOW(), NOW(), 'system', 'system', 0),
  -- channel:role（渠道角色，含 assign 权限授权）
  ('channel:role:list',   '角色列表',     'channel:role', 3, '/admin-api/channel-roles', 'GET',     520, 1, '渠道角色', NOW(), NOW(), 'system', 'system', 0),
  ('channel:role:query',  '角色详情',     'channel:role', 3, '/admin-api/channel-roles/*', 'GET',    521, 1, '渠道角色', NOW(), NOW(), 'system', 'system', 0),
  ('channel:role:create', '新增角色',     'channel:role', 3, '/admin-api/channel-roles', 'POST',    522, 1, '渠道角色', NOW(), NOW(), 'system', 'system', 0),
  ('channel:role:update', '修改角色',     'channel:role', 3, '/admin-api/channel-roles/*', 'PUT',    523, 1, '渠道角色', NOW(), NOW(), 'system', 'system', 0),
  ('channel:role:delete', '删除角色',     'channel:role', 3, '/admin-api/channel-roles/*', 'DELETE', 524, 1, '渠道角色', NOW(), NOW(), 'system', 'system', 0),
  ('channel:role:assign', '角色权限授权', 'channel:role', 3, '/admin-api/channel-roles/*/permissions', 'PUT', 525, 1, '渠道角色：权限授权', NOW(), NOW(), 'system', 'system', 0),
  -- channel:permission（渠道权限定义）
  ('channel:permission:list',   '权限列表', 'channel:permission', 3, '/admin-api/channel-permissions', 'GET',     530, 1, '渠道权限定义', NOW(), NOW(), 'system', 'system', 0),
  ('channel:permission:query',  '权限详情', 'channel:permission', 3, '/admin-api/channel-permissions/*', 'GET',    531, 1, '渠道权限定义', NOW(), NOW(), 'system', 'system', 0),
  ('channel:permission:create', '新增权限', 'channel:permission', 3, '/admin-api/channel-permissions', 'POST',    532, 1, '渠道权限定义', NOW(), NOW(), 'system', 'system', 0),
  ('channel:permission:update', '修改权限', 'channel:permission', 3, '/admin-api/channel-permissions/*', 'PUT',    533, 1, '渠道权限定义', NOW(), NOW(), 'system', 'system', 0),
  ('channel:permission:delete', '删除权限', 'channel:permission', 3, '/admin-api/channel-permissions/*', 'DELETE', 534, 1, '渠道权限定义', NOW(), NOW(), 'system', 'system', 0),
  -- channel:config（分发配置，list+save 全量覆盖模式）
  ('channel:config:query', '配置查询', 'channel:config', 3, '/admin-api/channel-configs', 'GET', 540, 1, '渠道分发配置：内容/场景/商品查询', NOW(), NOW(), 'system', 'system', 0),
  ('channel:config:save',  '配置保存', 'channel:config', 3, '/admin-api/channel-configs', 'POST', 541, 1, '渠道分发配置：内容/场景/商品全量覆盖保存', NOW(), NOW(), 'system', 'system', 0),
  -- channel:openplatform（开放平台）
  ('channel:openplatform:list',   '开放平台列表', 'channel:openplatform', 3, '/admin-api/open-platforms', 'GET',     550, 1, '开放平台', NOW(), NOW(), 'system', 'system', 0),
  ('channel:openplatform:query',  '开放平台详情', 'channel:openplatform', 3, '/admin-api/open-platforms/*', 'GET',    551, 1, '开放平台', NOW(), NOW(), 'system', 'system', 0),
  ('channel:openplatform:create', '新增开放平台', 'channel:openplatform', 3, '/admin-api/open-platforms', 'POST',    552, 1, '开放平台', NOW(), NOW(), 'system', 'system', 0),
  ('channel:openplatform:update', '修改开放平台', 'channel:openplatform', 3, '/admin-api/open-platforms/*', 'PUT',    553, 1, '开放平台', NOW(), NOW(), 'system', 'system', 0),
  ('channel:openplatform:delete', '删除开放平台', 'channel:openplatform', 3, '/admin-api/open-platforms/*', 'DELETE', 554, 1, '开放平台', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ============================================================
-- 五、Goods 域（26 个权限码，info 含 shelf 上下架）
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`,
   `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- goods:info（商品主信息，含 shelf 上下架）
  ('goods:info:list',   '商品列表',     'goods:info', 3, '/admin-api/goods/info', 'GET',     600, 1, '商品主信息', NOW(), NOW(), 'system', 'system', 0),
  ('goods:info:query',  '商品详情',     'goods:info', 3, '/admin-api/goods/info/*', 'GET',    601, 1, '商品主信息', NOW(), NOW(), 'system', 'system', 0),
  ('goods:info:create', '新增商品',     'goods:info', 3, '/admin-api/goods/info', 'POST',    602, 1, '商品主信息', NOW(), NOW(), 'system', 'system', 0),
  ('goods:info:update', '修改商品',     'goods:info', 3, '/admin-api/goods/info/*', 'PUT',    603, 1, '商品主信息', NOW(), NOW(), 'system', 'system', 0),
  ('goods:info:shelf',  '商品上下架',   'goods:info', 3, '/admin-api/goods/info/shelf', 'POST', 604, 1, '商品上下架', NOW(), NOW(), 'system', 'system', 0),
  ('goods:info:delete', '删除商品',     'goods:info', 3, '/admin-api/goods/info/*', 'DELETE', 605, 1, '商品主信息', NOW(), NOW(), 'system', 'system', 0),
  -- goods:sku-course（课程 SKU）
  ('goods:sku-course:list',   '课程SKU列表', 'goods:sku-course', 3, '/admin-api/goods/sku-course', 'GET',     610, 1, '商品课程SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-course:query',  '课程SKU详情', 'goods:sku-course', 3, '/admin-api/goods/sku-course/*', 'GET',    611, 1, '商品课程SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-course:create', '新增课程SKU', 'goods:sku-course', 3, '/admin-api/goods/sku-course', 'POST',    612, 1, '商品课程SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-course:update', '修改课程SKU', 'goods:sku-course', 3, '/admin-api/goods/sku-course/*', 'PUT',    613, 1, '商品课程SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-course:delete', '删除课程SKU', 'goods:sku-course', 3, '/admin-api/goods/sku-course/*', 'DELETE', 614, 1, '商品课程SKU', NOW(), NOW(), 'system', 'system', 0),
  -- goods:sku-equity（权益 SKU）
  ('goods:sku-equity:list',   '权益SKU列表', 'goods:sku-equity', 3, '/admin-api/goods/sku-equity', 'GET',     620, 1, '商品权益SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-equity:query',  '权益SKU详情', 'goods:sku-equity', 3, '/admin-api/goods/sku-equity/*', 'GET',    621, 1, '商品权益SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-equity:create', '新增权益SKU', 'goods:sku-equity', 3, '/admin-api/goods/sku-equity', 'POST',    622, 1, '商品权益SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-equity:update', '修改权益SKU', 'goods:sku-equity', 3, '/admin-api/goods/sku-equity/*', 'PUT',    623, 1, '商品权益SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-equity:delete', '删除权益SKU', 'goods:sku-equity', 3, '/admin-api/goods/sku-equity/*', 'DELETE', 624, 1, '商品权益SKU', NOW(), NOW(), 'system', 'system', 0),
  -- goods:sku-scene（场景 SKU）
  ('goods:sku-scene:list',   '场景SKU列表', 'goods:sku-scene', 3, '/admin-api/goods/sku-scene', 'GET',     630, 1, '商品场景SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-scene:query',  '场景SKU详情', 'goods:sku-scene', 3, '/admin-api/goods/sku-scene/*', 'GET',    631, 1, '商品场景SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-scene:create', '新增场景SKU', 'goods:sku-scene', 3, '/admin-api/goods/sku-scene', 'POST',    632, 1, '商品场景SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-scene:update', '修改场景SKU', 'goods:sku-scene', 3, '/admin-api/goods/sku-scene/*', 'PUT',    633, 1, '商品场景SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-scene:delete', '删除场景SKU', 'goods:sku-scene', 3, '/admin-api/goods/sku-scene/*', 'DELETE', 634, 1, '商品场景SKU', NOW(), NOW(), 'system', 'system', 0),
  -- goods:sku-sojourn（旅居 SKU）
  ('goods:sku-sojourn:list',   '旅居SKU列表', 'goods:sku-sojourn', 3, '/admin-api/goods/sku-sojourn', 'GET',     640, 1, '商品旅居SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-sojourn:query',  '旅居SKU详情', 'goods:sku-sojourn', 3, '/admin-api/goods/sku-sojourn/*', 'GET',    641, 1, '商品旅居SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-sojourn:create', '新增旅居SKU', 'goods:sku-sojourn', 3, '/admin-api/goods/sku-sojourn', 'POST',    642, 1, '商品旅居SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-sojourn:update', '修改旅居SKU', 'goods:sku-sojourn', 3, '/admin-api/goods/sku-sojourn/*', 'PUT',    643, 1, '商品旅居SKU', NOW(), NOW(), 'system', 'system', 0),
  ('goods:sku-sojourn:delete', '删除旅居SKU', 'goods:sku-sojourn', 3, '/admin-api/goods/sku-sojourn/*', 'DELETE', 644, 1, '商品旅居SKU', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ============================================================
-- 六、角色 ↔ 权限关联（授权给 ROLE_OPERATOR，demo 账号可用完整 CRUD）
-- 说明：demo 角色原先只有 10 个 :list 权限，本节补齐五大域全部权限码，
--       让 operator/op123 能执行完整业务操作（超管不受影响）。
-- ============================================================
INSERT INTO `organ_role_permission_ship`
  (`role_code`, `permission_code`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- Park 域（71 条）
  ('ROLE_OPERATOR', 'park:info:list',        NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:info:query',       NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:info:create',      NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:info:update',      NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:info:delete',      NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:info:transition',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:room-type:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:room-type:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:room-type:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:room-type:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:room-type:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:pricing:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:pricing:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:pricing:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:pricing:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:pricing:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:care-type:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:care-type:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:care-type:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:care-type:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:care-type:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:food-type:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:food-type:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:food-type:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:food-type:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:food-type:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:service-item:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:service-item:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:service-item:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:service-item:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:service-item:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:facility:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:facility:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:facility:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:facility:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:facility:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:adviser:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:adviser:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:adviser:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:adviser:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:adviser:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:periphery:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:periphery:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:periphery:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:periphery:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:periphery:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:asset:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:asset:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:asset:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:asset:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:asset:delete', NOW(), NOW(), 'system', 'system', 0),
  -- park:score（机构评分）
  ('ROLE_OPERATOR', 'park:score:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:score:update', NOW(), NOW(), 'system', 'system', 0),
  -- park:display-block（5 条）
  ('ROLE_OPERATOR', 'park:display-block:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:display-block:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:display-block:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:display-block:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'park:display-block:delete', NOW(), NOW(), 'system', 'system', 0),
  -- Scene 域（31 条）
  ('ROLE_OPERATOR', 'scene:info:list',     NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:info:query',    NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:info:create',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:info:update',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:info:delete',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:info:submit',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:info:audit',    NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:info:shelf',    NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:info:unshelf',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:info:reshelf',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:info:full',     NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:item:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:item:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:item:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:item:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:item:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:item-price:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:item-price:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:item-price:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:item-price:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:item-price:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:schedule:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:schedule:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:schedule:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:schedule:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:schedule:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:resource:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:resource:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:resource:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:resource:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'scene:resource:delete', NOW(), NOW(), 'system', 'system', 0),
  -- Service 域（46 条）
  ('ROLE_OPERATOR', 'service:session:list',            NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:query',           NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:create',          NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:update',          NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:delete',          NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:assign-butler',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:submit-demand',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:confirm-solution',NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:reject-solution', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:start-service',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:finish',          NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:cancel',          NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:transition',      NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:session:sub-status',      NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:evaluation:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:evaluation:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:evaluation:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:evaluation:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:evaluation:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-demand:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-demand:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-demand:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-demand:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-demand:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-solution:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-solution:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-solution:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-solution:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-solution:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-solution:accept', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-arrange:list',    NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-arrange:query',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-arrange:create',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-arrange:update',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-arrange:delete',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-arrange:confirm', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-followup:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-followup:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-followup:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-followup:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:equity-followup:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:visit-record:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:visit-record:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:visit-record:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:visit-record:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'service:visit-record:delete', NOW(), NOW(), 'system', 'system', 0),
  -- Channel 域（30 条）
  ('ROLE_OPERATOR', 'channel:info:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:info:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:info:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:info:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:info:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:account:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:account:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:account:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:account:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:account:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:account:reset',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:account:assign', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:role:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:role:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:role:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:role:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:role:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:role:assign', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:permission:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:permission:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:permission:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:permission:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:permission:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:config:query', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:config:save',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:openplatform:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:openplatform:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:openplatform:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:openplatform:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'channel:openplatform:delete', NOW(), NOW(), 'system', 'system', 0),
  -- Goods 域（26 条）
  ('ROLE_OPERATOR', 'goods:info:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:info:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:info:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:info:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:info:shelf',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:info:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-course:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-course:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-course:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-course:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-course:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-equity:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-equity:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-equity:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-equity:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-equity:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-scene:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-scene:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-scene:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-scene:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-scene:delete', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-sojourn:list',   NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-sojourn:query',  NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-sojourn:create', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-sojourn:update', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:sku-sojourn:delete', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;
