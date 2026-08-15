import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  Order,
  OrderQuery,
  OrderScene,
  OrderSceneQuery,
  OrderCourse,
  OrderCourseQuery,
  OrderSojourn,
  OrderSojournQuery
} from '@/types/order'

/**
 * 订单接口封装。
 *
 * 对应后端 Channel 端 OrderController（/channel-api/order-equities、
 * /channel-api/order-scenes、/channel-api/order-courses、/channel-api/order-sojourns）。
 *
 * 4 类订单各提供：分页（page）/ 详情（get）/ 取消（cancel）3 个接口。
 * 权益订单的分页接口命名保留增量1 的 `pageOrders`（避免破坏现有调用方），
 * 详情/取消命名为 `getOrderEquity` / `cancelOrderEquity`。
 *
 * 防越权：channelCode 由后端从 ContextHolder 强制注入，前端不可传；
 * 详情/取消接口后端做渠道归属二次校验。
 */

/** 取消订单入参（对齐后端 OrderCancelDTO 的前端可见字段）。 */
export interface OrderCancelData {
  /** 取消原因（必填，NotBlack） */
  cancelReason: string
}

// ==================== 权益订单（/channel-api/order-equities） ====================

/** 权益订单分页：GET /channel-api/order-equities */
export function pageOrders(query: OrderQuery): Promise<PageResult<Order>> {
  return request<PageResult<Order>>({
    url: '/channel-api/order-equities',
    method: 'get',
    params: query
  })
}

/** 权益订单详情：GET /channel-api/order-equities/{orderCode} */
export function getOrderEquity(orderCode: string): Promise<Order> {
  return request<Order>({
    url: `/channel-api/order-equities/${orderCode}`,
    method: 'get'
  })
}

/** 取消权益订单：POST /channel-api/order-equities/{orderCode}/cancel */
export function cancelOrderEquity(orderCode: string, data: OrderCancelData): Promise<void> {
  return request<void>({
    url: `/channel-api/order-equities/${orderCode}/cancel`,
    method: 'post',
    data
  })
}

/**
 * 权益商品下单入参（对齐 CreateOrderEquityDTO 前端可见子集）。
 *
 * 防篡改/防越权：channelCode/operatorCode 由后端 Controller 强制从 ContextHolder 注入；
 * unitPrice/goodsName 后端从商品目录权威解析覆盖前端值，故前端只需传 DTO @NotNull/@NotBlank
 * 要求的非空字段，价格传商品的 salePrice 即可（仅供参考，最终以服务端覆盖值为准）。
 */
export interface CreateOrderEquityData {
  /** 采购来源：1=对公 / 2=个人 */
  orderSource: number
  /** 商品编码 */
  goodsCode: string
  /** 商品名称（会被后端权威覆盖，但 DTO @NotBlank 需传非空） */
  goodsName: string
  /** 购买数量（≥1） */
  quantity: number
  /** 单价（会被后端权威覆盖，DTO @NotNull 需传非空，可用 salePrice） */
  unitPrice: number
  /** 备注（可选） */
  remark?: string
}

/** 权益商品下单：POST /channel-api/order-equities（返回 orderCode） */
export function createOrderEquity(data: CreateOrderEquityData): Promise<string> {
  return request<string>({
    url: '/channel-api/order-equities',
    method: 'post',
    data
  })
}

// ==================== 场景订单（/channel-api/order-scenes） ====================

/** 场景订单分页：GET /channel-api/order-scenes */
export function pageOrderScenes(query: OrderSceneQuery): Promise<PageResult<OrderScene>> {
  return request<PageResult<OrderScene>>({
    url: '/channel-api/order-scenes',
    method: 'get',
    params: query
  })
}

/** 场景订单详情：GET /channel-api/order-scenes/{orderCode} */
export function getOrderScene(orderCode: string): Promise<OrderScene> {
  return request<OrderScene>({
    url: `/channel-api/order-scenes/${orderCode}`,
    method: 'get'
  })
}

/** 取消场景订单：POST /channel-api/order-scenes/{orderCode}/cancel */
export function cancelOrderScene(orderCode: string, data: OrderCancelData): Promise<void> {
  return request<void>({
    url: `/channel-api/order-scenes/${orderCode}/cancel`,
    method: 'post',
    data
  })
}

// ==================== 课程订单（/channel-api/order-courses） ====================

/** 课程订单分页：GET /channel-api/order-courses */
export function pageOrderCourses(query: OrderCourseQuery): Promise<PageResult<OrderCourse>> {
  return request<PageResult<OrderCourse>>({
    url: '/channel-api/order-courses',
    method: 'get',
    params: query
  })
}

/** 课程订单详情：GET /channel-api/order-courses/{orderCode} */
export function getOrderCourse(orderCode: string): Promise<OrderCourse> {
  return request<OrderCourse>({
    url: `/channel-api/order-courses/${orderCode}`,
    method: 'get'
  })
}

/** 取消课程订单：POST /channel-api/order-courses/{orderCode}/cancel */
export function cancelOrderCourse(orderCode: string, data: OrderCancelData): Promise<void> {
  return request<void>({
    url: `/channel-api/order-courses/${orderCode}/cancel`,
    method: 'post',
    data
  })
}

// ==================== 旅游短居订单（/channel-api/order-sojourns） ====================

/** 旅游短居订单分页：GET /channel-api/order-sojourns */
export function pageOrderSojourns(query: OrderSojournQuery): Promise<PageResult<OrderSojourn>> {
  return request<PageResult<OrderSojourn>>({
    url: '/channel-api/order-sojourns',
    method: 'get',
    params: query
  })
}

/** 旅游短居订单详情：GET /channel-api/order-sojourns/{orderCode} */
export function getOrderSojourn(orderCode: string): Promise<OrderSojourn> {
  return request<OrderSojourn>({
    url: `/channel-api/order-sojourns/${orderCode}`,
    method: 'get'
  })
}

/** 取消旅游短居订单：POST /channel-api/order-sojourns/{orderCode}/cancel */
export function cancelOrderSojourn(orderCode: string, data: OrderCancelData): Promise<void> {
  return request<void>({
    url: `/channel-api/order-sojourns/${orderCode}/cancel`,
    method: 'post',
    data
  })
}
