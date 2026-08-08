import type { PageQuery } from './common'

/** 场景信息 */
export interface SceneInfo {
  id?: number
  sceneCode: string
  sceneName: string
  sceneType?: number
  parkCode?: string
  parkName?: string
  description?: string
  coverImage?: string
  capacity?: number
  usedCapacity?: number
  sceneStatus?: number
  auditStatus?: number
  startTime?: string
  endTime?: string
  createdAt?: string
  updatedAt?: string
}

/** 场景查询 */
export interface SceneInfoQuery extends PageQuery {
  sceneCode?: string
  sceneName?: string
  sceneType?: number
  parkCode?: string
  sceneStatus?: number
  auditStatus?: number
}

/** 场景类型选项 */
export const SCENE_TYPE_OPTIONS = [
  { value: 1, label: '线下活动' },
  { value: 2, label: '线上直播' },
  { value: 3, label: '体验探访' }
]

/** 场景状态选项 */
export const SCENE_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '已上架' },
  { value: 2, label: '已下架' },
  { value: 3, label: '已满期' }
]

// ==================== 场景活动日程（scene_schedule）====================

/**
 * 场景日程状态（DDL 权威：0=已取消 / 1=可预约 / 2=已约满 / 3=进行中 / 4=已结束）。
 */
export enum SceneScheduleStatus {
  /** 已取消 */
  CANCELLED = 0,
  /** 可预约 */
  AVAILABLE = 1,
  /** 已约满 */
  FULL = 2,
  /** 进行中 */
  IN_PROGRESS = 3,
  /** 已结束 */
  ENDED = 4
}

/** 场景日程状态选项 */
export const SCENE_SCHEDULE_STATUS_OPTIONS = [
  { label: '已取消', value: SceneScheduleStatus.CANCELLED },
  { label: '可预约', value: SceneScheduleStatus.AVAILABLE },
  { label: '已约满', value: SceneScheduleStatus.FULL },
  { label: '进行中', value: SceneScheduleStatus.IN_PROGRESS },
  { label: '已结束', value: SceneScheduleStatus.ENDED }
] as const

/**
 * 场景活动日程实体（对齐后端 SceneScheduleVO）。
 *
 * sceneName 非原生字段，由后端 Controller 分页后按 sceneCode 批量查 scene_info 回填。
 */
export interface SceneSchedule {
  id?: number
  sceneCode?: string
  /** 场景名称（后端回填） */
  sceneName?: string
  scheduleDate?: string
  startTime?: string
  endTime?: string
  maxPerson?: number
  currentPerson?: number
  priceOverride?: number
  remark?: string
  status?: SceneScheduleStatus
  createdAt?: string
  updatedAt?: string
}

/** 场景日程查询参数 */
export interface SceneScheduleQuery {
  sceneCode?: string
  status?: SceneScheduleStatus
  scheduleDateStart?: string
  scheduleDateEnd?: string
  current: number
  size: number
}
