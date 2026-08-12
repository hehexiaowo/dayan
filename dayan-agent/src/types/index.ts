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

/** 个人资料（GET /agent-api/profile，对齐后端 AgentProfileVO） */
export interface AgentProfile {
  agentCode: string;
  fullName?: string;
  /** 0 保密 / 1 男 / 2 女 */
  gender?: number;
  /** 头像 OSS key（展示需 formatFileUrl） */
  avatar?: string;
  phone?: string;
  email?: string;
  username?: string;
  channelCode?: string;
  channelName?: string;
  companyName?: string;
  branchName?: string;
  department?: string;
  position?: string;
  employeeNo?: string;
  licenseNo?: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  provinceName?: string;
  cityName?: string;
  districtName?: string;
  address?: string;
  serviceIntro?: string;
  /** 1 普通 / 2 银牌 / 3 金牌 / 4 钻石 */
  agentLevel?: number;
  /** 0 否 / 1 是 */
  isCertified?: number;
  lastLoginTime?: string;
}

/** 资料更新请求（PUT /agent-api/profile 白名单字段） */
export interface AgentProfileUpdatePayload {
  fullName?: string;
  gender?: number;
  email?: string;
  avatar?: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  address?: string;
  serviceIntro?: string;
}

/** 代理人等级文案映射 */
export const AGENT_LEVEL_MAP: Record<number, string> = {
  1: '普通',
  2: '银牌',
  3: '金牌',
  4: '钻石',
};

/** 通知/待办（GET /agent-api/notifications） */
export interface AgentNotification {
  id: string;
  title: string;
  content?: string;
  type?: number;
  readFlag?: 0 | 1;
  createdAt?: string;
}

/** 线索状态：1 新 / 2 跟进中 / 3 意向 / 4 已转化 / 5 已流失 */
export enum LeadStatus {
  NEW = 1,
  FOLLOWING = 2,
  INTENDED = 3,
  CONVERTED = 4,
  LOST = 5,
}

/** 客户线索（GET /agent-api/leads） */
export interface Lead {
  /** 雪花ID（后端序列化为字符串，防止 JS 精度丢失） */
  id: string;
  leadCode?: string;
  agentCode?: string;
  channelCode?: string;
  name: string;
  phone?: string;
  gender?: number;
  age?: number;
  /** 1 新 / 2 跟进中 / 3 意向 / 4 已转化 / 5 已流失 */
  leadStatus?: LeadStatus | number;
  sourceType?: number;
  sourceRef?: string;
  /** 访客令牌（匿名唯一标识） */
  visitorToken?: string;
  /** 访客来源（wechat/browser/unknown） */
  visitorSource?: string;
  /** 微信昵称 */
  wxNickname?: string;
  /** 微信头像URL */
  wxAvatar?: string;
  /** 最后互动时间 */
  lastTraceTime?: string;
  /** 最后互动类型（1=内容 2=工具 3=海报） */
  lastTraceType?: TraceType | number;
  /** 互动总次数 */
  traceCount?: number;
  intentionLevel?: number;
  interestType?: string;
  region?: string;
  lastFollowTime?: string;
  convertedClientCode?: string;
  convertedAt?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 互动类型：1 浏览内容 / 2 使用工具 / 3 查看海报 */
export enum TraceType {
  CONTENT = 1,
  TOOL = 2,
  POSTER = 3,
}

/** 线索互动记录（GET /agent-api/leads/{leadId}/traces） */
export interface LeadTrace {
  id: string;
  traceType: TraceType | number;
  bizCode?: string;
  bizTitle?: string;
  traceTime?: string;
}

// ===== 学习中心 =====

/** 学习内容分类：1=视频课程 2=图文课程 3=雁鸣中国 */
export enum LearningCategory {
  VIDEO = 1,
  ARTICLE = 2,
  YANMING = 3,
}

/** 学习内容（GET /agent-api/learning/contents） */
export interface LearningContent {
  id: string;
  contentCode: string;
  title: string;
  summary?: string;
  /** 1=视频 2=图文 3=雁鸣 */
  category: LearningCategory | number;
  author?: string;
  /** 时长（视频 28:30 / 图文 约15分钟） */
  duration?: string;
  viewCount?: number;
  /** 角标（热/新/要闻/人物/动态/洞察） */
  badge?: string;
  publishTime?: string;
  sortOrder?: number;
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

// ===== 权益卡 =====

/** 权益状态：0 库存 / 1 出库 / 2 激活 / 3 使用中 / 4 完成 / 5 过期 / 6 作废 / 7 更换中 */
export enum EquityStatus {
  STOCK = 0,
  OUTBOUND = 1,
  ACTIVATED = 2,
  IN_USE = 3,
  COMPLETED = 4,
  EXPIRED = 5,
  VOID = 6,
  CHANGING = 7,
}

/** 载体类型：1 权益卡 / 2 权益函 */
export const CARRIER_CARD = 1;
export const CARRIER_LETTER = 2;

/** 权益卡/函（GET /agent-api/equities） */
export interface EquityCard {
  id: string;
  equityCode: string;
  equityNo: string;
  goodsCode?: string;
  goodsName?: string;
  personCount?: number;
  validDays?: number;
  agentCode?: string;
  clientCode?: string;
  /** 客户姓名（从激活记录快照） */
  clientName?: string;
  /** 客户手机（从激活记录快照） */
  clientPhone?: string;
  /** 载体类型：1 卡 / 2 函 */
  carrierType?: number;
  activateCode?: string;
  bindCode?: string;
  qrCodeUrl?: string;
  /** 权益状态 */
  equityStatus?: EquityStatus | number;
  activateTime?: string;
  firstUseTime?: string;
  lastUseTime?: string;
  expireTime?: string;
  produceTime?: string;
  remark?: string;
  createdAt?: string;
}

/** 权益卡查询参数 */
export interface EquityQuery extends PageQuery {
  equityStatus?: number | null;
}

/** 权益卡状态统计（GET /agent-api/equities/stats） */
export interface EquityStats {
  total: number;
  stock: number;
  outbound: number;
  activated: number;
  inUse: number;
  completed: number;
}

// ===== 商城商品 =====

/** 商城权益商品（对齐后端 GoodsInfoVO 展示子集） */
export interface GoodsProduct {
  goodsCode: string;
  goodsName: string;
  goodsType?: number;
  coverImage?: string;
  imageUrls?: string;
  goodsDescription?: string;
  summary?: string;
  originalPrice?: number;
  salePrice?: number;
  priceUnit?: string;
  /** 库存（-1 表示不限） */
  stock?: number;
  salesCount?: number;
  goodsStatus?: number;
}

// ===== 内容文章 =====

/** 内容文章（对齐后端 ContentInfoVO 展示子集） */
export interface ContentArticle {
  contentCode: string;
  title: string;
  subtitle?: string;
  contentType?: number;
  categoryCode?: string;
  categoryName?: string;
  authorName?: string;
  authorAvatar?: string;
  coverImage?: string;
  summary?: string;
  contentBody?: string;
  tags?: string;
  isTop?: number;
  isRecommend?: number;
  viewCount?: number;
  likeCount?: number;
  shareCount?: number;
  collectCount?: number;
  publishTime?: string;
  contentStatus?: number;
  sourceType?: number;
  sourceUrl?: string;
  /** 前端计算：当前代理是否已收藏（非后端字段） */
  isFavorited?: boolean;
}

/** 内容分类选项（分类导航用） */
export interface ContentCategoryOption {
  categoryCode: string;
  categoryName: string;
}

// ===== 电子名片 =====

/** 电子名片（对齐后端 AgentCardVO） */
export interface BusinessCard {
  id: string;
  cardCode: string;
  agentCode?: string;
  channelCode?: string;
  cardName: string;
  displayName: string;
  title?: string;
  phone: string;
  wechat?: string;
  email?: string;
  company?: string;
  address?: string;
  avatar?: string;
  intro?: string;
  tags?: string;
  sortOrder?: number;
  status?: number;
  createdAt?: string;
  updatedAt?: string;
}

// ===== 场景营销 =====

/** 场景类型映射 */
export const SCENE_TYPE_MAP: Record<number, string> = {
  1: '参观体验',
  2: '健康讲座',
  3: '亲子互动',
  4: '节日活动',
  5: '文化娱乐',
  6: '健康检测',
  7: '美食品鉴',
  8: '其他',
};

/** 场景活动（对齐后端 SceneInfoVO） */
export interface SceneActivity {
  id?: string;
  sceneCode: string;
  sceneName: string;
  sceneType?: number;
  parkCode?: string;
  provinceCode?: string;
  cityCode?: string;
  districtCode?: string;
  address?: string;
  sceneDescription?: string;
  coverImage?: string;
  imageUrls?: string;
  videoUrl?: string;
  capacity?: number;
  durationHours?: number;
  targetAudience?: string;
  highlight?: string;
  notice?: string;
  minPerson?: number;
  maxPerson?: number;
  originalPrice?: number;
  salePrice?: number;
  priceUnit?: string;
  isFree?: number;
  sortOrder?: number;
  viewCount?: number;
  bookCount?: number;
  sceneStatus?: number;
  remark?: string;
}

/** 场景日程（对齐后端 SceneScheduleVO） */
export interface SceneScheduleItem {
  id?: string;
  sceneCode?: string;
  sceneName?: string;
  scheduleDate?: string;
  startTime?: string;
  endTime?: string;
  maxPerson?: number;
  currentPerson?: number;
  priceOverride?: number;
  remark?: string;
  status?: number;
}
