import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ParkFacilityType, ParkFacilityTypeQuery } from '@/types/park'

/**
 * 机构设施类型（ParkFacilityType）接口封装。
 *
 * 对应后端 /admin-api/park/facility-type/*。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - facilityTypeCode 为业务编码（必填），update 时不可改。
 * - /list 只接 parkCode 一参，返回数组非分页。
 * - 价格已迁移至统一定价表（ParkPricing），见 api/park-pricing.ts。
 */

/** 设施类型分页：GET /admin-api/park/facility-type/page */
export function pageFacilityTypes(query: ParkFacilityTypeQuery): Promise<PageResult<ParkFacilityType>> {
  return request<PageResult<ParkFacilityType>>({
    url: '/admin-api/park/facility-type/page',
    method: 'get',
    params: query
  })
}

/** 设施类型列表（全量，按 parkCode 过滤）：GET /admin-api/park/facility-type/list?parkCode=xxx */
export function listFacilityTypes(parkCode: string): Promise<ParkFacilityType[]> {
  return request<ParkFacilityType[]>({
    url: '/admin-api/park/facility-type/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 设施类型详情：GET /admin-api/park/facility-type/{id} */
export function getFacilityType(id: number): Promise<ParkFacilityType> {
  return request<ParkFacilityType>({
    url: `/admin-api/park/facility-type/${id}`,
    method: 'get'
  })
}

/** 新增设施类型：POST /admin-api/park/facility-type（返回新 id） */
export function createFacilityType(data: Partial<ParkFacilityType>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/facility-type',
    method: 'post',
    data
  })
}

/** 修改设施类型：PUT /admin-api/park/facility-type/{id} */
export function updateFacilityType(id: number, data: Partial<ParkFacilityType>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/facility-type/${id}`,
    method: 'put',
    data
  })
}

/** 删除设施类型：DELETE /admin-api/park/facility-type/{id} */
export function deleteFacilityType(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/facility-type/${id}`,
    method: 'delete'
  })
}
