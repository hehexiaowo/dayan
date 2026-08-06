/**
 * 服务域相关类型（管家信息 + 服务会话）。
 *
 * 字段对齐后端 com.dayan.butler.entity.ButlerInfo 及
 * com.dayan.service.entity.ServiceSession 及对应 QueryDTO。
 * - Integer → number
 * - LocalDateTime → string
 */
import type { PageQuery } from '@/types/common'

// ==================== 管家信息 ====================

/**
 * 管家等级（butler_info.butler_level）。
 *
 * 1=初级 / 2=中级 / 3=高级 / 4=专家。
 */
export const BUTLER_LEVEL_OPTIONS = [
  { label: '初级', value: 1 },
  { label: '中级', value: 2 },
  { label: '高级', value: 3 },
  { label: '专家', value: 4 }
] as const

/**
 * 管家信息实体（后端 ButlerInfoVO）。
 *
 * 仅 8 个业务字段，是最简单的标准 CRUD 实体。
 */
export interface ButlerInfo {
  id?: number
  /** 管家编码（服务端生成，编辑时只读） */
  butlerCode?: string
  /** 管家姓名（必填） */
  fullName: string
  /** 手机号 */
  phone?: string
  /** 头像 URL */
  avatar?: string
  /** 所属组织编码 */
  organCode?: string
  /** 管家等级：1初级/2中级/3高级/4专家 */
  butlerLevel?: number
  /** 状态：1在职 / 0离职（以 DDL 为准，非启用/禁用） */
  status?: number
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 管家信息分页查询参数（后端 ButlerInfoQueryDTO）。
 */
export interface ButlerInfoQuery extends PageQuery {
  butlerCode?: string
  fullName?: string
  phone?: string
  organCode?: string
  butlerLevel?: number
  status?: number
}

/**
 * 管家在职状态（butler_info.status，以 DDL 为准）。
 *
 * 1=在职 / 0=离职。注意：与 client 域的 ClientStatus 不同，
 * 与通用 COMMON_STATUS_OPTIONS（启用/禁用）语义也不同，单独定义。
 */
export const BUTLER_STATUS_OPTIONS = [
  { label: '在职', value: 1 },
  { label: '离职', value: 0 }
] as const

/**
 * 管家状态标签文本（在职/离职）。
 */
export function butlerStatusLabel(v?: number): string {
  const found = BUTLER_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/**
 * 管家状态 el-tag 配色：1在职=success，0离职=info。
 */
export function butlerStatusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}

/** 管家等级标签文本（兼容数字直传）。 */
export function butlerLevelLabel(v?: number): string {
  const found = BUTLER_LEVEL_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 管家等级 el-tag 配色。 */
export function butlerLevelTagType(v?: number): 'info' | 'warning' | 'success' | 'danger' {
  switch (v) {
    case 4:
      return 'danger'
    case 3:
      return 'success'
    case 2:
      return 'warning'
    case 1:
    default:
      return 'info'
  }
}

// ==================== 管家子表（5 张，用于详情页 tab） ====================

/**
 * 账号状态（butler_account.account_status，DDL 文档 3 态）。
 * 0=锁定 / 1=正常 / 2=禁用。
 */
export const BUTLER_ACCOUNT_STATUS_OPTIONS = [
  { label: '锁定', value: 0 },
  { label: '正常', value: 1 },
  { label: '禁用', value: 2 }
] as const

/**
 * 技能熟练度（butler_skill.proficiency，4 态）。
 * 1=了解 / 2=熟悉 / 3=熟练 / 4=精通。
 */
export const SKILL_PROFICIENCY_OPTIONS = [
  { label: '了解', value: 1 },
  { label: '熟悉', value: 2 },
  { label: '熟练', value: 3 },
  { label: '精通', value: 4 }
] as const

/**
 * 是否持证（butler_skill.is_certified）。
 * 0=否 / 1=是。
 */
export const IS_CERTIFIED_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 }
] as const

/**
 * 服务客户绑定状态（butler_client_rel.status）。
 * 0=已解绑 / 1=服务中。
 */
export const CLIENT_REL_STATUS_OPTIONS = [
  { label: '已解绑', value: 0 },
  { label: '服务中', value: 1 }
] as const

/**
 * 管家服务类型（butler_service_record.service_type，4 态）。
 * 注意：与 ServiceSession 的 SERVICE_TYPE_OPTIONS（5 态）含义不同，单独定义。
 * 1=需求评估 / 2=方案定制 / 3=全程安排 / 4=回访品控。
 */
export const BUTLER_SERVICE_TYPE_OPTIONS = [
  { label: '需求评估', value: 1 },
  { label: '方案定制', value: 2 },
  { label: '全程安排', value: 3 },
  { label: '回访品控', value: 4 }
] as const

/**
 * 服务记录状态（butler_service_record.status，3 态）。
 * 0=进行中 / 1=已完成 / 2=已取消。
 */
export const SERVICE_RECORD_STATUS_OPTIONS = [
  { label: '进行中', value: 0 },
  { label: '已完成', value: 1 },
  { label: '已取消', value: 2 }
] as const

/**
 * 沟通方式（butler_service_record.communicate_way，5 态）。
 * 1=电话 / 2=企业微信 / 3=微信 / 4=当面沟通 / 5=其他。
 */
export const COMMUNICATE_WAY_OPTIONS = [
  { label: '电话', value: 1 },
  { label: '企业微信', value: 2 },
  { label: '微信', value: 3 },
  { label: '当面沟通', value: 4 },
  { label: '其他', value: 5 }
] as const

/**
 * 评价状态（butler_rating.status）。
 * 0=已隐藏 / 1=正常。
 */
export const RATING_STATUS_OPTIONS = [
  { label: '已隐藏', value: 0 },
  { label: '正常', value: 1 }
] as const

// ---------- 子表枚举 label/tagType 辅助函数 ----------

/** 账号状态标签文本。 */
export function butlerAccountStatusLabel(v?: number): string {
  const found = BUTLER_ACCOUNT_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/** 账号状态 el-tag 配色：1正常=success，0锁定=warning，2禁用=info。 */
export function butlerAccountStatusTagType(v?: number): 'success' | 'info' | 'warning' {
  switch (v) {
    case 1:
      return 'success'
    case 0:
      return 'warning'
    case 2:
    default:
      return 'info'
  }
}

/** 技能熟练度标签文本。 */
export function skillProficiencyLabel(v?: number): string {
  const found = SKILL_PROFICIENCY_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 是否持证标签文本。 */
export function isCertifiedLabel(v?: number): string {
  return v === 1 ? '是' : v === 0 ? '否' : '--'
}

/** 绑定关系状态标签文本。 */
export function clientRelStatusLabel(v?: number): string {
  const found = CLIENT_REL_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/** 绑定关系状态 el-tag 配色：1服务中=success，0已解绑=info。 */
export function clientRelStatusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}

/** 管家服务类型标签文本（butler_service_record）。 */
export function butlerServiceTypeLabel(v?: number): string {
  const found = BUTLER_SERVICE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 服务记录状态标签文本。 */
export function serviceRecordStatusLabel(v?: number): string {
  const found = SERVICE_RECORD_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/** 服务记录状态 el-tag 配色：1已完成=success，0进行中=warning，2已取消=info。 */
export function serviceRecordStatusTagType(v?: number): 'success' | 'info' | 'warning' {
  switch (v) {
    case 1:
      return 'success'
    case 0:
      return 'warning'
    case 2:
    default:
      return 'info'
  }
}

/** 沟通方式标签文本。 */
export function communicateWayLabel(v?: number): string {
  const found = COMMUNICATE_WAY_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 评价状态标签文本。 */
export function ratingStatusLabel(v?: number): string {
  const found = RATING_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/** 评价状态 el-tag 配色：1正常=success，0已隐藏=info。 */
export function ratingStatusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}

/**
 * 管家账号实体（后端 ButlerAccountVO，主键自增 id）。
 */
export interface ButlerAccount {
  /** 主键 id（自增） */
  id?: number
  /** 所属管家编码 */
  butlerCode: string
  /** 登录用户名（创建后不可改） */
  username: string
  /** 绑定手机号 */
  phone?: string
  /** 微信 openId */
  openId?: string
  /** 微信 unionId */
  unionId?: string
  /** 最后登录时间 */
  lastLoginTime?: string
  /** 账号状态：0锁定/1正常/2禁用 */
  accountStatus: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 管家账号分页查询参数。
 */
export interface ButlerAccountQuery extends PageQuery {
  butlerCode?: string
  username?: string
  phone?: string
  accountStatus?: number
}

/**
 * 管家技能实体（后端 ButlerSkillVO，主键自增 id）。
 */
export interface ButlerSkill {
  /** 主键 id（自增） */
  id?: number
  /** 所属管家编码 */
  butlerCode: string
  /** 技能编码（字典 butler_skill，编辑时不可改） */
  skillCode: string
  /** 技能名称 */
  skillName: string
  /** 熟练度：1了解/2熟悉/3熟练/4精通 */
  proficiency: number
  /** 是否持证：0否/1是 */
  isCertified: number
  /** 证书编号 */
  certificateNo?: string
  /** 取得日期 */
  obtainDate?: string
  /** 排序号（空默认 0） */
  sortOrder?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 管家技能分页查询参数。
 */
export interface ButlerSkillQuery extends PageQuery {
  butlerCode?: string
  skillName?: string
  proficiency?: number
  isCertified?: number
}

/**
 * 管家-客户绑定关系实体（后端 ButlerClientRelVO）。
 *
 * 主键为雪花 id（前端用 string 防精度溢出）。
 * 无标准 update 端点，仅 bind(POST)/unbind(PUT /{id}/unbind)/delete。
 */
export interface ButlerClientRel {
  /** 主键 id（雪花，前端按 string 处理） */
  id?: string
  /** 管家编码 */
  butlerCode: string
  /** 客户编码 */
  clientCode: string
  /** 绑定时间（服务端设 now()，前端不传） */
  bindTime?: string
  /** 状态：0已解绑/1服务中 */
  status: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 管家-客户绑定关系查询参数。
 */
export interface ButlerClientRelQuery extends PageQuery {
  butlerCode?: string
  clientCode?: string
  status?: number
}

/**
 * 管家服务记录实体（后端 ButlerServiceRecordVO）。
 *
 * 主键为雪花 id（前端用 string）。
 */
export interface ButlerServiceRecord {
  /** 主键 id（雪花，前端按 string 处理） */
  id?: string
  /** 管家编码 */
  butlerCode: string
  /** 客户编码 */
  clientCode: string
  /** 服务类型：1需求评估/2方案定制/3全程安排/4回访品控 */
  serviceType: number
  /** 服务标题 */
  serviceTitle: string
  /** 服务日期 */
  serviceDate?: string
  /** 状态：0进行中/1已完成/2已取消 */
  status: number
  /** 沟通方式：1电话/2企业微信/3微信/4当面沟通/5其他 */
  communicateWay?: number
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 管家服务记录查询参数。
 */
export interface ButlerServiceRecordQuery extends PageQuery {
  butlerCode?: string
  clientCode?: string
  serviceType?: number
  status?: number
}

/**
 * 管家评价实体（后端 ButlerRatingVO）。
 *
 * 主键为雪花 id（前端用 string）。
 */
export interface ButlerRating {
  /** 主键 id（雪花，前端按 string 处理） */
  id?: string
  /** 管家编码 */
  butlerCode: string
  /** 客户编码 */
  clientCode: string
  /** 关联服务记录编码（悬空字段，无对应实体编码，可手填或留空） */
  serviceRecordCode?: string
  /** 评分 1-5（后端无范围校验，前端必须卡） */
  rating: number
  /** 评价内容 */
  content?: string
  /** 状态：0已隐藏/1正常 */
  status: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 管家评价查询参数。
 */
export interface ButlerRatingQuery extends PageQuery {
  butlerCode?: string
  clientCode?: string
  rating?: number
  status?: number
}

// ==================== 服务会话 ====================

/**
 * 服务会话状态（service_session.session_status）。
 *
 * 0=待受理 / 1=已受理 / 2=需求提交 / 3=方案确认 / 4=服务中 / 5=已完成 / 6=已取消。
 */
export enum SessionStatus {
  /** 待受理 */
  PENDING = 0,
  /** 已受理 */
  ACCEPTED = 1,
  /** 需求提交 */
  DEMAND_SUBMITTED = 2,
  /** 方案确认 */
  SOLUTION_CONFIRMED = 3,
  /** 服务中 */
  IN_SERVICE = 4,
  /** 已完成 */
  COMPLETED = 5,
  /** 已取消 */
  CANCELLED = 6
}

/** 服务会话状态选项 */
export const SESSION_STATUS_OPTIONS = [
  { label: '待受理', value: SessionStatus.PENDING },
  { label: '已受理', value: SessionStatus.ACCEPTED },
  { label: '需求提交', value: SessionStatus.DEMAND_SUBMITTED },
  { label: '方案确认', value: SessionStatus.SOLUTION_CONFIRMED },
  { label: '服务中', value: SessionStatus.IN_SERVICE },
  { label: '已完成', value: SessionStatus.COMPLETED },
  { label: '已取消', value: SessionStatus.CANCELLED }
] as const

/**
 * 服务类型（service_session.service_type）。
 *
 * 1=上门服务 / 2=电话咨询 / 3=远程协助 / 4=机构驻点 / 5=其他。
 */
export const SERVICE_TYPE_OPTIONS = [
  { label: '上门服务', value: 1 },
  { label: '电话咨询', value: 2 },
  { label: '远程协助', value: 3 },
  { label: '机构驻点', value: 4 },
  { label: '其他', value: 5 }
] as const

/**
 * 服务会话实体（后端 ServiceSessionVO）。
 */
export interface ServiceSession {
  id?: number
  /** 会话编码（主键） */
  sessionCode?: string
  /** 关联权益编码 */
  equityCode?: string
  /** 客户编码 */
  clientCode?: string
  /** 服务管家编码 */
  butlerCode?: string
  /** 服务管家姓名（快照） */
  butlerFullName?: string
  /** 服务类型 */
  serviceType?: number
  /** 服务标题 */
  serviceTitle?: string
  /** 服务描述 */
  serviceDescription?: string
  /** 优先级 */
  priority?: number
  /** 来源类型 */
  sourceType?: number
  /** 来源编码 */
  sourceCode?: string
  /** 关联养老机构编码 */
  parkCode?: string
  /** 关联养老机构名称（快照） */
  parkFullName?: string
  /** 关联代理人编码 */
  agentCode?: string
  /** 关联渠道编码 */
  channelCode?: string
  /** 受理时间 */
  acceptTime?: string
  /** 完成时间 */
  completeTime?: string
  /** 关闭时间 */
  closeTime?: string
  /** 总服务时长（小时） */
  totalDuration?: number
  /** 服务接触次数 */
  touchCount?: number
  /** 是否满意 */
  isSatisfied?: number
  /** 综合评分 */
  overallRating?: number
  /** 会话状态 */
  sessionStatus?: SessionStatus
  /** 子状态 */
  subStatus?: string
  /** 关闭原因 */
  closeReason?: string
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 服务会话分页查询参数（后端 ServiceSessionQueryDTO）。
 */
export interface ServiceSessionQuery extends PageQuery {
  sessionCode?: string
  equityCode?: string
  clientCode?: string
  butlerCode?: string
  serviceType?: number
  parkCode?: string
  agentCode?: string
  channelCode?: string
  sessionStatus?: SessionStatus
  subStatus?: string
  sourceType?: number
}
