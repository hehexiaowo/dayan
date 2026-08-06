/**
 * 结算域（finance）相关类型。
 *
 * 字段对齐后端 com.dayan.finance 包下实体：
 * - FinanceBill（结算单）/ FinanceFlow（资金流水）/ FinanceInvoice（发票）。
 *
 * 类型约定：Integer→number，BigDecimal→number，LocalDate/LocalDateTime→string。
 */
import type { PageQuery } from '@/types/common'

// ==================== 结算单（FinanceBill） ====================

/** 结算单类型：1=渠道结算 / 2=供应商结算 */
export enum BillType {
  /** 渠道结算 */
  CHANNEL = 1,
  /** 供应商结算 */
  SUPPLIER = 2
}

export const BILL_TYPE_OPTIONS = [
  { label: '渠道结算', value: BillType.CHANNEL },
  { label: '供应商结算', value: BillType.SUPPLIER }
] as const

/** 结算单状态：0=待审核 / 1=审核通过 / 2=结算中 / 3=已结算 / 4=审核拒绝 */
export enum BillStatus {
  /** 待审核 */
  PENDING_AUDIT = 0,
  /** 审核通过 */
  AUDIT_PASS = 1,
  /** 结算中 */
  SETTLING = 2,
  /** 已结算 */
  SETTLED = 3,
  /** 审核拒绝 */
  AUDIT_REJECT = 4
}

export const BILL_STATUS_OPTIONS = [
  { label: '待审核', value: BillStatus.PENDING_AUDIT },
  { label: '审核通过', value: BillStatus.AUDIT_PASS },
  { label: '结算中', value: BillStatus.SETTLING },
  { label: '已结算', value: BillStatus.SETTLED },
  { label: '审核拒绝', value: BillStatus.AUDIT_REJECT }
] as const

/** 结算方式：1=银行转账 / 2=线上转账 */
export enum SettlementMethod {
  /** 银行转账 */
  BANK_TRANSFER = 1,
  /** 线上转账 */
  ONLINE_TRANSFER = 2
}

export const SETTLEMENT_METHOD_OPTIONS = [
  { label: '银行转账', value: SettlementMethod.BANK_TRANSFER },
  { label: '线上转账', value: SettlementMethod.ONLINE_TRANSFER }
] as const

/**
 * 结算单实体（后端 FinanceBillVO）。
 */
export interface FinanceBill {
  id?: number
  /** 结算单编号（系统生成） */
  billCode?: string
  /** 结算类型 */
  billType?: BillType
  /** 结算对象类型（channel/supplier/distributor） */
  targetType?: string
  /** 结算对象编码 */
  targetCode?: string
  /** 结算对象名称 */
  targetName?: string
  /** 结算周期开始 */
  periodStart?: string
  /** 结算周期结束 */
  periodEnd?: string
  /** 订单数量 */
  orderCount?: number
  /** 结算总额 */
  totalAmount?: number
  /** 分销手续费金额 */
  commissionAmount?: number
  /** 退款金额 */
  refundAmount?: number
  /** 调整金额 */
  adjustAmount?: number
  /** 最终结算金额 */
  finalAmount?: number
  /** 关联流水 ID 列表（JSON 数组字符串） */
  flowIds?: string
  /** 结算方式 */
  settlementMethod?: SettlementMethod
  /** 收款银行信息 */
  bankInfo?: string
  /** 申请时间 */
  applyTime?: string
  /** 审核时间 */
  auditTime?: string
  /** 结算完成时间 */
  settleTime?: string
  /** 审核人编码 */
  auditorCode?: string
  /** 审核人姓名 */
  auditorName?: string
  /** 审核备注 */
  auditRemark?: string
  /** 状态 */
  billStatus?: BillStatus
  remark?: string
  createdAt?: string
}

/**
 * 结算单分页查询参数（后端 FinanceBillQueryDTO）。
 */
export interface FinanceBillQuery extends PageQuery {
  billCode?: string
  billType?: BillType
  targetType?: string
  targetCode?: string
  billStatus?: BillStatus
  /** 周期开始 ≥ */
  periodStartFrom?: string
  /** 周期结束 ≤ */
  periodEndTo?: string
}

// ==================== 资金流水（FinanceFlow） ====================

/** 流水类型：1=收入 / 2=支出 / 3=退款 / 4=结算 */
export enum FlowType {
  /** 收入 */
  INCOME = 1,
  /** 支出 */
  EXPENSE = 2,
  /** 退款 */
  REFUND = 3,
  /** 结算 */
  SETTLEMENT = 4
}

export const FLOW_TYPE_OPTIONS = [
  { label: '收入', value: FlowType.INCOME },
  { label: '支出', value: FlowType.EXPENSE },
  { label: '退款', value: FlowType.REFUND },
  { label: '结算', value: FlowType.SETTLEMENT }
] as const

/** 支付方式：1=微信支付 / 2=支付宝 / 3=银行转账 / 4=余额支付 / 5=线下支付 */
export enum PayType {
  /** 微信支付 */
  WECHAT = 1,
  /** 支付宝 */
  ALIPAY = 2,
  /** 银行转账 */
  BANK_TRANSFER = 3,
  /** 余额支付 */
  BALANCE = 4,
  /** 线下支付 */
  OFFLINE = 5
}

export const PAY_TYPE_OPTIONS = [
  { label: '微信支付', value: PayType.WECHAT },
  { label: '支付宝', value: PayType.ALIPAY },
  { label: '银行转账', value: PayType.BANK_TRANSFER },
  { label: '余额支付', value: PayType.BALANCE },
  { label: '线下支付', value: PayType.OFFLINE }
] as const

/** 流水状态：0=已冲正 / 1=正常 */
export enum FlowStatus {
  /** 已冲正 */
  REVERSED = 0,
  /** 正常 */
  NORMAL = 1
}

export const FLOW_STATUS_OPTIONS = [
  { label: '已冲正', value: FlowStatus.REVERSED },
  { label: '正常', value: FlowStatus.NORMAL }
] as const

/**
 * 资金流水实体（后端 FinanceFlowVO）。
 */
export interface FinanceFlow {
  id?: number
  /** 流水编号（系统生成） */
  flowCode?: string
  /** 流水类型 */
  flowType?: FlowType
  /** 业务类型（equity_order/scene_order/course_order/travel_order/settlement） */
  bizType?: string
  /** 业务编码 */
  bizCode?: string
  /** 账号类型（organ/channel/agent/client/supplier） */
  accountType?: string
  /** 账号编码 */
  accountCode?: string
  /** 流水金额 */
  flowAmount?: number
  /** 变动前余额 */
  balanceBefore?: number
  /** 变动后余额 */
  balanceAfter?: number
  /** 支付方式 */
  payType?: PayType
  /** 交易流水号 */
  tradeNo?: string
  /** 对方类型 */
  counterpartyType?: string
  /** 对方编码 */
  counterpartyCode?: string
  /** 对方名称 */
  counterpartyName?: string
  /** 流水描述 */
  flowDescription?: string
  /** 流水时间 */
  flowTime?: string
  /** 是否已结算：0=否 / 1=是 */
  isSettled?: number
  /** 结算单编码 */
  settleCode?: string
  /** 状态 */
  status?: FlowStatus
  remark?: string
  createdAt?: string
}

/**
 * 资金流水分页查询参数（后端 FinanceFlowQueryDTO）。
 */
export interface FinanceFlowQuery extends PageQuery {
  flowCode?: string
  flowType?: FlowType
  bizType?: string
  bizCode?: string
  accountType?: string
  accountCode?: string
  status?: FlowStatus
  isSettled?: number
  settleCode?: string
}

// ==================== 发票（FinanceInvoice） ====================

/** 发票类型：1=增值税普通发票 / 2=增值税专用发票 / 3=电子发票 */
export enum InvoiceType {
  /** 增值税普通发票 */
  GENERAL = 1,
  /** 增值税专用发票 */
  SPECIAL = 2,
  /** 电子发票 */
  ELECTRONIC = 3
}

export const INVOICE_TYPE_OPTIONS = [
  { label: '增值税普通发票', value: InvoiceType.GENERAL },
  { label: '增值税专用发票', value: InvoiceType.SPECIAL },
  { label: '电子发票', value: InvoiceType.ELECTRONIC }
] as const

/** 抬头类型：1=企业 / 2=个人 */
export enum TitleType {
  /** 企业 */
  ENTERPRISE = 1,
  /** 个人 */
  PERSONAL = 2
}

export const TITLE_TYPE_OPTIONS = [
  { label: '企业', value: TitleType.ENTERPRISE },
  { label: '个人', value: TitleType.PERSONAL }
] as const

/**
 * 发票状态：0=待审核 / 1=已审核 / 2=已开票 / 3=已寄出 / 4=已完成 / 5=已作废 / 6=已红冲
 */
export enum InvoiceStatus {
  /** 待审核 */
  PENDING_AUDIT = 0,
  /** 已审核 */
  AUDITED = 1,
  /** 已开票 */
  ISSUED = 2,
  /** 已寄出 */
  SENT = 3,
  /** 已完成 */
  FINISHED = 4,
  /** 已作废 */
  VOID = 5,
  /** 已红冲 */
  RED_FLUSH = 6
}

export const INVOICE_STATUS_OPTIONS = [
  { label: '待审核', value: InvoiceStatus.PENDING_AUDIT },
  { label: '已审核', value: InvoiceStatus.AUDITED },
  { label: '已开票', value: InvoiceStatus.ISSUED },
  { label: '已寄出', value: InvoiceStatus.SENT },
  { label: '已完成', value: InvoiceStatus.FINISHED },
  { label: '已作废', value: InvoiceStatus.VOID },
  { label: '已红冲', value: InvoiceStatus.RED_FLUSH }
] as const

/**
 * 发票实体（后端 FinanceInvoiceVO）。
 */
export interface FinanceInvoice {
  id?: number
  /** 发票编码（系统生成） */
  invoiceCode?: string
  /** 发票类型 */
  invoiceType?: InvoiceType
  /** 关联结算单编码 */
  billCode?: string
  /** 关联订单编码 */
  orderCode?: string
  /** 申请方类型（channel/agent/client） */
  applicantType?: string
  /** 申请方编码 */
  applicantCode?: string
  /** 申请方名称 */
  applicantName?: string
  /** 抬头类型 */
  titleType?: TitleType
  /** 发票抬头 */
  invoiceTitle?: string
  /** 纳税人识别号 */
  taxNo?: string
  /** 开户银行 */
  bankName?: string
  /** 银行账号 */
  bankAccount?: string
  /** 注册地址 */
  registerAddress?: string
  /** 注册电话 */
  registerPhone?: string
  /** 开票金额 */
  invoiceAmount?: number
  /** 发票内容 */
  invoiceContent?: string
  /** 收件人姓名 */
  receiverName?: string
  /** 收件人电话 */
  receiverPhone?: string
  /** 收件地址 */
  receiverAddress?: string
  /** 收件邮箱（电子发票） */
  receiverEmail?: string
  /** 发票号码（税务号码） */
  invoiceNo?: string
  /** 发票文件 URL */
  invoiceUrl?: string
  /** 申请时间 */
  applyTime?: string
  /** 开票时间 */
  issueTime?: string
  /** 寄出时间 */
  sendTime?: string
  /** 状态 */
  invoiceStatus?: InvoiceStatus
  remark?: string
  createdAt?: string
}

/**
 * 发票分页查询参数（后端 FinanceInvoiceQueryDTO）。
 */
export interface FinanceInvoiceQuery extends PageQuery {
  invoiceCode?: string
  invoiceType?: InvoiceType
  billCode?: string
  orderCode?: string
  applicantType?: string
  applicantCode?: string
  /** 发票号码（税务号码，非系统编码） */
  invoiceNo?: string
  invoiceStatus?: InvoiceStatus
}
