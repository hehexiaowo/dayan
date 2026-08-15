/**
 * 权益商品配置类型（goods_equity 1:1 + goods_service_item_rel N:M）。
 */

/** 权益期限类型 */
export enum ValidityType {
  FIXED = 1,  // 固定天数（validDays 生效）
  LIFETIME = 2, // 终身
}

export const VALIDITY_TYPE_OPTIONS = [
  { label: '固定天数', value: ValidityType.FIXED },
  { label: '终身', value: ValidityType.LIFETIME },
]

/** 配额归属 */
export enum ShareMode {
  PER_PERSON = 0, // 按人独立配额
  SHARED = 1,     // 权益人共享池
}

export const SHARE_MODE_OPTIONS = [
  { label: '权益人共享池', value: ShareMode.SHARED },
  { label: '按人独立配额', value: ShareMode.PER_PERSON },
]

/** 权益人构成规则（holder_rule JSON 结构化） */
export interface HolderRule {
  /** 本人席位（固定 1） */
  self: number
  /** 配偶席位（0/1） */
  spouse: number
  /** 父母席位（0~4，含公婆/岳父母） */
  parent: number
  /** 父母人选是否须激活时指定 */
  designateAtActivation: boolean
}

/** 单个机构的服务范围：勾选机构=整馆；只勾部分房型=具体编码列表 */
export interface ParkScope {
  parkCode: string
  /** 房型编码列表（空=该机构全部房型） */
  roomTypeCodes?: string[]
}

/** 服务网络范围（network_scope JSON 结构化；null=业态全部机构/继承服务项目） */
export interface NetworkScope {
  mode: 'all' | 'custom'
  parks?: ParkScope[]
}

/** 网络范围摘要文案：全部 / N家机构（M个房型） */
export function networkScopeSummary(scope?: NetworkScope | null): string {
  if (!scope || scope.mode !== 'custom' || !scope.parks?.length) return '全部机构'
  const wholeParks = scope.parks.filter((p) => !p.roomTypeCodes || p.roomTypeCodes.length === 0)
  const roomParks = scope.parks.filter((p) => p.roomTypeCodes && p.roomTypeCodes.length > 0)
  const roomCount = roomParks.reduce((s, p) => s + (p.roomTypeCodes?.length || 0), 0)
  const parts: string[] = [`${scope.parks.length}家机构`]
  if (roomCount > 0) parts.push(`${roomCount}个房型`)
  if (wholeParks.length > 0 && roomParks.length > 0) parts.unshift(`整馆${wholeParks.length}家`)
  return parts.join(' · ')
}

/** 取消退预定金政策档位 */
export interface RefundRule {
  /** 距入住小时数门槛（如 72/48/24） */
  beforeHours: number
  /** 退还比例（0~100） */
  refundRate: number
}

/** 单次使用规则（usage_rule JSON 结构化，随心住类） */
export interface UsageRule {
  maxDaysPerUse?: number
  maxNightsPerUse?: number
  maxRoomsPerUse?: number
  maxGuestsPerUse?: number
  requireBeneficiaryCheckIn?: boolean
  advanceBookDays?: number
  depositAmount?: number
  refundPolicy?: RefundRule[]
  /** 不可入住时段类型（spring_festival=春节） */
  blackoutType?: string
  blackoutDays?: number
}

/** 服务项目关联子项（联查 service_item 信息） */
export interface ServiceItemRel {
  id?: number
  goodsCode?: string
  itemCode: string
  itemName?: string
  itemCategory?: number
  itemSubtype?: number
  quantity: number
  /** 配额周期（1=权益期内总量,2=每年（按激活周年重置）） */
  quotaType?: number
  /** 服务网络范围（null=业态全部机构） */
  networkScope?: NetworkScope | null
  /** 保证入住权（0/1） */
  admissionGuaranteed?: number
  /** 优先入住权（0/1） */
  admissionPriority?: number
  /** 优惠入住权/旅居优惠权（0/1） */
  admissionDiscount?: number
  /** 优惠折扣率（90=门市价9折；null=按协议未定） */
  discountRate?: number | null
  /** 单次使用规则（随心住类） */
  usageRule?: UsageRule | null
  sortOrder?: number
  createdAt?: string
}

/** 权益商品配置 VO */
export interface GoodsEquity {
  id?: number
  goodsCode: string
  personCount: number
  validityType?: number
  holderRule?: HolderRule | null
  shareMode?: number
  validDays: number
  shelfLifeDays: number
  /** 可转让次数（0=不可转让） */
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
  validityType?: number
  holderRule?: HolderRule | null
  shareMode?: number
  validDays: number
  shelfLifeDays: number
  maxTransferable: number
  description?: string
  sortOrder?: number
  status?: number
  serviceItems: {
    itemCode: string
    quantity: number
    quotaType?: number
    networkScope?: NetworkScope | null
    admissionGuaranteed?: number
    admissionPriority?: number
    admissionDiscount?: number
    discountRate?: number | null
    usageRule?: UsageRule | null
    sortOrder?: number
  }[]
}
