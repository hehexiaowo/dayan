/**
 * 服务会话相关 API（client 端，持卡人视角）。
 * 对接后端 ClientServiceSessionController（/client-api/service-sessions/*）。
 */
import request from '@/utils/request';
import type { ServiceSession, PageResult, PageQuery } from '@/types';

export interface ServiceQuery extends PageQuery {
  /** 会话状态筛选（1-7，对齐后端 ServiceSessionEvent） */
  sessionStatus?: number;
}

/** 我的服务会话列表（分页） */
export function getServices(query: ServiceQuery = {}): Promise<PageResult<ServiceSession>> {
  return request<PageResult<ServiceSession>>({ url: '/service-sessions/my', method: 'GET', data: query });
}

/** 服务会话详情 */
export function getServiceDetail(sessionCode: string): Promise<ServiceSession> {
  return request<ServiceSession>({ url: `/service-sessions/${sessionCode}`, method: 'GET' });
}
