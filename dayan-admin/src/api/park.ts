import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ParkInfo, ParkInfoQuery } from '@/types/park'

/**
 * 养老机构接口封装。
 *
 * 对应后端 ParkInfoAdminController（/admin-api/park/info/*）。
 * 状态机操作统一走 transition 端点：approve / offline / online / suspend / resume。
 */

/** 机构分页：GET /admin-api/park/info/page */
export function pageParks(query: ParkInfoQuery): Promise<PageResult<ParkInfo>> {
  return request<PageResult<ParkInfo>>({
    url: '/admin-api/park/info/page',
    method: 'get',
    params: query
  })
}

/** 机构列表（全量）：GET /admin-api/park/info/list */
export function listParks(): Promise<ParkInfo[]> {
  return request<ParkInfo[]>({
    url: '/admin-api/park/info/list',
    method: 'get'
  })
}

/** 机构详情：GET /admin-api/park/info/{parkCode} */
export function getPark(parkCode: string): Promise<ParkInfo> {
  return request<ParkInfo>({
    url: `/admin-api/park/info/${parkCode}`,
    method: 'get'
  })
}

/** 新增机构：POST /admin-api/park/info（返回 parkCode） */
export function createPark(data: Partial<ParkInfo>): Promise<string> {
  return request<string>({
    url: '/admin-api/park/info',
    method: 'post',
    data
  })
}

/**
 * 修改机构：PUT /admin-api/park/info/{parkCode}（parkCode 走 path，非 query string）。
 */
export function updatePark(parkCode: string, data: Partial<ParkInfo>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/info/${parkCode}`,
    method: 'put',
    data
  })
}

/** 删除机构：DELETE /admin-api/park/info/{parkCode} */
export function deletePark(parkCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/park/info/${parkCode}`,
    method: 'delete'
  })
}

/**
 * 机构状态流转：POST /admin-api/park/info/transition?parkCode=&event=
 *
 * 事件取值（PARK_SM）：approve / offline / online / suspend / resume。
 * 返回值为流转后的新 operateStatus 状态码，调用方刷新列表即可，无需使用返回值。
 */
export function transitionPark(parkCode: string, event: string): Promise<number> {
  return request<number>({
    url: '/admin-api/park/info/transition',
    method: 'post',
    params: { parkCode, event }
  })
}
