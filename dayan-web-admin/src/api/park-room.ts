import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ParkRoomType,
  ParkRoomTypeQuery,
  ParkRoomPrice,
  ParkRoomPriceQuery
} from '@/types/park'

/**
 * 机构房型（ParkRoomType）+ 房型价格（ParkRoomPrice）接口封装。
 *
 * 对应后端 /admin-api/park/room-type/* 与 /admin-api/park/room-price/*。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - roomTypeCode 为业务编码，由录入方提供，非系统生成；update 时不可改。
 * - price 表展开行专用 /list（parkCode + roomTypeCode 两参必填），返回数组非分页。
 */

// ---------------- 房型类型（room-type）----------------

/** 房型分页：GET /admin-api/park/room-type/page */
export function pageRoomTypes(query: ParkRoomTypeQuery): Promise<PageResult<ParkRoomType>> {
  return request<PageResult<ParkRoomType>>({
    url: '/admin-api/park/room-type/page',
    method: 'get',
    params: query
  })
}

/** 房型列表（全量，按 parkCode 过滤）：GET /admin-api/park/room-type/list?parkCode=xxx */
export function listRoomTypes(parkCode: string): Promise<ParkRoomType[]> {
  return request<ParkRoomType[]>({
    url: '/admin-api/park/room-type/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 房型详情：GET /admin-api/park/room-type/{id} */
export function getRoomType(id: number): Promise<ParkRoomType> {
  return request<ParkRoomType>({
    url: `/admin-api/park/room-type/${id}`,
    method: 'get'
  })
}

/** 新增房型：POST /admin-api/park/room-type（返回新 id） */
export function createRoomType(data: Partial<ParkRoomType>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/room-type',
    method: 'post',
    data
  })
}

/** 修改房型：PUT /admin-api/park/room-type/{id} */
export function updateRoomType(id: number, data: Partial<ParkRoomType>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/room-type/${id}`,
    method: 'put',
    data
  })
}

/** 删除房型：DELETE /admin-api/park/room-type/{id} */
export function deleteRoomType(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/room-type/${id}`,
    method: 'delete'
  })
}

// ---------------- 房型价格（room-price）----------------

/** 房型价格分页：GET /admin-api/park/room-price/page */
export function pageRoomPrices(query: ParkRoomPriceQuery): Promise<PageResult<ParkRoomPrice>> {
  return request<PageResult<ParkRoomPrice>>({
    url: '/admin-api/park/room-price/page',
    method: 'get',
    params: query
  })
}

/**
 * 房型价格列表（展开行专用）：GET /admin-api/park/room-price/list
 *
 * parkCode + roomTypeCode 两参必填，返回数组（非分页）。
 */
export function listRoomPrices(parkCode: string, roomTypeCode: string): Promise<ParkRoomPrice[]> {
  return request<ParkRoomPrice[]>({
    url: '/admin-api/park/room-price/list',
    method: 'get',
    params: { parkCode, roomTypeCode }
  })
}

/** 房型价格详情：GET /admin-api/park/room-price/{id} */
export function getRoomPrice(id: number): Promise<ParkRoomPrice> {
  return request<ParkRoomPrice>({
    url: `/admin-api/park/room-price/${id}`,
    method: 'get'
  })
}

/** 新增房型价格：POST /admin-api/park/room-price */
export function createRoomPrice(data: Partial<ParkRoomPrice>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/room-price',
    method: 'post',
    data
  })
}

/** 修改房型价格：PUT /admin-api/park/room-price/{id} */
export function updateRoomPrice(id: number, data: Partial<ParkRoomPrice>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/room-price/${id}`,
    method: 'put',
    data
  })
}

/** 删除房型价格：DELETE /admin-api/park/room-price/{id} */
export function deleteRoomPrice(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/room-price/${id}`,
    method: 'delete'
  })
}
