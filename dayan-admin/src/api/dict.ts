import { request } from '@/utils/request'
import type { SystemDictCommon } from '@/types/dict'

/**
 * 字典接口封装。
 *
 * 对应后端 SystemDictAdminController（/admin-api/dicts/*）。
 * 字典为只读数据：后端仅提供查询，无新增/修改/删除（由 seed 初始化）。
 */

/** 按字典类型查询字典项列表：GET /admin-api/dicts/type/{dictType} */
export function listDictByType(dictType: string): Promise<SystemDictCommon[]> {
  return request<SystemDictCommon[]>({
    url: `/admin-api/dicts/type/${dictType}`,
    method: 'get'
  })
}

/** 查询单条字典详情：GET /admin-api/dicts/{dictType}/{dictCode} */
export function getDictDetail(dictType: string, dictCode: string): Promise<SystemDictCommon> {
  return request<SystemDictCommon>({
    url: `/admin-api/dicts/${dictType}/${dictCode}`,
    method: 'get'
  })
}
