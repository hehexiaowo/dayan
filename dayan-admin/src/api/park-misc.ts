import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ParkPeriphery,
  ParkPeripheryQuery,
  ParkServiceType,
  ParkServiceTypeQuery
} from '@/types/park'

/**
 * 机构周边配套（ParkPeriphery）+ 服务类型（ParkServiceType）接口封装。
 *
 * 对应后端 /admin-api/park/periphery 与 /admin-api/park/service-type。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - periphery 的 placeName 必填；service-type 的 serviceTypeCode（必填，update 不可改）/ serviceTypeName 必填。
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

// ---------------- 服务类型（service-type）----------------

/** 服务类型分页：GET /admin-api/park/service-type/page */
export function pageServiceTypes(query: ParkServiceTypeQuery): Promise<PageResult<ParkServiceType>> {
  return request<PageResult<ParkServiceType>>({
    url: '/admin-api/park/service-type/page',
    method: 'get',
    params: query
  })
}

/** 服务类型列表（全量，按 parkCode 过滤）：GET /admin-api/park/service-type/list?parkCode=xxx */
export function listServiceTypes(parkCode: string): Promise<ParkServiceType[]> {
  return request<ParkServiceType[]>({
    url: '/admin-api/park/service-type/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 服务类型详情：GET /admin-api/park/service-type/{id} */
export function getServiceType(id: number): Promise<ParkServiceType> {
  return request<ParkServiceType>({
    url: `/admin-api/park/service-type/${id}`,
    method: 'get'
  })
}

/** 新增服务类型：POST /admin-api/park/service-type（返回新 id） */
export function createServiceType(data: Partial<ParkServiceType>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/service-type',
    method: 'post',
    data
  })
}

/** 修改服务类型：PUT /admin-api/park/service-type/{id} */
export function updateServiceType(id: number, data: Partial<ParkServiceType>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/service-type/${id}`,
    method: 'put',
    data
  })
}

/** 删除服务类型：DELETE /admin-api/park/service-type/{id} */
export function deleteServiceType(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/service-type/${id}`,
    method: 'delete'
  })
}
