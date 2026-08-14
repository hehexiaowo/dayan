/**
 * 应收应付账目（finance_account）相关类型。
 *
 * 字段对齐后端 com.dayan.finance 包下：
 * - FinanceAccountVO / FinanceAccountQueryDTO / CreateAccountDTO / AccountReceiveDTO。
 *
 * 类型约定：Integer→number，BigDecimal→number，LocalDate/LocalDateTime→string。
 */
import type { PageQuery } from '@/types/common'

// ==================== 账目方向 ====================

/** 账目方向：1=应收 / 2=应付 */
export enum AccountDirection {
  /** 应收 */
  RECEIVABLE = 1,
  /** 应付 */
  PAYABLE = 2
}

export const ACCOUNT_DIRECTION_OPTIONS = [
  { label: '应收', value: AccountDirection.RECEIVABLE },
  { label: '应付', value: AccountDirection.PAYABLE }
] as const

// ==================== 账目状态 ====================

/**
 * 账目状态：0=待收付 / 1=部分收付 / 2=已结清 / 3=已逾期 / 4=已坏账
 */
export enum AccountStatus {
  /** 待收付 */
  PENDING = 0,
  /** 部分收付 */
  PARTIAL = 1,
  /** 已结清 */
  SETTLED = 2,
  /** 已逾期 */
  OVERDUE = 3,
  /** 已坏账 */
  BAD_DEBT = 4
}

export const ACCOUNT_STATUS_OPTIONS = [
  { label: '待收付', value: AccountStatus.PENDING },
  { label: '部分收付', value: AccountStatus.PARTIAL },
  { label: '已结清', value: AccountStatus.SETTLED },
  { label: '已逾期', value: AccountStatus.OVERDUE },
  { label: '已坏账', value: AccountStatus.BAD_DEBT }
] as const

/**
 * 应收应付账目实体（后端 FinanceAccountVO）。
 */
export interface FinanceAccount {
  id?: number
  /** 账目编号（系统生成） */
  accountCode?: string
  /** 账目方向：1=应收/2=应付 */
  direction?: AccountDirection
  /** 对象类型：channel/supplier/agent */
  accountType?: string
  /** 对象编码 */
  targetCode?: string
  /** 对象名称 */
  targetName?: string
  /** 业务类型：equity_purchase/scene_fee/service_fee */
  bizType?: string
  /** 业务编码 */
  bizCode?: string
  /** 应收/应付总额 */
  totalAmount?: number
  /** 已收/付金额 */
  receivedAmount?: number
  /** 剩余应收/应付金额 */
  remainAmount?: number
  /** 到期日期 */
  dueDate?: string
  /** 最近一次收/付款时间 */
  lastReceiveTime?: string
  /** 状态 */
  accountStatus?: AccountStatus
  remark?: string
  createdAt?: string
}

/**
 * 应收应付账目分页查询参数（后端 FinanceAccountQueryDTO）。
 */
export interface FinanceAccountQuery extends PageQuery {
  accountCode?: string
  direction?: AccountDirection
  accountType?: string
  targetCode?: string
  bizType?: string
  bizCode?: string
  accountStatus?: AccountStatus
  /** 到期日期 ≤ */
  dueDateTo?: string
}
