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

// ============================================================================
// 媒体库（ParkMediaImage / ParkMediaVideo / ParkMediaFile / ParkMediaVr）
// ============================================================================

/**
 * 机构媒体-图片（后端 ParkMediaImageVO）。
 *
 * 外键 parkCode；imageUrl 必填（@NotBlank）。
 */
export interface ParkMediaImage {
  id?: number
  parkCode?: string
  /** 图片 URL（必填） */
  imageUrl: string
  /** 图片名称 */
  imageName?: string
  /** 图片类型 */
  imageType?: number
  /** 图片描述 */
  imageDescription?: string
  /** 宽度 */
  width?: number
  /** 高度 */
  height?: number
  /** 文件大小（字节） */
  fileSize?: number
  /** 排序号 */
  sortOrder?: number
  /** 是否封面：1是 / 0否 */
  isCover?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 媒体-图片分页查询参数 */
export interface ParkMediaImageQuery extends PageQuery {
  parkCode?: string
  imageType?: number
  isCover?: number
  status?: number
}

/**
 * 机构媒体-视频（后端 ParkMediaVideoVO）。
 *
 * 外键 parkCode；videoUrl 必填。
 */
export interface ParkMediaVideo {
  id?: number
  parkCode?: string
  /** 视频 URL（必填） */
  videoUrl: string
  /** 封面图 URL */
  coverUrl?: string
  /** 视频名称 */
  videoName?: string
  /** 视频类型 */
  videoType?: number
  /** 视频描述 */
  videoDescription?: string
  /** 时长（秒） */
  duration?: number
  /** 文件大小（字节） */
  fileSize?: number
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 媒体-视频分页查询参数 */
export interface ParkMediaVideoQuery extends PageQuery {
  parkCode?: string
  videoType?: number
  status?: number
}

/**
 * 机构媒体-文档（后端 ParkMediaFileVO）。
 *
 * 外键 parkCode；fileUrl 必填。
 */
export interface ParkMediaFile {
  id?: number
  parkCode?: string
  /** 文档 URL（必填） */
  fileUrl: string
  /** 文档名称 */
  fileName?: string
  /** 文档类型 */
  fileType?: number
  /** 文档格式（扩展名） */
  fileFormat?: string
  /** 文件大小（字节） */
  fileSize?: number
  /** 文档描述 */
  fileDescription?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 媒体-文档分页查询参数 */
export interface ParkMediaFileQuery extends PageQuery {
  parkCode?: string
  fileType?: number
  status?: number
}

/**
 * 机构媒体-VR（后端 ParkMediaVrVO）。
 *
 * 外键 parkCode；vrUrl 必填。vrType：1全景图 / 2 3D 模型 / 3视频。
 */
export interface ParkMediaVr {
  id?: number
  parkCode?: string
  /** VR URL（必填） */
  vrUrl: string
  /** VR 提供方 */
  vrProvider?: string
  /** VR 名称 */
  vrName?: string
  /** VR 类型：1全景图 / 2 3D 模型 / 3视频 */
  vrType?: number
  /** 缩略图 URL */
  thumbnailUrl?: string
  /** VR 描述 */
  vrDescription?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 媒体-VR 分页查询参数 */
export interface ParkMediaVrQuery extends PageQuery {
  parkCode?: string
  vrType?: number
  status?: number
}

// ============================================================================
// 设施（ParkFacility）
// ============================================================================

/**
 * 机构设施（后端 ParkFacilityVO）。
 *
 * 外键 parkCode；facilityCode 必填（业务编码，update 不可改），facilityName 必填。
 */
export interface ParkFacility {
  id?: number
  parkCode?: string
  /** 设施编码（必填，业务语义编码，update 不可改） */
  facilityCode: string
  /** 设施名称（必填） */
  facilityName: string
  /** 设施类别 */
  facilityCategory?: number
  /** 楼栋名称 */
  buildingName?: string
  /** 楼层 */
  floor?: string
  /** 面积 */
  area?: number
  /** 容纳人数 */
  capacity?: number
  /** 开放时间 */
  openTime?: string
  /** 设施描述 */
  facilityDescription?: string
  /** 封面图 URL */
  coverImage?: string
  /** 图片列表（JSON 字符串原文） */
  images?: string
  /** 是否免费：1免费 / 0收费 */
  isFree?: number
  /** 收费说明 */
  feeDescription?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 设施分页查询参数 */
export interface ParkFacilityQuery extends PageQuery {
  parkCode?: string
  facilityCode?: string
  facilityName?: string
  facilityCategory?: number
  status?: number
}

// ============================================================================
// 顾问（ParkAdviser）
// ============================================================================

/**
 * 机构顾问（后端 ParkAdviserVO）。
 *
 * 外键 parkCode；adviserName 必填。isPrimary=1 首席，同机构唯一（后端自动互斥）。
 */
export interface ParkAdviser {
  id?: number
  parkCode?: string
  /** 顾问姓名（必填） */
  adviserName: string
  /** 顾问头衔 */
  adviserTitle?: string
  /** 顾问头像 URL */
  adviserImage?: string
  /** 顾问介绍 */
  adviserContent?: string
  /** 联系电话 */
  contactPhone?: string
  /** 是否首席：1首席 / 0普通（同机构唯一，后端自动互斥） */
  isPrimary?: number
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 顾问分页查询参数 */
export interface ParkAdviserQuery extends PageQuery {
  parkCode?: string
  adviserName?: string
  isPrimary?: number
  status?: number
}

// ============================================================================
// 周边（ParkPeriphery）+ 服务项（ParkServiceItem）
// ============================================================================

/**
 * 机构周边配套（后端 ParkPeripheryVO）。
 *
 * 外键 parkCode；placeName 必填。peripheryType 区分周边类型。
 */
export interface ParkPeriphery {
  id?: number
  parkCode?: string
  /** 周边类型 */
  peripheryType?: number
  /** 地点名称（必填） */
  placeName: string
  /** 地点地址 */
  placeAddress?: string
  /** 距离（字符串） */
  distance?: string
  /** 详细描述 */
  detailDescription?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 周边配套分页查询参数 */
export interface ParkPeripheryQuery extends PageQuery {
  parkCode?: string
  peripheryType?: number
  placeName?: string
  status?: number
}

/**
 * 机构服务项（后端 ParkServiceItemVO）。
 *
 * 外键 parkCode；serviceCode 必填（业务编码，update 不可改），serviceName 必填。
 */
export interface ParkServiceItem {
  id?: number
  parkCode?: string
  /** 服务编码（必填，业务语义编码，update 不可改） */
  serviceCode: string
  /** 服务名称（必填） */
  serviceName: string
  /** 服务类别 */
  serviceCategory?: number
  /** 服务描述 */
  serviceDescription?: string
  /** 是否包含：1包含 / 0不包含 */
  isIncluded?: number
  /** 收费标准 */
  feeStandard?: string
  /** 服务频次 */
  serviceFrequency?: string
  /** 服务时长 */
  serviceDuration?: string
  /** 封面图 URL */
  coverImage?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 服务项分页查询参数 */
export interface ParkServiceItemQuery extends PageQuery {
  parkCode?: string
  serviceCode?: string
  serviceName?: string
  serviceCategory?: number
  isIncluded?: number
  status?: number
}

// ============================================================================
// 子表枚举 OPTIONS 与 label 映射（DDL 为准，补全列表页/详情页渲染用）
// ============================================================================

/** 房型-房间类别（room_category）：1=单人间..5=VIP房 */
export const ROOM_CATEGORY_OPTIONS = [
  { label: '单人间', value: 1 },
  { label: '双人间', value: 2 },
  { label: '多人间', value: 3 },
  { label: '套间', value: 4 },
  { label: 'VIP房', value: 5 }
] as const

/** 房型-居住类型（stay_type）：1=长居, 2=旅居 */
export const STAY_TYPE_OPTIONS = [
  { label: '长居', value: 1 },
  { label: '旅居', value: 2 }
] as const

/** 照护-照护等级（care_level）：1=特级护理..5=生活自理 */
export const CARE_LEVEL_OPTIONS = [
  { label: '特级护理', value: 1 },
  { label: '一级护理', value: 2 },
  { label: '二级护理', value: 3 },
  { label: '三级护理', value: 4 },
  { label: '生活自理', value: 5 }
] as const

/** 餐饮-餐食方案（meal_plan）：1=三餐..4=自选 */
export const MEAL_PLAN_OPTIONS = [
  { label: '方案一', value: 1 },
  { label: '方案二', value: 2 },
  { label: '方案三', value: 3 },
  { label: '自选', value: 4 }
] as const

/** 餐饮-特殊饮食（special_diet）：0=否, 1=是 */
export const SPECIAL_DIET_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 }
] as const

/** 媒体-图片类型（image_type）：1=外观..11=其他 */
export const IMAGE_TYPE_OPTIONS = [
  { label: '外观', value: 1 },
  { label: '大堂', value: 2 },
  { label: '房间', value: 3 },
  { label: '餐厅', value: 4 },
  { label: '活动区', value: 5 },
  { label: '花园', value: 6 },
  { label: '医疗区', value: 7 },
  { label: '户型', value: 8 },
  { label: '文娱生活', value: 9 },
  { label: '康养状况', value: 10 },
  { label: '其他', value: 11 }
] as const

/** 媒体-视频类型（video_type）：1=宣传视频, 2=环境展示, 3=活动记录 */
export const VIDEO_TYPE_OPTIONS = [
  { label: '宣传视频', value: 1 },
  { label: '环境展示', value: 2 },
  { label: '活动记录', value: 3 }
] as const

/** 媒体-文档类型（file_type）：1=资质文件..5=其他 */
export const FILE_TYPE_OPTIONS = [
  { label: '资质文件', value: 1 },
  { label: '合同文件', value: 2 },
  { label: '宣传资料', value: 3 },
  { label: '费用文档', value: 4 },
  { label: '其他', value: 5 }
] as const

/** 媒体-VR类型（vr_type）：1=全景VR, 2=3D模型, 3=视频VR */
export const VR_TYPE_OPTIONS = [
  { label: '全景VR', value: 1 },
  { label: '3D模型', value: 2 },
  { label: '视频VR', value: 3 }
] as const

/** 设施-类别（facility_category）：1=休闲娱乐..6=安全保障 */
export const FACILITY_CATEGORY_OPTIONS = [
  { label: '休闲娱乐', value: 1 },
  { label: '医疗健康', value: 2 },
  { label: '运动健身', value: 3 },
  { label: '文化教育', value: 4 },
  { label: '生活服务', value: 5 },
  { label: '安全保障', value: 6 }
] as const

/** 服务项-类别（service_category）：1=生活照料..6=其他 */
export const SERVICE_CATEGORY_OPTIONS = [
  { label: '生活照料', value: 1 },
  { label: '医疗健康', value: 2 },
  { label: '康复训练', value: 3 },
  { label: '文化娱乐', value: 4 },
  { label: '心理关怀', value: 5 },
  { label: '其他', value: 6 }
] as const

/** 周边-类型（periphery_type）：1=交通-公交..8=其他 */
export const PERIPHERY_TYPE_OPTIONS = [
  { label: '交通-公交', value: 1 },
  { label: '交通-地铁', value: 2 },
  { label: '交通-自驾', value: 3 },
  { label: '景点', value: 4 },
  { label: '医疗', value: 5 },
  { label: '购物', value: 6 },
  { label: '公园', value: 7 },
  { label: '其他', value: 8 }
] as const

/** 通用 0/1 布尔选项（isCurrent / isPromotion / isIncluded 等） */
export const BOOL_INT_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 }
] as const

/** 通用状态（0=停用, 1=启用）——子表通用 */
export const SUB_TABLE_STATUS_OPTIONS = [
  { label: '停用', value: 0 },
  { label: '启用', value: 1 }
] as const

// ---- label 映射函数（列表渲染用，找不到回退原始值或 '--'）----

/** 通用：按 OPTIONS 数组查 label */
function labelOf(options: ReadonlyArray<{ label: string; value: number }>, v?: number): string {
  if (v == null) return '--'
  const found = options.find((o) => o.value === v)
  return found ? found.label : String(v)
}

export const roomCategoryLabel = (v?: number) => labelOf(ROOM_CATEGORY_OPTIONS, v)
export const stayTypeLabel = (v?: number) => labelOf(STAY_TYPE_OPTIONS, v)
export const careLevelLabel = (v?: number) => labelOf(CARE_LEVEL_OPTIONS, v)
export const mealPlanLabel = (v?: number) => labelOf(MEAL_PLAN_OPTIONS, v)
export const specialDietLabel = (v?: number) => labelOf(SPECIAL_DIET_OPTIONS, v)
export const imageTypeLabel = (v?: number) => labelOf(IMAGE_TYPE_OPTIONS, v)
export const videoTypeLabel = (v?: number) => labelOf(VIDEO_TYPE_OPTIONS, v)
export const fileTypeLabel = (v?: number) => labelOf(FILE_TYPE_OPTIONS, v)
export const vrTypeLabel = (v?: number) => labelOf(VR_TYPE_OPTIONS, v)
export const facilityCategoryLabel = (v?: number) => labelOf(FACILITY_CATEGORY_OPTIONS, v)
export const serviceCategoryLabel = (v?: number) => labelOf(SERVICE_CATEGORY_OPTIONS, v)
export const peripheryTypeLabel = (v?: number) => labelOf(PERIPHERY_TYPE_OPTIONS, v)
/** 0/1 布尔 label：是/否 */
export const boolIntLabel = (v?: number) => labelOf(BOOL_INT_OPTIONS, v)
/** 子表状态 label：启用/停用 */
export const subTableStatusLabel = (v?: number) => labelOf(SUB_TABLE_STATUS_OPTIONS, v)
/** 文件大小格式化：B → KB/MB 友好显示 */
export function fileSizeLabel(bytes?: number): string {
  if (bytes == null) return '--'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}
