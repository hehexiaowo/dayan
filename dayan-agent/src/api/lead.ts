import request from '@/utils/request';
import type { Lead, LeadPoolItem, LeadTrace, PageQuery, PageResult } from '@/types';

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

/**
 * 线索互动记录（GET /agent-api/leads/{leadId}/traces）。
 * 返回访客浏览内容、使用工具、查看海报的时间线。
 */
export function getLeadTraces(leadId: string): Promise<LeadTrace[]> {
  return request<LeadTrace[]>({
    url: `/leads/${leadId}/traces`,
    method: 'GET',
  });
}

/**
 * 线索池查询参数。
 */
export interface LeadPoolQuery extends PageQuery {
  keyword?: string;
  /** 仅看已留资（有手机号） */
  onlyWithPhone?: boolean;
}

/**
 * 线索池分页（GET /agent-api/leads/pool）。
 * 本渠道内尚未被任何代理人认领的访客线索。
 */
export function getLeadPool(query?: LeadPoolQuery): Promise<PageResult<LeadPoolItem>> {
  return request<PageResult<LeadPoolItem>>({
    url: '/leads/pool',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}

/**
 * 认领线索池线索（POST /agent-api/leads/claim/{visitorLeadCode}）。
 * 认领成功后该线索进入我的线索清单。
 */
export function claimLead(visitorLeadCode: string): Promise<number> {
  return request<number>({
    url: `/leads/claim/${visitorLeadCode}`,
    method: 'POST',
  });
}
