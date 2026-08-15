import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  OrderCourse,
  OrderCourseQuery,
  OrderEquity,
  OrderEquityQuery,
  OrderScene,
  OrderSceneQuery,
  OrderSojourn,
  OrderSojournQuery
} from '@/types/order'

/**
 * 订单域接口封装（权益订单 + 场景订单 + 课程订单 + 旅游短居订单）。
 *
 * 对应后端：
 * - OrderEquityAdminController（/admin-api/order/equity/*）
 * - OrderSceneAdminController（/admin-api/order/scene/*）
 * - OrderCourseAdminController（/admin-api/order/course/*）
 * - OrderSojournAdminController（/admin-api/order/sojourn/*）
 *
 * 第一版只实现列表 / 详情 / 取消；生命周期端点（pay-callback / deliver / complete /
 * apply-refund）暂留 TODO，由后续迭代补全。
 */

// ==================== 权益订单 ====================

/** 权益订单分页：GET /admin-api/order/equity/page */
export function pageOrderEquitys(query: OrderEquityQuery): Promise<PageResult<OrderEquity>> {
  return request<PageResult<OrderEquity>>({
    url: '/admin-api/order/equity/page',
    method: 'get',
    params: query
  })
}

/** 权益订单详情：GET /admin-api/order/equity/{orderCode} */
export function getOrderEquity(orderCode: string): Promise<OrderEquity> {
  return request<OrderEquity>({
    url: `/admin-api/order/equity/${orderCode}`,
    method: 'get'
  })
}

/**
 * 取消权益订单：POST /admin-api/order/equity/cancel
 *
 * 入参对齐后端 OrderCancelDTO（@RequestBody），状态机流转：0→5 或 6→5。
 */
export function cancelOrderEquity(data: {
  orderCode: string
  cancelReason: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/order/equity/cancel',
    method: 'post',
    data
  })
}

// TODO 第一版暂不开放的生命周期端点（后端已实现，按需放开注释）：
// - payCallback（0→1 已支付）：POST /admin-api/order/equity/pay-callback
// - deliver（部分发放 1→2 / 全部发放 1|2→3）：POST /admin-api/order/equity/deliver
// - complete（3→4 已完成）：POST /admin-api/order/equity/complete
// - applyRefund（1/2/3→6 退款中）：POST /admin-api/order/equity/apply-refund

// ==================== 场景订单 ====================

/** 场景订单分页：GET /admin-api/order/scene/page */
export function pageOrderScenes(query: OrderSceneQuery): Promise<PageResult<OrderScene>> {
  return request<PageResult<OrderScene>>({
    url: '/admin-api/order/scene/page',
    method: 'get',
    params: query
  })
}

/** 场景订单详情：GET /admin-api/order/scene/{orderCode} */
export function getOrderScene(orderCode: string): Promise<OrderScene> {
  return request<OrderScene>({
    url: `/admin-api/order/scene/${orderCode}`,
    method: 'get'
  })
}

/**
 * 取消场景订单：POST /admin-api/order/scene/cancel
 *
 * 入参对齐后端 OrderCancelDTO（@RequestBody），状态机流转：0→5 或 6→5。
 */
export function cancelOrderScene(data: {
  orderCode: string
  cancelReason: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/order/scene/cancel',
    method: 'post',
    data
  })
}

// TODO 第一版暂不开放的生命周期端点（后端已实现，按需放开注释）：
// - payCallback（0→1 已支付）：POST /admin-api/order/scene/pay-callback
// - complete（3→4 已完成）：POST /admin-api/order/scene/complete
// - applyRefund（1/2/3→6 退款中）：POST /admin-api/order/scene/apply-refund

// ==================== 课程订单 ====================

/** 课程订单分页：GET /admin-api/order/course/page */
export function pageOrderCourses(query: OrderCourseQuery): Promise<PageResult<OrderCourse>> {
  return request<PageResult<OrderCourse>>({
    url: '/admin-api/order/course/page',
    method: 'get',
    params: query
  })
}

/** 课程订单详情：GET /admin-api/order/course/{orderCode} */
export function getOrderCourse(orderCode: string): Promise<OrderCourse> {
  return request<OrderCourse>({
    url: `/admin-api/order/course/${orderCode}`,
    method: 'get'
  })
}

/**
 * 取消课程订单：POST /admin-api/order/course/cancel
 *
 * 入参对齐后端 OrderCancelDTO（@RequestBody），状态机流转：0→5 或 6→5。
 */
export function cancelOrderCourse(data: {
  orderCode: string
  cancelReason: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/order/course/cancel',
    method: 'post',
    data
  })
}

// TODO 第一版暂不开放的生命周期端点（后端已实现，按需放开注释）：
// - payCallback（0→1 已支付）：POST /admin-api/order/course/pay-callback
// - complete（3→4 已完成）：POST /admin-api/order/course/complete
// - applyRefund（1/2/3→6 退款中）：POST /admin-api/order/course/apply-refund

// ==================== 旅游短居订单 ====================

/** 旅游短居订单分页：GET /admin-api/order/sojourn/page */
export function pageOrderSojourns(query: OrderSojournQuery): Promise<PageResult<OrderSojourn>> {
  return request<PageResult<OrderSojourn>>({
    url: '/admin-api/order/sojourn/page',
    method: 'get',
    params: query
  })
}

/** 旅游短居订单详情：GET /admin-api/order/sojourn/{orderCode} */
export function getOrderSojourn(orderCode: string): Promise<OrderSojourn> {
  return request<OrderSojourn>({
    url: `/admin-api/order/sojourn/${orderCode}`,
    method: 'get'
  })
}

/**
 * 取消旅游短居订单：POST /admin-api/order/sojourn/cancel
 *
 * 入参对齐后端 OrderCancelDTO（@RequestBody），状态机流转：0→5 或 6→5。
 */
export function cancelOrderSojourn(data: {
  orderCode: string
  cancelReason: string
}): Promise<void> {
  return request<void>({
    url: '/admin-api/order/sojourn/cancel',
    method: 'post',
    data
  })
}

// TODO 第一版暂不开放的生命周期端点（后端已实现，按需放开注释）：
// - payCallback（0→1 已支付）：POST /admin-api/order/sojourn/pay-callback
// - complete（3→4 已完成，离店）：POST /admin-api/order/sojourn/complete
// - applyRefund（1/2/3→6 退款中）：POST /admin-api/order/sojourn/apply-refund
