/**
 * 订单相关 API（"我的订单"入口，旅游短居订单）。
 */
import request from '@/utils/request';
import type { Order, PageResult, PageQuery } from '@/types';

export interface OrderQuery extends PageQuery {
  /** 状态分组：ALL（全部）/ PENDING（待支付）/ ACTIVE（进行中）/ DONE（已完成） */
  group?: string;
}

/** 我的旅游短居订单列表（分页） */
export function getOrders(query: OrderQuery = {}): Promise<PageResult<Order>> {
  return request<PageResult<Order>>({ url: '/orders', method: 'GET', data: query });
}
