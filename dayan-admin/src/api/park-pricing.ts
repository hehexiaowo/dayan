import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ParkPricing, ParkPricingQuery } from '@/types/park'

/**
 * 机构统一定价（ParkPricing）接口封装。
 *
 * 对应后端 /admin-api/park/pricing/*。
 *
 * 设计要点：
 * - chargeType 区分费类（1房间 2照护 3餐费 4押金 5设施 6服务 9其他）。
 * - refType + refCode 关联具体 type 表（room_type/care_type/food_type/facility_type/service_type）。
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - 展开行专用 /list（parkCode + refType + refCode 三参必填），返回数组非分页。
 */

/** 定价分页：GET /admin-api/park/pricing/page */
export function pagePricings(query: ParkPricingQuery): Promise<PageResult<ParkPricing>> {
  return request<PageResult<ParkPricing>>({
    url: '/admin-api/park/pricing/page',
    method: 'get',
    params: query
  })
}

/**
 * 定价列表（展开行专用）：GET /admin-api/park/pricing/list
 *
 * parkCode + refType + refCode 三参必填，返回数组（非分页）。
 * 替代旧的 listRoomPrices / listCarePrices / listFoodPrices 等。
 */
export function listPricingsByRef(
  parkCode: string,
  refType: string,
  refCode: string
): Promise<ParkPricing[]> {
  return request<ParkPricing[]>({
    url: '/admin-api/park/pricing/list',
    method: 'get',
    params: { parkCode, refType, refCode }
  })
}

/** 定价详情：GET /admin-api/park/pricing/{id} */
export function getPricing(id: number): Promise<ParkPricing> {
  return request<ParkPricing>({
    url: `/admin-api/park/pricing/${id}`,
    method: 'get'
  })
}

/** 新增定价：POST /admin-api/park/pricing（返回新 id） */
export function createPricing(data: Partial<ParkPricing>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/pricing',
    method: 'post',
    data
  })
}

/** 修改定价：PUT /admin-api/park/pricing/{id} */
export function updatePricing(id: number, data: Partial<ParkPricing>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/pricing/${id}`,
    method: 'put',
    data
  })
}

/** 删除定价：DELETE /admin-api/park/pricing/{id} */
export function deletePricing(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/pricing/${id}`,
    method: 'delete'
  })
}
