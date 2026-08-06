/**
 * 养老机构相关类型。
 *
 * 字段对齐后端 com.dayan.park.vo.ParkInfoVO 及 ParkInfoQueryDTO / ParkInfoCreateDTO。
 * 机构运营状态（operateStatus）由状态机 PARK_SM 驱动，前端通过 transition 端点流转，
 * 不允许在 create/update 时直接指定。
 */
import type { PageQuery } from '@/types/common'

/**
 * 机构运营状态（operateStatus，PARK_SM 驱动）。
 *
 * 对齐后端 park_info.operate_status：0=待审核 / 1=已上线 / 2=已下架 / 3=暂停营业。
 */
export enum ParkOperateStatus {
  /** 待审核 */
  PENDING = 0,
  /** 已上线 */
  ONLINE = 1,
  /** 已下架 */
  OFFLINE = 2,
  /** 暂停营业 */
  SUSPENDED = 3
}

/** 机构运营状态选项 */
export const PARK_OPERATE_STATUS_OPTIONS = [
  { label: '待审核', value: ParkOperateStatus.PENDING },
  { label: '已上线', value: ParkOperateStatus.ONLINE },
  { label: '已下架', value: ParkOperateStatus.OFFLINE },
  { label: '暂停营业', value: ParkOperateStatus.SUSPENDED }
] as const

/** 大雁等级选项（dayanLevel 字段） */
export const DAYAN_LEVEL_OPTIONS = [
  { label: '普通', value: 0 },
  { label: '一级', value: 1 },
  { label: '二级', value: 2 },
  { label: '三级', value: 3 },
  { label: '五级', value: 5 }
] as const

/** 能力类型选项（abilityType 字段） */
export const ABILITY_TYPE_OPTIONS = [
  { label: '自理', value: 1 },
  { label: '半自理', value: 2 },
  { label: '不能自理', value: 3 },
  { label: '综合', value: 4 }
] as const

/** 性质类型选项（natureType 字段） */
export const NATURE_TYPE_OPTIONS = [
  { label: '公办', value: 1 },
  { label: '民办', value: 2 },
  { label: '公建民营', value: 3 },
  { label: '合资', value: 4 }
] as const

/**
 * 机构主信息实体（后端 ParkInfoVO）。
 *
 * VO 字段较多，此处取核心子集，其余字段均设为可选；后续按需补充。
 */
export interface ParkInfo {
  id?: number
  /** 机构编码（PK+5 位，系统生成，编辑时只读） */
  parkCode?: string
  /** 机构全称（必填） */
  fullName: string
  /** 机构简称 */
  shortName?: string
  /** 所属供应商编码（须存在且 status=2 已通过，create 时必填） */
  supplierCode?: string
  /** 品牌 */
  brand?: string
  /** 品牌介绍 */
  brandIntroduction?: string
  /** 品牌 logo URL */
  brandLogo?: string
  /** 能力类型：1自理/2半自理/3不能自理/4综合 */
  abilityType?: number
  /** 性质类型：1公办/2民办/3公建民营/4合资 */
  natureType?: number
  /** 特色标签 */
  specialtyTag?: string
  /** 大雁等级 */
  dayanLevel?: number
  /** 省编码 */
  provinceCode?: string
  /** 省名 */
  province?: string
  /** 市编码 */
  cityCode?: string
  /** 市名 */
  city?: string
  /** 区编码 */
  districtCode?: string
  /** 区名 */
  district?: string
  /** 详细地址 */
  address?: string
  /** 经度（字符串） */
  longitude?: string
  /** 纬度（字符串） */
  latitude?: string
  /** 服务热线 */
  serviceHotline?: string
  /** 基地简介 */
  baseDescription?: string
  /** 特色简介 */
  specialtyDescription?: string
  /** 占地面积 */
  totalArea?: string
  /** 建筑面积 */
  buildingArea?: string
  /** 总床位数 */
  totalBeds?: number
  /** 可用床位数 */
  availableBeds?: number
  /** 员工总数 */
  staffCount?: number
  /** 护理员数量 */
  nurseCount?: number
  /** 机构运营状态（PARK_SM 驱动，不可直接修改） */
  operateStatus?: ParkOperateStatus
  /** 是否已发布：1=已发布 / 0=未发布 */
  isPublished?: number
  /** 排序号 */
  sortOrder?: number
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createdAt?: string
  /** 更新时间（VO 当前未回填，预留） */
  updatedAt?: string
}

/**
 * 机构分页查询参数（对齐后端 ParkInfoQueryDTO）。
 */
export interface ParkInfoQuery extends PageQuery {
  parkCode?: string
  fullName?: string
  supplierCode?: string
  /** 城市编码（精确匹配） */
  cityCode?: string
  abilityType?: number
  natureType?: number
  dayanLevel?: number
  operateStatus?: ParkOperateStatus
  isPublished?: number
  isHot?: number
}

// ============================================================================
// 房型（ParkRoomType / ParkRoomPrice）
// ============================================================================

/**
 * 机构房型实体（后端 ParkRoomTypeVO）。
 *
 * 外键 parkCode 关联机构主表；roomTypeCode 为业务编码（录入方提供，非系统生成）。
 */
export interface ParkRoomType {
  id?: number
  parkCode?: string
  /** 房型编码（必填，业务语义编码，同 parkCode 下唯一） */
  roomTypeCode: string
  /** 房型名称（必填） */
  roomTypeName: string
  /** 居住类型 */
  stayType?: number
  /** 楼栋名称 */
  buildingName?: string
  /** 楼层 */
  floor?: string
  /** 房型分类 */
  roomCategory?: number
  /** 面积（㎡） */
  area?: number
  /** 朝向 */
  orientation?: string
  /** 床位数 */
  bedCount?: number
  /** 总房间数 */
  totalRooms?: number
  /** 可用房间数 */
  availableRooms?: number
  /** 独立卫生间 */
  hasBathroom?: number
  /** 厨房 */
  hasKitchen?: number
  /** 阳台 */
  hasBalcony?: number
  /** 电视 */
  hasTv?: number
  /** 空调 */
  hasAircon?: number
  /** 冰箱 */
  hasFridge?: number
  /** 洗衣机 */
  hasWasher?: number
  /** WiFi */
  hasWifi?: number
  /** 紧急呼叫 */
  hasEmergency?: number
  /** 监控 */
  hasMonitor?: number
  /** 设施（JSON 字符串原文） */
  facilities?: string
  /** 描述 */
  description?: string
  /** 封面图 URL */
  coverImage?: string
  /** 图片列表（JSON 字符串原文） */
  images?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停售 */
  status?: number
  /** 设计说明 */
  designDescription?: string
  /** 设计图 URL */
  designImage?: string
  /** 附加图片（JSON 字符串原文） */
  additionalImages?: string
  /** 创建时间 */
  createdAt?: string
}

/** 房型分页查询参数 */
export interface ParkRoomTypeQuery extends PageQuery {
  parkCode?: string
  roomTypeCode?: string
  roomTypeName?: string
  roomCategory?: number
  stayType?: number
  status?: number
}

/**
 * 机构房型价格实体（后端 ParkRoomPriceVO）。
 *
 * 外键 parkCode + roomTypeCode；展开行场景用 /list 按 (parkCode, roomTypeCode) 加载。
 */
export interface ParkRoomPrice {
  id?: number
  parkCode?: string
  /** 房型编码（外键，从展开行上下文带入） */
  roomTypeCode: string
  /** 价格类型：1月/2季/3半年/4年/5押金 */
  priceType?: number
  /** 原价 */
  originalPrice?: number
  /** 售价（业务必填） */
  salePrice?: number
  /** 折扣率 */
  discountRate?: number
  /** 价格说明 */
  priceDescription?: string
  /** 包含项目（JSON 字符串原文，房型独有） */
  includesItems?: string
  /** 生效日期（业务必填） */
  effectiveDate?: string
  /** 失效日期 */
  expireDate?: string
  /** 是否当前价：1是 / 0否 */
  isCurrent?: number
  /** 是否促销：1是 / 0否 */
  isPromotion?: number
  /** 促销说明 */
  promotionDescription?: string
  /** 价格变更原因（房型独有） */
  priceChangeReason?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停售 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 房型价格分页查询参数 */
export interface ParkRoomPriceQuery extends PageQuery {
  parkCode?: string
  roomTypeCode?: string
  priceType?: number
  isCurrent?: number
  status?: number
}

// ============================================================================
// 照护（ParkCareType / ParkCarePrice）
// ============================================================================

/**
 * 机构照护类型实体（后端 ParkCareTypeVO）。
 *
 * 外键 parkCode；careTypeCode 为业务编码（录入方提供）。
 */
export interface ParkCareType {
  id?: number
  parkCode?: string
  /** 照护编码（必填，业务语义编码，同 parkCode 下唯一） */
  careTypeCode: string
  /** 照护名称（必填） */
  careTypeName: string
  /** 照护等级 1-5 */
  careLevel?: number
  /** 照护对象 */
  careTarget?: string
  /** 照护项目（JSON 字符串原文） */
  careItems?: string
  /** 照护频次 */
  careFrequency?: string
  /** 护患比 */
  nursePatientRatio?: string
  /** 评估标准 */
  assessmentCriteria?: string
  /** 描述 */
  description?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停售 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 照护类型分页查询参数 */
export interface ParkCareTypeQuery extends PageQuery {
  parkCode?: string
  careTypeCode?: string
  careTypeName?: string
  careLevel?: number
  status?: number
}

/**
 * 机构照护价格实体（后端 ParkCarePriceVO）。
 *
 * 与 ParkRoomPrice 字段集相同，但外键是 careTypeCode，且无 includesItems / priceChangeReason。
 */
export interface ParkCarePrice {
  id?: number
  parkCode?: string
  /** 照护编码（外键，从展开行上下文带入） */
  careTypeCode: string
  /** 价格类型：1月/2季/3半年/4年 */
  priceType?: number
  originalPrice?: number
  /** 售价（业务必填） */
  salePrice?: number
  discountRate?: number
  priceDescription?: string
  /** 生效日期（业务必填） */
  effectiveDate?: string
  expireDate?: string
  /** 是否当前价：1是 / 0否 */
  isCurrent?: number
  /** 是否促销：1是 / 0否 */
  isPromotion?: number
  promotionDescription?: string
  sortOrder?: number
  /** 状态：1启用 / 0停售 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 照护价格分页查询参数 */
export interface ParkCarePriceQuery extends PageQuery {
  parkCode?: string
  careTypeCode?: string
  priceType?: number
  isCurrent?: number
  status?: number
}

// ============================================================================
// 餐饮（ParkFoodType / ParkFoodPrice）
// ============================================================================

/**
 * 机构餐饮类型实体（后端 ParkFoodTypeVO）。
 *
 * 外键 parkCode；foodTypeCode 为业务编码（录入方提供）。
 */
export interface ParkFoodType {
  id?: number
  parkCode?: string
  /** 餐饮编码（必填，业务语义编码，同 parkCode 下唯一） */
  foodTypeCode: string
  /** 餐饮名称（必填） */
  foodTypeName: string
  /** 餐食方案：1/2/3/4 */
  mealPlan?: number
  /** 饮食特色 */
  dietFeatures?: string
  /** 样板菜单（JSON 字符串原文） */
  sampleMenu?: string
  /** 是否特殊饮食：0否 / 1是 */
  specialDiet?: number
  /** 特殊饮食说明 */
  specialDietDescription?: string
  /** 描述 */
  description?: string
  /** 封面图 URL */
  coverImage?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停售 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 餐饮类型分页查询参数 */
export interface ParkFoodTypeQuery extends PageQuery {
  parkCode?: string
  foodTypeCode?: string
  foodTypeName?: string
  mealPlan?: number
  status?: number
}

/**
 * 机构餐饮价格实体（后端 ParkFoodPriceVO）。
 *
 * 与 ParkCarePrice 完全一致，仅外键换为 foodTypeCode。
 */
export interface ParkFoodPrice {
  id?: number
  parkCode?: string
  /** 餐饮编码（外键，从展开行上下文带入） */
  foodTypeCode: string
  /** 价格类型：1月/2季/3半年/4年 */
  priceType?: number
  originalPrice?: number
  /** 售价（业务必填） */
  salePrice?: number
  discountRate?: number
  priceDescription?: string
  /** 生效日期（业务必填） */
  effectiveDate?: string
  expireDate?: string
  /** 是否当前价：1是 / 0否 */
  isCurrent?: number
  /** 是否促销：1是 / 0否 */
  isPromotion?: number
  promotionDescription?: string
  sortOrder?: number
  /** 状态：1启用 / 0停售 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 餐饮价格分页查询参数 */
export interface ParkFoodPriceQuery extends PageQuery {
  parkCode?: string
  foodTypeCode?: string
  priceType?: number
  isCurrent?: number
  status?: number
}

// ============================================================================
// 通用字典选项（房型/照护/餐饮枚举，供 el-select 使用）
// ============================================================================

/** 房型价格类型选项（priceType）：含 5押金（房型独有） */
export const ROOM_PRICE_TYPE_OPTIONS = [
  { label: '月', value: 1 },
  { label: '季', value: 2 },
  { label: '半年', value: 3 },
  { label: '年', value: 4 },
  { label: '押金', value: 5 }
] as const

/** 照护/餐饮价格类型选项（priceType）：不含押金 */
export const CARE_FOOD_PRICE_TYPE_OPTIONS = [
  { label: '月', value: 1 },
  { label: '季', value: 2 },
  { label: '半年', value: 3 },
  { label: '年', value: 4 }
] as const

/** 价格类型 label 映射（房型场景，含押金） */
export function roomPriceTypeLabel(v?: number): string {
  const found = ROOM_PRICE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 价格类型 label 映射（照护/餐饮场景） */
export function careFoodPriceTypeLabel(v?: number): string {
  const found = CARE_FOOD_PRICE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}
