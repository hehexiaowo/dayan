/**
 * 服务会话相关 API（client 端，持卡人视角）。
 * 对接后端 ClientServiceSessionController（/client-api/service-sessions/*）。
 */
import request from '@/utils/request';
import type { ServiceSession, PageResult, PageQuery, Timeline, EvaluationCreate } from '@/types';

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

// ---- 服务跟进（时间线/状态机操作/评价） ----

/** 服务进度时间线（需求/方案/安排/探访） */
export function getTimeline(sessionCode: string): Promise<Timeline> {
  return request<Timeline>({ url: `/service-sessions/${sessionCode}/timeline`, method: 'GET' });
}

/** 确认方案（3→4） */
export function confirmSolution(sessionCode: string): Promise<void> {
  return request<void>({ url: `/service-sessions/${sessionCode}/confirm-solution`, method: 'POST' });
}

/** 驳回方案（3→2） */
export function rejectSolution(sessionCode: string): Promise<void> {
  return request<void>({ url: `/service-sessions/${sessionCode}/reject`, method: 'POST' });
}

/** 取消服务（1/2/5→7） */
export function cancelSession(sessionCode: string, reason?: string): Promise<void> {
  return request<void>({ url: `/service-sessions/${sessionCode}/cancel`, method: 'POST', data: { closeReason: reason } });
}

/** 评价服务（仅已完成可评价） */
export function evaluateSession(sessionCode: string, dto: EvaluationCreate): Promise<void> {
  return request<void>({ url: `/service-sessions/${sessionCode}/evaluate`, method: 'POST', data: dto });
}
