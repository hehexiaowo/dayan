/**
 * 代理人相关类型。
 *
 * 字段对齐后端 com.dayan.agent（表 agent_info 及子表，位于 dayan-module-agent）。
 *
 * 注意：后端代理人控制器为 RESTful 复数风格（/admin-api/agents），list 接口
 * 返回 PageResult（有分页字段），但 url 无 /page 后缀。
 *
 * 枚举档位说明（对齐 DDL，重要）：
 * - AgentLevel 为 4 档（普通/银牌/金牌/钻石），对齐 agent_info.agent_level DDL 注释。
 * - AgentStatus 为 3 态（0禁用 / 1正常 / 2冻结），对齐 agent_info.status DDL 注释。
 *
 * 子表主键混合模式（本域最大坑）：
 * - AgentAccount：业务主键 agentCode（1:1）。
 * - AgentClientRel/AgentShareRecord/AgentFavorite：雪花 id（前端 string；业绩域管理入口已下线）。
 */

import type { PageQuery } from '@/types/common'

/** 性别（从 common 复用） */
export { Gender, GENDER_OPTIONS } from '@/types/common'

/**
 * 代理人等级：1普通 2银牌 3金牌 4钻石（对齐 DDL agent_info.agent_level）。
 */
export enum AgentLevel {
  NORMAL = 1,
  SILVER = 2,
  GOLD = 3,
  DIAMOND = 4
}

/** 代理人等级选项 */
export const AGENT_LEVEL_OPTIONS = [
  { label: '普通', value: AgentLevel.NORMAL },
  { label: '银牌', value: AgentLevel.SILVER },
  { label: '金牌', value: AgentLevel.GOLD },
  { label: '钻石', value: AgentLevel.DIAMOND }
] as const

/** 代理人等级标签文本（兼容数字直传）。 */
export function agentLevelLabel(v?: number): string {
  const found = AGENT_LEVEL_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 代理人等级 el-tag 配色。 */
export function agentLevelTagType(v?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (v) {
    case AgentLevel.DIAMOND:
      return 'danger'
    case AgentLevel.GOLD:
      return 'warning'
    case AgentLevel.SILVER:
      return 'success'
    case AgentLevel.NORMAL:
    default:
      return 'info'
  }
}

/**
 * 代理人状态：0禁用 1正常 2冻结（对齐 DDL agent_info.status）。
 */
export enum AgentStatus {
  DISABLED = 0,
  NORMAL = 1,
  FROZEN = 2
}

/** 代理人状态选项 */
export const AGENT_STATUS_OPTIONS = [
  { label: '禁用', value: AgentStatus.DISABLED },
  { label: '正常', value: AgentStatus.NORMAL },
  { label: '冻结', value: AgentStatus.FROZEN }
] as const

/** 代理人状态标签文本（用于列表/摘要展示）。 */
export function agentStatusLabel(v?: number): string {
  const found = AGENT_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/** 代理人状态 el-tag 配色：0禁用=info，1正常=success，2冻结=warning。 */
export function agentStatusTagType(v?: number): 'success' | 'info' | 'warning' {
  switch (v) {
    case AgentStatus.NORMAL:
      return 'success'
    case AgentStatus.FROZEN:
      return 'warning'
    case AgentStatus.DISABLED:
    default:
      return 'info'
  }
}

/** 是否认证：0否 1是 */
export enum CertifiedFlag {
  NO = 0,
  YES = 1
}

/** 认证选项 */
export const CERTIFIED_OPTIONS = [
  { label: '未认证', value: CertifiedFlag.NO },
  { label: '已认证', value: CertifiedFlag.YES }
] as const

/**
 * 代理人信息实体（后端 AgentInfo）。
 */
export interface AgentInfo {
  id?: number
  /** 代理人编码（主键，服务端生成） */
  agentCode?: string
  /** 姓名 */
  fullName: string
  /** 性别：0未知 1男 2女 */
  gender?: Gender
  /** 头像 */
  avatar?: string
  /** 手机号 */
  phone?: string
  /** 邮箱 */
  email?: string
  /** 身份证号 */
  idCard?: string
  /** 所属渠道编码 */
  channelCode?: string
  /** 保险公司名称 */
  companyName?: string
  /** 分公司名称 */
  branchName?: string
  /** 部门 */
  department?: string
  /** 职位 */
  position?: string
  /** 保险公司工号 */
  employeeNo?: string
  /** 从业资格证号 */
  licenseNo?: string
  /** 省级编码 */
  provinceCode?: string
  /** 市级编码 */
  cityCode?: string
  /** 区县编码 */
  districtCode?: string
  /** 详细地址 */
  address?: string
  /** 服务介绍 */
  serviceIntro?: string
  /** 客户数量（统计字段） */
  clientCount?: number
  /** 订单总数（统计字段） */
  totalOrderCount?: number
  /** 订单总金额（统计字段，后端 VO 有，前端原漏） */
  totalOrderAmount?: number
  /** 代理人等级：1普通 2银牌 3金牌 4钻石 */
  agentLevel?: AgentLevel
  /** 是否认证：0否 1是 */
  isCertified?: CertifiedFlag
  /** 状态：0禁用 1正常 2冻结 */
  status?: AgentStatus
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 代理人分页查询参数（后端 AgentInfoQueryDTO）。
 */
export interface AgentInfoQuery extends PageQuery {
  /** 代理人编码 */
  agentCode?: string
  /** 所属渠道编码 */
  channelCode?: string
  /** 姓名（模糊匹配） */
  fullName?: string
  /** 手机号（模糊匹配） */
  phone?: string
  /** 代理人等级 */
  agentLevel?: AgentLevel
  /** 是否认证：0否 1是 */
  isCertified?: CertifiedFlag
  /** 状态：0禁用 1正常 2冻结 */
  status?: AgentStatus
}

// ============================================================================
// 子表类型（代理人域 5 个子表，对齐后端 com.dayan.agent.vo.*）
//
// 主键规则（重要，混合模式，本域最大坑）：
// - AgentAccount：业务主键 agentCode（非 id）。get/update/delete/reset-password 都用 agentCode。
//   一代理人一账号（1:1），同 agentCode 仅允许一条。
// - AgentClientRel：雪花 id（前端 string 防精度溢出）。无 update，bind/unbind/delete。
//   unbind 路径用 id（Long）。
// - AgentShareRecord：雪花 id（前端 string）。只增不改不删；create 返回 shareCode string。
// - AgentFavorite：雪花 id（前端 string）。幂等 add（重复返回既有 id）；remove 路径用 id。
//   无 update。
// ============================================================================

/**
 * 代理人账号（AgentAccount，主键 agentCode，非 id）。
 *
 * 一代理人一账号（1:1 强约束）。VO 不含 password（不返回）。
 * get/update/delete/reset-password 都用 agentCode 作 path 变量。
 */
export interface AgentAccount {
  /** 自增/雪花 id（仅展示，不作为接口主键） */
  id?: number
  /** 代理人编码（业务主键） */
  agentCode: string
  /** 所属渠道编码 */
  channelCode?: string
  /** 用户名（渠道内唯一，create 后不可改） */
  username?: string
  /** 手机号 */
  phone?: string
  /** 微信 openId */
  openId?: string
  /** 微信 unionId */
  unionId?: string
  /** 外部账号 */
  extAccountNo?: string
  /** 账号状态：0锁定 1正常 2禁用 */
  accountStatus?: number
  /** 最后登录时间（只读） */
  lastLoginTime?: string
  /** 最后登录 IP（只读） */
  lastLoginIp?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 代理人账号分页查询参数。
 */
export interface AgentAccountQuery extends PageQuery {
  /** 代理人编码（详情页 tab 固定携带） */
  agentCode?: string
  /** 用户名 */
  username?: string
  /** 手机号 */
  phone?: string
  /** 账号状态 */
  accountStatus?: number
}

/**
 * 代理人-客户绑定关系（AgentClientRel，主键雪花 id string）。
 *
 * 无 update 端点：bind(POST) / unbind(PUT /{id}/unbind) / 无标准 delete（unbind 即软删）。
 * bindTime 服务端设 now()，前端不传。无 channelCode 字段。
 */
export interface AgentClientRel {
  /** 雪花 id（前端 string 防精度溢出） */
  id?: string
  /** 代理人编码 */
  agentCode: string
  /** 客户编码 */
  clientCode: string
  /** 绑定类型：1权益赠送 2活动邀请 3自主（默认1） */
  bindType?: number
  /** 绑定时间（服务端 now()，只读） */
  bindTime?: string
  /** 状态：0已解绑 1服务中 */
  status?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 代理人客户绑定分页查询参数。
 */
export interface AgentClientRelQuery extends PageQuery {
  /** 代理人编码（详情页 tab 固定携带） */
  agentCode?: string
  /** 客户编码 */
  clientCode?: string
  /** 绑定类型 */
  bindType?: number
  /** 状态 */
  status?: number
}


/**
 * 代理人分享记录（AgentShareRecord，主键雪花 id string）。
 *
 * 只增不改不删：create 返回 shareCode string（服务端 UUID 生成）。
 * viewCount 只读（create 写0）。shareTime 服务端 now()。无 channelCode 字段。
 */
export interface AgentShareRecord {
  /** 雪花 id（前端 string 防精度溢出） */
  id?: string
  /** 分享编码（服务端 UUID 生成，create 返回） */
  shareCode?: string
  /** 代理人编码 */
  agentCode: string
  /** 分享类型：1内容 2场景 3机构 4权益 5课程 */
  shareType?: number
  /** 关联业务编码（按 shareType 指向不同实体） */
  bizCode?: string
  /** 分享渠道：1微信 2朋友圈 3复制链接 4二维码 5短信 */
  shareChannel?: number
  /** 客户编码 */
  clientCode?: string
  /** 浏览次数（只读，create 写0） */
  viewCount?: number
  /** 分享时间（服务端 now()，只读） */
  shareTime?: string
  createdAt?: string
}

/**
 * 代理人收藏（AgentFavorite，主键雪花 id string）。
 *
 * 幂等 add（重复收藏返回既有 id 不报错）；无 update。
 * remove 路径用 id。无 channelCode 字段。
 */
export interface AgentFavorite {
  /** 雪花 id（前端 string 防精度溢出） */
  id?: string
  /** 代理人编码 */
  agentCode: string
  /** 收藏对象类型：1养老机构 2场景 3课程 4内容 */
  targetType: number
  /** 收藏对象编码（按 targetType 指向不同实体） */
  targetCode: string
  createdAt?: string
}

// ============================================================================
// 子表枚举 OPTIONS
// ============================================================================

/** 账号状态选项：0锁定 1正常 2禁用 */
export const ACCOUNT_STATUS_OPTIONS = [
  { label: '正常', value: 1 },
  { label: '锁定', value: 0 },
  { label: '禁用', value: 2 }
] as const

/** 绑定类型选项：1权益赠送 2活动邀请 3自主 */
export const BIND_TYPE_OPTIONS = [
  { label: '权益赠送绑定', value: 1 },
  { label: '活动邀请绑定', value: 2 },
  { label: '自主绑定', value: 3 }
] as const

/** 客户绑定状态选项：0已解绑 1服务中 */
export const CLIENT_REL_STATUS_OPTIONS = [
  { label: '服务中', value: 1 },
  { label: '已解绑', value: 0 }
] as const

/** 周期类型选项：1日 2周 3月 4季 5年 */
export const PERIOD_TYPE_OPTIONS = [
  { label: '日', value: 1 },
  { label: '周', value: 2 },
  { label: '月', value: 3 },
  { label: '季', value: 4 },
  { label: '年', value: 5 }
] as const

/** 分享类型选项：1内容 2场景 3机构 4权益 5课程 */
export const SHARE_TYPE_OPTIONS = [
  { label: '内容', value: 1 },
  { label: '场景', value: 2 },
  { label: '机构', value: 3 },
  { label: '权益', value: 4 },
  { label: '课程', value: 5 }
] as const

/** 分享渠道选项：1微信 2朋友圈 3复制链接 4二维码 5短信 */
export const SHARE_CHANNEL_OPTIONS = [
  { label: '微信', value: 1 },
  { label: '朋友圈', value: 2 },
  { label: '复制链接', value: 3 },
  { label: '二维码', value: 4 },
  { label: '短信', value: 5 }
] as const

/** 收藏对象类型选项：1养老机构 2场景 3课程 4内容 */
export const FAVORITE_TARGET_TYPE_OPTIONS = [
  { label: '养老机构', value: 1 },
  { label: '场景', value: 2 },
  { label: '课程', value: 3 },
  { label: '内容', value: 4 }
] as const

// ---------- 子表枚举 label 辅助函数 ----------

/** 账号状态标签 */
export function accountStatusLabel(v?: number): string {
  const found = ACCOUNT_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/** 账号状态 el-tag 配色：1正常=success，0锁定=warning，2禁用=info */
export function accountStatusTagType(v?: number): 'success' | 'info' | 'warning' {
  if (v === 1) return 'success'
  if (v === 0) return 'warning'
  return 'info'
}

/** 绑定类型 label */
export function bindTypeLabel(v?: number): string {
  const found = BIND_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 客户绑定状态 label */
export function clientRelStatusLabel(v?: number): string {
  const found = CLIENT_REL_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/** 客户绑定状态 el-tag 配色：1服务中=success，0已解绑=info */
export function clientRelStatusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}

/** 周期类型 label */
export function periodTypeLabel(v?: number): string {
  const found = PERIOD_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 分享类型 label */
export function shareTypeLabel(v?: number): string {
  const found = SHARE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 分享渠道 label */
export function shareChannelLabel(v?: number): string {
  const found = SHARE_CHANNEL_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 收藏对象类型 label */
export function favoriteTargetTypeLabel(v?: number): string {
  const found = FAVORITE_TARGET_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}
