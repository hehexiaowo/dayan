import { request } from '@/utils/request'

/** 素材登记上下文：随上传一并提交，服务端同事务登记素材库 */
export interface AssetUploadContext {
  registerAsset?: boolean
  assetParkCode?: string
  assetType?: number
  assetSourceType?: string
  assetSourceRef?: string
}

/** 文件上传返回结构 */
export interface FileUploadDTO {
  url: string
  /** 完整 URL（富文本插图用，agent/client rich-text 零改写可直接渲染） */
  absoluteUrl?: string
  key: string
  originalName: string
  size: number
}

/** 上传文件：POST /admin-api/v1/files/upload（assetCtx 见 AssetUploadContext） */
export function uploadFile(file: File, module?: string, assetCtx?: AssetUploadContext): Promise<FileUploadDTO> {
  const formData = new FormData()
  formData.append('file', file)
  if (module) formData.append('module', module)
  if (assetCtx?.registerAsset) formData.append('assetRegister', 'true')
  if (assetCtx?.assetParkCode) formData.append('assetParkCode', assetCtx.assetParkCode)
  if (assetCtx?.assetType) formData.append('assetType', String(assetCtx.assetType))
  if (assetCtx?.assetSourceType) formData.append('assetSourceType', assetCtx.assetSourceType)
  if (assetCtx?.assetSourceRef) formData.append('assetSourceRef', assetCtx.assetSourceRef)
  return request<FileUploadDTO>({
    url: '/admin-api/v1/files/upload',
    method: 'post',
    data: formData,
    timeout: 60000
  })
}
