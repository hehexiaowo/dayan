import { request } from '@/utils/request';
import type { LearningContent } from '@/types';

/**
 * 学习中心内容列表（按分类）。
 * @param category 1=视频课程 2=图文课程 3=雁鸣中国，不传 = 全部
 */
export function getLearningContents(category?: number) {
  const url = '/learning/contents' + (category != null ? `?category=${category}` : '');
  return request<LearningContent[]>({ url, method: 'GET' });
}

/**
 * 学习中心内容详情（同时累加浏览量）。
 */
export function getLearningDetail(contentCode: string) {
  return request<LearningContent>({ url: `/learning/contents/${contentCode}`, method: 'GET' });
}
