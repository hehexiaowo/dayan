import { request } from '@/utils/request'
import type { Menu, DomainType } from '@/types/menu'

/**
 * 菜单接口封装。
 *
 * 对应后端 SystemMenuAdminController（/admin-api/menus/*）。
 */

/** 菜单列表（平铺，可按端过滤）：GET /admin-api/menus */
export function listMenus(domainType?: DomainType): Promise<Menu[]> {
  return request<Menu[]>({
    url: '/admin-api/menus',
    method: 'get',
    params: domainType ? { domainType } : {}
  })
}

/** 菜单树（后端已组装 children）：GET /admin-api/menus/tree */
export function getMenuTree(domainType?: DomainType): Promise<Menu[]> {
  return request<Menu[]>({
    url: '/admin-api/menus/tree',
    method: 'get',
    params: domainType ? { domainType } : {}
  })
}

/**
 * 当前账号可见菜单树（RBAC 数据权限，后端已组装 children）：GET /admin-api/menus/mine
 *
 * 后端按当前登录账号的角色过滤可见菜单，并自动补全祖先目录、组装树。
 * 超管返回全部菜单；非超管仅返回角色在 organ_role_menu_rel 中关联的菜单。
 */
export function getMyMenuTree(domainType?: DomainType): Promise<Menu[]> {
  return request<Menu[]>({
    url: '/admin-api/menus/mine',
    method: 'get',
    params: domainType ? { domainType } : {}
  })
}

/** 新增菜单：POST /admin-api/menus */
export function createMenu(data: Menu): Promise<string> {
  return request<string>({
    url: '/admin-api/menus',
    method: 'post',
    data
  })
}

/** 修改菜单：PUT /admin-api/menus/{menuCode} */
export function updateMenu(menuCode: string, data: Menu): Promise<void> {
  return request<void>({
    url: `/admin-api/menus/${menuCode}`,
    method: 'put',
    data
  })
}

/** 删除菜单：DELETE /admin-api/menus/{menuCode} */
export function deleteMenu(menuCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/menus/${menuCode}`,
    method: 'delete'
  })
}
