/**
 * 订单域相关类型（权益订单 + 场景订单）。
 *
 * 字段对齐后端 com.dayan.order.entity.OrderEquity / OrderScene 及对应 QueryDTO。
 * - Integer → number
 * - BigDecimal → number
 * - LocalDateTime / LocalDate → string
 */
import type { PageQuery } from '@/types/common'

// ==================== 权益订单 ====================

/**
 * 权益订单状态（order_equity.order_status）。
 *
 * 0=待支付 / 1=已支付 / 2=已发货 / 3=已完成 / 4=已取消 / 5=退款中 / 6=已退款。
 */
export enum EquityOrderStatus {
  /** 待支付 */
  PENDING = 0,
  /** 已支付 */
  PAID = 1,
  /** 已发货 */
  DELIVERED = 2,
  /** 已完成 */
  COMPLETED = 3,
  /** 已取消 */
  CANCELLED = 4,
  /** 退款中 */
  REFUNDING = 5,
  /** 已退款 */
  REFUNDED = 6
}

/** 权益订单状态选项 */
export const EQUITY_ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: EquityOrderStatus.PENDING },
  { label: '已支付', value: EquityOrderStatus.PAID },
  { label: '已发货', value: EquityOrderStatus.DELIVERED },
  { label: '已完成', value: EquityOrderStatus.COMPLETED },
  { label: '已取消', value: EquityOrderStatus.CANCELLED },
  { label: '退款中', value: EquityOrderStatus.REFUNDING },
  { label: '已退款', value: EquityOrderStatus.REFUNDED }
] as const

/**
 * 场景订单状态（order_scene.order_status）。
 *
 * 0=待支付 / 1=已支付 / 2=已完成 / 3=已取消 / 4=退款中 / 5=已退款。
 */
export enum SceneOrderStatus {
  /** 待支付 */
  PENDING = 0,
  /** 已支付 */
  PAID = 1,
  /** 已完成 */
  COMPLETED = 2,
  /** 已取消 */
  CANCELLED = 3,
  /** 退款中 */
  REFUNDING = 4,
  /** 已退款 */
  REFUNDED = 5
}

/** 场景订单状态选项 */
export const SCENE_ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: SceneOrderStatus.PENDING },
  { label: '已支付', value: SceneOrderStatus.PAID },
  { label: '已完成', value: SceneOrderStatus.COMPLETED },
  { label: '已取消', value: SceneOrderStatus.CANCELLED },
  { label: '退款中', value: SceneOrderStatus.REFUNDING },
  { label: '已退款', value: SceneOrderStatus.REFUNDED }
] as const

/**
 * 采购来源（order_equity.order_source）。
 *
 * 1=渠道采购 / 2=代理人下单 / 3=分销商下单 / 4=平台直采。
 */
export const ORDER_SOURCE_OPTIONS = [
  { label: '渠道采购', value: 1 },
  { label: '代理人下单', value: 2 },
  { label: '分销商下单', value: 3 },
  { label: '平台直采', value: 4 }
] as const

/**
 * 支付方式（pay_type）。
 *
 * 1=微信 / 2=支付宝 / 3=余额 / 4=线下 / 5=混合支付。
 */
export const PAY_TYPE_OPTIONS = [
  { label: '微信', value: 1 },
  { label: '支付宝', value: 2 },
  { label: '余额', value: 3 },
  { label: '线下', value: 4 },
  { label: '混合支付', value: 5 }
] as const

/**
 * 权益订单实体（后端 OrderEquityVO）。
 */
export interface OrderEquity {
  id?: number
  /** 订单编号（主键，业务生成） */
  orderCode?: string
  /** 采购来源：1渠道/2代理人/3分销商/4平台直采 */
  orderSource?: number
  /** 渠道编码 */
  channelCode?: string
  /** 渠道名称（快照） */
  channelFullName?: string
  /** 代理人编码 */
  agentCode?: string
  /** 代理人姓名（快照） */
  agentFullName?: string
  /** 分销商编码 */
  distributorCode?: string
  /** 分销商名称（快照） */
  distributorFullName?: string
  /** 商品编码 */
  goodsCode?: string
  /** 商品名称 */
  goodsName?: string
  /** SKU 编码 */
  skuCode?: string
  /** SKU 名称 */
  skuName?: string
  /** 购买数量 */
  quantity?: number
  /** 单价 */
  unitPrice?: number
  /** 订单总额 */
  totalAmount?: number
  /** 优惠金额 */
  discountAmount?: number
  /** 实付金额 */
  payAmount?: number
  /** 支付方式 */
  payType?: number
  /** 支付时间 */
  payTime?: string
  /** 支付流水号 */
  payTradeNo?: string
  /** 权益入库方式 */
  deliverType?: number
  /** 已入库数量 */
  deliverCount?: number
  /** 入库完成时间 */
  deliverTime?: string
  /** 订单过期时间 */
  expireTime?: string
  /** 发票状态 */
  invoiceStatus?: number
  /** 平台运营方编码 */
  organCode?: string
  /** 订单状态 */
  orderStatus?: EquityOrderStatus
  /** 取消原因 */
  cancelReason?: string
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 权益订单分页查询参数（后端 OrderEquityQueryDTO）。
 */
export interface OrderEquityQuery extends PageQuery {
  orderCode?: string
  orderSource?: number
  channelCode?: string
  agentCode?: string
  distributorCode?: string
  goodsCode?: string
  skuCode?: string
  organCode?: string
  orderStatus?: EquityOrderStatus
  payType?: number
}

// ==================== 场景订单 ====================

/**
 * 场景订单实体（后端 OrderSceneVO）。
 */
export interface OrderScene {
  id?: number
  /** 订单编号（主键） */
  orderCode?: string
  /** 订单类型 */
  orderType?: number
  /** 渠道编码 */
  channelCode?: string
  /** 渠道名称（快照） */
  channelFullName?: string
  /** 代理人编码 */
  agentCode?: string
  /** 代理人姓名（快照） */
  agentFullName?: string
  /** 分销商编码 */
  distributorCode?: string
  /** 分销商名称（快照） */
  distributorFullName?: string
  /** 客户编码 */
  clientCode?: string
  /** 客户姓名（快照） */
  clientFullName?: string
  /** 商品编码 */
  goodsCode?: string
  /** 场景编码 */
  sceneCode?: string
  /** 场景名称 */
  sceneName?: string
  /** SKU 编码 */
  skuCode?: string
  /** 排期编码 */
  scheduleCode?: string
  /** 活动日期 */
  activityDate?: string
  /** 参与人数 */
  participantCount?: number
  /** 参与人姓名 */
  participantNames?: string
  /** 单价 */
  unitPrice?: number
  /** 订单总额 */
  totalAmount?: number
  /** 优惠金额 */
  discountAmount?: number
  /** 实付金额 */
  payAmount?: number
  /** 优惠券编码 */
  couponCode?: string
  /** 优惠券抵扣 */
  couponAmount?: number
  /** 支付方式 */
  payType?: number
  /** 支付时间 */
  payTime?: string
  /** 使用的权益编码 */
  equityCode?: string
  /** 联系人姓名 */
  contactName?: string
  /** 联系人电话 */
  contactPhone?: string
  /** 备注 */
  remark?: string
  /** 订单状态 */
  orderStatus?: SceneOrderStatus
  /** 取消原因 */
  cancelReason?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 场景订单分页查询参数（后端 OrderSceneQueryDTO）。
 */
export interface OrderSceneQuery extends PageQuery {
  orderCode?: string
  orderType?: number
  channelCode?: string
  agentCode?: string
  distributorCode?: string
  clientCode?: string
  sceneCode?: string
  scheduleCode?: string
  couponCode?: string
  equityCode?: string
  /** 活动日期范围起（yyyy-MM-dd） */
  activityDateStart?: string
  /** 活动日期范围止（yyyy-MM-dd） */
  activityDateEnd?: string
  orderStatus?: SceneOrderStatus
  payType?: number
}
