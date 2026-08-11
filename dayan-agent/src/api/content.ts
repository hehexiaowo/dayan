import request from '@/utils/request';
import type { ContentArticle, PageQuery, PageResult } from '@/types';

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

export interface ContentQuery extends PageQuery {
  title?: string;
  contentType?: number;
}
