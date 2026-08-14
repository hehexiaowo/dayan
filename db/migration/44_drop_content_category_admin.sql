-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 44_drop_content_category_admin.sql  下线 admin「内容分类」管理页
--
-- 背景：41_content_category_dict.sql 已将内容分类能力并入业务字典
--   （system_dict_business, dict_type='content_category'），分类的新增/改名/排序/启停
--   一律在「系统管理 → 字典管理」维护；资源管理下的独立「内容分类」页与
--   content:category:* 权限成为冗余入口，予以清除。
-- 影响面（本文件只删菜单/权限数据，不动业务数据）：
--   system_menu                admin_resource_content_category 菜单行
--   organ_permission           content:category:list/query/create/update/delete 五行
--   organ_role_menu_rel        上述菜单的角色授权（当前无引用，防御性删除）
--   organ_role_permission_ship 上述权限的角色授权（当前无引用，防御性删除）
-- 种子侧同步：rbac_resource_perm.sql 中对应行已移除，全新初始化不再生成。
-- =====================================================================

DELETE FROM `system_menu` WHERE `menu_code` = 'admin_resource_content_category';
DELETE FROM `organ_role_menu_rel` WHERE `menu_code` = 'admin_resource_content_category';
DELETE FROM `organ_role_permission_ship` WHERE `permission_code` LIKE 'content:category:%';
DELETE FROM `organ_permission` WHERE `permission_code` LIKE 'content:category:%';
