/**
 * 客户相关类型。
 *
 * 字段对齐后端 com.dayan.client（表 client_info 及子表，位于 dayan-module-client）。
 *
 * 注意：后端客户控制器为 RESTful 复数风格（/admin-api/clients），list 接口
 * 返回 PageResult（有分页字段），但 url 无 /page 后缀。
 *
 * 枚举档位说明（重要）：
 * - ClientStatus 为 3 态（0禁用 / 1正常 / 2冻结），对齐 client_info.status DDL 注释。
 * - ClientLevel 为 4 档（普通/银卡/金卡/钻石）。DDL 注释写作"1普通 2VIP 3SVIP"系过时，
 *   以前端 4 档为准（与列表页一致）。
 *
 * 子表枚举字段（careLevel/targetType/bloodType/mobilityLevel/cognitiveLevel/mentalStatus/
 * sleepQuality/accountStatus 等）后端 VO 暂无 @Schema 文档，前端暂用 el-input-number 兜底，
 * 待后端补文档后改为 el-select（详见各 Tab 内 TODO 注释）。
 */

import type { PageQuery } from '@/types/common'

/** 性别：0未知 1男 2女 */
export enum Gender {
  UNKNOWN = 0,
  MALE = 1,
  FEMALE = 2
}

/** 性别选项 */
export const GENDER_OPTIONS = [
  { label: '未知', value: Gender.UNKNOWN },
  { label: '男', value: Gender.MALE },
  { label: '女', value: Gender.FEMALE }
] as const

/** 客户等级：1普通 2银卡 3金卡 4钻石 */
export enum ClientLevel {
  NORMAL = 1,
  SILVER = 2,
  GOLD = 3,
  DIAMOND = 4
}

/** 客户等级选项 */
export const CLIENT_LEVEL_OPTIONS = [
  { label: '普通', value: ClientLevel.NORMAL },
  { label: '银卡', value: ClientLevel.SILVER },
  { label: '金卡', value: ClientLevel.GOLD },
  { label: '钻石', value: ClientLevel.DIAMOND }
] as const

/** 客户状态：0禁用 1正常 2冻结 */
export enum ClientStatus {
  DISABLED = 0,
  ENABLED = 1,
  FROZEN = 2
}

/** 客户状态选项 */
export const CLIENT_STATUS_OPTIONS = [
  { label: '正常', value: ClientStatus.ENABLED },
  { label: '禁用', value: ClientStatus.DISABLED },
  { label: '冻结', value: ClientStatus.FROZEN }
] as const

/**
 * 客户状态标签文本（用于列表/摘要展示）。
 */
export function clientStatusLabel(v?: number): string {
  const found = CLIENT_STATUS_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

/**
 * 客户状态 el-tag 配色：0禁用=info，1正常=success，2冻结=warning。
 */
export function clientStatusTagType(v?: number): 'success' | 'info' | 'warning' {
  switch (v) {
    case ClientStatus.ENABLED:
      return 'success'
    case ClientStatus.FROZEN:
      return 'warning'
    case ClientStatus.DISABLED:
    default:
      return 'info'
  }
}

/** 客户等级标签文本（兼容数字直传）。 */
export function clientLevelLabel(v?: number): string {
  const found = CLIENT_LEVEL_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 客户等级 el-tag 配色。 */
export function clientLevelTagType(v?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (v) {
    case ClientLevel.DIAMOND:
      return 'danger'
    case ClientLevel.GOLD:
      return 'warning'
    case ClientLevel.SILVER:
      return 'success'
    case ClientLevel.NORMAL:
    default:
      return 'info'
  }
}

/** 是否 VIP：0否 1是 */
export enum VipFlag {
  NO = 0,
  YES = 1
}

/** VIP 选项 */
export const VIP_OPTIONS = [
  { label: '否', value: VipFlag.NO },
  { label: '是', value: VipFlag.YES }
] as const

/** 学历：1小学 2初中 3高中 4专科 5本科 6硕士 7博士 */
export enum Education {
  PRIMARY = 1,
  JUNIOR = 2,
  SENIOR = 3,
  COLLEGE = 4,
  BACHELOR = 5,
  MASTER = 6,
  DOCTOR = 7
}

/** 学历选项 */
export const EDUCATION_OPTIONS = [
  { label: '小学', value: Education.PRIMARY },
  { label: '初中', value: Education.JUNIOR },
  { label: '高中', value: Education.SENIOR },
  { label: '专科', value: Education.COLLEGE },
  { label: '本科', value: Education.BACHELOR },
  { label: '硕士', value: Education.MASTER },
  { label: '博士', value: Education.DOCTOR }
] as const

/**
 * 客户信息实体（后端 ClientInfo，共 31 个字段）。
 */
export interface ClientInfo {
  id?: number
  /** 客户编码（主键，服务端生成） */
  clientCode?: string
  /** 所属渠道编码 */
  channelCode?: string
  /** 姓名 */
  fullName: string
  /** 性别：0未知 1男 2女 */
  gender?: Gender
  /** 头像 */
  avatar?: string
  /** 生日（yyyy-MM-dd） */
  birthday?: string
  /** 年龄（统计字段） */
  age?: number
  /** 身份证号 */
  idCard?: string
  /** 手机号 */
  phone?: string
  /** 邮箱 */
  email?: string
  /** 省级编码 */
  provinceCode?: string
  /** 市级编码 */
  cityCode?: string
  /** 区县编码 */
  districtCode?: string
  /** 详细地址 */
  address?: string
  /** 学历 */
  education?: Education
  /** 婚姻状况 */
  maritalStatus?: number
  /** 职业 */
  profession?: string
  /** 来源类型 */
  sourceType?: number
  /** 客户等级：1普通 2银卡 3金卡 4钻石 */
  clientLevel?: ClientLevel
  /** 权益数量（统计字段） */
  equityCount?: number
  /** 服务次数（统计字段） */
  serviceCount?: number
  /** 是否 VIP：0否 1是 */
  isVip?: VipFlag
  /** 状态：0禁用 1正常 2冻结 */
  status?: ClientStatus
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 客户分页查询参数（后端 ClientInfoQueryDTO）。
 */
export interface ClientInfoQuery extends PageQuery {
  /** 所属渠道编码 */
  channelCode?: string
  /** 客户编码 */
  clientCode?: string
  /** 姓名（模糊匹配） */
  fullName?: string
  /** 手机号（模糊匹配） */
  phone?: string
  /** 性别 */
  gender?: Gender
  /** 客户等级 */
  clientLevel?: ClientLevel
  /** 是否 VIP：0否 1是 */
  isVip?: VipFlag
  /** 状态：0禁用 1正常 2冻结 */
  status?: ClientStatus
}

// ============================================================================
// 子表类型（客户域 6 个子表，对齐后端 com.dayan.client.vo.*）
// ============================================================================

/**
 * 客户账号（ClientAccount，主键 clientCode，非 id）。
 *
 * 注意：update/delete/reset-password 都用 clientCode 作 path 变量；
 * VO 不含 password（不返回）；账号状态 accountStatus 后端文档为 0=锁定 1=正常 2=禁用。
 */
export interface ClientAccount {
  /** 自增 id（仅列表展示用，不作为主键参与接口） */
  id?: number
  /** 客户编码（业务主键） */
  clientCode: string
  /** 所属渠道编码 */
  channelCode?: string
  /** 用户名 */
  username?: string
  /** 手机号 */
  phone?: string
  /** 微信 openId */
  openId?: string
  /** 微信 unionId */
  unionId?: string
  /** 支付宝账号 */
  alipayId?: string
  /** 外部账号 */
  extAccountNo?: string
  /** 最后登录时间 */
  lastLoginTime?: string
  /** 最后登录 IP */
  lastLoginIp?: string
  /** 登录次数 */
  loginCount?: number
  /** 账号状态：0锁定 1正常 2禁用（ClientAccountCreateDTO 文档） */
  accountStatus?: number
  createdAt?: string
}

/**
 * 客户账号分页查询参数（ClientAccountQueryDTO）。
 */
export interface ClientAccountQuery extends PageQuery {
  /** 客户编码（详情页 tab 固定携带） */
  clientCode?: string
  /** 用户名 */
  username?: string
  /** 手机号 */
  phone?: string
  /** 账号状态 */
  accountStatus?: number
}

/**
 * 客户家庭成员（ClientFamilyMember，主键自增 id）。
 */
export interface ClientFamilyMember {
  /** 自增 id */
  id?: number
  /** 客户编码 */
  clientCode: string
  /** 成员姓名 */
  memberName: string
  /** 关系（如 父亲/母亲/配偶） */
  relation?: string
  /** 性别：0未知 1男 2女 */
  gender?: Gender
  /** 手机号 */
  phone?: string
  /** 邮箱 */
  email?: string
  /** 是否紧急联系人：0否 1是 */
  isEmergencyContact?: number
  /** 是否主要联系人：0否 1是 */
  isPrimaryContact?: number
  /** 是否决策人：0否 1是 */
  isDecisionMaker?: number
  /** 地址 */
  address?: string
  /** 备注 */
  remark?: string
  /** 状态：0禁用 1启用 */
  status?: number
  /** 排序号 */
  sortOrder?: number
  createdAt?: string
}

/**
 * 客户收货地址（ClientAddress，主键自增 id）。
 *
 * fullAddress 由后端拼装（province+city+district+detail），编辑表单不含 fullAddress。
 * 默认地址互斥：后端在 create/update/setDefault 时自动处理。
 */
export interface ClientAddress {
  /** 自增 id */
  id?: number
  /** 客户编码 */
  clientCode: string
  /** 收件人姓名 */
  receiverName?: string
  /** 收件人电话 */
  receiverPhone?: string
  /** 省级编码 */
  provinceCode?: string
  /** 市级编码 */
  cityCode?: string
  /** 区县编码 */
  districtCode?: string
  /** 详细地址 */
  detailAddress?: string
  /** 完整地址（后端拼装，只读） */
  fullAddress?: string
  /** 是否默认：0否 1是（互斥，后端保证同 clientCode 仅一条为 1） */
  isDefault?: number
  /** 地址标签（如 家/公司） */
  tag?: string
  createdAt?: string
}

/**
 * 客户健康档案（ClientHealthProfile，一客户一档案，主键 clientCode）。
 *
 * 后端无 update 端点：编辑走 POST（saveOrUpdate 语义，upsert）。
 * lastAssessmentTime 由后端自动设为 now()，表单不编辑。
 */
export interface ClientHealthProfile {
  /** 自增 id */
  id?: number
  /** 客户编码（业务主键） */
  clientCode: string
  /** 身高(cm) */
  height?: number
  /** 体重(kg) */
  weight?: number
  // TODO: bloodType 枚举值待后端补 @Schema 文档后改为 select
  /** 血型 */
  bloodType?: number
  /** 血压（如 120/80） */
  bloodPressure?: string
  /** 血糖 */
  bloodSugar?: number
  /** 心率 */
  heartRate?: number
  /** 慢性病列表（JSON 字符串） */
  chronicDiseases?: string
  /** 过敏史（JSON 字符串） */
  allergyHistory?: string
  /** 手术史（JSON 字符串） */
  surgeryHistory?: string
  /** 家族病史（JSON 字符串） */
  familyHistory?: string
  /** 当前用药信息（JSON 字符串） */
  medicationInfo?: string
  // TODO: mobilityLevel 枚举值待后端补 @Schema 文档后改为 select
  /** 行动能力等级 */
  mobilityLevel?: number
  // TODO: cognitiveLevel 枚举值待后端补 @Schema 文档后改为 select
  /** 认知等级 */
  cognitiveLevel?: number
  // TODO: mentalStatus 枚举值待后端补 @Schema 文档后改为 select
  /** 精神状态 */
  mentalStatus?: number
  /** 饮食偏好 */
  dietPreference?: string
  // TODO: sleepQuality 枚举值待后端补 @Schema 文档后改为 select
  /** 睡眠质量 */
  sleepQuality?: number
  /** 紧急联系人姓名 */
  emergencyContactName?: string
  /** 紧急联系人电话 */
  emergencyContactPhone?: string
  /** 紧急联系人关系 */
  emergencyContactRelation?: string
  /** 健康评分 */
  healthScore?: number
  /** 最近评估时间（后端自动设为 now()，只读） */
  lastAssessmentTime?: string
  /** 备注 */
  remark?: string
  createdAt?: string
}

/**
 * 客户照护需求评估（ClientCareNeed，主键自增 id）。
 */
export interface ClientCareNeed {
  /** 自增 id */
  id?: number
  /** 客户编码 */
  clientCode: string
  /** 评估管家编码 */
  butlerCode?: string
  /** 评估管家姓名（快照） */
  butlerFullName?: string
  /** 评估日期（yyyy-MM-dd） */
  evalDate?: string
  // TODO: careLevel 枚举值待后端补 @Schema 文档后改为 select
  /** 照护等级 */
  careLevel?: number
  /** 照护类型偏好 */
  careTypePreference?: string
  /** 居住偏好 */
  livingPreference?: string
  /** 餐饮偏好 */
  foodPreference?: string
  /** 预算下限 */
  budgetMin?: number
  /** 预算上限 */
  budgetMax?: number
  /** 区域偏好 */
  areaPreference?: string
  /** 特殊需求 */
  specialRequirements?: string
  /** 期望入住日期（yyyy-MM-dd） */
  expectedCheckinDate?: string
  /** 推荐机构 */
  parkRecommendations?: string
  /** 评估结果 */
  evalResult?: string
  /** 状态：0禁用 1启用 */
  status?: number
  /** 备注 */
  remark?: string
  createdAt?: string
}

/**
 * 客户照护需求分页查询参数（ClientCareNeedQueryDTO）。
 */
export interface ClientCareNeedQuery extends PageQuery {
  /** 客户编码（详情页 tab 固定携带） */
  clientCode?: string
  /** 照护等级 */
  careLevel?: number
  /** 状态 */
  status?: number
}

/**
 * 客户收藏（ClientFavorite，主键自增 id，无 update 端点）。
 *
 * 收藏不可编辑，要改就先删再加。
 */
export interface ClientFavorite {
  /** 自增 id */
  id?: number
  /** 客户编码 */
  clientCode: string
  // TODO: targetType 枚举值待后端补 @Schema 文档后改为 select
  /** 收藏对象类型 */
  targetType: number
  /** 收藏对象编码 */
  targetCode: string
  /** 收藏对象名称 */
  targetName?: string
  /** 备注 */
  remark?: string
  createdAt?: string
}

/**
 * 客户收藏分页查询参数（ClientFavoriteQueryDTO）。
 */
export interface ClientFavoriteQuery extends PageQuery {
  /** 客户编码（详情页 tab 固定携带） */
  clientCode?: string
  /** 收藏对象类型 */
  targetType?: number
  /** 收藏对象编码 */
  targetCode?: string
}
