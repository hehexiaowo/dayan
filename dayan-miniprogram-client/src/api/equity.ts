/**
 * 权益相关 API（"我的权益"入口）。
 * 后端业务接口未实现，调用方 try/catch 降级。
 */
import request from '@/utils/request';
import type { Equity, PageResult, PageQuery } from '@/types';

export interface EquityQuery extends PageQuery {
  /** 状态筛选 */
  status?: number;
}

/** 我的权益列表（分页） */
export function getEquities(query: EquityQuery = {}): Promise<PageResult<Equity>> {
  return request<PageResult<Equity>>({ url: '/equities', method: 'GET', data: query });
}
