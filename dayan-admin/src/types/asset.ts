import type { PageQuery } from './common'
import { labelOf } from './common'

// ============================================================================
// 系统素材仓库（SystemAsset，system_asset）—— 全系统文件/外链资源登记中心
// 对应后端 com.dayan.system.vo.SystemAssetVO（/admin-api/system/asset）。
//
// 模型：本表只存地址与冗余分类三元组（类型1/类型2/关联编码），
// 真实引用关系由各业务表持有（删除保护按 AssetRefMap 反查业务表）。
// ============================================================================

/**
 * 系统素材（后端 SystemAssetVO）。
 *
 * assetType 区分媒体类型（1图片 2视频 3文件 4VR）；storageType 区分本地 OSS 与外链；
 * refType1/refType2/refCode 为冗余分类三元组（业务维度/细分分类/关联编码）。
 */
export interface SystemAsset {
  id?: number
  /** 素材类型（1=图片 2=视频 3=文件 4=VR） */
  assetType?: number
  /** 类型1：业务维度（park/platform/goods/content/course/scene） */
  refType1?: string
  /** 类型2：细分分类（字典 asset_ref_type2，如 room_type/display_block） */
  refType2?: string
  /** 关联编码：业务实体编码（机构编码/商品编码等；平台素材为空） */
  refCode?: string
  /** 存储方式（1=本地OSS 2=外链） */
  storageType?: number
  /** 资源地址：OSS key 或完整外链 URL */
  assetUrl?: string
  /** 文件名称 */
  assetName?: string
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
  sortOrder?: number
  status?: number
  createdAt?: string
}

/** 素材分页查询参数 */
export interface SystemAssetQuery extends PageQuery {
  /** 名称/URL 模糊搜索 */
  keyword?: string
  assetType?: number
  refType1?: string
  refType2?: string
  refCode?: string
  /** 存储方式（1=本地OSS 2=外链） */
  storageType?: number
  isCover?: number
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

/** 类型1（ref_type1）：业务维度——素材登记中心不绑定单一业务，park 只是维度之一 */
export const REF_TYPE1_OPTIONS = [
  { label: '机构素材', value: 'park' },
  { label: '平台素材', value: 'platform' },
  { label: '商品素材', value: 'goods' },
  { label: '内容素材', value: 'content' },
  { label: '课程素材', value: 'course' },
  { label: '场景素材', value: 'scene' }
] as const

// ---- label 映射函数（列表渲染用，找不到回退原始值或 '--'）----

export const assetTypeLabel = (v?: number) => labelOf(ASSET_TYPE_OPTIONS, v)
export const storageTypeLabel = (v?: number) => labelOf(STORAGE_TYPE_OPTIONS, v)

/** 类型1 label（按 REF_TYPE1_OPTIONS，未知值回退原值） */
export function refType1Label(v?: string): string {
  if (!v) return '--'
  const found = REF_TYPE1_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v
}

/** 类型2 label（字典 asset_ref_type2 的 dictName 映射，未知值回退原值） */
export function refType2Label(dictOptions: ReadonlyArray<{ dictCode: string; dictName: string }>, v?: string): string {
  if (!v) return '--'
  const found = dictOptions.find((o) => o.dictCode === v)
  return found ? found.dictName : v
}

/** 文件大小格式化：B → KB/MB 友好显示 */
export function fileSizeLabel(bytes?: number): string {
  if (bytes == null) return '--'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}
