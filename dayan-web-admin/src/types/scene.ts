/**
 * 场景活动相关类型。
 *
 * 字段对齐后端 com.dayan.scene.vo.SceneInfoVO 及 DTO。
 */
import type { PageQuery } from '@/types/common'

/** 场景类型：8 类场景活动 */
export enum SceneType {
  /** 参观体验 */
  VISIT = 1,
  /** 健康讲座 */
  LECTURE = 2,
  /** 亲子互动 */
  FAMILY = 3,
  /** 节日活动 */
  FESTIVAL = 4,
  /** 文化娱乐 */
  CULTURE = 5,
  /** 健康检测 */
  HEALTH = 6,
  /** 美食品鉴 */
  FOOD = 7,
  /** 其他 */
  OTHER = 8
}

/** 场景类型选项 */
export const SCENE_TYPE_OPTIONS = [
  { label: '参观体验', value: SceneType.VISIT },
  { label: '健康讲座', value: SceneType.LECTURE },
  { label: '亲子互动', value: SceneType.FAMILY },
  { label: '节日活动', value: SceneType.FESTIVAL },
  { label: '文化娱乐', value: SceneType.CULTURE },
  { label: '健康检测', value: SceneType.HEALTH },
  { label: '美食品鉴', value: SceneType.FOOD },
  { label: '其他', value: SceneType.OTHER }
] as const

/** 场景状态：0草稿/1已上架/2已下架/3已满期 */
export enum SceneStatus {
  DRAFT = 0,
  PUBLISHED = 1,
  OFFLINE = 2,
  FULL = 3
}

/** 场景状态选项 */
export const SCENE_STATUS_OPTIONS = [
  { label: '草稿', value: SceneStatus.DRAFT },
  { label: '已上架', value: SceneStatus.PUBLISHED },
  { label: '已下架', value: SceneStatus.OFFLINE },
  { label: '已满期', value: SceneStatus.FULL }
] as const

/** 审核状态：0待审核/1通过/2驳回 */
export enum AuditStatus {
  PENDING = 0,
  PASS = 1,
  REJECT = 2
}

/** 审核状态选项 */
export const AUDIT_STATUS_OPTIONS = [
  { label: '待审核', value: AuditStatus.PENDING },
  { label: '审核通过', value: AuditStatus.PASS },
  { label: '审核驳回', value: AuditStatus.REJECT }
] as const

/**
 * 场景信息实体（后端 SceneInfoVO）。
 */
export interface SceneInfo {
  id?: number
  /** 场景编码（SC 前缀，系统生成） */
  sceneCode?: string
  sceneName: string
  sceneType: SceneType
  /** 关联养老机构编码 */
  parkCode?: string
  provinceCode?: string
  cityCode?: string
  districtCode?: string
  address?: string
  sceneDescription?: string
  coverImage?: string
  /** 图集，JSON 字符串或逗号分隔 */
  imageUrls?: string
  videoUrl?: string
  /** 容量 */
  capacity?: number
  /** 时长（小时） */
  durationHours?: number
  targetAudience?: string
  highlight?: string
  notice?: string
  /** 最小人数 */
  minPerson?: number
  /** 最大人数 */
  maxPerson?: number
  originalPrice?: number
  salePrice?: number
  priceUnit?: string
  /** 是否免费：1是 0否 */
  isFree?: number
  sortOrder?: number
  viewCount?: number
  bookCount?: number
  /** 场景状态：0草稿/1已上架/2已下架/3已满期 */
  sceneStatus?: SceneStatus
  /** 审核状态：0待审核/1通过/2驳回 */
  auditStatus?: AuditStatus
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 场景分页查询参数（后端 SceneInfoQueryDTO）。
 *
 * 后端字段：current / size / sceneCode / sceneName / sceneType / parkCode / sceneStatus / auditStatus。
 */
export interface SceneInfoQuery extends PageQuery {
  sceneCode?: string
  sceneName?: string
  sceneType?: SceneType
  parkCode?: string
  sceneStatus?: SceneStatus
  auditStatus?: AuditStatus
}
