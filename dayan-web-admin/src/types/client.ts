/**
 * 客户相关类型。
 *
 * 字段对齐后端 com.dayan.client.entity.ClientInfo（表 client_info，位于 dayan-module-client）。
 *
 * 注意：后端客户控制器为 RESTful 复数风格（/admin-api/clients），list 接口
 * 返回 PageResult（有分页字段），但 url 无 /page 后缀。
 */

import type { PageQuery } from '@/types/common'

/** 性别：0未知 1男 2女 */
export enum Gender {
  UNKNOWN = 0,
  MALE = 1,
  FEMALE = 2
}

/** 性别选项 */
export const GENDER_OPTIONS = [
  { label: '未知', value: Gender.UNKNOWN },
  { label: '男', value: Gender.MALE },
  { label: '女', value: Gender.FEMALE }
] as const

/** 客户等级：1普通 2银卡 3金卡 4钻石 */
export enum ClientLevel {
  NORMAL = 1,
  SILVER = 2,
  GOLD = 3,
  DIAMOND = 4
}

/** 客户等级选项 */
export const CLIENT_LEVEL_OPTIONS = [
  { label: '普通', value: ClientLevel.NORMAL },
  { label: '银卡', value: ClientLevel.SILVER },
  { label: '金卡', value: ClientLevel.GOLD },
  { label: '钻石', value: ClientLevel.DIAMOND }
] as const

/** 客户状态：1启用 0禁用 */
export enum ClientStatus {
  ENABLED = 1,
  DISABLED = 0
}

/** 客户状态选项 */
export const CLIENT_STATUS_OPTIONS = [
  { label: '启用', value: ClientStatus.ENABLED },
  { label: '禁用', value: ClientStatus.DISABLED }
] as const

/** 是否 VIP：0否 1是 */
export enum VipFlag {
  NO = 0,
  YES = 1
}

/** VIP 选项 */
export const VIP_OPTIONS = [
  { label: '否', value: VipFlag.NO },
  { label: '是', value: VipFlag.YES }
] as const

/** 学历：1小学 2初中 3高中 4专科 5本科 6硕士 7博士 */
export enum Education {
  PRIMARY = 1,
  JUNIOR = 2,
  SENIOR = 3,
  COLLEGE = 4,
  BACHELOR = 5,
  MASTER = 6,
  DOCTOR = 7
}

/** 学历选项 */
export const EDUCATION_OPTIONS = [
  { label: '小学', value: Education.PRIMARY },
  { label: '初中', value: Education.JUNIOR },
  { label: '高中', value: Education.SENIOR },
  { label: '专科', value: Education.COLLEGE },
  { label: '本科', value: Education.BACHELOR },
  { label: '硕士', value: Education.MASTER },
  { label: '博士', value: Education.DOCTOR }
] as const

/**
 * 客户信息实体（后端 ClientInfo，共 31 个字段）。
 */
export interface ClientInfo {
  id?: number
  /** 客户编码（主键，服务端生成） */
  clientCode?: string
  /** 所属渠道编码 */
  channelCode?: string
  /** 姓名 */
  fullName: string
  /** 性别：0未知 1男 2女 */
  gender?: Gender
  /** 头像 */
  avatar?: string
  /** 生日（yyyy-MM-dd） */
  birthday?: string
  /** 年龄（统计字段） */
  age?: number
  /** 身份证号 */
  idCard?: string
  /** 手机号 */
  phone?: string
  /** 邮箱 */
  email?: string
  /** 省级编码 */
  provinceCode?: string
  /** 市级编码 */
  cityCode?: string
  /** 区县编码 */
  districtCode?: string
  /** 详细地址 */
  address?: string
  /** 学历 */
  education?: Education
  /** 婚姻状况 */
  maritalStatus?: number
  /** 职业 */
  profession?: string
  /** 来源类型 */
  sourceType?: number
  /** 客户等级：1普通 2银卡 3金卡 4钻石 */
  clientLevel?: ClientLevel
  /** 权益数量（统计字段） */
  equityCount?: number
  /** 服务次数（统计字段） */
  serviceCount?: number
  /** 是否 VIP：0否 1是 */
  isVip?: VipFlag
  /** 状态：1启用 0禁用 */
  status?: ClientStatus
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 客户分页查询参数（后端 ClientInfoQueryDTO）。
 */
export interface ClientInfoQuery extends PageQuery {
  /** 所属渠道编码 */
  channelCode?: string
  /** 客户编码 */
  clientCode?: string
  /** 姓名（模糊匹配） */
  fullName?: string
  /** 手机号（模糊匹配） */
  phone?: string
  /** 性别 */
  gender?: Gender
  /** 客户等级 */
  clientLevel?: ClientLevel
  /** 是否 VIP：0否 1是 */
  isVip?: VipFlag
  /** 状态：1启用 0禁用 */
  status?: ClientStatus
}
