import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { Order, OrderQuery } from '@/types/order'

/**
 * 订单接口封装。
 *
 * 对应后端 Channel 端 OrderController（/channel-api/orders/*）。
 * 注意：本期后端业务端点尚未实现，调用会走 request.ts 响应拦截器报错，
 * 调用方需 try/catch 降级处理。
 */

/** 订单分页：GET /channel-api/orders */
export function pageOrders(query: OrderQuery): Promise<PageResult<Order>> {
  return request<PageResult<Order>>({
    url: '/channel-api/orders',
    method: 'get',
    params: query
  })
}
