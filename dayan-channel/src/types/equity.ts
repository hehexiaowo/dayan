import type { PageQuery } from './common'

/**
 * 权益相关类型。
 *
 * 字段对齐后端 Equity 域 Entity（com.dayan.equity.entity.Equity），
 * 渠道后台视角取本渠道权益子集。
 */

/**
 * 权益状态：对齐后端 EquityEvent 状态机（EQUITY_SM）。
 * 0=库存中 / 1=已出库 / 2=已激活 / 3=使用中 / 4=已完成 / 5=已过期 / 6=已作废 / 7=更换权益人中。
 */
export enum EquityStatus {
  /** 库存中 */
  STOCK = 0,
  /** 已出库 */
  OUTBOUND = 1,
  /** 已激活 */
  ACTIVATED = 2,
  /** 使用中 */
  IN_USE = 3,
  /** 已完成 */
  COMPLETED = 4,
  /** 已过期 */
  EXPIRED = 5,
  /** 已作废 */
  VOID = 6,
  /** 更换权益人中 */
  CHANGING_HOLDER = 7
}

/** 权益状态选项 */
export const EQUITY_STATUS_OPTIONS = [
  { label: '库存中', value: EquityStatus.STOCK },
  { label: '已出库', value: EquityStatus.OUTBOUND },
  { label: '已激活', value: EquityStatus.ACTIVATED },
  { label: '使用中', value: EquityStatus.IN_USE },
  { label: '已完成', value: EquityStatus.COMPLETED },
  { label: '已过期', value: EquityStatus.EXPIRED },
  { label: '已作废', value: EquityStatus.VOID },
  { label: '更换权益人中', value: EquityStatus.CHANGING_HOLDER }
] as const

/**
 * 权益类型选项：对齐后端 DDL 10_equity.sql:17。
 * 1=机构入住权益 / 2=机构参观权益 / 3=场景活动权益 / 4=居家护理权益 /
 * 5=健康检测权益 / 6=课程学习权益 / 7=旅居体验权益。
 */
export const EQUITY_TYPE_OPTIONS = [
  { label: '机构入住权益', value: 1 },
  { label: '机构参观权益', value: 2 },
  { label: '场景活动权益', value: 3 },
  { label: '居家护理权益', value: 4 },
  { label: '健康检测权益', value: 5 },
  { label: '课程学习权益', value: 6 },
  { label: '旅居体验权益', value: 7 }
] as const

/**
 * 载体类型选项：对齐后端运行时校验（EquityDepotServiceImpl 只允许 1/2）。
 * 1=权益卡 / 2=权益函。
 */
export const CARRIER_TYPE_OPTIONS = [
  { label: '权益卡', value: 1 },
  { label: '权益函', value: 2 }
] as const

/**
 * 权益实体（对齐后端 EquityDepotVO，渠道视角）。
 *
 * 后端渠道端 GET /channel-api/equities 返回与管理端相同的 EquityDepotVO，
 * 字段覆盖完整生命周期（入库/分配/出库/激活/使用/到期时间轴 + 批次/物流/使用人快照），
 * channelCode 由后端 ContextHolder 强制注入防越权。
 *
 * 注意：personCount/validDays 为激活时快照，costPrice 单位是「元」。
 */
export interface Equity {
  id?: number
  /** 权益编码（主键业务码） */
  equityCode?: string
  /** 权益卡号（入库时 = equityCode） */
  equityNo?: string
  /** 关联商品编码 */
  goodsCode?: string
  /** 关联批次编码 */
  batchCode?: string
  /** 使用人人数快照 */
  personCount?: number
  /** 激活后有效天数快照 */
  validDays?: number
  /** 成本价 */
  costPrice?: number
  /** 载体类型（1权益卡/2权益函） */
  carrierType?: number
  /** 分配渠道编码（后端强制注入） */
  channelCode?: string
  /** 分配代理人编码 */
  agentCode?: string
  /** 领取客户编码 */
  clientCode?: string
  // ====== 生命周期时间轴 ======
  /** 入库时间 */
  produceTime?: string
  /** 分配时间 */
  allocateTime?: string
  /** 出库时间 */
  outboundTime?: string
  /** 出库渠道编码 */
  outboundChannelCode?: string
  /** 出库代理人编码 */
  outboundAgentCode?: string
  /** 物流单号 */
  logisticsNo?: string
  /** 激活时间 */
  activateTime?: string
  /** 首次使用时间 */
  firstUseTime?: string
  /** 最近使用时间 */
  lastUseTime?: string
  /** 到期时间 */
  expireTime?: string
  /** 库存到期时间（上架有效期截止） */
  shelfExpireTime?: string
  // ====== 激活与关联 ======
  /** 激活码（DY-8位，权益卡专用） */
  activateCode?: string
  /** 绑定码（BF-12位，权益函专用） */
  bindCode?: string
  /** 二维码 URL */
  qrCodeUrl?: string
  /** 关联订单编码 */
  orderCode?: string
  /** 商品名称（关联订单快照，orderCode 为空时为空） */
  goodsName?: string
  /** 商品规格（关联订单快照，orderCode 为空时为空） */
  skuName?: string
  // ====== 状态 ======
  /** 权益状态（0-7，见 EquityStatus） */
  equityStatus?: EquityStatus
  /** 作废原因 */
  voidReason?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createdAt?: string
}

/** 权益分页查询参数 */
export interface EquityQuery {
  /** 权益编码（模糊匹配，可选） */
  equityCode?: string
  /** 权益状态（可选） */
  equityStatus?: EquityStatus
  /** 载体类型（1权益卡/2权益函，可选） */
  carrierType?: number
  /** 关联客户编码（模糊匹配，可选） */
  clientCode?: string
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}

// ==================== 权益激活记录 ====================

/** 激活记录 */
export interface EquityActivate {
  id?: number
  activateCode: string
  equityCode: string
  goodsCode?: string
  clientCode?: string
  clientFullName?: string
  clientPhone?: string
  activateChannel?: number
  activateSourceCode?: string
  activateTime?: string
  expireTime?: string
  isIdCardVerified?: number
  isAgreementSigned?: number
}

/** 激活记录查询 */
export interface EquityActivateQuery extends PageQuery {
  activateCode?: string
  equityCode?: string
  goodsCode?: string
  clientCode?: string
  activateChannel?: number
}
