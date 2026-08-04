/**
 * Client 小程序业务类型定义。
 * 字段参考设计规格 §P10-B（Park/Service/Equity/Order/Banner）。
 */

/** 通用分页结果 */
export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

/** 通用分页查询参数 */
export interface PageQuery {
  page?: number;
  size?: number;
  keyword?: string;
}

/** 机构 */
export interface Park {
  parkCode: string;
  parkName: string;
  /** 详细地址 */
  address?: string;
  /** 区域 */
  region?: string;
  /** 评分（0-5） */
  ratingScore?: number;
  /** 起价（元/月） */
  startPrice?: number;
  /** 标签（如 医养结合/失能照护/高端） */
  tags?: string[];
  /** 封面图 URL */
  coverImage?: string;
  /** 简介 */
  description?: string;
}

/** 服务会话状态：1 待受理 2 服务中 3 待评价 4 已完成 5 已取消 6 暂停 7 异常 */
export type ServiceSessionStatus = 1 | 2 | 3 | 4 | 5 | 6 | 7;

/** 服务会话 */
export interface ServiceSession {
  sessionCode: string;
  /** 服务标题 */
  title: string;
  /** 管家姓名 */
  butlerName?: string;
  /** 会话状态 */
  sessionStatus: ServiceSessionStatus;
  /** 子状态/进度描述 */
  subStatus?: string;
  /** 进度百分比 0-100 */
  progress?: number;
  /** 创建时间（时间戳 ms 或 ISO 字符串） */
  createdAt?: number | string;
  /** 备注 */
  remark?: string;
}

/** 权益状态：0 未激活 1 有效 2 使用中 3 已冻结 4 已失效 5 已退订 6 待生效 7 已用尽 */
export type EquityStatus = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7;

/** 权益 */
export interface Equity {
  equityCode: string;
  equityStatus: EquityStatus;
  /** 权益类型（如 入住权益/服务包/照护套餐） */
  equityType?: string;
  /** 权益名称 */
  equityName?: string;
  /** 到期时间 */
  expireTime?: number | string;
  /** 关联机构 */
  parkCode?: string;
  parkName?: string;
}

/** 订单状态：0 待支付 1 已支付 2 服务中 3 已完成 4 已取消 5 退款中 6 已退款 7 异常 */
export type OrderStatus = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7;

/** 订单 */
export interface Order {
  orderCode: string;
  orderStatus: OrderStatus;
  /** 订单标题 */
  title?: string;
  /** 支付金额（元） */
  payAmount?: number;
  /** 创建时间 */
  createdAt?: number | string;
  /** 关联机构 */
  parkName?: string;
}

/** 轮播 Banner */
export interface Banner {
  bannerId: number;
  title: string;
  /** 图片 URL */
  imageUrl: string;
  /** 跳转链接 */
  linkUrl?: string;
}
