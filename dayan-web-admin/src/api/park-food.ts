import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ParkFoodType,
  ParkFoodTypeQuery,
  ParkFoodPrice,
  ParkFoodPriceQuery
} from '@/types/park'

/**
 * 机构餐饮类型（ParkFoodType）+ 餐饮价格（ParkFoodPrice）接口封装。
 *
 * 对应后端 /admin-api/park/food-type/* 与 /admin-api/park/food-price/*。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - foodTypeCode 为业务编码，由录入方提供，非系统生成；update 时不可改。
 * - price 表展开行专用 /list（parkCode + foodTypeCode 两参必填），返回数组非分页。
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

// ---------------- 餐饮价格（food-price）----------------

/** 餐饮价格分页：GET /admin-api/park/food-price/page */
export function pageFoodPrices(query: ParkFoodPriceQuery): Promise<PageResult<ParkFoodPrice>> {
  return request<PageResult<ParkFoodPrice>>({
    url: '/admin-api/park/food-price/page',
    method: 'get',
    params: query
  })
}

/**
 * 餐饮价格列表（展开行专用）：GET /admin-api/park/food-price/list
 *
 * parkCode + foodTypeCode 两参必填，返回数组（非分页）。
 */
export function listFoodPrices(parkCode: string, foodTypeCode: string): Promise<ParkFoodPrice[]> {
  return request<ParkFoodPrice[]>({
    url: '/admin-api/park/food-price/list',
    method: 'get',
    params: { parkCode, foodTypeCode }
  })
}

/** 餐饮价格详情：GET /admin-api/park/food-price/{id} */
export function getFoodPrice(id: number): Promise<ParkFoodPrice> {
  return request<ParkFoodPrice>({
    url: `/admin-api/park/food-price/${id}`,
    method: 'get'
  })
}

/** 新增餐饮价格：POST /admin-api/park/food-price */
export function createFoodPrice(data: Partial<ParkFoodPrice>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/food-price',
    method: 'post',
    data
  })
}

/** 修改餐饮价格：PUT /admin-api/park/food-price/{id} */
export function updateFoodPrice(id: number, data: Partial<ParkFoodPrice>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/food-price/${id}`,
    method: 'put',
    data
  })
}

/** 删除餐饮价格：DELETE /admin-api/park/food-price/{id} */
export function deleteFoodPrice(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/food-price/${id}`,
    method: 'delete'
  })
}
