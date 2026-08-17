/**
 * 订单相关类型。
 *
 * 字段对齐后端 Order 域 Entity（com.dayan.order.entity.Order），
 * 渠道后台视角取本渠道订单子集。
 *
 * 4 类订单（订单管理 4 tab）：
 * - OrderEquity（权益订单，对齐 OrderEquityVO，本文件下方 `Order` interface）
 * - OrderScene（场景订单，对齐 OrderSceneVO）
 * - OrderCourse（课程订单，对齐 OrderCourseVO）
 * - OrderSojourn（旅游短居订单，对齐 OrderSojournVO）
 *
 * 金额单位均为「元」（后端 BigDecimal），前端直接显示，不要除以 100。
 */
import type { PageQuery } from '@/types/common'

/**
 * 订单类型：1=权益 / 2=场景 / 3=课程 / 4=旅游短居。
 *
 * 对齐后端 finance_payment.order_type 与各订单表 order_type 字段。
 */
export enum OrderType {
  /** 权益订单 */
  EQUITY = 1,
  /** 场景订单 */
  SCENE = 2,
  /** 课程订单 */
  COURSE = 3,
  /** 旅游短居订单 */
  SOJOURN = 4
}

/** 订单类型选项（订单管理 4 tab 用，四端统一命名） */
export const ORDER_TYPE_OPTIONS = [
  { label: '养老权益订单', value: OrderType.EQUITY },
  { label: '场景营销订单', value: OrderType.SCENE },
  { label: '培训课程订单', value: OrderType.COURSE },
  { label: '旅游短居订单', value: OrderType.SOJOURN }
] as const

/**
 * 订单状态：对齐后端 OrderEvent 状态机（ORDER_SM）。
 * 0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款。
 */
export enum OrderStatus {
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

/** 订单状态选项 */
export const ORDER_STATUS_OPTIONS = [
  { label: '待支付', value: OrderStatus.PENDING_PAY },
  { label: '已支付', value: OrderStatus.PAID },
  { label: '部分发放', value: OrderStatus.PARTIAL_DELIVERED },
  { label: '已发放', value: OrderStatus.DELIVERED },
  { label: '已完成', value: OrderStatus.COMPLETED },
  { label: '已取消', value: OrderStatus.CANCELLED },
  { label: '退款中', value: OrderStatus.REFUNDING },
  { label: '已退款', value: OrderStatus.REFUNDED }
] as const

/**
 * 订单实体（渠道视角子集，对齐 OrderEquityVO）。
 *
 * 注意：金额单位是「元」（后端 DECIMAL），前端直接显示，不要除以 100。
 */
export interface Order {
  id?: number
  /** 订单编码（主键业务码） */
  orderCode?: string
  /** 采购来源：1=对公 / 2=个人 */
  orderSource?: number
  /** 渠道编码 */
  channelCode?: string
  /** 渠道全称 */
  channelFullName?: string
  /** 经销商编码 */
  agentCode?: string
  /** 经销商全称 */
  agentFullName?: string
  /** 分销商编码 */
  distributorCode?: string
  /** 分销商全称 */
  distributorFullName?: string
  /** 商品编码 */
  goodsCode?: string
  /** 商品名称 */
  goodsName?: string
  /** 规格编码 */
  skuCode?: string
  /** 规格名称 */
  skuName?: string
  /** 数量 */
  quantity?: number
  /** 单价（元） */
  unitPrice?: number
  /** 订单总金额（单位：元，不要除以100） */
  totalAmount?: number
  /** 优惠金额（元） */
  discountAmount?: number
  /** 实付金额（单位：元，不要除以100） */
  payAmount?: number
  /** 支付方式：1=微信支付 / 2=支付宝 / 3=银行转账 / 4=余额支付 / 5=线下支付 */
  payType?: number
  /** 支付时间（yyyy-MM-dd HH:mm:ss） */
  payTime?: string
  /** 第三方交易号 */
  payTradeNo?: string
  /** 入库方式：1=批量 / 2=逐张 / 3=自动入库 */
  deliverType?: number
  /** 发放数量 */
  deliverCount?: number
  /** 发放时间（yyyy-MM-dd HH:mm:ss） */
  deliverTime?: string
  /** 到期时间（yyyy-MM-dd HH:mm:ss） */
  expireTime?: string
  /** 发票状态 */
  invoiceStatus?: number
  /** 机构编码 */
  organCode?: string
  /** 订单状态（0-7，见 OrderStatus） */
  orderStatus?: OrderStatus
  /** 取消原因 */
  cancelReason?: string
  /** 备注 */
  remark?: string
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createdAt?: string
  /** 更新时间（yyyy-MM-dd HH:mm:ss） */
  updatedAt?: string
}

/** 采购来源选项（对公 / 个人，对齐 OrderEquityVO.orderSource） */
export const ORDER_SOURCE_OPTIONS = [
  { label: '对公', value: 1 },
  { label: '个人', value: 2 }
] as const

/** 订单分页查询参数（对齐 OrderEquityQueryDTO） */
export interface OrderQuery {
  /** 订单编码（模糊匹配，可选） */
  orderCode?: string
  /** 订单状态（可选） */
  orderStatus?: OrderStatus
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}

// ==================== 场景订单（OrderScene，对齐 OrderSceneVO） ====================

/**
 * 场景订单实体（渠道视角子集，对齐后端 OrderSceneVO）。
 *
 * 字段对齐后端 `com.dayan.order.vo.OrderSceneVO` 的常用列子集
 * （完整列见后端 VO，前端按需取字段，全部 optional）。
 */
export interface OrderScene {
  id?: number
  /** 订单编码（主键业务码） */
  orderCode?: string
  /** 订单类型：2=场景（OrderType.SCENE） */
  orderType?: number
  /** 渠道编码 */
  channelCode?: string
  /** 经销商编码 */
  agentCode?: string
  /** 分销商编码 */
  distributorCode?: string
  /** 客户编码 */
  clientCode?: string
  /** 客户姓名 */
  clientFullName?: string
  /** 场景编码 */
  sceneCode?: string
  /** 场景名称 */
  sceneName?: string
  /** 活动日期（yyyy-MM-dd） */
  activityDate?: string
  /** 参与人数 */
  participantCount?: number
  /** 单价（元） */
  unitPrice?: number
  /** 订单总金额（元） */
  totalAmount?: number
  /** 优惠金额（元） */
  discountAmount?: number
  /** 实付金额（元） */
  payAmount?: number
  /** 支付方式：1=微信支付 / 2=支付宝 / 3=银行转账 / 4=余额支付 / 5=线下支付 */
  payType?: number
  /** 支付时间（yyyy-MM-dd HH:mm:ss） */
  payTime?: string
  /** 订单状态（0-7，见 OrderStatus） */
  orderStatus?: OrderStatus
  /** 取消原因 */
  cancelReason?: string
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createdAt?: string
}

/**
 * 场景订单分页查询参数（对齐后端 OrderSceneQueryDTO）。
 *
 * 注意：channelCode 由后端从 ContextHolder 强制注入，前端不可传。
 */
export interface OrderSceneQuery extends PageQuery {
  /** 订单编码（模糊匹配） */
  orderCode?: string
  /** 订单状态 */
  orderStatus?: OrderStatus
  /** 场景编码 */
  sceneCode?: string
  /** 客户编码 */
  clientCode?: string
}

// ==================== 课程订单（OrderCourse，对齐 OrderCourseVO） ====================

/**
 * 课程订单实体（渠道视角子集，对齐后端 OrderCourseVO）。
 */
export interface OrderCourse {
  id?: number
  /** 订单编码（主键业务码） */
  orderCode?: string
  /** 订单类型：3=课程（OrderType.COURSE） */
  orderType?: number
  /** 渠道编码 */
  channelCode?: string
  /** 经销商编码 */
  agentCode?: string
  /** 分销商编码 */
  distributorCode?: string
  /** 客户编码 */
  clientCode?: string
  /** 客户姓名 */
  clientFullName?: string
  /** 课程编码 */
  courseCode?: string
  /** 课程名称 */
  courseName?: string
  /** 购买数量 */
  quantity?: number
  /** 单价（元） */
  unitPrice?: number
  /** 订单总金额（元） */
  totalAmount?: number
  /** 优惠金额（元） */
  discountAmount?: number
  /** 实付金额（元） */
  payAmount?: number
  /** 支付方式：1=微信支付 / 2=支付宝 / 3=银行转账 / 4=余额支付 / 5=线下支付 */
  payType?: number
  /** 支付时间（yyyy-MM-dd HH:mm:ss） */
  payTime?: string
  /** 订单状态（0-7，见 OrderStatus） */
  orderStatus?: OrderStatus
  /** 取消原因 */
  cancelReason?: string
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createdAt?: string
}

/**
 * 课程订单分页查询参数（对齐后端 OrderCourseQueryDTO）。
 *
 * 注意：channelCode 由后端从 ContextHolder 强制注入，前端不可传。
 */
export interface OrderCourseQuery extends PageQuery {
  /** 订单编码（模糊匹配） */
  orderCode?: string
  /** 订单状态 */
  orderStatus?: OrderStatus
  /** 课程编码 */
  courseCode?: string
  /** 客户编码 */
  clientCode?: string
}

// ==================== 旅游短居订单（OrderSojourn，对齐 OrderSojournVO） ====================

/**
 * 旅游短居订单实体（渠道视角子集，对齐后端 OrderSojournVO）。
 */
export interface OrderSojourn {
  id?: number
  /** 订单编码（主键业务码） */
  orderCode?: string
  /** 订单类型：4=旅游短居（OrderType.SOJOURN） */
  orderType?: number
  /** 渠道编码 */
  channelCode?: string
  /** 经销商编码 */
  agentCode?: string
  /** 分销商编码 */
  distributorCode?: string
  /** 客户编码 */
  clientCode?: string
  /** 客户姓名 */
  clientFullName?: string
  /** 基地/园区编码 */
  parkCode?: string
  /** 基地/园区名称 */
  parkFullName?: string
  /** 房型编码 */
  roomTypeCode?: string
  /** 入住日期（yyyy-MM-dd） */
  checkinDate?: string
  /** 退房日期（yyyy-MM-dd） */
  checkoutDate?: string
  /** 入住天数 */
  stayDays?: number
  /** 入住人数 */
  residentCount?: number
  /** 房费（元） */
  roomFee?: number
  /** 护理费（元） */
  careFee?: number
  /** 餐费（元） */
  foodFee?: number
  /** 其他费用（元） */
  otherFee?: number
  /** 订单总金额（元） */
  totalAmount?: number
  /** 优惠金额（元） */
  discountAmount?: number
  /** 实付金额（元） */
  payAmount?: number
  /** 支付方式：1=微信支付 / 2=支付宝 / 3=银行转账 / 4=余额支付 / 5=线下支付 */
  payType?: number
  /** 支付时间（yyyy-MM-dd HH:mm:ss） */
  payTime?: string
  /** 押金（元） */
  depositAmount?: number
  /** 订单状态（0-7，见 OrderStatus） */
  orderStatus?: OrderStatus
  /** 取消原因 */
  cancelReason?: string
  /** 创建时间（yyyy-MM-dd HH:mm:ss） */
  createdAt?: string
}

/**
 * 旅游短居订单分页查询参数（对齐后端 OrderSojournQueryDTO）。
 *
 * 注意：channelCode 由后端从 ContextHolder 强制注入，前端不可传。
 */
export interface OrderSojournQuery extends PageQuery {
  /** 订单编码（模糊匹配） */
  orderCode?: string
  /** 订单状态 */
  orderStatus?: OrderStatus
  /** 基地/园区编码 */
  parkCode?: string
  /** 客户编码 */
  clientCode?: string
}
