import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ParkPeriphery,
  ParkPeripheryQuery,
  ParkServiceItem,
  ParkServiceItemQuery
} from '@/types/park'

/**
 * 机构周边配套（ParkPeriphery）+ 服务项（ParkServiceItem）接口封装。
 *
 * 对应后端 /admin-api/park/periphery 与 /admin-api/park/service-item。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - periphery 的 placeName 必填；service-item 的 serviceCode（必填，update 不可改）/ serviceName 必填。
 * - /list 只接 parkCode 一参，返回数组非分页。
 * - 服务价格已迁移至统一定价表（ParkPricing），见 api/park-pricing.ts。
 */

// ---------------- 周边配套（periphery）----------------

/** 周边配套分页：GET /admin-api/park/periphery/page */
export function pagePeripheries(query: ParkPeripheryQuery): Promise<PageResult<ParkPeriphery>> {
  return request<PageResult<ParkPeriphery>>({
    url: '/admin-api/park/periphery/page',
    method: 'get',
    params: query
  })
}

/** 周边配套列表（全量，按 parkCode 过滤）：GET /admin-api/park/periphery/list?parkCode=xxx */
export function listPeripheries(parkCode: string): Promise<ParkPeriphery[]> {
  return request<ParkPeriphery[]>({
    url: '/admin-api/park/periphery/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 周边配套详情：GET /admin-api/park/periphery/{id} */
export function getPeriphery(id: number): Promise<ParkPeriphery> {
  return request<ParkPeriphery>({
    url: `/admin-api/park/periphery/${id}`,
    method: 'get'
  })
}

/** 新增周边配套：POST /admin-api/park/periphery（返回新 id） */
export function createPeriphery(data: Partial<ParkPeriphery>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/periphery',
    method: 'post',
    data
  })
}

/** 修改周边配套：PUT /admin-api/park/periphery/{id} */
export function updatePeriphery(id: number, data: Partial<ParkPeriphery>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/periphery/${id}`,
    method: 'put',
    data
  })
}

/** 删除周边配套：DELETE /admin-api/park/periphery/{id} */
export function deletePeriphery(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/periphery/${id}`,
    method: 'delete'
  })
}

// ---------------- 服务项（service-item）----------------

/** 服务项分页：GET /admin-api/park/service-item/page */
export function pageServiceItems(query: ParkServiceItemQuery): Promise<PageResult<ParkServiceItem>> {
  return request<PageResult<ParkServiceItem>>({
    url: '/admin-api/park/service-item/page',
    method: 'get',
    params: query
  })
}

/** 服务项列表（全量，按 parkCode 过滤）：GET /admin-api/park/service-item/list?parkCode=xxx */
export function listServiceItems(parkCode: string): Promise<ParkServiceItem[]> {
  return request<ParkServiceItem[]>({
    url: '/admin-api/park/service-item/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 服务项详情：GET /admin-api/park/service-item/{id} */
export function getServiceItem(id: number): Promise<ParkServiceItem> {
  return request<ParkServiceItem>({
    url: `/admin-api/park/service-item/${id}`,
    method: 'get'
  })
}

/** 新增服务项：POST /admin-api/park/service-item（返回新 id） */
export function createServiceItem(data: Partial<ParkServiceItem>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/service-item',
    method: 'post',
    data
  })
}

/** 修改服务项：PUT /admin-api/park/service-item/{id} */
export function updateServiceItem(id: number, data: Partial<ParkServiceItem>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/service-item/${id}`,
    method: 'put',
    data
  })
}

/** 删除服务项：DELETE /admin-api/park/service-item/{id} */
export function deleteServiceItem(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/service-item/${id}`,
    method: 'delete'
  })
}
