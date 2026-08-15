import type { PageQuery, PageResult } from '@/types/common'
import type { NetworkScope } from '@/types/goods-equity'
import { networkScopeSummary } from '@/types/goods-equity'

export { networkScopeSummary }
export type { NetworkScope }

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
  SOJOURN = 1,         // 旅游短居
  VITAL_LONG_STAY = 2, // 活力长居
  CARE_LONG_STAY = 3,  // 照护长居
}

export const ITEM_SUBTYPE_OPTIONS = [
  { label: '旅游短居', value: ItemSubtype.SOJOURN },
  { label: '活力长居', value: ItemSubtype.VITAL_LONG_STAY },
  { label: '照护长居', value: ItemSubtype.CARE_LONG_STAY },
]

/** 配额周期 */
export enum QuotaType {
  LIFETIME = 1, // 权益期内总量
  ANNUAL = 2,   // 年度配额（按激活周年重置）
}

export const QUOTA_TYPE_OPTIONS = [
  { label: '每年（按激活周年）', value: QuotaType.ANNUAL, tagType: 'primary' as const },
  { label: '权益期内总量', value: QuotaType.LIFETIME, tagType: 'info' as const },
]

export interface ServiceItem {
  id?: number
  itemCode?: string
  itemName: string
  itemCategory: number
  itemSubtype?: number
  itemValue?: number
  costBearing?: number
  /** 服务网络范围（结构化；null=业态全部机构，custom=自选可精确到房型） */
  networkScope?: NetworkScope | null
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
