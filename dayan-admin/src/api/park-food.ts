import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ParkFoodType,
  ParkFoodTypeQuery
} from '@/types/park'

/**
 * 机构餐饮类型（ParkFoodType）接口封装。
 *
 * 对应后端 /admin-api/park/food-type/*。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - foodTypeCode 为业务编码，由录入方提供，非系统生成；update 时不可改。
 * - 价格已迁移至统一定价表（ParkPricing），见 api/park-pricing.ts。
 */

// ---------------- 餐饮类型（food-type）----------------

/** 餐饮类型分页：GET /admin-api/park/food-type/page */
export function pageFoodTypes(query: ParkFoodTypeQuery): Promise<PageResult<ParkFoodType>> {
  return request<PageResult<ParkFoodType>>({
    url: '/admin-api/park/food-type/page',
    method: 'get',
    params: query
  })
}

/** 餐饮类型列表（全量，按 parkCode 过滤）：GET /admin-api/park/food-type/list?parkCode=xxx */
export function listFoodTypes(parkCode: string): Promise<ParkFoodType[]> {
  return request<ParkFoodType[]>({
    url: '/admin-api/park/food-type/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 餐饮类型详情：GET /admin-api/park/food-type/{id} */
export function getFoodType(id: number): Promise<ParkFoodType> {
  return request<ParkFoodType>({
    url: `/admin-api/park/food-type/${id}`,
    method: 'get'
  })
}

/** 新增餐饮类型：POST /admin-api/park/food-type（返回新 id） */
export function createFoodType(data: Partial<ParkFoodType>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/food-type',
    method: 'post',
    data
  })
}

/** 修改餐饮类型：PUT /admin-api/park/food-type/{id} */
export function updateFoodType(id: number, data: Partial<ParkFoodType>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/food-type/${id}`,
    method: 'put',
    data
  })
}

/** 删除餐饮类型：DELETE /admin-api/park/food-type/{id} */
export function deleteFoodType(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/food-type/${id}`,
    method: 'delete'
  })
}
