/**
 * 服务会话相关 API。
 * 后端业务接口未实现，调用方 try/catch 降级。
 */
import request from '@/utils/request';
import type { ServiceSession, PageResult, PageQuery } from '@/types';

export interface ServiceQuery extends PageQuery {
  /** 状态筛选 */
  status?: number;
}

/** 我的服务会话列表（分页） */
export function getServices(query: ServiceQuery = {}): Promise<PageResult<ServiceSession>> {
  return request<PageResult<ServiceSession>>({ url: '/services', method: 'GET', data: query });
}
