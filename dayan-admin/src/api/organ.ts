import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { Organ, OrganQuery, OrganSimple } from '@/types/organ'

/**
 * 机构（组织）接口封装。
 *
 * 对应后端 OrganInfoAdminController（/admin-api/organs/*）。
 */

/** 机构分页：GET /admin-api/organs */
export function pageOrgans(query: OrganQuery): Promise<PageResult<Organ>> {
  return request<PageResult<Organ>>({
    url: '/admin-api/organs',
    method: 'get',
    params: query
  })
}

/** 机构详情：GET /admin-api/organs/{organCode} */
export function getOrgan(organCode: string): Promise<Organ> {
  return request<Organ>({
    url: `/admin-api/organs/${organCode}`,
    method: 'get'
  })
}

/** 新增机构：POST /admin-api/organs（organCode 后端生成） */
export function createOrgan(data: Organ): Promise<string> {
  return request<string>({
    url: '/admin-api/organs',
    method: 'post',
    data
  })
}

/** 修改机构：PUT /admin-api/organs/{organCode} */
export function updateOrgan(organCode: string, data: Organ): Promise<void> {
  return request<void>({
    url: `/admin-api/organs/${organCode}`,
    method: 'put',
    data
  })
}

/** 删除机构：DELETE /admin-api/organs/{organCode} */
export function deleteOrgan(organCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/organs/${organCode}`,
    method: 'delete'
  })
}

/** 启用机构全量列表（下拉用，不分页）：GET /admin-api/organs/all */
export function listAllOrgans(): Promise<OrganSimple[]> {
  return request<OrganSimple[]>({
    url: '/admin-api/organs/all',
    method: 'get'
  })
}
