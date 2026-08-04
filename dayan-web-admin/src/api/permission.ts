import { request } from '@/utils/request'
import type { Permission } from '@/types/permission'

/**
 * 权限接口封装。
 *
 * 对应后端 OrganPermissionAdminController（/admin-api/permissions/*）。
 */

/** 权限列表（平铺，可分页）：GET /admin-api/permissions */
export function listPermissions(params?: {
  permissionName?: string
  permissionType?: number
  status?: number
  current?: number
  size?: number
}): Promise<Permission[]> {
  return request<Permission[]>({
    url: '/admin-api/permissions',
    method: 'get',
    params
  })
}

/** 全部权限（不分页平铺）：GET /admin-api/permissions/all */
export function listAllPermissions(): Promise<Permission[]> {
  return request<Permission[]>({
    url: '/admin-api/permissions/all',
    method: 'get'
  })
}

/** 权限树（后端已组装 children）：GET /admin-api/permissions/tree */
export function getPermissionTree(): Promise<Permission[]> {
  return request<Permission[]>({
    url: '/admin-api/permissions/tree',
    method: 'get'
  })
}

/** 新增权限：POST /admin-api/permissions */
export function createPermission(data: Permission): Promise<string> {
  return request<string>({
    url: '/admin-api/permissions',
    method: 'post',
    data
  })
}

/** 修改权限：PUT /admin-api/permissions/{permissionCode} */
export function updatePermission(permissionCode: string, data: Permission): Promise<void> {
  return request<void>({
    url: `/admin-api/permissions/${permissionCode}`,
    method: 'put',
    data
  })
}

/** 删除权限：DELETE /admin-api/permissions/{permissionCode} */
export function deletePermission(permissionCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/permissions/${permissionCode}`,
    method: 'delete'
  })
}
