import type { PageQuery } from './common'

/** 服务会话（服务记录）*/
export interface ServiceSession {
  id?: number
  sessionCode: string
  equityCode?: string
  clientCode?: string
  clientName?: string
  butlerCode?: string
  butlerFullName?: string
  serviceType?: number
  serviceTitle?: string
  serviceDescription?: string
  priority?: number
  sourceType?: number
  parkCode?: string
  parkFullName?: string
  agentCode?: string
  channelCode?: string
  acceptTime?: string
  completeTime?: string
  closeTime?: string
  totalDuration?: number
  touchCount?: number
  isSatisfied?: number
  overallRating?: number
  sessionStatus?: number
  subStatus?: string
  closeReason?: string
  remark?: string
}

/** 服务会话查询 */
export interface ServiceSessionQuery extends PageQuery {
  sessionCode?: string
  equityCode?: string
  clientCode?: string
  butlerCode?: string
  serviceType?: number
  parkCode?: string
  agentCode?: string
  sessionStatus?: number
  subStatus?: string
  sourceType?: number
}

/** 服务类型选项 */
export const SERVICE_TYPE_OPTIONS = [
  { value: 1, label: '电话关怀' },
  { value: 2, label: '上门探访' },
  { value: 3, label: '陪同就医' },
  { value: 4, label: '紧急救援' }
]

/** 会话状态选项（7 主状态）*/
export const SESSION_STATUS_OPTIONS = [
  { value: 0, label: '待受理' },
  { value: 1, label: '待确认方案' },
  { value: 2, label: '服务中' },
  { value: 3, label: '已完成' },
  { value: 4, label: '已关闭' },
  { value: 5, label: '已取消' },
  { value: 6, label: '已挂起' }
]
