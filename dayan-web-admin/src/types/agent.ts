/**
 * 代理人相关类型。
 *
 * 字段对齐后端 com.dayan.agent.entity.AgentInfo（表 agent_info，位于 dayan-module-agent）。
 *
 * 注意：后端代理人控制器为 RESTful 复数风格（/admin-api/agents），list 接口
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

/** 代理人等级：1实习 2正式 3资深 4金牌 */
export enum AgentLevel {
  INTERN = 1,
  REGULAR = 2,
  SENIOR = 3,
  GOLD = 4
}

/** 代理人等级选项 */
export const AGENT_LEVEL_OPTIONS = [
  { label: '实习', value: AgentLevel.INTERN },
  { label: '正式', value: AgentLevel.REGULAR },
  { label: '资深', value: AgentLevel.SENIOR },
  { label: '金牌', value: AgentLevel.GOLD }
] as const

/** 代理人状态：1启用 0禁用 */
export enum AgentStatus {
  ENABLED = 1,
  DISABLED = 0
}

/** 代理人状态选项 */
export const AGENT_STATUS_OPTIONS = [
  { label: '启用', value: AgentStatus.ENABLED },
  { label: '禁用', value: AgentStatus.DISABLED }
] as const

/** 是否认证：0否 1是 */
export enum CertifiedFlag {
  NO = 0,
  YES = 1
}

/** 认证选项 */
export const CERTIFIED_OPTIONS = [
  { label: '未认证', value: CertifiedFlag.NO },
  { label: '已认证', value: CertifiedFlag.YES }
] as const

/**
 * 代理人信息实体（后端 AgentInfo）。
 */
export interface AgentInfo {
  id?: number
  /** 代理人编码（主键，服务端生成） */
  agentCode?: string
  /** 姓名 */
  fullName: string
  /** 性别：0未知 1男 2女 */
  gender?: Gender
  /** 头像 */
  avatar?: string
  /** 手机号 */
  phone?: string
  /** 邮箱 */
  email?: string
  /** 身份证号 */
  idCard?: string
  /** 所属渠道编码 */
  channelCode?: string
  /** 保险公司名称 */
  companyName?: string
  /** 分公司名称 */
  branchName?: string
  /** 部门 */
  department?: string
  /** 职位 */
  position?: string
  /** 保险公司工号 */
  employeeNo?: string
  /** 从业资格证号 */
  licenseNo?: string
  /** 省级编码 */
  provinceCode?: string
  /** 市级编码 */
  cityCode?: string
  /** 区县编码 */
  districtCode?: string
  /** 详细地址 */
  address?: string
  /** 服务介绍 */
  serviceIntro?: string
  /** 客户数量（统计字段） */
  clientCount?: number
  /** 订单总数（统计字段） */
  totalOrderCount?: number
  /** 代理人等级：1实习 2正式 3资深 4金牌 */
  agentLevel?: AgentLevel
  /** 是否认证：0否 1是 */
  isCertified?: CertifiedFlag
  /** 状态：1启用 0禁用 */
  status?: AgentStatus
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 代理人分页查询参数（后端 AgentInfoQueryDTO）。
 */
export interface AgentInfoQuery extends PageQuery {
  /** 代理人编码 */
  agentCode?: string
  /** 所属渠道编码 */
  channelCode?: string
  /** 姓名（模糊匹配） */
  fullName?: string
  /** 手机号（模糊匹配） */
  phone?: string
  /** 代理人等级 */
  agentLevel?: AgentLevel
  /** 是否认证：0否 1是 */
  isCertified?: CertifiedFlag
  /** 状态：1启用 0禁用 */
  status?: AgentStatus
}
