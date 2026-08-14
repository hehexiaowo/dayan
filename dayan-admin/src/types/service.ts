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
  /** 关联后台账号编码（organ_account.account_code，未开通为 null） */
  accountCode?: string
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

// ==================== 子表：服务评价（ServiceEvaluation，1:1 一会话一评价）====================

/** 评价状态：0已隐藏/1正常 */
export const EVALUATION_STATUS_OPTIONS = [
  { label: '已隐藏', value: 0 },
  { label: '正常', value: 1 }
] as const

/** 是否匿名：0否/1是 */
export const EVALUATION_IS_ANONYMOUS_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 }
] as const

/**
 * 服务评价实体（后端 ServiceEvaluationVO）。
 *
 * 一会话一评价（业务约束，非 upsert）：前端需先 list?sessionCode 判断有无评价，
 * 有则 PUT /{id}，无则 POST。create 时后端校验已存在则抛业务异常。
 *
 * 主键 id 雪花 Long（无业务 code），路径参数用 id。
 * 4 维评分均 1-5，建议 el-rate。
 */
export interface ServiceEvaluation {
  id?: number
  sessionCode: string
  clientCode?: string
  butlerCode?: string
  parkCode?: string
  /** 服务态度评分 1-5 */
  attitudeRating?: number
  /** 专业度评分 1-5 */
  professionalRating?: number
  /** 响应速度评分 1-5 */
  responsivenessRating?: number
  /** 满意度评分 1-5 */
  satisfactionRating?: number
  content?: string
  /** 评价图片（JSON 数组字符串） */
  imageUrls?: string
  /** 是否匿名：0否/1是 */
  isAnonymous?: number
  /** 回复内容（运营回复） */
  replyContent?: string
  replyTime?: string
  replyByCode?: string
  /** 状态：0已隐藏/1正常 */
  status?: number
  createdAt?: string
}

export interface ServiceEvaluationQuery extends PageQuery {
  sessionCode?: string
  clientCode?: string
  butlerCode?: string
  parkCode?: string
  isAnonymous?: number
  status?: number
}

// ==================== 子表：权益需求（ServiceEquityDemand）====================

/** 需求类型：1机构入住/2日间照料/3居家护理/4场景活动/5旅居 */
export const DEMAND_TYPE_OPTIONS = [
  { label: '机构入住', value: 1 },
  { label: '日间照料', value: 2 },
  { label: '居家护理', value: 3 },
  { label: '场景活动', value: 4 },
  { label: '旅居', value: 5 }
] as const

/** 联系偏好：1电话/2微信/3短信 */
export const CONTACT_PREFERENCE_OPTIONS = [
  { label: '电话', value: 1 },
  { label: '微信', value: 2 },
  { label: '短信', value: 3 }
] as const

/** 收集方式：1电话沟通/2上门拜访/3在线填写/4代理人转述 */
export const COLLECT_METHOD_OPTIONS = [
  { label: '电话沟通', value: 1 },
  { label: '上门拜访', value: 2 },
  { label: '在线填写', value: 3 },
  { label: '代理人转述', value: 4 }
] as const

/** 需求状态：0待处理/1已整理/2已确认 */
export const DEMAND_STATUS_OPTIONS = [
  { label: '待处理', value: 0 },
  { label: '已整理', value: 1 },
  { label: '已确认', value: 2 }
] as const

/**
 * 权益需求实体（后端 ServiceEquityDemandVO）。
 *
 * 主键 id 雪花 Long，业务键 demandCode（DM+10，服务端生成）。
 * collectTime 服务端取当前时间（前端不传）；status create 时固定 0。
 * UpdateDTO 不含 sessionCode/clientCode/butlerCode/demandCode/collectTime。
 */
export interface ServiceEquityDemand {
  id?: number
  sessionCode: string
  /** 需求编码（DM+10，服务端生成） */
  demandCode?: string
  clientCode: string
  butlerCode?: string
  demandType?: number
  usePersonName?: string
  usePersonAge?: number
  usePersonGender?: number
  healthSummary?: string
  careLevelNeed?: number
  /** 城市偏好（JSON 数组） */
  cityPreference?: string
  /** 区域偏好（JSON 数组） */
  areaPreference?: string
  budgetMin?: number
  budgetMax?: number
  /** 房间偏好（JSON 数组） */
  roomPreference?: string
  foodPreference?: string
  specialNeeds?: string
  expectedTime?: string
  contactPreference?: number
  collectMethod?: number
  /** 收集时间（服务端取当前时间） */
  collectTime?: string
  demandSummary?: string
  /** 需求资料图片（JSON 数组） */
  demandImages?: string
  /** 状态：0待处理/1已整理/2已确认（create 固定 0） */
  status?: number
  remark?: string
  createdAt?: string
}

export interface ServiceEquityDemandQuery extends PageQuery {
  sessionCode?: string
  demandCode?: string
  clientCode?: string
  butlerCode?: string
  demandType?: number
  status?: number
}

// ==================== 子表：权益方案（ServiceEquitySolution）====================

/** 方案类型：1推荐/2备选 */
export const SOLUTION_TYPE_OPTIONS = [
  { label: '推荐方案', value: 1 },
  { label: '备选方案', value: 2 }
] as const

/** 呈现方式：1当面/2电话/3文档发送 */
export const PRESENTATION_METHOD_OPTIONS = [
  { label: '当面', value: 1 },
  { label: '电话', value: 2 },
  { label: '文档发送', value: 3 }
] as const

/** 客户是否接受：0否/1是/2需调整 */
export const SOLUTION_IS_ACCEPTED_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 },
  { label: '需调整', value: 2 }
] as const

/** 方案状态：0制定中/1待呈现/2已呈现/3已确认/4已拒绝/5需调整 */
export const SOLUTION_STATUS_OPTIONS = [
  { label: '制定中', value: 0 },
  { label: '待呈现', value: 1 },
  { label: '已呈现', value: 2 },
  { label: '已确认', value: 3 },
  { label: '已拒绝', value: 4 },
  { label: '需调整', value: 5 }
] as const

/**
 * 权益方案实体（后端 ServiceEquitySolutionVO）。
 *
 * 主键 id 雪花 Long，业务键 solutionCode（SO+10，服务端生成）。
 * demandCode 必填（关联需求，业务链 demand→solution）。
 * 有独立 /accept 端点（切换 isAccepted，会话 confirm_solution 依赖 isAccepted=1）。
 * UpdateDTO 不含 sessionCode/demandCode/clientCode/butlerCode/solutionCode/presentationTime/adjustCount。
 */
export interface ServiceEquitySolution {
  id?: number
  sessionCode: string
  /** 关联需求编码（外键→demand，必填） */
  demandCode: string
  clientCode: string
  butlerCode?: string
  /** 方案编码（SO+10，服务端生成） */
  solutionCode?: string
  solutionName?: string
  solutionType?: number
  /** 推荐机构列表（JSON 数组） */
  recommendedParks?: string
  planSummary?: string
  /** 服务项目明细（JSON 数组） */
  serviceItems?: string
  estimatedCost?: number
  /** 费用明细（JSON） */
  costBreakdown?: string
  timeline?: string
  advantages?: string
  risks?: string
  comparison?: string
  presentationTime?: string
  presentationMethod?: number
  clientFeedback?: string
  /** 客户是否接受：0否/1是/2需调整（通过 /accept 端点切换） */
  isAccepted?: number
  /** 调整次数（默认 0） */
  adjustCount?: number
  /** 状态：0制定中/1待呈现/2已呈现/3已确认/4已拒绝/5需调整 */
  status?: number
  remark?: string
  createdAt?: string
}

export interface ServiceEquitySolutionQuery extends PageQuery {
  sessionCode?: string
  solutionCode?: string
  demandCode?: string
  clientCode?: string
  butlerCode?: string
  solutionType?: number
  isAccepted?: number
  status?: number
}

// ==================== 子表：全程安排（ServiceEquityArrange）====================

/** 安排类型：1参观预约/2入住安排/3活动报名/4服务预约/5交通安排/6其他 */
export const ARRANGE_TYPE_OPTIONS = [
  { label: '参观预约', value: 1 },
  { label: '入住安排', value: 2 },
  { label: '活动报名', value: 3 },
  { label: '服务预约', value: 4 },
  { label: '交通安排', value: 5 },
  { label: '其他', value: 6 }
] as const

/** 安排状态：0待安排/1已安排/2进行中/3已完成/4已取消 */
export const ARRANGE_STATUS_OPTIONS = [
  { label: '待安排', value: 0 },
  { label: '已安排', value: 1 },
  { label: '进行中', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 }
] as const

/**
 * 权益安排实体（后端 ServiceEquityArrangeVO）。
 *
 * 主键 id 雪花 Long，业务键 arrangeCode（AR+10，服务端生成）。
 * solutionCode 可空（软关联方案）。有独立 /confirm 端点（isConfirmed=1 时自动写 confirmTime，
 * 会话 start_service 依赖 isConfirmed=1）。
 * UpdateDTO 不含 sessionCode/clientCode/butlerCode/arrangeCode/confirmTime/completeTime/isConfirmed。
 */
export interface ServiceEquityArrange {
  id?: number
  sessionCode: string
  /** 关联方案编码（外键→solution，可空） */
  solutionCode?: string
  clientCode: string
  butlerCode?: string
  /** 安排编码（AR+10，服务端生成） */
  arrangeCode?: string
  arrangeType?: number
  parkCode?: string
  parkFullName?: string
  arrangeDate?: string
  arrangeTimeStart?: string
  arrangeTimeEnd?: string
  arrangeAddress?: string
  contactPerson?: string
  contactPhone?: string
  participantCount?: number
  /** 准备事项（JSON 数组） */
  prepareItems?: string
  progressNotes?: string
  confirmTime?: string
  completeTime?: string
  /** 是否已确认：0否/1是（通过 /confirm 端点切换，confirm 后自动写 confirmTime） */
  isConfirmed?: number
  /** 状态：0待安排/1已安排/2进行中/3已完成/4已取消 */
  status?: number
  cancelReason?: string
  remark?: string
  createdAt?: string
}

export interface ServiceEquityArrangeQuery extends PageQuery {
  sessionCode?: string
  arrangeCode?: string
  solutionCode?: string
  clientCode?: string
  butlerCode?: string
  arrangeType?: number
  isConfirmed?: number
  status?: number
}

// ==================== 子表：回访品控（ServiceEquityFollowup）====================

/** 回访类型：1服务后回访/2入住后回访/3定期回访/4投诉回访 */
export const FOLLOWUP_TYPE_OPTIONS = [
  { label: '服务后回访', value: 1 },
  { label: '入住后回访', value: 2 },
  { label: '定期回访', value: 3 },
  { label: '投诉回访', value: 4 }
] as const

/** 回访方式：1电话/2微信/3上门/4问卷 */
export const FOLLOWUP_METHOD_OPTIONS = [
  { label: '电话', value: 1 },
  { label: '微信', value: 2 },
  { label: '上门', value: 3 },
  { label: '问卷', value: 4 }
] as const

/** 回访状态：0待回访/1回访中/2已完成/3需再跟进 */
export const FOLLOWUP_STATUS_OPTIONS = [
  { label: '待回访', value: 0 },
  { label: '回访中', value: 1 },
  { label: '已完成', value: 2 },
  { label: '需再跟进', value: 3 }
] as const

/** 是否（isFollowupNeeded/isResolved 共用 0/1） */
export const FOLLOWUP_YES_NO_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 }
] as const

/**
 * 回访品控实体（后端 ServiceEquityFollowupVO）。
 *
 * 主键 id 雪花 Long，业务键 followupCode（FU+10，服务端生成）。
 * arrangeCode 可空（软关联安排）。
 * 服务端自动逻辑（create 时）：isFollowupNeeded 由满意度算（任一<3 则1）、
 * nextFollowupDate 自动 +7 天、status 固定 2。前端 create 表单不含这些字段。
 * 4 维满意度均 1-5，建议 el-rate。
 * UpdateDTO 不含 sessionCode/arrangeCode/clientCode/butlerCode/followupCode/followupTime。
 */
export interface ServiceEquityFollowup {
  id?: number
  sessionCode: string
  /** 关联安排编码（外键→arrange，可空） */
  arrangeCode?: string
  clientCode: string
  butlerCode?: string
  /** 回访编码（FU+10，服务端生成） */
  followupCode?: string
  followupType?: number
  followupMethod?: number
  followupDate?: string
  /** 回访时间（服务端控制） */
  followupTime?: string
  /** 服务满意度 1-5 */
  serviceSatisfaction?: number
  /** 机构满意度 1-5 */
  parkSatisfaction?: number
  /** 管家满意度 1-5 */
  butlerSatisfaction?: number
  /** 综合满意度 1-5 */
  overallSatisfaction?: number
  serviceEvaluation?: string
  improvementSuggestions?: string
  complaints?: string
  complaintHandle?: string
  /** 是否需要后续跟进：0否/1是（服务端按满意度自动算，update 可手改） */
  isFollowupNeeded?: number
  followupPlan?: string
  /** 下次回访日期（服务端可能自动 +7 天） */
  nextFollowupDate?: string
  /** 问题是否已解决：0否/1是 */
  isResolved?: number
  /** 状态：0待回访/1回访中/2已完成/3需再跟进（create 固定 2） */
  status?: number
  remark?: string
  createdAt?: string
}

export interface ServiceEquityFollowupQuery extends PageQuery {
  sessionCode?: string
  followupCode?: string
  arrangeCode?: string
  clientCode?: string
  butlerCode?: string
  followupType?: number
  status?: number
}
