/**
 * Agent 端业务类型定义。
 * 与后端 Entity 字段对齐（参考设计规格 §2.4）；接口未实现时降级。
 */

/** 通用分页结果（对齐后端 PageResult：records/total/current/size） */
export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

/** 通用分页查询参数（后端用 current/size） */
export interface PageQuery {
  current?: number;
  size?: number;
  keyword?: string;
}

/** 代理人信息（GET /agent-api/agent/info） */
export interface Agent {
  /** 代理人编码（登录后由 token 携带的 agentCode） */
  agentCode: string;
  /** 真实姓名 */
  realName?: string;
  /** 手机号 */
  phone?: string;
  /** 代理人等级 */
  agentLevel?: string;
  /** 渠道编码 */
  channelCode?: string;
  /** 渠道名称 */
  channelName?: string;
  /** 头像 URL */
  avatar?: string;
}

/** 通知/待办（GET /agent-api/notifications） */
export interface AgentNotification {
  id: string;
  title: string;
  content?: string;
  type?: number;
  readFlag?: 0 | 1;
  createdAt?: string;
}

/** 客户线索状态：1 新 / 2 跟进中 / 3 已转化 */
export enum LeadStatus {
  NEW = 1,
  FOLLOWING = 2,
  CONVERTED = 3,
}

/** 客户线索（GET /agent-api/leads） */
export interface Lead {
  leadId: string;
  clientName: string;
  phone?: string;
  /** 1 新 / 2 跟进中 / 3 已转化 */
  leadStatus?: LeadStatus | number;
  createdAt?: string;
}

/** 客户类型：1 本人 / 2 家属 / 3 老人 */
export enum ClientType {
  SELF = 1,
  FAMILY = 2,
  ELDER = 3,
}

/** 客户（GET /agent-api/customers） */
export interface Customer {
  clientCode: string;
  clientName: string;
  phone?: string;
  /** 1 本人 / 2 家属 / 3 老人 */
  clientType?: ClientType | number;
  bindTime?: string;
}

/** 活动/内容素材（GET /agent-api/activities） */
export interface Activity {
  activityId: string;
  title: string;
  summary?: string;
  coverImage?: string;
  readCount?: number;
  createdAt?: string;
}
