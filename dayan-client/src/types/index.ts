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
  /** 权益期限类型（1=固定天数,2=终身；终身时 expireTime 为空） */
  validityType?: number;
  /** 配额归属（0=按人独立配额,1=权益人共享池） */
  shareMode?: number;
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
  /** 与持有人关系字典code（self/spouse/parent/parent_in_law/child/other） */
  relationWithHolder?: string;
  healthStatus?: string;
  careNeed?: string;
  isDefaultHolder?: number;
  remark?: string;
}

/** 与持有人关系字典（relation_with_holder，存 code 显示 label） */
export const RELATION_OPTIONS: { value: string; label: string }[] = [
  { value: 'self', label: '本人' },
  { value: 'spouse', label: '配偶' },
  { value: 'parent', label: '父母' },
  { value: 'parent_in_law', label: '公婆/岳父母' },
  { value: 'child', label: '子女' },
  { value: 'other', label: '其他' },
];

/** 关系 code → 中文标签（未知值原样返回，兼容存量数据） */
export function relationLabel(v?: string | null): string {
  if (!v) return '—';
  return RELATION_OPTIONS.find((o) => o.value === v)?.label ?? v;
}

/** 配额周期 */
export type QuotaType = 1 | 2;

/** 取消退预定金政策档位 */
export interface RefundRule {
  /** 距入住小时数门槛（如 72/48/24） */
  beforeHours: number;
  /** 退还比例（0~100） */
  refundRate: number;
}

/** 单次使用规则（随心住类：晚数/间数/人数/预订/预定金/取消政策/黑名单） */
export interface UsageRule {
  maxDaysPerUse?: number;
  maxNightsPerUse?: number;
  maxRoomsPerUse?: number;
  maxGuestsPerUse?: number;
  requireBeneficiaryCheckIn?: boolean;
  advanceBookDays?: number;
  depositAmount?: number;
  refundPolicy?: RefundRule[];
  blackoutType?: string;
  blackoutDays?: number;
}

/** 单个机构的服务范围（roomTypeCodes 空=整馆全部房型） */
export interface ParkScope {
  parkCode: string;
  roomTypeCodes?: string[];
}

/** 服务网络范围（null=业态全部机构；custom=自选范围） */
export interface NetworkScope {
  mode: 'all' | 'custom';
  parks?: ParkScope[];
}

/** 客户端服务项目（含配额剩余 + 结构化权益内容，对齐 ClientServiceItemVO） */
export interface ClientServiceItem {
  itemCode: string;
  itemName: string;
  itemCategory?: number;
  itemSubtype?: number;
  quantity: number;
  quotaType: QuotaType;
  consumed: number;
  remaining: number;
  /** 保证入住权（0=无,1=有；长居/照护） */
  admissionGuaranteed?: number;
  /** 优先入住权（0=无,1=有） */
  admissionPriority?: number;
  /** 优惠入住权/旅居优惠权（0=无,1=有） */
  admissionDiscount?: number;
  /** 优惠折扣率（90=门市价9折；null=按协议未定） */
  discountRate?: number | null;
  /** 单次使用规则（随心住类） */
  usageRule?: UsageRule | null;
  /** 服务网络范围（null=业态全部机构） */
  networkScope?: NetworkScope | null;
}

/** 发起服务请求入参 */
export interface ServiceRequestDTO {
  equityCode: string;
  itemCode: string;
  usePersonId: string;
  demandDesc?: string;
}

/** 订单状态（对齐后端 OrderEvent）：0待支付/1已支付/2部分发放/3已发放/4已完成/5已取消/6退款中/7已退款 */
export type OrderStatus = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7;

/** 订单（旅游短居订单，对齐后端 ClientOrderVO） */
export interface Order {
  orderCode: string;
  orderStatus: OrderStatus;
  /** 状态文案（后端预计算） */
  statusText?: string;
  title?: string;
  parkName?: string;
  /** 规格名（房型快照） */
  skuName?: string;
  checkinDate?: string;
  checkoutDate?: string;
  stayDays?: number;
  payAmount?: number;
  totalAmount?: number;
  createdAt?: number | string;
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

/**
 * 轮播 Banner（来源：已发布置顶内容）
 * 后端 /contents/banners 返回 ContentInfoVO，调用方需把 coverImage（OSS key）
 * 经 formatFileUrl（@/utils/file）转 URL 后映射到 imageUrl。
 */
export interface Banner {
  bannerId: number;
  title: string;
  imageUrl: string;
  linkUrl?: string;
}

/**
 * 推荐内容卡片（对齐后端 ContentInfoVO 摘要字段）
 * coverImage 为 OSS key，展示需经 formatFileUrl（@/utils/file）拼 URL。
 */
export interface ContentCard {
  contentCode: string;
  title: string;
  coverImage?: string;
  summary?: string;
  contentType?: number;
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
