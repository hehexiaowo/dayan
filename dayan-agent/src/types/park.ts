/**
 * 机构查询相关类型（与后端 park 模块 VO 对齐）。
 */

/** 分类标识 */
export type ParkCategory = 'vital' | 'care' | 'sojourn';

/** 下钻层级 */
export type DrillLevel = 'province' | 'city' | 'district' | 'park';

/** 分类入口数量统计 */
export interface CategoryCount {
  category: ParkCategory;
  categoryName: string;
  count: number;
  available: boolean;
}

/** 下级区域项 */
export interface RegionItem {
  code: string;
  name: string;
  count: number;
}

/** 机构卡片精简信息 */
export interface ParkCard {
  parkCode: string;
  fullName: string;
  shortName?: string;
  address?: string;
  province?: string;
  provinceCode?: string;
  city?: string;
  cityCode?: string;
  district?: string;
  districtCode?: string;
  longitude?: number;
  latitude?: number;
  totalBeds?: number;
  availableBeds?: number;
  minPriceDisplay?: number;
  maxPriceDisplay?: number;
  priceUnit?: string;
  operateStatus?: number;
  abilityTypeDescription?: string;
}

/** 区域下钻结果 */
export interface RegionDrillResult {
  level: DrillLevel;
  breadcrumb: string;
  items?: RegionItem[];
  parkList?: ParkCard[];
  centerLng?: number;
  centerLat?: number;
}

/** 机构详情（完整字段，与后端 ParkInfoVO 对齐） */
export interface ParkDetail {
  id: number;
  parkCode: string;
  fullName: string;
  shortName?: string;
  brand?: string;
  brandLogo?: string;
  abilityType?: number;
  abilityTypeDescription?: string;
  natureType?: number;
  natureTypeDescription?: string;
  specialtyTag?: string;
  dayanLevel?: number;
  province?: string;
  provinceCode?: string;
  city?: string;
  cityCode?: string;
  district?: string;
  districtCode?: string;
  address?: string;
  longitude?: number;
  latitude?: number;
  totalBeds?: number;
  availableBeds?: number;
  occupancyRate?: number;
  staffCount?: number;
  nurseCount?: number;
  nursePatientRatio?: string;
  minPriceDisplay?: number;
  maxPriceDisplay?: number;
  priceUnit?: string;
  checkInAgeMin?: number;
  checkInAgeMax?: number;
  checkInDescription?: string;
  depositAmount?: number;
  contractPeriod?: string;
  baseDescription?: string;
  specialtyDescription?: string;
  brandIntroduction?: string;
  serviceHotline?: string;
  openingTime?: string;
  isHot?: number;
  operateStatus?: number;
  viewCount?: number;
  collectCount?: number;
}

/** 区域查询参数 */
export interface RegionQuery {
  category: ParkCategory;
  level: DrillLevel;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
}

/** 直辖市 provinceCode 列表（前端跳过 city 层级判断用） */
export const MUNICIPALITIES = ['110000', '120000', '310000', '500000'];
