import request from '@/utils/request';
import type { BusinessCard, PageQuery, PageResult } from '@/types';

/**
 * 名片查询参数。
 */
export interface CardQuery extends PageQuery {
  status?: number;
}

/**
 * 名片新增参数。
 */
export interface CardCreateData {
  cardName: string;
  displayName: string;
  title?: string;
  phone: string;
  wechat?: string;
  email?: string;
  company?: string;
  address?: string;
  avatar?: string;
  intro?: string;
  tags?: string;
}

/**
 * 名片更新参数（选择性更新）。
 */
export interface CardUpdateData extends Partial<CardCreateData> {
  sortOrder?: number;
  status?: number;
}

/**
 * 分页查询我的名片（GET /agent-api/agent-cards）。
 */
export function getCards(query?: CardQuery): Promise<PageResult<BusinessCard>> {
  return request<PageResult<BusinessCard>>({
    url: '/agent-cards',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}

/**
 * 名片详情（GET /agent-api/agent-cards/{id}）。
 */
export function getCardDetail(id: string): Promise<BusinessCard> {
  return request<BusinessCard>({
    url: `/agent-cards/${id}`,
    method: 'GET',
  });
}

/**
 * 新增名片（POST /agent-api/agent-cards）。
 */
export function createCard(data: CardCreateData): Promise<number> {
  return request<number>({
    url: '/agent-cards',
    method: 'POST',
    data,
  });
}

/**
 * 更新名片（PUT /agent-api/agent-cards/{id}）。
 */
export function updateCard(id: string, data: CardUpdateData): Promise<void> {
  return request<void>({
    url: `/agent-cards/${id}`,
    method: 'PUT',
    data,
  });
}

/**
 * 删除名片（DELETE /agent-api/agent-cards/{id}）。
 */
export function deleteCard(id: string): Promise<void> {
  return request<void>({
    url: `/agent-cards/${id}`,
    method: 'DELETE',
  });
}
