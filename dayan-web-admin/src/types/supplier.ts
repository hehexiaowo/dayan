/**
 * 供应商相关类型。
 *
 * 字段对齐后端 com.dayan.supplier.entity.SupplierInfo 及
 * SupplierInfoQueryDTO / SupplierAuditDTO。
 */
import type { PageQuery } from '@/types/common'
import { AuditStatus, AUDIT_STATUS_OPTIONS } from '@/types/scene'

/**
 * 供应商类型：1=机构 / 2=服务商 / 3=商品供应商。
 */
export enum SupplierType {
  /** 机构 */
  ORGANIZATION = 1,
  /** 服务商 */
  SERVICE_PROVIDER = 2,
  /** 商品供应商 */
  GOODS_SUPPLIER = 3
}

/** 供应商类型选项 */
export const SUPPLIER_TYPE_OPTIONS = [
  { label: '机构', value: SupplierType.ORGANIZATION },
  { label: '服务商', value: SupplierType.SERVICE_PROVIDER },
  { label: '商品供应商', value: SupplierType.GOODS_SUPPLIER }
] as const

/**
 * 供应商合作状态（对齐 DDL supplier_info.status 注释）：
 * - 0 = 待审核（新建默认，等待平台审核）
 * - 1 = 已合作（审核通过，正常业务可见）
 * - 2 = 已暂停（冻结，暂时不可合作）
 * - 3 = 已终止（永久停止合作）
 *
 * 注意：status 由后端审核流程驱动（create 强制 0，audit 通过置 1），
 * 前端表单字段仅作展示，提交后端不会据此覆盖。
 */
export enum SupplierStatus {
  /** 待审核 */
  PENDING_AUDIT = 0,
  /** 已合作 */
  COOPERATING = 1,
  /** 已暂停（冻结） */
  SUSPENDED = 2,
  /** 已终止 */
  TERMINATED = 3
}

export const SUPPLIER_STATUS_OPTIONS = [
  { label: '待审核', value: SupplierStatus.PENDING_AUDIT },
  { label: '已合作', value: SupplierStatus.COOPERATING },
  { label: '已暂停', value: SupplierStatus.SUSPENDED },
  { label: '已终止', value: SupplierStatus.TERMINATED }
] as const

/**
 * 审核状态（复用 scene 的枚举）：0待审 / 1通过 / 2驳回。
 */
export { AuditStatus as SupplierAuditStatus }
export { AUDIT_STATUS_OPTIONS as SUPPLIER_AUDIT_STATUS_OPTIONS }

/**
 * 供应商信息实体（后端 SupplierInfo，表 supplier_info）。
 *
 * 主键 supplierCode 由服务端 CodeGenerator 生成，新增表单不包含此字段。
 */
export interface SupplierInfo {
  id?: number
  /** 供应商编码（服务端生成） */
  supplierCode?: string
  /** 供应商全称 */
  fullName: string
  /** 简称 */
  shortName?: string
  /** 供应商类型：1机构/2服务商/3商品供应商 */
  supplierType?: number
  /** 统一社会信用代码 */
  unifiedCreditCode?: string
  /** 法定代表人 */
  legalPerson?: string
  /** 注册资本 */
  registeredCapital?: number
  /** 成立日期（yyyy-MM-dd） */
  establishDate?: string
  /** 营业执照号 */
  businessLicenseNo?: string
  /** 经营范围 */
  businessScope?: string
  /** 省级编码 */
  provinceCode?: string
  /** 市级编码 */
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
  /** Logo URL */
  logoUrl?: string
  /** 描述 */
  description?: string
  /** 默认佣金比例 */
  commissionRate?: number
  /** 状态：0待审核 / 1已合作 / 2已暂停 / 3已终止 */
  status?: number
  /** 审核状态：0待审/1通过/2驳回 */
  auditStatus?: number
  /** 审核备注 */
  auditRemark?: string
  /** 排序号 */
  sortOrder?: number
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 供应商分页查询参数（后端 SupplierInfoQueryDTO）。
 *
 * 后端字段：current / size / supplierCode / fullName / shortName / supplierType /
 * unifiedCreditCode / status / auditStatus。
 */
export interface SupplierInfoQuery extends PageQuery {
  supplierCode?: string
  fullName?: string
  shortName?: string
  supplierType?: number
  unifiedCreditCode?: string
  status?: number
  auditStatus?: number
}

/**
 * 供应商审核参数（后端 SupplierAuditDTO）。
 */
export interface SupplierAuditDTO {
  supplierCode: string
  auditStatus: number
  auditRemark?: string
}

// =========================================================================
// 供应商合同 / 联系人 / 评价（任务 3 新增）
// =========================================================================

/**
 * 合同状态（对齐 DDL supplier_contract.status 注释）：
 * - 0 草稿 / 1 待审核 / 2 已生效 / 3 已到期 / 4 已终止 / 5 已作废
 *
 * 后端不校验状态流转合法性，前端严格按状态守卫表显示流转按钮。
 */
export enum ContractStatus {
  /** 草稿 */
  DRAFT = 0,
  /** 待审核 */
  PENDING_AUDIT = 1,
  /** 已生效 */
  EFFECTIVE = 2,
  /** 已到期 */
  EXPIRED = 3,
  /** 已终止 */
  TERMINATED = 4,
  /** 已作废 */
  VOID = 5
}

export const CONTRACT_STATUS_OPTIONS = [
  { label: '草稿', value: ContractStatus.DRAFT },
  { label: '待审核', value: ContractStatus.PENDING_AUDIT },
  { label: '已生效', value: ContractStatus.EFFECTIVE },
  { label: '已到期', value: ContractStatus.EXPIRED },
  { label: '已终止', value: ContractStatus.TERMINATED },
  { label: '已作废', value: ContractStatus.VOID }
] as const

/** 合同类型（DDL supplier_contract.contract_type 枚举）：1合作框架/2年度/3单次/4补充协议 */
export enum ContractType {
  FRAMEWORK = 1,
  ANNUAL = 2,
  SINGLE = 3,
  SUPPLEMENT = 4
}

export const CONTRACT_TYPE_OPTIONS = [
  { label: '合作框架', value: ContractType.FRAMEWORK },
  { label: '年度合同', value: ContractType.ANNUAL },
  { label: '单次合同', value: ContractType.SINGLE },
  { label: '补充协议', value: ContractType.SUPPLEMENT }
] as const

/** 合同结算周期（DDL supplier_contract.settlement_cycle 枚举）：1月/2季/3半年/4年 */
export enum SettlementCycle {
  MONTHLY = 1,
  QUARTERLY = 2,
  HALF_YEAR = 3,
  YEARLY = 4
}

export const SETTLEMENT_CYCLE_OPTIONS = [
  { label: '月结', value: SettlementCycle.MONTHLY },
  { label: '季结', value: SettlementCycle.QUARTERLY },
  { label: '半年结', value: SettlementCycle.HALF_YEAR },
  { label: '年结', value: SettlementCycle.YEARLY }
] as const

// ===== 合同 =====
export interface SupplierContract {
  id?: number
  /** 合同编码（HT 前缀，后端 CodeGenerator 生成；create 表单不含） */
  contractCode: string
  contractName: string
  supplierCode: string
  organCode?: string
  /** 合同类型：1合作框架/2年度/3单次/4补充协议 */
  contractType?: number
  signDate?: string
  effectiveDate?: string
  expireDate?: string
  contractAmount?: number
  commissionRate?: number
  /** 结算周期：1月/2季/3半年/4年 */
  settlementCycle?: number
  terms?: string
  /** 附件 URL 列表（JSON 数组字符串） */
  attachmentUrls?: string
  signPerson?: string
  signSealImage?: string
  /** 是否自动续约：0/1 */
  isAutoRenew?: number
  renewCount?: number
  /** 续约指向父合同 */
  parentContractCode?: string
  /** 状态：0草稿/1待审核/2已生效/3已到期/4已终止/5已作废（create 默认 1） */
  status?: number
  auditRemark?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface SupplierContractQuery extends PageQuery {
  contractCode?: string
  contractName?: string
  supplierCode?: string
  organCode?: string
  contractType?: number
  settlementCycle?: number
  status?: number
  parentContractCode?: string
}

// ===== 联系人 =====
/**
 * 联系人类型（DDL supplier_contact.contact_type 枚举）：1商务/2财务/3技术/4运营/5其他
 */
export enum ContactType {
  BUSINESS = 1,
  FINANCE = 2,
  TECH = 3,
  OPERATION = 4,
  OTHER = 5
}

export const CONTACT_TYPE_OPTIONS = [
  { label: '商务', value: ContactType.BUSINESS },
  { label: '财务', value: ContactType.FINANCE },
  { label: '技术', value: ContactType.TECH },
  { label: '运营', value: ContactType.OPERATION },
  { label: '其他', value: ContactType.OTHER }
] as const

export interface SupplierContact {
  id?: number
  supplierCode: string
  contactName: string
  /** 联系人类型：1商务/2财务/3技术/4运营/5其他 */
  contactType?: number
  position?: string
  phone?: string
  email?: string
  wechat?: string
  /** 是否主联系人：0/1，同供应商唯一（后端自动互斥） */
  isPrimary?: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface SupplierContactQuery extends PageQuery {
  supplierCode?: string
  contactName?: string
  contactType?: number
  isPrimary?: number
}

// ===== 评价 =====
/**
 * 评价类型（DDL supplier_evaluation.eval_type 枚举）：1定期/2临时/3客户投诉触发
 */
export enum EvaluationType {
  PERIODIC = 1,
  TEMPORARY = 2,
  COMPLAINT = 3
}

export const EVALUATION_TYPE_OPTIONS = [
  { label: '定期评价', value: EvaluationType.PERIODIC },
  { label: '临时评价', value: EvaluationType.TEMPORARY },
  { label: '客户投诉触发', value: EvaluationType.COMPLAINT }
] as const

/** 评价等级（后端按综合分自动计算）：≥90→1(A)/80-89→2(B)/70-79→3(C)/<70→4(D) */
export enum ScoreLevel {
  A = 1,
  B = 2,
  C = 3,
  D = 4
}

export const SCORE_LEVEL_OPTIONS = [
  { label: 'A', value: ScoreLevel.A },
  { label: 'B', value: ScoreLevel.B },
  { label: 'C', value: ScoreLevel.C },
  { label: 'D', value: ScoreLevel.D }
] as const

export interface SupplierEvaluation {
  id?: number
  supplierCode: string
  /** 评价周期，YYYYQN 如 2026Q3 */
  evalPeriod?: string
  /** 评价类型：1定期/2临时/3客户投诉触发 */
  evalType?: number
  /** 服务质量评分 0-100 */
  serviceQualityScore?: number
  /** 设施质量评分 0-100 */
  facilityQualityScore?: number
  /** 合作配合度评分 0-100 */
  cooperationScore?: number
  /** 投诉率 0-100（百分比） */
  complaintRate?: number
  totalOrderCount?: number
  complaintCount?: number
  /** 综合分（后端自动计算，为空时算） */
  totalScore?: number
  /** 等级（后端自动计算）：1A/2B/3C/4D */
  scoreLevel?: number
  evalContent?: string
  improvementSuggestions?: string
  evaluatorCode?: string
  evaluatorName?: string
  evalDate?: string
  /** 状态：0草稿/1已提交（create 默认 1） */
  status?: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface SupplierEvaluationQuery extends PageQuery {
  supplierCode?: string
  evalPeriod?: string
  evalType?: number
  scoreLevel?: number
  status?: number
}
