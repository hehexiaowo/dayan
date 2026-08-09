import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ParkCareType,
  ParkCareTypeQuery
} from '@/types/park'

/**
 * 机构照护类型（ParkCareType）接口封装。
 *
 * 对应后端 /admin-api/park/care-type/*。
 *
 * 注意：
 * - 主键为自增 id（Long），useCrud 传 idKey:'id'。
 * - careTypeCode 为业务编码，由录入方提供，非系统生成；update 时不可改。
 * - 价格已迁移至统一定价表（ParkPricing），见 api/park-pricing.ts。
 */

// ---------------- 照护类型（care-type）----------------

/** 照护类型分页：GET /admin-api/park/care-type/page */
export function pageCareTypes(query: ParkCareTypeQuery): Promise<PageResult<ParkCareType>> {
  return request<PageResult<ParkCareType>>({
    url: '/admin-api/park/care-type/page',
    method: 'get',
    params: query
  })
}

/** 照护类型列表（全量，按 parkCode 过滤）：GET /admin-api/park/care-type/list?parkCode=xxx */
export function listCareTypes(parkCode: string): Promise<ParkCareType[]> {
  return request<ParkCareType[]>({
    url: '/admin-api/park/care-type/list',
    method: 'get',
    params: { parkCode }
  })
}

/** 照护类型详情：GET /admin-api/park/care-type/{id} */
export function getCareType(id: number): Promise<ParkCareType> {
  return request<ParkCareType>({
    url: `/admin-api/park/care-type/${id}`,
    method: 'get'
  })
}

/** 新增照护类型：POST /admin-api/park/care-type（返回新 id） */
export function createCareType(data: Partial<ParkCareType>): Promise<number> {
  return request<number>({
    url: '/admin-api/park/care-type',
    method: 'post',
    data
  })
}

/** 修改照护类型：PUT /admin-api/park/care-type/{id} */
export function updateCareType(id: number, data: Partial<ParkCareType>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/care-type/${id}`,
    method: 'put',
    data
  })
}

/** 删除照护类型：DELETE /admin-api/park/care-type/{id} */
export function deleteCareType(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/park/care-type/${id}`,
    method: 'delete'
  })
}
