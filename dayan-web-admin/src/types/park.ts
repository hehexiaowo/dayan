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
 * 机构分页查询参数（后端 ParkInfoQueryDTO）。
 *
 * 后端 QueryDTO 当前未声明 cityCode，传参会被无害忽略；保留以便未来扩展。
 */
export interface ParkInfoQuery extends PageQuery {
  parkCode?: string
  fullName?: string
  supplierCode?: string
  /** 城市编码（前端筛选用，后端暂未消费） */
  cityCode?: string
  abilityType?: number
  natureType?: number
  dayanLevel?: number
  operateStatus?: ParkOperateStatus
  isPublished?: number
  isHot?: number
}
