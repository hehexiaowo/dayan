import request from '@/utils/request';
import type { ContentArticle, ContentCategoryOption, PageQuery, PageResult } from '@/types';

/**
 * 内容列表（GET /agent-api/contents）。
 * 返回渠道配置给 agent 端的已发布内容。
 */
export function getContentList(query?: ContentQuery): Promise<PageResult<ContentArticle>> {
  return request<PageResult<ContentArticle>>({
    url: '/contents',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}

/**
 * 内容详情（GET /agent-api/contents/{contentCode}）。
 */
export function getContentDetail(contentCode: string): Promise<ContentArticle> {
  return request<ContentArticle>({
    url: `/contents/${contentCode}`,
    method: 'GET',
  });
}

/**
 * 内容分类列表（GET /agent-api/contents/categories）。
 * 仅返回当前渠道已配置内容涉及的分类。
 */
export function getContentCategories(): Promise<ContentCategoryOption[]> {
  return request<ContentCategoryOption[]>({ url: '/contents/categories', method: 'GET' });
}

export interface ContentQuery extends PageQuery {
  title?: string;
  contentType?: number;
  categoryCode?: string;
}
