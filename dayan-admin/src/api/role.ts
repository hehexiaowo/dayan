import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { Role, RoleQuery, RoleGrants } from '@/types/role'

/**
 * 角色接口封装。
 *
 * 对应后端 OrganRoleAdminController（/admin-api/roles/*）。
 */

/** 角色分页：GET /admin-api/roles */
export function pageRoles(query: RoleQuery): Promise<PageResult<Role>> {
  return request<PageResult<Role>>({
    url: '/admin-api/roles',
    method: 'get',
    params: query
  })
}

/** 角色详情：GET /admin-api/roles/{roleCode} */
export function getRole(roleCode: string): Promise<Role> {
  return request<Role>({
    url: `/admin-api/roles/${roleCode}`,
    method: 'get'
  })
}

/** 新增角色：POST /admin-api/roles */
export function createRole(data: Role): Promise<string> {
  return request<string>({
    url: '/admin-api/roles',
    method: 'post',
    data
  })
}

/** 修改角色：PUT /admin-api/roles/{roleCode} */
export function updateRole(roleCode: string, data: Role): Promise<void> {
  return request<void>({
    url: `/admin-api/roles/${roleCode}`,
    method: 'put',
    data
  })
}

/** 删除角色：DELETE /admin-api/roles/{roleCode} */
export function deleteRole(roleCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/roles/${roleCode}`,
    method: 'delete'
  })
}

/** 查询角色授权（菜单码+权限码）：GET /admin-api/roles/{roleCode}/permissions */
export function getRolePermissions(roleCode: string): Promise<RoleGrants> {
  return request<RoleGrants>({
    url: `/admin-api/roles/${roleCode}/permissions`,
    method: 'get'
  })
}

/** 保存角色授权（菜单+权限，全量覆盖）：PUT /admin-api/roles/{roleCode}/permissions */
export function updateRolePermissions(roleCode: string, grants: RoleGrants): Promise<void> {
  return request<void>({
    url: `/admin-api/roles/${roleCode}/permissions`,
    method: 'put',
    data: grants
  })
}
