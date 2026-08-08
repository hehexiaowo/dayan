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
