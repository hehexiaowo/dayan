import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ParkFacility, ParkFacilityQuery } from '@/types/park'

/**
 * 机构设施（ParkFacility）接口封装。
 *
 * 对应后端 /admin-api/park/facility/*。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - facilityCode 为业务编码（必填），update 时不可改。
 * - /list 只接 parkCode 一参，返回数组非分页。
 * - 价格已迁移至统一定价表（ParkPricing），见 api/park-pricing.ts。
 */

/** 设施分页：GET /admin-api/park/facility/page */
export function pageFacilities(query: ParkFacilityQuery): Promise<PageResult<ParkFacility>> {
  return request<PageResult<ParkFacility>>({
    url: '/admin-api/park/facility/page',
    method: 'get',
    params: query
  })
}

/** 设施列表（全量，按 parkCode 过滤）：GET /admin-api/park/facility/list?parkCode=xxx */
export function listFacilities(parkCode: string): Promise<ParkFacility[]> {
  return request<ParkFacility[]>({
    url: '/admin-api/park/facility/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 设施详情：GET /admin-api/park/facility/{id} */
export function getFacility(id: number): Promise<ParkFacility> {
  return request<ParkFacility>({
    url: `/admin-api/park/facility/${id}`,
    method: 'get'
  })
}

/** 新增设施：POST /admin-api/park/facility（返回新 id） */
export function createFacility(data: Partial<ParkFacility>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/facility',
    method: 'post',
    data
  })
}

/** 修改设施：PUT /admin-api/park/facility/{id} */
export function updateFacility(id: number, data: Partial<ParkFacility>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/facility/${id}`,
    method: 'put',
    data
  })
}

/** 删除设施：DELETE /admin-api/park/facility/{id} */
export function deleteFacility(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/facility/${id}`,
    method: 'delete'
  })
}
