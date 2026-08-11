import request from '@/utils/request';
import type { GoodsProduct } from '@/types';

/**
 * 商城可购权益商品列表（GET /agent-api/goods-infos）。
 * 后端返回 List（非分页），已按渠道白名单 + 上架 + 权益类型过滤。
 */
export function getGoodsList(query?: { goodsName?: string }): Promise<GoodsProduct[]> {
  return request<GoodsProduct[]>({
    url: '/goods-infos',
    method: 'GET',
    data: (query || {}) as Record<string, any>,
  });
}

/**
 * 商品详情（GET /agent-api/goods-infos/{goodsCode}）。
 */
export function getGoodsDetail(goodsCode: string): Promise<GoodsProduct> {
  return request<GoodsProduct>({
    url: `/goods-infos/${goodsCode}`,
    method: 'GET',
  });
}
