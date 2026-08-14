/**
 * 首页相关 API（banners / recommend）。
 * 对接 ClientContentController（/client-api/contents/*）。
 */
import request from '@/utils/request';
import type { Banner, ContentCard } from '@/types';

/**
 * 首页轮播：已发布+置顶内容（封面+标题+跳转）
 * GET /client-api/contents/banners?network=vital|care|sojourn
 */
export function getBanners(network?: string): Promise<Banner[]> {
  return request<Banner[]>({
    url: '/contents/banners',
    method: 'GET',
    data: network ? { network } : undefined,
  });
}

/**
 * 推荐内容：已发布+推荐
 * GET /client-api/contents/recommend?network=&limit=
 */
export function getRecommend(network?: string): Promise<ContentCard[]> {
  return request<ContentCard[]>({
    url: '/contents/recommend',
    method: 'GET',
    data: network ? { network } : undefined,
  });
}
