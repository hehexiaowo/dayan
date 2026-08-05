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
 * 供应商启用状态（后端实际取值与通用启用/禁用略有差异）：
 * - 0 = 禁用
 * - 1 = 启用
 * - 2 = 已暂停（冻结，暂时不可合作）
 *
 * 这里单独维护一份选项，便于把 2 一并映射成中文，避免列表出现裸数字。
 */
export enum SupplierStatus {
  /** 禁用 */
  DISABLED = 0,
  /** 启用 */
  ENABLED = 1,
  /** 已暂停（冻结） */
  SUSPENDED = 2
}

export const SUPPLIER_STATUS_OPTIONS = [
  { label: '启用', value: SupplierStatus.ENABLED },
  { label: '禁用', value: SupplierStatus.DISABLED },
  { label: '已暂停', value: SupplierStatus.SUSPENDED }
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
  /** 状态：1启用 / 0禁用 / 2已暂停 */
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
