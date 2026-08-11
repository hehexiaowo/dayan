import request from '@/utils/request';

/** 营销海报模板 */
export interface PosterTemplate {
  id: string;
  templateCode: string;
  title: string;
  subtitle: string;
  bodyText: string;
  coverImage: string;
  categoryCode: string;
  categoryName: string;
  sortOrder: number;
  createdAt: string;
}

/** 获取海报模板列表 */
export function getPosterTemplates(category?: string): Promise<PosterTemplate[]> {
  return request<PosterTemplate[]>({
    url: '/poster-templates',
    method: 'GET',
    data: category ? { category } : {},
  });
}

/** 获取海报模板详情 */
export function getPosterDetail(templateCode: string): Promise<PosterTemplate> {
  return request<PosterTemplate>({
    url: '/poster-templates/' + templateCode,
    method: 'GET',
  });
}

/** 分类选项 */
export interface PosterCategory {
  code: string;
  name: string;
}

export const POSTER_CATEGORIES: PosterCategory[] = [
  { code: '', name: '全部' },
  { code: 'festival', name: '节日营销' },
  { code: 'knowledge', name: '养老知识' },
  { code: 'product', name: '产品推广' },
];
