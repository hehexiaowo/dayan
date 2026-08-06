import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { Employee, EmployeeQuery } from '@/types/employee'

/**
 * 员工接口封装。
 *
 * 对应后端 OrganEmployeeAdminController（/admin-api/employees/*）。
 * 注意：主键为 (organCode, employeeCode) 联合键，update/delete/by-dept 均带 organCode 路径段。
 */

/** 员工分页：GET /admin-api/employees */
export function pageEmployees(query: EmployeeQuery): Promise<PageResult<Employee>> {
  return request<PageResult<Employee>>({
    url: '/admin-api/employees',
    method: 'get',
    params: query
  })
}

/** 按部门查询员工列表：GET /admin-api/employees/by-dept/{organCode}/{deptCode} */
export function listEmployeesByDept(organCode: string, deptCode: string): Promise<Employee[]> {
  return request<Employee[]>({
    url: `/admin-api/employees/by-dept/${organCode}/${deptCode}`,
    method: 'get'
  })
}

/** 新增员工：POST /admin-api/employees */
export function createEmployee(data: Employee): Promise<string> {
  return request<string>({
    url: '/admin-api/employees',
    method: 'post',
    data
  })
}

/** 修改员工：PUT /admin-api/employees/{organCode}/{employeeCode} */
export function updateEmployee(organCode: string, employeeCode: string, data: Employee): Promise<void> {
  return request<void>({
    url: `/admin-api/employees/${organCode}/${employeeCode}`,
    method: 'put',
    data
  })
}

/** 删除员工：DELETE /admin-api/employees/{organCode}/{employeeCode} */
export function deleteEmployee(organCode: string, employeeCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/employees/${organCode}/${employeeCode}`,
    method: 'delete'
  })
}
