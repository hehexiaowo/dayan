/**
 * 权益商品配置类型（goods_equity 1:1 + goods_service_item_rel N:M）。
 */

/** 服务项目关联子项（联查 service_item 信息） */
export interface ServiceItemRel {
  id?: number
  goodsCode?: string
  itemCode: string
  itemName?: string
  itemCategory?: number
  itemSubtype?: number
  quantity: number
  sortOrder?: number
  createdAt?: string
}

/** 权益商品配置 VO */
export interface GoodsEquity {
  id?: number
  goodsCode: string
  personCount: number
  validDays: number
  shelfLifeDays: number
  maxTransferable: number
  description?: string
  sortOrder?: number
  status?: number
  createdAt?: string
  updatedAt?: string
  serviceItems?: ServiceItemRel[]
}

/** 权益配置保存 DTO（1:1 配置 + rel 子表先删后插） */
export interface GoodsEquitySaveDTO {
  goodsCode: string
  personCount: number
  validDays: number
  shelfLifeDays: number
  maxTransferable: number
  description?: string
  sortOrder?: number
  status?: number
  serviceItems: { itemCode: string; quantity: number; sortOrder?: number }[]
}
