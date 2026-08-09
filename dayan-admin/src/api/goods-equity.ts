import { request } from '@/utils/request'
import type { GoodsEquity, GoodsEquitySaveDTO } from '@/types/goods-equity'

const BASE = '/admin-api/goods/equity-config'

/** 获取商品权益配置（含关联服务项目列表） */
export function getGoodsEquity(goodsCode: string) {
  return request<GoodsEquity>({ url: `${BASE}/${goodsCode}`, method: 'get' })
}

/** 保存权益配置（新建或更新，rel 先删后插） */
export function saveGoodsEquity(data: GoodsEquitySaveDTO) {
  return request<void>({ url: BASE, method: 'post', data })
}

/** 删除权益配置 */
export function deleteGoodsEquity(goodsCode: string) {
  return request<void>({ url: `${BASE}/${goodsCode}`, method: 'delete' })
}
