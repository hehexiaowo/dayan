import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ParkAsset, ParkAssetQuery } from '@/types/park'

/**
 * 机构素材库 API（后端 /admin-api/park/asset）。
 *
 * 统一管理所有来源的图片/视频/文件/VR。
 * registerAsset 供 FileUploader 组件在其他 tab 上传后自动幂等注册。
 */

/** 素材分页查询 */
export function pageAssets(query: ParkAssetQuery): Promise<PageResult<ParkAsset>> {
  return request<PageResult<ParkAsset>>({ url: '/admin-api/park/asset/page', method: 'get', params: query })
}

/** 按机构编码查询全部素材（可选按类型过滤） */
export function listAssets(parkCode: string, assetType?: number): Promise<ParkAsset[]> {
  return request<ParkAsset[]>({ url: '/admin-api/park/asset/list', method: 'get', params: { parkCode, assetType } })
}

/** 素材详情 */
export function getAsset(id: number): Promise<ParkAsset> {
  return request<ParkAsset>({ url: `/admin-api/park/asset/${id}`, method: 'get' })
}

/** 新增素材 */
export function createAsset(data: Partial<ParkAsset>): Promise<number> {
  return request<number>({ url: '/admin-api/park/asset', method: 'post', data })
}

/** 修改素材 */
export function updateAsset(id: number, data: Partial<ParkAsset>): Promise<void> {
  return request<void>({ url: `/admin-api/park/asset/${id}`, method: 'put', data })
}

/** 删除素材 */
export function deleteAsset(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/park/asset/${id}`, method: 'delete' })
}

/**
 * 幂等注册素材（其他业务 tab 上传后自动注册）。
 * 同 (parkCode, assetUrl, sourceType, sourceRefCode) 已存在则返回已存 id。
 */
export function registerAsset(data: Partial<ParkAsset>): Promise<number> {
  return request<number>({ url: '/admin-api/park/asset/register', method: 'post', data })
}
