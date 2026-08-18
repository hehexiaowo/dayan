SET NAMES utf8mb4;
-- =====================================================================
-- 82_system_knowledge_repo.sql  知识仓库迁入 system 域
--
-- 参照 51_system_asset.sql 先例：表名、权限码、菜单权限统一收口。
-- 1) knowledge_repo → system_knowledge_repo（RENAME 保留全量数据）
-- 2) 菜单 admin_system_knowledge 权限码 knowledge:repo:list → system:knowledge:repo:list
-- 3) 权限码：新建 system:knowledge:*（repo/doc/chat 12 条，对齐
--    @SaCheckPermission 注解）；存量库无 knowledge:* 定义行与角色绑定
--    （本库现状仅菜单引用），此处 DELETE 旧码为防御性兜底
-- 4) system_ 前缀命中租户拦截默认忽略清单，agent 端 ignore-tables
--    同步改名为 system_knowledge_repo；channel 端隔离改由业务层
--    requireRepoVisible / 渠道树校验承担
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 表改名
-- ---------------------------------------------------------------------
RENAME TABLE `knowledge_repo` TO `system_knowledge_repo`;

ALTER TABLE `system_knowledge_repo` COMMENT = '百炼知识仓库（system 域全局表：平台 + 每渠道一个，远端索引元数据）';

-- ---------------------------------------------------------------------
-- 2. 菜单权限码
-- ---------------------------------------------------------------------
UPDATE `system_menu`
   SET `permission_code` = 'system:knowledge:repo:list',
       `remark` = '系统知识仓库：百炼远端索引与文档管理',
       `updated_at` = NOW()
 WHERE `menu_code` = 'admin_system_knowledge';

-- ---------------------------------------------------------------------
-- 3. 权限定义：新建 system:knowledge:*（parent system:knowledge）
-- ---------------------------------------------------------------------
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`,
   `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('system:knowledge:repo:list',   '知识仓库列表', 'system:knowledge', 3, '/admin-api/system/knowledge/repos/page',    'GET',    220, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0),
  ('system:knowledge:repo:query',  '知识仓库详情', 'system:knowledge', 3, '/admin-api/system/knowledge/repos/*',       'GET',    221, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0),
  ('system:knowledge:repo:create', '新建知识仓库', 'system:knowledge', 3, '/admin-api/system/knowledge/repos',          'POST',   222, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0),
  ('system:knowledge:repo:update', '修改知识仓库', 'system:knowledge', 3, '/admin-api/system/knowledge/repos/*',       'PUT',    223, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0),
  ('system:knowledge:repo:delete', '删除知识仓库', 'system:knowledge', 3, '/admin-api/system/knowledge/repos/*',       'DELETE', 224, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0),
  ('system:knowledge:repo:sync',   '同步知识仓库', 'system:knowledge', 3, '/admin-api/system/knowledge/repos/*/sync',  'POST',   225, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0),
  ('system:knowledge:doc:list',    '文档列表',     'system:knowledge', 3, '/admin-api/system/knowledge/repos/*/docs',  'GET',    226, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0),
  ('system:knowledge:doc:upload',  '上传文档',     'system:knowledge', 3, '/admin-api/system/knowledge/repos/*/docs',  'POST',   227, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0),
  ('system:knowledge:doc:import',  '导入文档',     'system:knowledge', 3, '/admin-api/system/knowledge/repos/*/import','POST',   228, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0),
  ('system:knowledge:doc:delete',  '删除文档',     'system:knowledge', 3, '/admin-api/system/knowledge/repos/*/docs/*','DELETE', 229, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0),
  ('system:knowledge:chat',        '知识库问答',   'system:knowledge', 3, '/admin-api/system/knowledge/chat',          'POST',   230, 1, '系统知识仓库', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ---------------------------------------------------------------------
-- 4. 旧权限码清理（防御性：本库现状无定义行与角色绑定）
-- ---------------------------------------------------------------------
DELETE FROM `organ_role_permission_ship` WHERE `permission_code` LIKE 'knowledge:repo:%' OR `permission_code` LIKE 'knowledge:doc:%' OR `permission_code` = 'knowledge:chat';
DELETE FROM `organ_permission` WHERE `permission_code` LIKE 'knowledge:repo:%' OR `permission_code` LIKE 'knowledge:doc:%' OR `permission_code` = 'knowledge:chat';
