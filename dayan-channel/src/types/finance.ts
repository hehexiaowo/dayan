/**
 * 渠道端结算域（finance）相关类型。
 *
 * 对应后端 com.dayan.finance 包下实体（finance_payment / finance_invoice）的渠道端 VO 子集。
 *
 * 注意：枚举与 admin 端 `dayan-admin/src/types/finance.ts` 保持同源一致
 * （同源后端 VO，渠道端独立工程，不 import admin 模块，而是把枚举定义复制过来）。
 * 渠道端只涉及「支付单」+「发票」两类，结算单 / 资金流水属 admin 专有，此处不导出。
 *
 * 类型约定：Integer→number，BigDecimal→number，LocalDate/LocalDateTime→string。
 * 金额单位均为「元」（后端 BigDecimal），前端直接显示，不要除以 100。
 */
import type { PageQuery } from '@/types/common'

// ==================== 支付方式 / 支付状态 ====================

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

/**
 * 支付状态：0=待支付 / 1=支付成功 / 2=支付失败 / 3=已退款 / 4=部分退款。
 *
 * 对齐后端 `finance_payment.pay_status`。
 */
export enum PaymentStatus {
  /** 待支付 */
  PENDING = 0,
  /** 支付成功 */
  SUCCESS = 1,
  /** 支付失败 */
  FAILED = 2,
  /** 已退款 */
  REFUNDED = 3,
  /** 部分退款 */
  PARTIAL_REFUND = 4
}

export const PAYMENT_STATUS_OPTIONS = [
  { label: '待支付', value: PaymentStatus.PENDING },
  { label: '支付成功', value: PaymentStatus.SUCCESS },
  { label: '支付失败', value: PaymentStatus.FAILED },
  { label: '已退款', value: PaymentStatus.REFUNDED },
  { label: '部分退款', value: PaymentStatus.PARTIAL_REFUND }
] as const

// ==================== 发票相关枚举 ====================

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
 * 发票状态：0=待审核 / 1=已审核 / 2=已开票 / 3=已寄出 / 4=已完成 / 5=已作废 / 6=已红冲。
 *
 * 对齐后端 `finance_invoice.invoice_status`（7 态状态机）。
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

// ==================== 支付单（FinancePayment，对齐 FinancePaymentVO） ====================

/**
 * 支付单实体（对齐后端 `com.dayan.finance.vo.FinancePaymentVO`）。
 *
 * 注意：`finance_payment` 表无 `channel_code` 字段，渠道端读接口靠反查
 * 本渠道 4 类订单的 `orderCode` 集合做归属过滤（后端 ChannelFinanceController 实现）。
 */
export interface FinancePayment {
  id?: number
  /** 支付流水号（系统生成，PAY+序号） */
  paymentCode?: string
  /** 订单类型：1=权益/2=场景/3=课程/4=旅游短居 */
  orderType?: number
  /** 关联订单编码 */
  orderCode?: string
  /** 支付方式 */
  payType?: PayType
  /** 支付金额（元） */
  payAmount?: number
  /** 第三方交易号 */
  tradeNo?: string
  /** 付款方账号 */
  payerAccount?: string
  /** 收款方账号 */
  payeeAccount?: string
  /** 支付时间（yyyy-MM-dd HH:mm:ss） */
  payTime?: string
  /** 回调通知时间 */
  notifyTime?: string
  /** 支付状态（0-4，见 PaymentStatus） */
  payStatus?: PaymentStatus
  /** 支付说明 */
  payDescription?: string
  /** 扩展数据（JSON 字符串） */
  extraData?: string
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createdAt?: string
}

/**
 * 支付单分页查询参数（对齐后端 `FinancePaymentQueryDTO`）。
 *
 * 注意：`orderCodes`（多值 IN 过滤）由后端 ChannelFinanceController 从本渠道订单反查注入，
 * 前端不可传；此处不暴露该字段。
 */
export interface FinancePaymentQuery extends PageQuery {
  /** 支付流水号 */
  paymentCode?: string
  /** 订单类型：1=权益/2=场景/3=课程/4=旅游短居 */
  orderType?: number
  /** 关联订单编码 */
  orderCode?: string
  /** 第三方交易号 */
  tradeNo?: string
  /** 支付方式 */
  payType?: PayType
  /** 支付状态 */
  payStatus?: PaymentStatus
  /** 支付时间 ≥（yyyy-MM-dd HH:mm:ss） */
  payTimeFrom?: string
  /** 支付时间 ≤（yyyy-MM-dd HH:mm:ss） */
  payTimeTo?: string
}

// ==================== 发票（FinanceInvoice，对齐 FinanceInvoiceVO） ====================

/**
 * 发票实体（对齐后端 `com.dayan.finance.vo.FinanceInvoiceVO`）。
 *
 * 渠道端归属过滤：`applicantCode` = 当前渠道 channelCode，`applicantType` = "channel"
 * （后端 ChannelInvoiceController 强制注入，覆盖前端任何传入）。
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
  /** 申请方类型（渠道端固定为 "channel"） */
  applicantType?: string
  /** 申请方编码（渠道端固定为当前 channelCode） */
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
  /** 开票金额（元） */
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
  /** 发票号码（税务号码，非系统编码） */
  invoiceNo?: string
  /** 发票文件 URL */
  invoiceUrl?: string
  /** 申请时间（yyyy-MM-dd HH:mm:ss） */
  applyTime?: string
  /** 开票时间 */
  issueTime?: string
  /** 寄出时间 */
  sendTime?: string
  /** 状态（0-6，见 InvoiceStatus） */
  invoiceStatus?: InvoiceStatus
  /** 备注 */
  remark?: string
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createdAt?: string
}

/**
 * 发票分页查询参数（对齐后端 `FinanceInvoiceQueryDTO`）。
 *
 * 注意：`applicantCode` / `applicantType` 由后端 ChannelInvoiceController 强制注入，
 * 前端不可传，此处不暴露。
 */
export interface FinanceInvoiceQuery extends PageQuery {
  /** 发票编码 */
  invoiceCode?: string
  /** 发票类型 */
  invoiceType?: InvoiceType
  /** 关联结算单编码 */
  billCode?: string
  /** 关联订单编码 */
  orderCode?: string
  /** 发票号码（税务号码，非系统编码） */
  invoiceNo?: string
  /** 发票状态 */
  invoiceStatus?: InvoiceStatus
}

// ==================== 写操作入参（对齐后端 DTO） ====================

/**
 * 创建支付单入参（对齐后端 `CreatePaymentDTO`）。
 *
 * - orderType / orderCode / payType 必填；
 * - 权益订单（orderType=1）的 payAmount 由后端从订单表权威解析覆盖（防篡改）；
 * - paymentCode、payStatus、tradeNo 由服务端生成/管理。
 */
export interface CreatePaymentData {
  /** 订单类型：1=权益/2=场景/3=课程/4=旅游短居（必填） */
  orderType: number
  /** 订单编码（必填） */
  orderCode: string
  /** 支付方式（必填） */
  payType: number
  /** 支付金额（权益订单由服务端权威解析，可不传） */
  payAmount?: number
  /** 付款方账号 */
  payerAccount?: string
  /** 收款方账号 */
  payeeAccount?: string
  /** 支付说明 */
  payDescription?: string
  /** 扩展数据（JSON 字符串） */
  extraData?: string
}

/**
 * 标记支付成功入参（对齐后端 `PaymentMarkSuccessDTO`）。
 *
 * paymentCode 走 path 参数（api 函数签名），tradeNo 必填，
 * payTime / notifyTime 为空时服务端取当前时间。
 */
export interface PaymentMarkSuccessData {
  /** 第三方交易号（必填） */
  tradeNo: string
  /** 支付时间（为空时取当前时间） */
  payTime?: string
  /** 回调通知时间（为空时取当前时间） */
  notifyTime?: string
  /** 付款方账号 */
  payerAccount?: string
  /** 收款方账号 */
  payeeAccount?: string
  /** 备注 */
  payDescription?: string
}

/**
 * 申请发票入参（对齐后端 `ApplyInvoiceDTO`）。
 *
 * - invoiceCode / applyTime / issueTime / sendTime / invoiceStatus 由服务端管理；
 * - applicantCode / applicantType 由后端 ChannelInvoiceController 强制注入
 *   （= 当前 channelCode / "channel"），前端不可传，故本类型不包含这两个字段；
 * - titleType 留空时后端默认 1（企业）；
 * - 电子发票（invoiceType=3）时 receiverEmail 必填；
 * - 企业（titleType=1）时 taxNo 必填。
 */
export interface ApplyInvoiceData {
  /** 发票类型：1普票/2专票/3电子（必填） */
  invoiceType: number
  /** 关联结算单编码 */
  billCode?: string
  /** 关联订单编码 */
  orderCode?: string
  /** 抬头类型：1企业/2个人，默认 1 */
  titleType?: number
  /** 发票抬头（必填） */
  invoiceTitle: string
  /** 纳税人识别号（企业必填） */
  taxNo?: string
  /** 开户银行 */
  bankName?: string
  /** 银行账号 */
  bankAccount?: string
  /** 注册地址 */
  registerAddress?: string
  /** 注册电话 */
  registerPhone?: string
  /** 开票金额（必填） */
  invoiceAmount: number
  /** 发票内容（必填） */
  invoiceContent: string
  /** 收件人姓名 */
  receiverName?: string
  /** 收件人电话 */
  receiverPhone?: string
  /** 收件地址 */
  receiverAddress?: string
  /** 收件邮箱（电子发票时必填） */
  receiverEmail?: string
  /** 备注 */
  remark?: string
}
