import type { PageQuery } from './common'

// ============================================================================
// 系统素材库（SystemAsset，system_asset）—— 系统级文件/外链资源统一管理
// 对应后端 com.dayan.system.vo.SystemAssetVO（/admin-api/system/asset）。
// ============================================================================

/**
 * 系统素材（后端 SystemAssetVO）。
 *
 * assetType 区分类型（1图片 2视频 3文件 4VR）；storageType 区分本地 OSS 与外链；
 * 类型专属字段按需填写；sourceType + sourceRefCode 追踪来源，media_mgmt=素材库直录。
 */
export interface SystemAsset {
  id?: number
  /** 归属机构编码（空=平台素材） */
  parkCode?: string
  /** 素材类型（1=图片 2=视频 3=文件 4=VR） */
  assetType?: number
  /** 存储方式（1=本地OSS 2=外链） */
  storageType?: number
  /** 资源地址：OSS key 或完整外链 URL */
  assetUrl?: string
  /** 文件名称 */
  assetName?: string
  /** 业务分类（图片:1-11 视频:1-3 文件:1-5 VR:1-3） */
  assetCategory?: number
  /** 描述 */
  description?: string
  /** 文件大小（字节，外链可空） */
  fileSize?: number
  // 图片专属
  width?: number
  height?: number
  isCover?: number
  // 视频专属
  coverUrl?: string
  duration?: number
  // 文件专属
  fileFormat?: string
  // VR 专属
  vrProvider?: string
  thumbnailUrl?: string
  // 来源追踪
  /** 来源（media_mgmt/room_type/food_type/facility_type/service_type/display_block/adviser/park_info） */
  sourceType?: string
  /** 来源编码（media_mgmt 时为空） */
  sourceRefCode?: string
  sortOrder?: number
  status?: number
  createdAt?: string
}

/** 素材分页查询参数 */
export interface SystemAssetQuery extends PageQuery {
  parkCode?: string
  /** 名称/URL 模糊搜索 */
  keyword?: string
  assetType?: number
  /** 存储方式（1=本地OSS 2=外链） */
  storageType?: number
  assetCategory?: number
  isCover?: number
  sourceType?: string
  status?: number
}

/** 素材类型（asset_type）：1=图片 2=视频 3=文件 4=VR */
export const ASSET_TYPE_OPTIONS = [
  { label: '图片', value: 1 },
  { label: '视频', value: 2 },
  { label: '文件', value: 3 },
  { label: 'VR', value: 4 }
] as const

/** 存储方式（storage_type）：1=本地OSS 2=外链 */
export const STORAGE_TYPE_OPTIONS = [
  { label: '本地OSS', value: 1 },
  { label: '外链', value: 2 }
] as const

/** 图片业务分类（asset_category, asset_type=1）：1=外观..11=其他 */
export const IMAGE_CATEGORY_OPTIONS = [
  { label: '外观', value: 1 },
  { label: '大堂', value: 2 },
  { label: '房间', value: 3 },
  { label: '餐厅', value: 4 },
  { label: '活动区', value: 5 },
  { label: '花园', value: 6 },
  { label: '医疗区', value: 7 },
  { label: '户型', value: 8 },
  { label: '文娱生活', value: 9 },
  { label: '康养状况', value: 10 },
  { label: '其他', value: 11 }
] as const

/** 视频业务分类（asset_category, asset_type=2）：1=宣传视频, 2=环境展示, 3=活动记录 */
export const VIDEO_CATEGORY_OPTIONS = [
  { label: '宣传视频', value: 1 },
  { label: '环境展示', value: 2 },
  { label: '活动记录', value: 3 }
] as const

/** 文件业务分类（asset_category, asset_type=3）：1=资质文件..5=其他 */
export const FILE_CATEGORY_OPTIONS = [
  { label: '资质文件', value: 1 },
  { label: '合同文件', value: 2 },
  { label: '宣传资料', value: 3 },
  { label: '费用文档', value: 4 },
  { label: '其他', value: 5 }
] as const

/** VR 业务分类（asset_category, asset_type=4）：1=全景VR, 2=3D模型, 3=视频VR */
export const VR_CATEGORY_OPTIONS = [
  { label: '全景VR', value: 1 },
  { label: '3D模型', value: 2 },
  { label: '视频VR', value: 3 }
] as const

/** 素材来源（source_type） */
export const SOURCE_TYPE_OPTIONS = [
  { label: '素材库', value: 'media_mgmt' },
  { label: '房型', value: 'room_type' },
  { label: '餐饮', value: 'food_type' },
  { label: '设施', value: 'facility_type' },
  { label: '服务项目', value: 'service_type' },
  { label: '展示板块', value: 'display_block' },
  { label: '顾问', value: 'adviser' },
  { label: '机构信息', value: 'park_info' }
] as const

// ---- label 映射函数（列表渲染用，找不到回退原始值或 '--'）----

/** 通用：按 OPTIONS 数组查 label */
function labelOf(options: ReadonlyArray<{ label: string; value: number }>, v?: number): string {
  if (v == null) return '--'
  const found = options.find((o) => o.value === v)
  return found ? found.label : String(v)
}

export const assetTypeLabel = (v?: number) => labelOf(ASSET_TYPE_OPTIONS, v)
export const storageTypeLabel = (v?: number) => labelOf(STORAGE_TYPE_OPTIONS, v)
export const imageCategoryLabel = (v?: number) => labelOf(IMAGE_CATEGORY_OPTIONS, v)
export const videoCategoryLabel = (v?: number) => labelOf(VIDEO_CATEGORY_OPTIONS, v)
export const fileCategoryLabel = (v?: number) => labelOf(FILE_CATEGORY_OPTIONS, v)
export const vrCategoryLabel = (v?: number) => labelOf(VR_CATEGORY_OPTIONS, v)
/** 按 assetType 返回对应的分类 OPTIONS */
export function categoryOptionsByType(assetType?: number) {
  switch (assetType) {
    case 1: return IMAGE_CATEGORY_OPTIONS
    case 2: return VIDEO_CATEGORY_OPTIONS
    case 3: return FILE_CATEGORY_OPTIONS
    case 4: return VR_CATEGORY_OPTIONS
    default: return []
  }
}
/** 按 assetType 返回对应的分类 label */
export function categoryLabel(assetType?: number, category?: number): string {
  return labelOf(categoryOptionsByType(assetType), category)
}
/** 来源 label */
export function sourceTypeLabel(v?: string): string {
  if (!v) return '素材库'
  const found = SOURCE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v
}
/** 文件大小格式化：B → KB/MB 友好显示 */
export function fileSizeLabel(bytes?: number): string {
  if (bytes == null) return '--'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}
