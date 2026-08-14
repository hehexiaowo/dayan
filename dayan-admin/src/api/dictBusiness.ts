import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SystemDictBusiness, SystemDictBusinessQuery } from '@/types/dict'

/**
 * 业务字典接口封装。
 *
 * 对应后端 SystemDictBusinessAdminController（/admin-api/dicts-business/*）。
 * 主键 id（自增），update/delete 用 path id。
 */

/** 业务字典分页：GET /admin-api/dicts-business */
export function pageDictBusiness(query: SystemDictBusinessQuery): Promise<PageResult<SystemDictBusiness>> {
  return request<PageResult<SystemDictBusiness>>({
    url: '/admin-api/dicts-business',
    method: 'get',
    params: query
  })
}

/** 新增业务字典项：POST /admin-api/dicts-business */
export function createDictBusiness(data: Partial<SystemDictBusiness>): Promise<number> {
  return request<number>({ url: '/admin-api/dicts-business', method: 'post', data })
}

/** 修改业务字典项：PUT /admin-api/dicts-business/{id} */
export function updateDictBusiness(id: number, data: Partial<SystemDictBusiness>): Promise<void> {
  return request<void>({ url: `/admin-api/dicts-business/${id}`, method: 'put', data })
}

/** 删除业务字典项：DELETE /admin-api/dicts-business/{id} */
export function deleteDictBusiness(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/dicts-business/${id}`, method: 'delete' })
}
