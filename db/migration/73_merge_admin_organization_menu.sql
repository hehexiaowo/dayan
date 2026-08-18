SET NAMES utf8mb4;
-- ============================================================
-- 73 Admin 基础管理：机构管理与部门管理合并为组织管理
--
-- admin_basic_organ 作为唯一入口；部门 CRUD 仍保留，由组织管理页面内按机构维护。
-- 历史 admin_basic_department 菜单停用隐藏，权限记录与角色权限关系保持不变。
-- ============================================================

UPDATE system_menu
SET menu_name = '组织管理',
    remark = '机构及部门管理',
    icon = 'OfficeBuilding',
    sort_order = 4
WHERE menu_code = 'admin_basic_organ';

UPDATE system_menu
SET is_visible = 0,
    status = 0,
    remark = '已并入组织管理'
WHERE menu_code = 'admin_basic_department';
