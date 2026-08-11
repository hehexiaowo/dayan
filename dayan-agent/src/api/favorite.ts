import request from '@/utils/request';

/** 收藏对象类型常量 */
export const TARGET_TYPE = {
  PARK: 1,
  SCENE: 2,
  COURSE: 3,
  CONTENT: 4,
} as const;

/** 新增收藏（POST /agent-api/favorites） */
export function addFavoriteApi(targetType: number, targetCode: string): Promise<number> {
  return request<number>({
    url: '/favorites',
    method: 'POST',
    data: { targetType, targetCode },
  });
}

/** 取消收藏（POST /agent-api/favorites/cancel） */
export function removeFavoriteApi(targetType: number, targetCode: string): Promise<void> {
  return request<void>({
    url: '/favorites/cancel',
    method: 'POST',
    data: { targetType, targetCode },
  });
}

/** 查询当前代理人某类型下已收藏的 targetCode 列表（GET /agent-api/favorites/codes） */
export function getFavoritedCodesApi(targetType: number): Promise<string[]> {
  return request<string[]>({
    url: '/favorites/codes',
    method: 'GET',
    data: { targetType },
  });
}
