/**
 * Client 小程序业务类型定义。
 * 字段对齐后端 VO（EquityDepotVO / ServiceSessionVO / EquityUsePersonVO）。
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

/** 权益使用人（对齐后端 EquityUsePersonVO，id 序列化为字符串防精度丢失） */
export interface EquityUsePerson {
  id: string;
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

/** 客户个人资料（对齐后端 ClientProfileVO，phone/idCard 已服务端脱敏） */
export interface ClientProfile {
  clientCode: string;
  channelCode?: string;
  channelName?: string;
  fullName?: string;
  gender?: number;
  avatar?: string;
  phone?: string;
  email?: string;
  birthday?: string;
  age?: number;
  idCard?: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  provinceName?: string;
  cityName?: string;
  districtName?: string;
  address?: string;
  clientLevel?: number;
  isVip?: number;
  registerTime?: number | string;
  lastLoginTime?: number | string;
  equityCount?: number;
  usedEquityCount?: number;
  serviceCount?: number;
  totalOrderAmount?: number;
  lastServiceTime?: number | string;
}

/** 资料更新入参（对齐后端 ClientProfileUpdateDTO） */
export interface ClientProfileUpdatePayload {
  fullName?: string;
  gender?: number;
  email?: string;
  avatar?: string;
  birthday?: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  address?: string;
}

/** 轮播 Banner */
export interface Banner {
  bannerId: number;
  title: string;
  imageUrl: string;
  linkUrl?: string;
}

// ============ 权益使用人（管理/复用） ============

/** 新增使用人入参（对齐后端 EquityUsePersonCreateDTO） */
export interface EquityUsePersonCreate {
  equityCode: string;
  usePersonName: string;
  usePersonGender?: number;
  usePersonPhone?: string;
  usePersonIdCard?: string;
  relationWithHolder?: string;
  isDefaultHolder?: number;
}

/** 修改使用人入参（对齐后端 EquityUsePersonUpdateDTO） */
export type EquityUsePersonUpdate = Partial<Omit<EquityUsePersonCreate, 'equityCode'>>;

// ============ 服务跟进（时间线/评价） ============

/** 时间线节点（对齐后端 ServiceTimelineVO.Node） */
export interface TimelineNode {
  /** 节点类型：demand/solution/arrange/visit */
  type: string;
  title: string;
  content?: string;
  time?: number | string;
  status?: number;
}

/** 服务进度时间线（对齐后端 ServiceTimelineVO） */
export interface Timeline {
  demands: TimelineNode[];
  solutions: TimelineNode[];
  arranges: TimelineNode[];
  visits: TimelineNode[];
}

/** 客户端评价入参（对齐后端 ClientEvaluationDTO） */
export interface EvaluationCreate {
  attitudeRating: number;
  professionalRating: number;
  responsivenessRating: number;
  satisfactionRating: number;
  content?: string;
}
