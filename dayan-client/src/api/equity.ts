/**
 * 权益相关 API（client 端，持卡人视角）。
 * 对接后端 ClientEquityController（/client-api/equity/*）。
 */
import request from '@/utils/request';
import type {
  Equity, EquityUsePerson, EquityUsePersonCreate, EquityUsePersonUpdate,
  ClientServiceItem, ServiceRequestDTO, PageResult, PageQuery,
} from '@/types';

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

// ---- 权益激活 + 使用人管理 ----

/** 激活权益（输入卡面激活码，clientCode 走登录态） */
export function activateEquity(activateCode: string): Promise<Equity> {
  return request<Equity>({ url: '/equity/activate', method: 'POST', data: { activateCode } });
}

/** 新增使用人（返回新建 id） */
export function createUsePerson(dto: EquityUsePersonCreate): Promise<number> {
  return request<number>({ url: '/equity/use-persons', method: 'POST', data: dto });
}

/** 修改使用人 */
export function updateUsePerson(id: string, dto: EquityUsePersonUpdate): Promise<void> {
  return request<void>({ url: `/equity/use-persons/${id}`, method: 'PUT', data: dto });
}

/** 删除使用人 */
export function deleteUsePerson(id: string): Promise<void> {
  return request<void>({ url: `/equity/use-persons/${id}`, method: 'DELETE' });
}

/** 设为默认权益人 */
export function setDefaultUsePerson(id: string): Promise<void> {
  return request<void>({ url: `/equity/use-persons/${id}/default`, method: 'PUT' });
}

/** 常用权益人（跨权益去重，新增/激活时复用预填） */
export function suggestUsePersons(): Promise<EquityUsePerson[]> {
  return request<EquityUsePerson[]>({ url: '/equity/use-persons/suggest', method: 'GET' });
}
