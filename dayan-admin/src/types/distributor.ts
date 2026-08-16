/**
 * 分销商相关类型。
 *
 * 字段对齐后端 com.dayan.distributor.entity.DistributorInfo 及
 * DistributorInfoQueryDTO / DistributorInfoCreateDTO。
 */
import type { PageQuery } from '@/types/common'

/**
 * 主体类型：1=企业 / 2=个人。
 */
export enum SubjectType {
  /** 企业 */
  ENTERPRISE = 1,
  /** 个人 */
  PERSONAL = 2
}

/** 主体类型选项 */
export const SUBJECT_TYPE_OPTIONS = [
  { label: '企业', value: SubjectType.ENTERPRISE },
  { label: '个人', value: SubjectType.PERSONAL }
] as const

/**
 * 性别：0=未知 / 1=男 / 2=女。
 */
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

/**
 * 分销商状态：0=待审核 / 1=已合作 / 2=已暂停 / 3=已终止。
 */
export enum DistributorStatus {
  /** 待审核 */
  PENDING = 0,
  /** 已合作 */
  COOPERATING = 1,
  /** 已暂停 */
  SUSPENDED = 2,
  /** 已终止 */
  TERMINATED = 3
}

/** 分销商状态选项 */
export const DISTRIBUTOR_STATUS_OPTIONS = [
  { label: '待审核', value: DistributorStatus.PENDING },
  { label: '已合作', value: DistributorStatus.COOPERATING },
  { label: '已暂停', value: DistributorStatus.SUSPENDED },
  { label: '已终止', value: DistributorStatus.TERMINATED }
] as const

/**
 * 分销商信息实体（后端 DistributorInfo，表 distributor_info）。
 *
 * 主键 distributorCode 由服务端 CodeGenerator 生成，新增表单不包含此字段。
 */
export interface DistributorInfo {
  id?: number
  /** 分销商编码（服务端生成） */
  distributorCode?: string
  /** 分销商全称 */
  fullName: string
  /** 简称 */
  shortName?: string
  /** 主体类型：1企业/2个人 */
  subjectType?: number
  /** 统一社会信用代码 */
  unifiedCreditCode?: string
  /** 法定代表人 */
  legalPerson?: string
  /** 营业执照号 */
  businessLicenseNo?: string
  /** 注册资本 */
  registeredCapital?: number
  /** 成立日期（yyyy-MM-dd） */
  establishDate?: string
  /** 身份证号（个人，加密存储） */
  idCard?: string
  /** 性别：0未知/1男/2女 */
  gender?: number
  /** 联系电话 */
  phone?: string
  /** 联系人 */
  contactPerson?: string
  /** 联系邮箱 */
  contactEmail?: string
  /** 省级编码 */
  provinceCode?: string
  /** 市级编码 */
  cityCode?: string
  /** 区县编码 */
  districtCode?: string
  /** 详细地址 */
  address?: string
  /** 开户行 */
  bankName?: string
  /** 银行账号 */
  bankAccount?: string
  /** 开户名 */
  bankAccountName?: string
  /** 状态：0待审核/1已合作/2已暂停/3已终止 */
  status?: number
  /** 排序号 */
  sortOrder?: number
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 分销商分页查询参数（后端 DistributorInfoQueryDTO）。
 *
 * 后端字段：current / size / distributorCode / fullName / subjectType /
 * unifiedCreditCode / phone / status。
 */
export interface DistributorInfoQuery extends PageQuery {
  distributorCode?: string
  fullName?: string
  subjectType?: number
  unifiedCreditCode?: string
  phone?: string
  status?: number
}
