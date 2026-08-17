/**
 * 线索域相关类型（渠道视角的客户线索池）。
 *
 * 字段对齐后端 com.dayan.lead.vo.LeadInfoVO / LeadTraceVO（与 admin 端 types/lead.ts 同源）。
 */
import type { PageQuery } from '@/types/common'

/**
 * 线索来源类型（lead_info.source_type）。
 * 1=内容分享 / 2=工具分享 / 3=海报分享 / 4=直接访问。
 */
export const LEAD_SOURCE_TYPE_OPTIONS = [
  { label: '内容分享', value: 1 },
  { label: '工具分享', value: 2 },
  { label: '海报分享', value: 3 },
  { label: '直接访问', value: 4 }
] as const

/** 来源类型标签文本。 */
export function leadSourceTypeLabel(v?: number): string {
  const found = LEAD_SOURCE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/**
 * 互动类型（lead_info.last_interact_type / LeadTraceVO.traceType）。
 * 1=浏览内容 / 2=使用工具 / 3=查看海报。
 */
export const LEAD_INTERACT_TYPE_OPTIONS = [
  { label: '浏览内容', value: 1 },
  { label: '使用工具', value: 2 },
  { label: '查看海报', value: 3 }
] as const

/** 互动类型标签文本。 */
export function leadInteractTypeLabel(v?: number): string {
  const found = LEAD_INTERACT_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 互动类型对应的 el-tag 颜色。 */
export function leadInteractTagType(v?: number): 'primary' | 'success' | 'warning' | 'info' {
  if (v === 1) return 'primary'
  if (v === 2) return 'success'
  if (v === 3) return 'warning'
  return 'info'
}

/**
 * 访客线索（后端 LeadInfoVO）。
 */
export interface LeadInfo {
  id?: string
  /** 线索编码 */
  leadCode?: string
  /** 访客令牌 */
  visitorToken?: string
  /** 所属渠道编码 */
  channelCode?: string
  /** 微信OpenID */
  openid?: string
  /** 手机号（留资后回填） */
  phone?: string
  /** 姓名/称呼 */
  name?: string
  /** 微信昵称 */
  wxNickname?: string
  /** 微信头像URL */
  wxAvatar?: string
  /** 访客环境来源（wechat/browser/unknown） */
  visitorSource?: string
  /** 来源类型：1内容分享/2工具分享/3海报分享/4直接访问 */
  sourceType?: number
  /** 来源编码 */
  sourceCode?: string
  /** 关联客户编码（留资建档后回填） */
  clientCode?: string
  /** 最后互动时间 */
  lastInteractTime?: string
  /** 最后互动类型：1内容/2工具/3海报 */
  lastInteractType?: number
  /** 互动总次数 */
  interactCount?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 线索互动时间线项（后端 LeadTraceVO）。
 */
export interface LeadTrace {
  id?: string
  /** 互动类型：1浏览内容/2使用工具/3查看海报 */
  traceType?: number
  /** 业务编码 */
  bizCode?: string
  /** 业务标题 */
  bizTitle?: string
  /** 互动时间 */
  traceTime?: string
}

/**
 * 线索分页查询参数（渠道端：channelCode 由后端上下文强制，不开放入参）。
 */
export interface LeadInfoQuery extends PageQuery {
  keyword?: string
  /** 仅看已留资（有手机号） */
  onlyWithPhone?: boolean
  /** 排除已被代理人认领 */
  excludeClaimed?: boolean
}
