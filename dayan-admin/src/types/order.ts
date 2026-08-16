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
 * 对齐后端 DDL 15_order.sql：0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款。
 */
export enum EquityOrderStatus {
  /** 待支付 */
  PENDING_PAY = 0,
  /** 已支付 */
  PAID = 1,
  /** 部分发放 */
  PARTIAL_DELIVERED = 2,
  /** 已发放 */
  DELIVERED = 3,
  /** 已完成 */
  COMPLETED = 4,
  /** 已取消 */
  CANCELLED = 5,
  /** 退款中 */
  REFUNDING = 6,
  /** 已退款 */
  REFUNDED = 7
}

/** 权益订单状态选项 */
export const EQUITY_ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: EquityOrderStatus.PENDING_PAY },
  { label: '已支付', value: EquityOrderStatus.PAID },
  { label: '部分发放', value: EquityOrderStatus.PARTIAL_DELIVERED },
  { label: '已发放', value: EquityOrderStatus.DELIVERED },
  { label: '已完成', value: EquityOrderStatus.COMPLETED },
  { label: '已取消', value: EquityOrderStatus.CANCELLED },
  { label: '退款中', value: EquityOrderStatus.REFUNDING },
  { label: '已退款', value: EquityOrderStatus.REFUNDED }
] as const

/**
 * 场景订单状态（order_scene.order_status）。
 *
 * 对齐后端 DDL 15_order.sql：0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款。
 */
export enum SceneOrderStatus {
  /** 待支付 */
  PENDING_PAY = 0,
  /** 已支付 */
  PAID = 1,
  /** 部分发放 */
  PARTIAL_DELIVERED = 2,
  /** 已发放 */
  DELIVERED = 3,
  /** 已完成 */
  COMPLETED = 4,
  /** 已取消 */
  CANCELLED = 5,
  /** 退款中 */
  REFUNDING = 6,
  /** 已退款 */
  REFUNDED = 7
}

/** 场景订单状态选项 */
export const SCENE_ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: SceneOrderStatus.PENDING_PAY },
  { label: '已支付', value: SceneOrderStatus.PAID },
  { label: '部分发放', value: SceneOrderStatus.PARTIAL_DELIVERED },
  { label: '已发放', value: SceneOrderStatus.DELIVERED },
  { label: '已完成', value: SceneOrderStatus.COMPLETED },
  { label: '已取消', value: SceneOrderStatus.CANCELLED },
  { label: '退款中', value: SceneOrderStatus.REFUNDING },
  { label: '已退款', value: SceneOrderStatus.REFUNDED }
] as const

/**
 * 采购来源（order_equity.order_source）。
 *
 * 对齐后端 DDL 15_order.sql：1=渠道对公采购 / 2=代理人个人采购。
 */
export const ORDER_SOURCE_OPTIONS = [
  { label: '渠道对公采购', value: 1 },
  { label: '代理人个人采购', value: 2 }
] as const

/**
 * 支付方式（pay_type）。
 *
 * 1=微信 / 2=支付宝 / 3=银行转账 / 4=余额支付 / 5=线下支付。
 */
export const PAY_TYPE_OPTIONS = [
  { label: '微信', value: 1 },
  { label: '支付宝', value: 2 },
  { label: '银行转账', value: 3 },
  { label: '余额支付', value: 4 },
  { label: '线下支付', value: 5 }
] as const

/**
 * 权益订单实体（后端 OrderEquityVO）。
 */
export interface OrderEquity {
  id?: number
  /** 订单编号（主键，业务生成） */
  orderCode?: string
  /** 采购来源：1渠道对公采购 / 2代理人个人采购 */
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

// ==================== 课程订单 ====================

/**
 * 课程订单状态（order_course.order_status）。
 *
 * 对齐后端 DDL 15_order.sql：0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款。
 */
export enum CourseOrderStatus {
  /** 待支付 */
  PENDING_PAY = 0,
  /** 已支付 */
  PAID = 1,
  /** 部分发放 */
  PARTIAL_DELIVERED = 2,
  /** 已发放 */
  DELIVERED = 3,
  /** 已完成 */
  COMPLETED = 4,
  /** 已取消 */
  CANCELLED = 5,
  /** 退款中 */
  REFUNDING = 6,
  /** 已退款 */
  REFUNDED = 7
}

/** 课程订单状态选项 */
export const COURSE_ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: CourseOrderStatus.PENDING_PAY },
  { label: '已支付', value: CourseOrderStatus.PAID },
  { label: '部分发放', value: CourseOrderStatus.PARTIAL_DELIVERED },
  { label: '已发放', value: CourseOrderStatus.DELIVERED },
  { label: '已完成', value: CourseOrderStatus.COMPLETED },
  { label: '已取消', value: CourseOrderStatus.CANCELLED },
  { label: '退款中', value: CourseOrderStatus.REFUNDING },
  { label: '已退款', value: CourseOrderStatus.REFUNDED }
] as const

/**
 * 课程/旅游短居订单支付方式（pay_type）。
 *
 * 与权益/场景订单不同：1=微信 / 2=支付宝 / 3=银行转账 / 4=余额 / 5=线下。
 * 课程与旅游短居共用同一枚举，故以 COURSE_PAY_TYPE_OPTIONS 命名并复用。
 */
export const COURSE_PAY_TYPE_OPTIONS = [
  { label: '微信', value: 1 },
  { label: '支付宝', value: 2 },
  { label: '银行转账', value: 3 },
  { label: '余额', value: 4 },
  { label: '线下', value: 5 }
] as const

/**
 * 课程订单实体（后端 OrderCourseVO）。
 */
export interface OrderCourse {
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
  /** 商品名称（快照） */
  goodsName?: string
  /** 课程编码 */
  courseCode?: string
  /** 课程名称 */
  courseName?: string
  /** SKU 编码 */
  skuCode?: string
  /** 规格名称（快照） */
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
  /** 优惠券编码 */
  couponCode?: string
  /** 支付方式 */
  payType?: number
  /** 支付时间 */
  payTime?: string
  /** 使用的权益编码 */
  equityCode?: string
  /** 订单状态 */
  orderStatus?: CourseOrderStatus
  /** 取消原因 */
  cancelReason?: string
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 课程订单分页查询参数（后端 OrderCourseQueryDTO）。
 */
export interface OrderCourseQuery extends PageQuery {
  orderCode?: string
  orderType?: number
  channelCode?: string
  agentCode?: string
  distributorCode?: string
  clientCode?: string
  courseCode?: string
  couponCode?: string
  equityCode?: string
  orderStatus?: CourseOrderStatus
  payType?: number
}

// ==================== 旅游短居订单 ====================

/**
 * 旅游短居订单状态（order_sojourn.order_status）。
 *
 * 对齐后端 DDL 15_order.sql：0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款。
 */
export enum SojournOrderStatus {
  /** 待支付 */
  PENDING_PAY = 0,
  /** 已支付 */
  PAID = 1,
  /** 部分发放 */
  PARTIAL_DELIVERED = 2,
  /** 已发放 */
  DELIVERED = 3,
  /** 已完成 */
  COMPLETED = 4,
  /** 已取消 */
  CANCELLED = 5,
  /** 退款中 */
  REFUNDING = 6,
  /** 已退款 */
  REFUNDED = 7
}

/** 旅游短居订单状态选项 */
export const SOJOURN_ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: SojournOrderStatus.PENDING_PAY },
  { label: '已支付', value: SojournOrderStatus.PAID },
  { label: '部分发放', value: SojournOrderStatus.PARTIAL_DELIVERED },
  { label: '已发放', value: SojournOrderStatus.DELIVERED },
  { label: '已完成', value: SojournOrderStatus.COMPLETED },
  { label: '已取消', value: SojournOrderStatus.CANCELLED },
  { label: '退款中', value: SojournOrderStatus.REFUNDING },
  { label: '已退款', value: SojournOrderStatus.REFUNDED }
] as const

/**
 * 旅游短居订单实体（后端 OrderSojournVO）。
 */
export interface OrderSojourn {
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
  /** 商品名称（快照） */
  goodsName?: string
  /** 旅游短居基地编码 */
  parkCode?: string
  /** 旅游短居基地名称（快照） */
  parkFullName?: string
  /** 房型编码 */
  roomTypeCode?: string
  /** SKU 编码 */
  skuCode?: string
  /** 规格名称（快照） */
  skuName?: string
  /** 入住日期 */
  checkinDate?: string
  /** 离店日期 */
  checkoutDate?: string
  /** 入住天数 */
  stayDays?: number
  /** 入住人数 */
  residentCount?: number
  /** 入住人姓名 */
  residentNames?: string
  /** 照护类型编码 */
  careTypeCode?: string
  /** 餐食类型编码 */
  foodTypeCode?: string
  /** 房费 */
  roomFee?: number
  /** 照护费 */
  careFee?: number
  /** 餐费 */
  foodFee?: number
  /** 其他费用 */
  otherFee?: number
  /** 订单总额 */
  totalAmount?: number
  /** 优惠金额 */
  discountAmount?: number
  /** 实付金额 */
  payAmount?: number
  /** 优惠券编码 */
  couponCode?: string
  /** 支付方式 */
  payType?: number
  /** 支付时间 */
  payTime?: string
  /** 押金金额 */
  depositAmount?: number
  /** 使用的权益编码 */
  equityCode?: string
  /** 联系人姓名 */
  contactName?: string
  /** 联系人电话 */
  contactPhone?: string
  /** 特殊需求 */
  specialNeeds?: string
  /** 订单状态 */
  orderStatus?: SojournOrderStatus
  /** 取消原因 */
  cancelReason?: string
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 旅游短居订单分页查询参数（后端 OrderSojournQueryDTO）。
 */
export interface OrderSojournQuery extends PageQuery {
  orderCode?: string
  orderType?: number
  channelCode?: string
  agentCode?: string
  distributorCode?: string
  clientCode?: string
  parkCode?: string
  roomTypeCode?: string
  couponCode?: string
  equityCode?: string
  /** 入住日期范围起（yyyy-MM-dd） */
  checkinDateStart?: string
  /** 入住日期范围止（yyyy-MM-dd） */
  checkinDateEnd?: string
  orderStatus?: SojournOrderStatus
  payType?: number
}
