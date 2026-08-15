/**
 * 消息管理相关类型（消息模板 + 发送记录）。
 *
 * 字段逐字对齐后端 com.dayan.system.entity.SystemMessageTemplate /
 * SystemMessage（含 BaseEntity 的 createdAt 等）。
 * 对应表 system_message_template（01 迁移 3.1.8）/ system_message（3.1.16）。
 */

/** 渠道类型（两表共用枚举） */
export const CHANNEL_TYPE_OPTIONS = [
  { label: '短信', value: 1 },
  { label: '站内信', value: 2 },
  { label: 'APP推送', value: 3 },
  { label: '企业微信', value: 4 },
  { label: '微信模板消息', value: 5 },
  { label: '邮件', value: 6 }
] as const

/** 渠道类型标签文案 */
export function channelTypeLabel(type?: number | null): string {
  return CHANNEL_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? String(type ?? '')
}

/** 常见业务类型（模板/记录筛选下拉，后端为自由文本） */
export const BIZ_TYPE_OPTIONS = [
  { label: '注册 register', value: 'register' },
  { label: '登录 login', value: 'login' },
  { label: '激活 activate', value: 'activate' },
  { label: '通知 notify', value: 'notify' },
  { label: '提醒 remind', value: 'remind' },
  { label: '订单 order', value: 'order' },
  { label: '退款 refund', value: 'refund' },
  { label: '活动 activity', value: 'activity' }
] as const

/** 状态（0=禁用, 1=启用） */
export const TEMPLATE_STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
] as const

/** 消息模板（后端 SystemMessageTemplate 实体） */
export interface MessageTemplate {
  id?: number
  /** 模板编码（全局唯一，创建后不可改） */
  templateCode?: string
  /** 模板名称 */
  templateName?: string
  /** 业务类型（register/login/order/refund 等自由文本） */
  bizType?: string
  /** 渠道类型，见 CHANNEL_TYPE_OPTIONS */
  channelType?: number
  /** 消息标题（站内信/推送/邮件必填） */
  title?: string | null
  /** 模板正文（含 ${var} 占位符） */
  content?: string
  /** 变量定义（JSON 数组字符串，如 [{"name":"code","label":"验证码"}]） */
  variables?: string | null
  /** 渠道差异配置（JSON） */
  channelConfig?: string | null
  /** 降级渠道（本渠道失败时备选） */
  fallbackChannelType?: number | null
  /** 渠道编码（NULL=平台通用模板） */
  channelCode?: string | null
  /** 状态：0=禁用 1=启用 */
  status?: number
  sortOrder?: number
  remark?: string | null
  createdAt?: string
  updatedAt?: string
}

/** 消息模板分页查询参数 */
export interface MessageTemplateQuery {
  templateCode?: string
  templateName?: string
  bizType?: string
  channelType?: number
  status?: number
  current: number
  size: number
}

/** 发送状态（system_message.send_status） */
export const SEND_STATUS_OPTIONS = [
  { label: '待发送', value: 0 },
  { label: '发送中', value: 1 },
  { label: '发送成功', value: 2 },
  { label: '发送失败', value: 3 },
  { label: '已送达', value: 4 },
  { label: '已读', value: 5 },
  { label: '已撤回', value: 6 }
] as const

/** 发送状态标签文案 */
export function sendStatusLabel(status?: number | null): string {
  return SEND_STATUS_OPTIONS.find((o) => o.value === status)?.label ?? String(status ?? '')
}

/** 发送状态 → tag 类型（失败红/成功绿/流转中蓝灰） */
export function sendStatusTagType(status?: number | null): 'info' | 'warning' | 'success' | 'danger' | 'primary' {
  if (status === 3) return 'danger'
  if (status === 2 || status === 4 || status === 5) return 'success'
  if (status === 1) return 'warning'
  if (status === 6) return 'info'
  return 'primary'
}

/** 消息类型（1=系统通知 2=业务提醒 3=活动通知 4=权益通知 5=服务通知） */
export const MESSAGE_TYPE_OPTIONS = [
  { label: '系统通知', value: 1 },
  { label: '业务提醒', value: 2 },
  { label: '活动通知', value: 3 },
  { label: '权益通知', value: 4 },
  { label: '服务通知', value: 5 }
] as const

export function messageTypeLabel(type?: number | null): string {
  return MESSAGE_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? String(type ?? '')
}

/** 接收者类型（organ/butler/supplier/channel/agent/client） */
export const TARGET_TYPE_OPTIONS = [
  { label: '机构', value: 'organ' },
  { label: '管家', value: 'butler' },
  { label: '供应商', value: 'supplier' },
  { label: '渠道', value: 'channel' },
  { label: '代理人', value: 'agent' },
  { label: '客户', value: 'client' }
] as const

export function targetTypeLabel(type?: string | null): string {
  return TARGET_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? String(type ?? '')
}

/** 消息发送记录（后端 SystemMessage 实体，只读审计） */
export interface MessageRecord {
  id?: number
  /** 消息实例编码 */
  messageCode?: string
  /** 发送批次编码 */
  batchCode?: string | null
  /** 关联模板编码 */
  templateCode?: string | null
  bizType?: string
  /** 实际发送渠道 */
  channelType?: number
  /** 消息类型，见 MESSAGE_TYPE_OPTIONS */
  messageType?: number
  /** 消息标题（渲染后） */
  title?: string | null
  /** 消息正文（变量替换后的最终内容） */
  content?: string
  /** 接收者类型/编码/名称/联系方式 */
  targetType?: string
  targetCode?: string | null
  targetName?: string | null
  targetContact?: string | null
  /** 发送者类型/编码 */
  senderType?: string
  senderCode?: string | null
  /** 跳转链接与类型 */
  linkUrl?: string | null
  linkType?: number | null
  /** 发送状态，见 SEND_STATUS_OPTIONS */
  sendStatus?: number
  providerMsgId?: string | null
  sendTime?: string | null
  deliverTime?: string | null
  readTime?: string | null
  expireTime?: string | null
  retryCount?: number
  errorCode?: string | null
  errorMsg?: string | null
  /** 优先级：0=普通 1=重要 2=紧急 */
  priority?: number
  createdAt?: string
  updatedAt?: string
}

/** 发送记录分页查询参数 */
export interface MessageRecordQuery {
  bizType?: string
  channelType?: number
  sendStatus?: number
  targetType?: string
  templateCode?: string
  startTime?: string
  endTime?: string
  current: number
  size: number
}
