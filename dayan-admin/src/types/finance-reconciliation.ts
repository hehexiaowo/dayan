/**
 * 对账记录（finance_reconciliation）相关类型。
 *
 * 字段对齐后端 com.dayan.finance 包下：
 * - FinanceReconciliationVO / FinanceReconciliationQueryDTO。
 *
 * 类型约定：Integer→number，BigDecimal→number，LocalDate/LocalDateTime→string。
 */
import type { PageQuery } from '@/types/common'

// ==================== 对账类型 ====================

/** 对账类型：1=渠道对账 / 2=供应商对账 */
export enum ReconType {
  /** 渠道对账 */
  CHANNEL = 1,
  /** 供应商对账 */
  SUPPLIER = 2
}

export const RECON_TYPE_OPTIONS = [
  { label: '渠道对账', value: ReconType.CHANNEL },
  { label: '供应商对账', value: ReconType.SUPPLIER }
] as const

// ==================== 对账结果 ====================

/** 对账结果：0=有差异 / 1=一致 */
export enum ReconResult {
  /** 有差异 */
  DIFF = 0,
  /** 一致 */
  MATCHED = 1
}

export const RECON_RESULT_OPTIONS = [
  { label: '有差异', value: ReconResult.DIFF },
  { label: '一致', value: ReconResult.MATCHED }
] as const

// ==================== 对账状态 ====================

/**
 * 对账状态：0=对账中 / 1=已完成 / 2=待确认 / 3=已确认
 */
export enum ReconStatus {
  /** 对账中 */
  RECONCILING = 0,
  /** 已完成 */
  COMPLETED = 1,
  /** 待确认 */
  PENDING_CONFIRM = 2,
  /** 已确认 */
  CONFIRMED = 3
}

export const RECON_STATUS_OPTIONS = [
  { label: '对账中', value: ReconStatus.RECONCILING },
  { label: '已完成', value: ReconStatus.COMPLETED },
  { label: '待确认', value: ReconStatus.PENDING_CONFIRM },
  { label: '已确认', value: ReconStatus.CONFIRMED }
] as const

/**
 * 对账记录实体（后端 FinanceReconciliationVO）。
 */
export interface FinanceReconciliation {
  id?: number
  /** 对账编号（系统生成） */
  reconCode?: string
  /** 对账类型：1=渠道对账/2=供应商对账 */
  reconType?: ReconType
  /** 对账对象编码 */
  targetCode?: string
  /** 对账对象名称 */
  targetName?: string
  /** 对账周期开始 */
  periodStart?: string
  /** 对账周期结束 */
  periodEnd?: string
  /** 我方订单数 */
  ourOrderCount?: number
  /** 我方总金额 */
  ourTotalAmount?: number
  /** 对方订单数 */
  theirOrderCount?: number
  /** 对方总金额 */
  theirTotalAmount?: number
  /** 差异订单数 */
  diffCount?: number
  /** 差异金额 */
  diffAmount?: number
  /** 差异明细（JSON 字符串） */
  diffDetail?: string
  /** 对账结果：0=有差异/1=一致 */
  reconResult?: ReconResult
  /** 差异处理结果 */
  handleResult?: string
  /** 对账时间 */
  reconTime?: string
  /** 操作人编码 */
  operatorCode?: string
  /** 操作人姓名 */
  operatorName?: string
  /** 状态 */
  status?: ReconStatus
  remark?: string
  createdAt?: string
}

/**
 * 对账记录分页查询参数（后端 FinanceReconciliationQueryDTO）。
 */
export interface FinanceReconciliationQuery extends PageQuery {
  reconCode?: string
  reconType?: ReconType
  targetCode?: string
  reconResult?: ReconResult
  status?: ReconStatus
  /** 周期开始 ≥ */
  periodStartFrom?: string
  /** 周期结束 ≤ */
  periodEndTo?: string
}
