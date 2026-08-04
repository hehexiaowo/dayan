/**
 * 首页相关 API（banners / recommend）。
 * 后端业务接口未实现，调用方 try/catch 降级。
 */
import request from '@/utils/request';
import type { Banner, Park } from '@/types';

/** 首页轮播 */
export function getBanners(): Promise<Banner[]> {
  return request<Banner[]>({ url: '/banners', method: 'GET' });
}

/** 推荐机构/内容 */
export function getRecommend(): Promise<Park[]> {
  return request<Park[]>({ url: '/recommend', method: 'GET' });
}
