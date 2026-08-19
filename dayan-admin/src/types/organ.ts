/**
 * 机构（组织）相关类型。
 *
 * 字段对齐后端 com.dayan.organ.entity.OrganInfo / vo.OrganInfoVO。
 */

/** 机构状态：1启用 0禁用 */
export enum OrganStatus {
  ENABLED = 1,
  DISABLED = 0
}

/** 机构状态选项 */
export const ORGAN_STATUS_OPTIONS = [
  { label: '启用', value: OrganStatus.ENABLED },
  { label: '禁用', value: OrganStatus.DISABLED }
] as const

/** 机构类型 */
export enum OrganType {
  /** 养老院 */
  NURSING_HOME = 1,
  /** 社区中心 */
  COMMUNITY_CENTER = 2,
  /** 分公司 */
  BRANCH = 3,
}

/** 机构类型选项 */
export const ORGAN_TYPE_OPTIONS = [
  { label: '运营方', value: OrganType.NURSING_HOME },
  { label: '子公司', value: OrganType.COMMUNITY_CENTER },
  { label: '分公司', value: OrganType.BRANCH }
] as const

/**
 * 机构实体（后端 OrganInfo / OrganInfoVO）。
 *
 * 列表 / 详情接口字段；create 时 organCode 由后端自动生成。
 */
export interface Organ {
  id?: number
  /** 机构编码（主键业务码，后端生成） */
  organCode?: string
  /** 机构全称 */
  fullName: string
  /** 简称 */
  shortName?: string
  /** 机构类型：1运营方 2子公司 3分公司 */
  organType?: OrganType
  /** 统一社会信用代码 */
  unifiedCreditCode?: string
  /** 法定代表人 */
  legalPerson?: string
  /** 注册资本（万元） */
  registeredCapital?: number
  /** 成立日期 */
  establishDate?: string
  /** 经营范围 */
  businessScope?: string
  /** 省编码 */
  provinceCode?: string
  /** 市编码 */
  cityCode?: string
  /** 区县编码 */
  districtCode?: string
  /** 详细地址 */
  address?: string
  /** 联系人 */
  contactPerson?: string
  /** 联系电话 */
  contactPhone?: string
  /** 联系邮箱 */
  contactEmail?: string
  /** Logo 地址 */
  logoUrl?: string
  /** 官网 */
  website?: string
  /** 机构简介 */
  description?: string
  /** 状态：1启用 0禁用 */
  status: OrganStatus
  /** 排序号 */
  sortOrder?: number
  /** 备注 */
  remark?: string
}

/** 机构分页查询参数 */
export interface OrganQuery {
  /** 机构编码（精确，可选） */
  organCode?: string
  /** 机构全称（模糊匹配，可选） */
  fullName?: string
  /** 机构类型（可选） */
  organType?: OrganType
  /** 状态（可选） */
  status?: OrganStatus
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}

/** 机构精简信息（下拉选择用，对应后端 OrganInfoSimpleVO） */
export interface OrganSimple {
  organCode: string
  fullName?: string
  shortName?: string
  organType?: OrganType
  status?: number
}
