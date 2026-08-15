import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SystemAsset, SystemAssetQuery } from '@/types/asset'

/**
 * 系统素材仓库 API（后端 /admin-api/system/asset）。
 *
 * 统一管理整个系统的文件与外链资源（本地 OSS 对象 / 外部存储链接）。
 */

/** 素材分页查询 */
export function pageAssets(query: SystemAssetQuery): Promise<PageResult<SystemAsset>> {
  return request<PageResult<SystemAsset>>({ url: '/admin-api/system/asset/page', method: 'get', params: query })
}

/** 按机构编码查询全部素材（可选按类型过滤） */
export function listAssets(parkCode: string, assetType?: number): Promise<SystemAsset[]> {
  return request<SystemAsset[]>({ url: '/admin-api/system/asset/list', method: 'get', params: { parkCode, assetType } })
}

/** 素材详情 */
export function getAsset(id: number): Promise<SystemAsset> {
  return request<SystemAsset>({ url: `/admin-api/system/asset/${id}`, method: 'get' })
}

/** 新增素材（本地OSS 或 外链） */
export function createAsset(data: Partial<SystemAsset>): Promise<number> {
  return request<number>({ url: '/admin-api/system/asset', method: 'post', data })
}

/** 修改素材 */
export function updateAsset(id: number, data: Partial<SystemAsset>): Promise<void> {
  return request<void>({ url: `/admin-api/system/asset/${id}`, method: 'put', data })
}

/** 删除素材 */
export function deleteAsset(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/system/asset/${id}`, method: 'delete' })
}
