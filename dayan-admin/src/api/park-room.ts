import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ParkRoomType,
  ParkRoomTypeQuery
} from '@/types/park'

/**
 * 机构房型（ParkRoomType）接口封装。
 *
 * 对应后端 /admin-api/park/room-type/*。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - roomTypeCode 为业务编码，由录入方提供，非系统生成；update 时不可改。
 * - 价格已迁移至统一定价表（ParkPricing），见 api/park-pricing.ts。
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
