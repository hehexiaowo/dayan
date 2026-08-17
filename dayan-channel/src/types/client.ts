import type { PageQuery } from './common'

/**
 * 客户相关类型。
 *
 * 字段对齐后端 Client 域 Entity（com.dayan.client.entity.Client），
 * 渠道后台视角取本渠道客户子集。
 */

/** 性别：1 男 / 2 女 / 0 未知 */
export enum Gender {
  /** 未知 */
  UNKNOWN = 0,
  /** 男 */
  MALE = 1,
  /** 女 */
  FEMALE = 2
}

/** 性别选项 */
export const GENDER_OPTIONS = [
  { label: '未知', value: Gender.UNKNOWN },
  { label: '男', value: Gender.MALE },
  { label: '女', value: Gender.FEMALE }
] as const

/**
 * 客户实体（渠道视角子集，对齐 ClientInfoVO）。
 */
export interface Client {
  id?: number
  /** 客户编码（主键业务码） */
  clientCode?: string
  /** 客户全名（对齐后端 ClientInfoVO.fullName） */
  fullName?: string
  /** 手机号 */
  phone?: string
  /** 性别（1 男 / 2 女 / 0 未知） */
  gender?: Gender
  /** 所属渠道编码 */
  channelCode?: string
}

/** 客户分页查询参数 */
export interface ClientQuery {
  /** 客户编码（模糊匹配，可选） */
  clientCode?: string
  /** 客户全名（模糊匹配，可选，对齐后端 fullName） */
  fullName?: string
  /** 手机号（模糊匹配，可选） */
  phone?: string
  /** 当前页码 */
  current: number
  /** 每页条数 */
  size: number
}

// ==================== 客户账号 ====================

/** 客户账号 */
export interface ClientAccount {
  id?: number
  clientCode: string
  channelCode?: string
  username?: string
  realName?: string
  phone?: string
  avatar?: string
  gender?: number
  accountStatus?: number
  lastLoginTime?: string
  createdAt?: string
  updatedAt?: string
}

/** 客户账号查询 */
export interface ClientAccountQuery extends PageQuery {
  clientCode?: string
  username?: string
  phone?: string
  accountStatus?: number
}
