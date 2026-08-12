/**
 * 机构查询相关类型（与后端 park 模块 VO 对齐）。
 */

/** 分类标识 */
export type ParkCategory = 'vital' | 'care' | 'sojourn';

/** 网络标签 → 中文名 + 主题色映射 */
export const NETWORK_TAG_LABELS: Record<string, { label: string; color: string }> = {
  vital: { label: '活力长居', color: 'blue' },
  care: { label: '照护长居', color: 'orange' },
  sojourn: { label: '旅居养老', color: 'green' },
};

/** 将逗号分隔的网络标签字符串解析为标签数组 */
export function parseNetworkTags(raw?: string): Array<{ label: string; color: string }> {
  if (!raw) return [];
  return raw
    .split(',')
    .map((t) => t.trim())
    .filter((t) => NETWORK_TAG_LABELS[t])
    .map((t) => NETWORK_TAG_LABELS[t]);
}

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
  /** 网络标签（逗号分隔：vital/care/sojourn） */
  networkTags?: string;
  /** 列表缩略图 key（从对应网络 config JSON 提取） */
  thumbnailUrl?: string;
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
  /** 网络标签（vital/care/sojourn 多选） */
  networkTags?: string[];
  /** 活力长居展示配置JSON（{banners:[], thumbnail:""}） */
  vitalConfig?: string;
  /** 照护长居展示配置JSON */
  careConfig?: string;
  /** 旅居展示配置JSON */
  sojournConfig?: string;
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

// ===== 详情页子实体类型（与后端 VO 字段对齐） =====

/** 媒体素材 */
export interface ParkAsset {
  id: number;
  parkCode?: string;
  assetType?: number; // 1=图 2=视频 3=文件 4=VR
  assetUrl?: string;
  assetName?: string;
  isCover?: number;
  coverUrl?: string;
  thumbnailUrl?: string;
  sortOrder?: number;
}

/** 房型 */
export interface ParkRoomType {
  id: number;
  roomTypeName?: string;
  area?: number;
  orientation?: string;
  bedCount?: number;
  totalRooms?: number;
  availableRooms?: number;
  hasBathroom?: number;
  hasKitchen?: number;
  hasBalcony?: number;
  hasTv?: number;
  hasAircon?: number;
  hasFridge?: number;
  hasWasher?: number;
  hasWifi?: number;
  facilities?: string;
  description?: string;
  coverImage?: string;
  images?: string;
}

/** 收费方案 */
export interface ParkPricing {
  id: number;
  planName?: string;
  chargeType?: number; // 1=房间费 2=照护费 3=餐费 4=押金 5=设施费 6=服务费 9=其他
  refName?: string;
  billingCycle?: number; // 1=月 2=季 3=半年 4=年 5=一次性
  priceUnit?: string;
  originalPrice?: number;
  salePrice?: number;
  discountRate?: number;
  includesItems?: string;
  isCurrent?: number;
  isPromotion?: number;
  promotionDescription?: string;
}

/** 照护等级 */
export interface ParkCareType {
  id: number;
  careTypeName?: string;
  careLevel?: number;
  careTarget?: string;
  careItems?: string;
  careFrequency?: string;
  nursePatientRatio?: string;
  description?: string;
}

/** 餐饮类型 */
export interface ParkFoodType {
  id: number;
  foodTypeName?: string;
  mealPlan?: number;
  dietFeatures?: string;
  sampleMenu?: string;
  description?: string;
  coverImage?: string;
}

/** 设施类型 */
export interface ParkFacilityType {
  id: number;
  facilityTypeName?: string;
  facilityTypeCategory?: number;
  buildingName?: string;
  openTime?: string;
  facilityTypeDescription?: string;
  coverImage?: string;
}

/** 服务类型 */
export interface ParkServiceType {
  id: number;
  serviceTypeName?: string;
  serviceTypeCategory?: number;
  serviceTypeDescription?: string;
  serviceTypeFrequency?: string;
  coverImage?: string;
}

/** 周边配套 */
export interface ParkPeriphery {
  id: number;
  peripheryType?: number; // 分类：交通/景点/医疗/购物等
  placeName?: string;
  placeAddress?: string;
  distance?: string;
  detailDescription?: string;
}

/** 评分 */
export interface ParkScore {
  id: number;
  scoreTotal?: number;
  scoreEnvironment?: number;
  scoreRecreation?: number;
  scoreNursing?: number;
  scoreFood?: number;
  scoreService?: number;
  scorePrice?: number;
  scoreDescription?: string;
}

/** 图文展示板块 */
export interface ParkDisplayBlock {
  id: number;
  blockType?: string;
  blockTitle?: string;
  content?: string;
  images?: string;
}

/** 机构完整详情（主表 + 全部子实体聚合） */
export interface ParkFullDetail {
  parkInfo: ParkDetail;
  assets?: ParkAsset[];
  roomTypes?: ParkRoomType[];
  pricingList?: ParkPricing[];
  careTypes?: ParkCareType[];
  foodTypes?: ParkFoodType[];
  facilityTypes?: ParkFacilityType[];
  serviceTypes?: ParkServiceType[];
  peripheries?: ParkPeriphery[];
  score?: ParkScore | null;
  displayBlocks?: ParkDisplayBlock[];
}
