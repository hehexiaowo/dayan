import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ParkAdviser, ParkAdviserQuery } from '@/types/park'

/**
 * 机构顾问（ParkAdviser）接口封装。
 *
 * 对应后端 /admin-api/park/adviser/*。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - adviserName 必填；isPrimary=1 首席，同机构唯一（后端自动互斥）。
 * - /list 只接 parkCode 一参，返回数组非分页。
 */

/** 顾问分页：GET /admin-api/park/adviser/page */
export function pageAdvisers(query: ParkAdviserQuery): Promise<PageResult<ParkAdviser>> {
  return request<PageResult<ParkAdviser>>({
    url: '/admin-api/park/adviser/page',
    method: 'get',
    params: query
  })
}

/** 顾问列表（全量，按 parkCode 过滤）：GET /admin-api/park/adviser/list?parkCode=xxx */
export function listAdvisers(parkCode: string): Promise<ParkAdviser[]> {
  return request<ParkAdviser[]>({
    url: '/admin-api/park/adviser/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 顾问详情：GET /admin-api/park/adviser/{id} */
export function getAdviser(id: number): Promise<ParkAdviser> {
  return request<ParkAdviser>({
    url: `/admin-api/park/adviser/${id}`,
    method: 'get'
  })
}

/** 新增顾问：POST /admin-api/park/adviser（返回新 id） */
export function createAdviser(data: Partial<ParkAdviser>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/adviser',
    method: 'post',
    data
  })
}

/** 修改顾问：PUT /admin-api/park/adviser/{id} */
export function updateAdviser(id: number, data: Partial<ParkAdviser>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/adviser/${id}`,
    method: 'put',
    data
  })
}

/** 删除顾问：DELETE /admin-api/park/adviser/{id} */
export function deleteAdviser(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/adviser/${id}`,
    method: 'delete'
  })
}
