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

/** 网络归属选项（networkTags 字段，多选） */
export const NETWORK_TAG_OPTIONS = [
  { label: '活力长居', value: 'vital' },
  { label: '照护长居', value: 'care' },
  { label: '旅游短居', value: 'sojourn' }
] as const

/** 性质类型选项（natureType 字段） */
export const NATURE_TYPE_OPTIONS = [
  { label: '公办', value: 1 },
  { label: '民办', value: 2 },
  { label: '公建民营', value: 3 },
  { label: '合资', value: 4 }
] as const

/** 合同期限选项（contractPeriod 字段） */
export const CONTRACT_PERIOD_OPTIONS = [
  { label: '月签', value: 1 },
  { label: '季签', value: 2 },
  { label: '半年签', value: 3 },
  { label: '年签', value: 4 }
] as const

/** 平台评级选项（isHot 字段，注意 1/2 不是 0/1） */
export const IS_HOT_OPTIONS = [
  { label: '付费广告', value: 1 },
  { label: '热门', value: 2 }
] as const

/** 首页角标选项（subScript 字段） */
export const SUB_SCRIPT_OPTIONS = [
  { label: '最新', value: '1' },
  { label: '最热', value: '2' },
  { label: '优惠', value: '3' },
  { label: '店庆', value: '4' }
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
  /** 网络归属（多选：vital=活力长居/care=照护长居/sojourn=旅游短居） */
  networkTags?: string[]
  /** 活力长居展示配置JSON（{banners:[], thumbnail:""}） */
  vitalConfig?: string
  /** 照护长居展示配置JSON */
  careConfig?: string
  /** 旅游短居展示配置JSON */
  sojournConfig?: string
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
  /** 绿化率 */
  greenAreaRate?: string
  /** 入住率 */
  occupancyRate?: string
  /** 护患比（如 1:5） */
  nursePatientRatio?: string
  /** 最低价展示（元，列表/详情头部展示用） */
  minPriceDisplay?: number
  /** 最高价展示（元） */
  maxPriceDisplay?: number
  /** 价格单位（如 元/月，与 minPriceDisplay 配合展示） */
  priceUnit?: string
  /** 入住最低年龄 */
  checkInAgeMin?: number
  /** 入住最高年龄 */
  checkInAgeMax?: number
  /** 入住说明 */
  checkInDescription?: string
  /** 押金金额（元） */
  depositAmount?: number
  /** 押金说明 */
  depositDescription?: string
  /** 合同期限：1月签/2季签/3半年/4年签 */
  contractPeriod?: number
  /** 员工总数 */
  staffCount?: number
  /** 护理员数量 */
  nurseCount?: number
  /** 运营主体 */
  operationSubject?: string
  /** 运营主体介绍 */
  operationSubjectDescription?: string
  /** 重要股东 */
  importantShareholders?: string
  /** 合作公司 */
  partnerCompany?: string
  /** 营业执照号 */
  businessLicenseNo?: string
  /** 商务 BD */
  businessBd?: string
  /** 机构类型描述 */
  abilityTypeDescription?: string
  /** 机构性质描述 */
  natureTypeDescription?: string
  /** 平台评级：1=付费广告/2=热门 */
  isHot?: number
  /** 首页角标：1=最新/2=最热/3=优惠/4=店庆 */
  subScript?: string
  /** 开业时间 */
  openingTime?: string
  /** 浏览次数 */
  viewCount?: number
  /** 收藏次数 */
  collectCount?: number
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
  /** 网络归属筛选（vital/care/sojourn） */
  networkTag?: string
  natureType?: number
  dayanLevel?: number
  operateStatus?: ParkOperateStatus
  isPublished?: number
  isHot?: number
}

// ============================================================================
// 房型（ParkRoomType）
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


// ============================================================================
// 照护（ParkCareType）
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

// ============================================================================
// 餐饮（ParkFoodType）
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

// ============================================================================
// 统一定价（ParkPricing / ParkPricingItem）
// ============================================================================

/** 费类 */
export type ChargeType = 1 | 2 | 3 | 4 | 5 | 6 | 9

/** 费类选项（1=房间费 2=照护费 3=餐费 4=押金 5=设施费 6=服务费 9=其他） */
export const CHARGE_TYPE_OPTIONS = [
  { label: '房间费', value: 1 },
  { label: '照护费', value: 2 },
  { label: '餐费', value: 3 },
  { label: '押金', value: 4 },
  { label: '设施费', value: 5 },
  { label: '服务费', value: 6 },
  { label: '其他', value: 9 }
] as const

/** 计费周期选项（billing_cycle） */
export const BILLING_CYCLE_OPTIONS = [
  { label: '月费', value: 1 },
  { label: '季费', value: 2 },
  { label: '半年费', value: 3 },
  { label: '年费', value: 4 },
  { label: '一次性', value: 5 }
] as const

/** 费类 label 映射 */
export function chargeTypeLabel(v?: number): string {
  const found = CHARGE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/** 计费周期 label 映射 */
export function billingCycleLabel(v?: number): string {
  const found = BILLING_CYCLE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/**
 * 机构统一定价实体（后端 ParkPricingVO）。
 *
 * 合并原 room/care/food/facility/service 5 张 price 表。
 * chargeType 标识费类（押金/房间/照护/餐费/设施/服务）；
 * refType+refCode 关联具体 type 表。
 */
export interface ParkPricing {
  id?: number
  parkCode?: string
  /** 方案名称 */
  planName?: string
  /** 费类（1房间 2照护 3餐费 4押金 5设施 6服务 9其他） */
  chargeType?: number
  /** 关联类型（room_type/care_type/food_type/facility_type/service_type/park） */
  refType?: string
  /** 关联编码 */
  refCode?: string
  /** 关联名称（冗余） */
  refName?: string
  /** 计费周期（1月 2季 3半年 4年 5一次性） */
  billingCycle?: number
  /** 自由文本计费单位（设施/服务的 次/小时/场） */
  priceUnit?: string
  /** 原价 */
  originalPrice?: number
  /** 售价（业务必填） */
  salePrice?: number
  /** 折扣率 */
  discountRate?: number
  /** 价格说明 */
  priceDescription?: string
  /** 包含项目（JSON 字符串原文） */
  includesItems?: string
  /** 生效日期（业务必填） */
  effectiveDate?: string
  /** 失效日期 */
  expireDate?: string
  /** 是否当前价：1是 / 0否 */
  isCurrent?: number
  /** 预约生效标记：1=待生效（到点自动切换） */
  pendingFlag?: number
  /** 是否促销：1是 / 0否 */
  isPromotion?: number
  /** 促销说明 */
  promotionDescription?: string
  /** 价格变更原因 */
  priceChangeReason?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 乐观锁版本 */
  version?: number
  /** 创建时间 */
  createdAt?: string
}

/** 定价分页查询参数 */
export interface ParkPricingQuery extends PageQuery {
  parkCode?: string
  chargeType?: number
  refType?: string
  refCode?: string
  billingCycle?: number
  isCurrent?: number
  status?: number
}

// ============================================================================
// 机构评分（ParkScore，从 park_info 拆出）
// ============================================================================

/** 机构评分实体（后端 ParkScoreVO） */
export interface ParkScore {
  id?: number
  parkCode?: string
  scoreTotal?: number
  scoreEnvironment?: number
  scoreRecreation?: number
  scoreNursing?: number
  scoreFood?: number
  scoreService?: number
  scorePrice?: number
  scoreDescription?: string
  createdAt?: string
  updatedAt?: string
}

// ============================================================================
// 设施类型（ParkFacilityType）
// ============================================================================

/**
 * 机构设施类型（后端 ParkFacilityTypeVO）。
 *
 * 外键 parkCode；facilityTypeCode 必填（业务编码，update 不可改），facilityTypeName 必填。
 */
export interface ParkFacilityType {
  id?: number
  parkCode?: string
  /** 设施类型编码（必填，业务语义编码，update 不可改） */
  facilityTypeCode: string
  /** 设施类型名称（必填） */
  facilityTypeName: string
  /** 设施类别 */
  facilityTypeCategory?: number
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
  facilityTypeDescription?: string
  /** 封面图 URL */
  coverImage?: string
  /** 图片列表（JSON 字符串原文） */
  images?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 设施类型分页查询参数 */
export interface ParkFacilityTypeQuery extends PageQuery {
  parkCode?: string
  facilityTypeCode?: string
  facilityTypeName?: string
  facilityTypeCategory?: number
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
// 周边（ParkPeriphery）+ 服务类型（ParkServiceType）
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
 * 机构服务类型（后端 ParkServiceTypeVO）。
 *
 * 外键 parkCode；serviceTypeCode 必填（业务编码，update 不可改），serviceTypeName 必填。
 */
export interface ParkServiceType {
  id?: number
  parkCode?: string
  /** 服务类型编码（必填，业务语义编码，update 不可改） */
  serviceTypeCode: string
  /** 服务类型名称（必填） */
  serviceTypeName: string
  /** 服务类别 */
  serviceTypeCategory?: number
  /** 服务描述 */
  serviceTypeDescription?: string
  /** 服务频次 */
  serviceTypeFrequency?: string
  /** 服务时长 */
  serviceTypeDuration?: string
  /** 封面图 URL */
  coverImage?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 / 0停用 */
  status?: number
  /** 创建时间 */
  createdAt?: string
}

/** 服务类型分页查询参数 */
export interface ParkServiceTypeQuery extends PageQuery {
  parkCode?: string
  serviceTypeCode?: string
  serviceTypeName?: string
  serviceTypeCategory?: number
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

/** 房型-居住类型（stay_type）：1=长居, 2=旅游短居 */
export const STAY_TYPE_OPTIONS = [
  { label: '长居', value: 1 },
  { label: '旅游短居', value: 2 }
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

/** 设施类型-类别（facility_type_category）：1=休闲娱乐..6=安全保障 */
export const FACILITY_TYPE_CATEGORY_OPTIONS = [
  { label: '休闲娱乐', value: 1 },
  { label: '医疗健康', value: 2 },
  { label: '运动健身', value: 3 },
  { label: '文化教育', value: 4 },
  { label: '生活服务', value: 5 },
  { label: '安全保障', value: 6 }
] as const

/** 服务类型-类别（service_type_category）：1=生活照料..6=其他 */
export const SERVICE_TYPE_CATEGORY_OPTIONS = [
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
export const facilityTypeCategoryLabel = (v?: number) => labelOf(FACILITY_TYPE_CATEGORY_OPTIONS, v)
export const serviceTypeCategoryLabel = (v?: number) => labelOf(SERVICE_TYPE_CATEGORY_OPTIONS, v)
export const peripheryTypeLabel = (v?: number) => labelOf(PERIPHERY_TYPE_OPTIONS, v)
/** 0/1 布尔 label：是/否 */
export const boolIntLabel = (v?: number) => labelOf(BOOL_INT_OPTIONS, v)
/** 子表状态 label：启用/停用 */
export const subTableStatusLabel = (v?: number) => labelOf(SUB_TABLE_STATUS_OPTIONS, v)

// ==================== 展示板块 ====================

/** 展示板块类型选项 */
export const DISPLAY_BLOCK_TYPE_OPTIONS = [
  { label: '品牌介绍', value: 'brand_intro' },
  { label: '运营主体', value: 'operation_intro' },
  { label: '缴费方式', value: 'payment_way' },
  { label: '居住环境', value: 'live_env' },
  { label: '餐饮服务', value: 'catering' },
  { label: '文娱生活', value: 'entertainment' },
  { label: '康养状况', value: 'health_status' },
  { label: '入住指南', value: 'checkin_guide' },
  { label: '费用说明', value: 'fee_explain' },
  { label: '自定义', value: 'custom' }
] as const

export const displayBlockTypeLabel = (v?: string) =>
  DISPLAY_BLOCK_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? (v || '--')

/** 业态选项（与后端 NetworkType 枚举 / 字典 network_type 一致） */
export const NETWORK_TYPE_OPTIONS = [
  { label: '活力长居', value: 'vital' },
  { label: '照护长居', value: 'care' },
  { label: '旅游短居', value: 'sojourn' }
] as const

export const networkTypeLabel = (v?: string) =>
  NETWORK_TYPE_OPTIONS.find((o) => o.value === v)?.label ?? (v || '--')

/** 业态标签 → string[]（后端 VO 返回数组；兼容历史逗号串） */
export const networkTagsToList = (s?: string | string[]): string[] =>
  Array.isArray(s) ? s.filter(Boolean) : (s || '').split(',').map((t) => t.trim()).filter(Boolean)

/** 机构展示板块实体 */
export interface ParkDisplayBlock {
  id?: number
  parkCode?: string
  /** 板块类型（brand_intro/payment_way/live_env/catering/entertainment/health_status/checkin_guide/fee_explain/custom） */
  blockType: string
  /** 板块标题（C端展示用） */
  blockTitle?: string
  /** 富文本内容（HTML） */
  content?: string
  /** 图片key列表（JSON数组字符串，后端存 TEXT） */
  images?: string
  /** 图片描述列表（JSON数组字符串） */
  imageDescriptions?: string
  sortOrder?: number
  status?: number
  /** 适用业态（后端 VO 返回 JSON 数组，元素 vital/care/sojourn），空=全部 */
  networkTags?: string[]
  createdAt?: string
}

/** 展示板块查询入参 */
export interface ParkDisplayBlockQuery extends PageQuery {
  parkCode?: string
  blockType?: string
  status?: number
}

/**
 * 展示板块提交载荷：networkTags 为逗号分隔串（后端 Create/Update DTO 为 String），
 * 区别于响应 VO 的数组形态（ParkDisplayBlock.networkTags: string[]）。
 */
export type ParkDisplayBlockPayload = Partial<Omit<ParkDisplayBlock, 'networkTags'>> & {
  /** 适用业态（逗号分隔 vital/care/sojourn），空串=全部 */
  networkTags?: string
}
