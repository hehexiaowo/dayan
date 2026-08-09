/**
 * 权益相关 API（client 端，持卡人视角）。
 * 对接后端 ClientEquityController（/client-api/equity/*）。
 */
import request from '@/utils/request';
import type { Equity, EquityUsePerson, ClientServiceItem, ServiceRequestDTO, PageResult, PageQuery } from '@/types';

export interface EquityQuery extends PageQuery {
  /** 权益状态筛选 */
  equityStatus?: number;
}

/** 我的权益列表（分页） */
export function getEquities(query: EquityQuery = {}): Promise<PageResult<Equity>> {
  return request<PageResult<Equity>>({ url: '/equity/my', method: 'GET', data: query });
}

/** 权益详情 */
export function getEquityDetail(equityCode: string): Promise<Equity> {
  return request<Equity>({ url: `/equity/${equityCode}`, method: 'GET' });
}

/** 权益下的使用人列表 */
export function getUsePersons(equityCode: string): Promise<EquityUsePerson[]> {
  return request<EquityUsePerson[]>({ url: `/equity/${equityCode}/use-persons`, method: 'GET' });
}

/** 权益可用服务项目 + 配额剩余 */
export function getServiceItems(equityCode: string): Promise<ClientServiceItem[]> {
  return request<ClientServiceItem[]>({ url: `/equity/${equityCode}/service-items`, method: 'GET' });
}

/** 发起服务请求（创建 service_session，返回 sessionCode） */
export function createServiceRequest(dto: ServiceRequestDTO): Promise<string> {
  return request<string>({ url: '/equity/service-request', method: 'POST', data: dto });
}
