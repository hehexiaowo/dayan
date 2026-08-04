/**
 * 订单相关 API（"我的订单"入口）。
 * 后端业务接口未实现，调用方 try/catch 降级。
 */
import request from '@/utils/request';
import type { Order, PageResult, PageQuery } from '@/types';

export interface OrderQuery extends PageQuery {
  /** 状态筛选 */
  status?: number;
}

/** 我的订单列表（分页） */
export function getOrders(query: OrderQuery = {}): Promise<PageResult<Order>> {
  return request<PageResult<Order>>({ url: '/orders', method: 'GET', data: query });
}
