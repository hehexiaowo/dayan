/**
 * Client 小程序业务类型定义。
 * 字段对齐后端 VO（EquityDepotVO / ServiceSessionVO / EquityUsePersonVO）。
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
  address?: string;
  region?: string;
  ratingScore?: number;
  startPrice?: number;
  tags?: string[];
  coverImage?: string;
  description?: string;
}

/**
 * 服务会话状态（对齐后端 ServiceSessionEvent）：
 * 1=待分配 2=待收集 3=方案中 4=安排中 5=服务中 6=已完成 7=已取消
 */
export type ServiceSessionStatus = 1 | 2 | 3 | 4 | 5 | 6 | 7;

/** 服务会话（对齐后端 ServiceSessionVO） */
export interface ServiceSession {
  sessionCode: string;
  equityCode?: string;
  itemCode?: string;
  clientCode?: string;
  serviceTitle?: string;
  title?: string;
  butlerCode?: string;
  butlerFullName?: string;
  butlerName?: string;
  sessionStatus: ServiceSessionStatus;
  subStatus?: string;
  serviceDescription?: string;
  acceptTime?: number | string;
  completeTime?: number | string;
  createdAt?: number | string;
  remark?: string;
}

/**
 * 权益状态（对齐后端 EquityEvent）：
 * 0=库存中 1=已出库 2=已激活 3=使用中 4=已完成 5=已过期 6=已作废 7=更换权益人中
 */
export type EquityStatus = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7;

/** 权益（对齐后端 EquityDepotVO） */
export interface Equity {
  equityCode: string;
  equityNo?: string;
  goodsCode?: string;
  goodsName?: string;
  skuName?: string;
  equityStatus: EquityStatus;
  personCount?: number;
  validDays?: number;
  clientCode?: string;
  channelCode?: string;
  activateTime?: number | string;
  expireTime?: number | string;
  equityType?: string;
  equityName?: string;
  parkCode?: string;
  parkName?: string;
}

/** 权益使用人（对齐后端 EquityUsePersonVO） */
export interface EquityUsePerson {
  id: number;
  equityCode?: string;
  usePersonName: string;
  usePersonGender?: number;
  usePersonAge?: number;
  usePersonPhone?: string;
  relationWithHolder?: string;
  healthStatus?: string;
  careNeed?: string;
  isDefaultHolder?: number;
  remark?: string;
}

/** 配额周期 */
export type QuotaType = 1 | 2;

/** 客户端服务项目（含配额剩余，对齐 ClientServiceItemVO） */
export interface ClientServiceItem {
  itemCode: string;
  itemName: string;
  itemCategory?: number;
  itemSubtype?: number;
  quantity: number;
  quotaType: QuotaType;
  consumed: number;
  remaining: number;
}

/** 发起服务请求入参 */
export interface ServiceRequestDTO {
  equityCode: string;
  itemCode: string;
  usePersonId: string;
  demandDesc?: string;
}

/** 订单状态：0 待支付 1 已支付 2 服务中 3 已完成 4 已取消 5 退款中 6 已退款 7 异常 */
export type OrderStatus = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7;

/** 订单 */
export interface Order {
  orderCode: string;
  orderStatus: OrderStatus;
  title?: string;
  payAmount?: number;
  createdAt?: number | string;
  parkName?: string;
}

/** 轮播 Banner */
export interface Banner {
  bannerId: number;
  title: string;
  imageUrl: string;
  linkUrl?: string;
}
