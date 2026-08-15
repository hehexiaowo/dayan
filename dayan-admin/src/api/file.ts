import { request } from '@/utils/request'

/** 素材登记上下文：随上传一并提交，服务端同事务登记素材仓库 */
export interface AssetUploadContext {
  registerAsset?: boolean
  /** 类型1：业务维度（park/platform/goods/content/course/scene，空=platform） */
  assetRefType1?: string
  /** 关联编码：业务实体编码（如机构编码/商品编码，空=无关联） */
  assetRefCode?: string
  assetType?: number
  /** 类型2：细分分类（如 room_type/display_block，空=media_mgmt） */
  assetRefType2?: string
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
  if (assetCtx?.assetRefType1) formData.append('assetRefType1', assetCtx.assetRefType1)
  if (assetCtx?.assetRefCode) formData.append('assetRefCode', assetCtx.assetRefCode)
  if (assetCtx?.assetType) formData.append('assetType', String(assetCtx.assetType))
  if (assetCtx?.assetRefType2) formData.append('assetRefType2', assetCtx.assetRefType2)
  return request<FileUploadDTO>({
    url: '/admin-api/v1/files/upload',
    method: 'post',
    data: formData,
    timeout: 60000
  })
}
