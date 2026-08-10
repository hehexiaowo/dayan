import request from '@/utils/request';
import type { Lead, PageQuery, PageResult } from '@/types';

/**
 * 线索查询参数。
 */
export interface LeadQuery extends PageQuery {
  keyword?: string;
  leadStatus?: number;
}

/**
 * 线索新增参数。
 */
export interface LeadCreateData {
  name?: string;
  phone?: string;
  gender?: number;
  age?: number;
  sourceType?: number;
  intentionLevel?: number;
  interestType?: string;
  region?: string;
  remark?: string;
}

/**
 * 线索更新参数。
 */
export interface LeadUpdateData {
  name?: string;
  phone?: string;
  gender?: number;
  age?: number;
  leadStatus?: number;
  intentionLevel?: number;
  interestType?: string;
  region?: string;
  remark?: string;
}

/**
 * 分页查询我的线索（GET /agent-api/leads）。
 */
export function getLeads(query?: LeadQuery): Promise<PageResult<Lead>> {
  return request<PageResult<Lead>>({
    url: '/leads',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}

/**
 * 线索详情（GET /agent-api/leads/{leadId}）。
 */
export function getLeadDetail(leadId: string): Promise<Lead> {
  return request<Lead>({
    url: `/leads/${leadId}`,
    method: 'GET',
  });
}

/**
 * 新增线索（POST /agent-api/leads）。
 */
export function createLead(data: LeadCreateData): Promise<number> {
  return request<number>({
    url: '/leads',
    method: 'POST',
    data,
  });
}

/**
 * 更新线索（PUT /agent-api/leads/{leadId}）。
 */
export function updateLead(leadId: string, data: LeadUpdateData): Promise<void> {
  return request<void>({
    url: `/leads/${leadId}`,
    method: 'PUT',
    data,
  });
}

/**
 * 删除线索（DELETE /agent-api/leads/{leadId}）。
 */
export function deleteLead(leadId: string): Promise<void> {
  return request<void>({
    url: `/leads/${leadId}`,
    method: 'DELETE',
  });
}
