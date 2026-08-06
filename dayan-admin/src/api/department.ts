import { request } from '@/utils/request'
import type { Department } from '@/types/department'

/**
 * 部门接口封装。
 *
 * 对应后端 OrganDepartmentAdminController（/admin-api/departments/*）。
 * 注意：主键为 (organCode, deptCode) 联合键，update/delete 均带 organCode 路径段。
 */

/** 部门列表（平铺，前端组树）：GET /admin-api/departments?organCode=xxx */
export function listDepartments(organCode: string): Promise<Department[]> {
  return request<Department[]>({
    url: '/admin-api/departments',
    method: 'get',
    params: { organCode }
  })
}

/** 新增部门：POST /admin-api/departments */
export function createDepartment(data: Department): Promise<string> {
  return request<string>({
    url: '/admin-api/departments',
    method: 'post',
    data
  })
}

/** 修改部门：PUT /admin-api/departments/{organCode}/{deptCode} */
export function updateDepartment(organCode: string, deptCode: string, data: Department): Promise<void> {
  return request<void>({
    url: `/admin-api/departments/${organCode}/${deptCode}`,
    method: 'put',
    data
  })
}

/** 删除部门：DELETE /admin-api/departments/{organCode}/{deptCode} */
export function deleteDepartment(organCode: string, deptCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/departments/${organCode}/${deptCode}`,
    method: 'delete'
  })
}
