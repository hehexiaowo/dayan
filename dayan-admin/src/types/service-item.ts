import type { PageQuery, PageResult } from '@/types/common'

/** 项目大类 */
export enum ItemCategory {
  ARRANGEMENT = 1, // 安排权益
  COST = 2,        // 费用权益
}

export const ITEM_CATEGORY_OPTIONS = [
  { label: '安排权益', value: ItemCategory.ARRANGEMENT, tagType: 'warning' as const },
  { label: '费用权益', value: ItemCategory.COST, tagType: 'success' as const },
]

/** 安排权益子类 */
export enum ItemSubtype {
  SOJOURN = 1,         // 旅居
  VITAL_LONG_STAY = 2, // 活力长居
  CARE_LONG_STAY = 3,  // 照护长居
}

export const ITEM_SUBTYPE_OPTIONS = [
  { label: '旅居', value: ItemSubtype.SOJOURN },
  { label: '活力长居', value: ItemSubtype.VITAL_LONG_STAY },
  { label: '照护长居', value: ItemSubtype.CARE_LONG_STAY },
]

export interface ServiceItem {
  id?: number
  itemCode?: string
  itemName: string
  itemCategory: number
  itemSubtype?: number
  itemValue?: number
  costBearing?: number
  serviceNetwork?: string
  coveredItems?: string
  validDays?: number
  maxUseCount?: number
  description?: string
  sortOrder?: number
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface ServiceItemQuery extends PageQuery {
  itemCode?: string
  itemName?: string
  itemCategory?: number
  itemSubtype?: number
  status?: number
}

export type ServiceItemPageResult = PageResult<ServiceItem>
