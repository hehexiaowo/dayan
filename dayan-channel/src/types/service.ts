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

/**
 * 会话状态选项（service_session.session_status，DB 现有注释权威，1-7）：
 * 1=待分配 / 2=处理中 / 3=方案待确认 / 4=服务安排中 / 5=服务中 / 6=已完成 / 7=已取消。
 */
export const SESSION_STATUS_OPTIONS = [
  { value: 1, label: '待分配' },
  { value: 2, label: '处理中' },
  { value: 3, label: '方案待确认' },
  { value: 4, label: '服务安排中' },
  { value: 5, label: '服务中' },
  { value: 6, label: '已完成' },
  { value: 7, label: '已取消' }
]

/**
 * 会话优先级（service_session.priority）：0=普通 / 1=优先 / 2=紧急 / 3=非常紧急。
 */
export const SESSION_PRIORITY_OPTIONS = [
  { label: '普通', value: 0 },
  { label: '优先', value: 1 },
  { label: '紧急', value: 2 },
  { label: '非常紧急', value: 3 }
] as const

/** 会话优先级标签文本。 */
export function sessionPriorityLabel(v?: number): string {
  const found = SESSION_PRIORITY_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/**
 * 会话状态 tag 色（业务语义，对齐 1-7）：
 * 1待分配 warning / 2处理中 primary / 3方案待确认 primary / 4服务安排中 primary /
 * 5服务中 primary / 6已完成 success / 7已取消 danger。
 */
export function sessionStatusTagType(v?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (v) {
    case 6: return 'success'
    case 7: return 'danger'
    case 1: return 'warning'
    default: return 'primary'
  }
}
