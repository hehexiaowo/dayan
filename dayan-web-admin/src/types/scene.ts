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

// ==================== 子表：项目明细（scene_item）====================

/** 场景项目类型：1体验/2讲座/3互动/4餐饮/5检测/6赠品 */
export const SCENE_ITEM_TYPE_OPTIONS = [
  { label: '体验项目', value: 1 },
  { label: '讲座环节', value: 2 },
  { label: '互动游戏', value: 3 },
  { label: '餐饮服务', value: 4 },
  { label: '检测项目', value: 5 },
  { label: '赠品', value: 6 }
] as const

/** 通用启用/禁用（item/price/resource 三子表共用） */
export const COMMON_ENABLE_STATUS_OPTIONS = [
  { label: '禁用', value: 0 },
  { label: '启用', value: 1 }
] as const

/** 场景项目实体（后端 SceneItemVO） */
export interface SceneItem {
  id?: number
  sceneCode: string
  itemCode: string
  itemName: string
  itemType: number
  itemDescription?: string
  durationMinutes?: number
  sortOrder?: number
  /** 是否必选：1是 0否 */
  isRequired: number
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface SceneItemQuery extends PageQuery {
  sceneCode?: string
  itemCode?: string
  itemName?: string
  itemType?: number
  status?: number
}

// ==================== 子表：价格档位（scene_item_price）====================

/** 定价类型：1按人/2按组/3按场 */
export const SCENE_PRICE_TYPE_OPTIONS = [
  { label: '按人', value: 1 },
  { label: '按组', value: 2 },
  { label: '按场', value: 3 }
] as const

export interface SceneItemPrice {
  id?: number
  sceneCode: string
  sceneItemCode: string
  priceType: number
  /** 原价（表单态可空，提交时由 rules 强制必填） */
  originalPrice?: number
  /** 售价（表单态可空，提交时由 rules 强制必填） */
  salePrice?: number
  channelPrice?: number
  priceDescription?: string
  effectiveDate?: string
  expireDate?: string
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface SceneItemPriceQuery extends PageQuery {
  sceneCode?: string
  sceneItemCode?: string
  priceType?: number
  status?: number
}

// ==================== 子表：活动日程（scene_schedule）====================

/**
 * 活动日程状态：0已取消/1可预约/2已约满/3进行中/4已结束。
 * 权威来源为 DDL 5 态（VO 注释只写了 3 态是过时信息）。
 */
export const SCENE_SCHEDULE_STATUS_OPTIONS = [
  { label: '已取消', value: 0 },
  { label: '可预约', value: 1 },
  { label: '已约满', value: 2 },
  { label: '进行中', value: 3 },
  { label: '已结束', value: 4 }
] as const

export interface SceneSchedule {
  id?: number
  sceneCode: string
  scheduleDate: string
  startTime: string
  endTime: string
  maxPerson: number
  /** 当前已预约人数（与 maxPerson 联动，后端有乐观锁自动状态机，编辑时不手改） */
  currentPerson: number
  priceOverride?: number
  remark?: string
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface SceneScheduleQuery extends PageQuery {
  sceneCode?: string
  status?: number
  scheduleDate?: string
}

// ==================== 子表：所需资源（scene_resource）====================

/** 资源类型：1场地/2设备/3物料/4人员/5餐饮 */
export const SCENE_RESOURCE_TYPE_OPTIONS = [
  { label: '场地', value: 1 },
  { label: '设备', value: 2 },
  { label: '物料', value: 3 },
  { label: '人员', value: 4 },
  { label: '餐饮', value: 5 }
] as const

export interface SceneResource {
  id?: number
  sceneCode: string
  resourceType: number
  resourceName: string
  resourceDescription?: string
  quantity?: number
  unit?: string
  unitCost?: number
  /** 是否由平台提供：1是 0否 */
  isProvided: number
  sortOrder?: number
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface SceneResourceQuery extends PageQuery {
  sceneCode?: string
  resourceType?: number
  resourceName?: string
  status?: number
}
