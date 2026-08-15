import { request } from '@/utils/request'
import type { SystemDict } from '@/types/dict'

/**
 * 字典接口封装。
 *
 * 对应后端 SystemDictAdminController（/admin-api/dicts/*，统一单表 system_dict）。
 * 查询：listByType（业务消费，仅启用）/ listAllByType（管理页，含禁用）/ listTypes（类型枚举）。
 * 管理：字典项 CRUD（写入后后端失效缓存）。
 */

/** 全部字典类型枚举：GET /admin-api/dicts/types */
export function listDictTypes(): Promise<string[]> {
  return request<string[]>({ url: '/admin-api/dicts/types', method: 'get' })
}

/** 按类型查询字典项列表（仅启用，业务消费用）：GET /admin-api/dicts/type/{dictType} */
export function listDictByType(dictType: string): Promise<SystemDict[]> {
  return request<SystemDict[]>({
    url: `/admin-api/dicts/type/${dictType}`,
    method: 'get'
  })
}

/** 按类型查询全部字典项（含禁用，管理页用）：GET /admin-api/dicts/type/{dictType}/all */
export function listAllDictByType(dictType: string): Promise<SystemDict[]> {
  return request<SystemDict[]>({
    url: `/admin-api/dicts/type/${dictType}/all`,
    method: 'get'
  })
}

/** 查询单条字典详情：GET /admin-api/dicts/{dictType}/{dictCode} */
export function getDictDetail(dictType: string, dictCode: string): Promise<SystemDict> {
  return request<SystemDict>({
    url: `/admin-api/dicts/${dictType}/${dictCode}`,
    method: 'get'
  })
}

/** 新增字典项：POST /admin-api/dicts */
export function createDict(data: Partial<SystemDict>): Promise<number> {
  return request<number>({ url: '/admin-api/dicts', method: 'post', data })
}

/** 修改字典项：PUT /admin-api/dicts/{id} */
export function updateDict(id: number, data: Partial<SystemDict>): Promise<void> {
  return request<void>({ url: `/admin-api/dicts/${id}`, method: 'put', data })
}

/** 删除字典项：DELETE /admin-api/dicts/{id} */
export function deleteDict(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/dicts/${id}`, method: 'delete' })
}

