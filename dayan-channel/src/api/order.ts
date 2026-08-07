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

// ==================== 旅居订单（/channel-api/order-sojourns） ====================

/** 旅居订单分页：GET /channel-api/order-sojourns */
export function pageOrderSojourns(query: OrderSojournQuery): Promise<PageResult<OrderSojourn>> {
  return request<PageResult<OrderSojourn>>({
    url: '/channel-api/order-sojourns',
    method: 'get',
    params: query
  })
}

/** 旅居订单详情：GET /channel-api/order-sojourns/{orderCode} */
export function getOrderSojourn(orderCode: string): Promise<OrderSojourn> {
  return request<OrderSojourn>({
    url: `/channel-api/order-sojourns/${orderCode}`,
    method: 'get'
  })
}

/** 取消旅居订单：POST /channel-api/order-sojourns/{orderCode}/cancel */
export function cancelOrderSojourn(orderCode: string, data: OrderCancelData): Promise<void> {
  return request<void>({
    url: `/channel-api/order-sojourns/${orderCode}/cancel`,
    method: 'post',
    data
  })
}
