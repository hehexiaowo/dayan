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
  /** 关联访客线索编码（lead_info.lead_code，线索池认领后回填） */
  visitorLeadCode?: string;
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

/** 线索池项（GET /agent-api/leads/pool，字段对齐后端 lead 域 LeadInfoVO） */
export interface LeadPoolItem {
  /** 雪花ID（后端序列化为字符串） */
  id: string;
  /** 线索编码（VL 前缀） */
  leadCode: string;
  /** 访客令牌 */
  visitorToken?: string;
  /** 所属渠道编码 */
  channelCode?: string;
  /** 手机号（留资后回填） */
  phone?: string;
  /** 姓名/称呼 */
  name?: string;
  /** 微信昵称 */
  wxNickname?: string;
  /** 微信头像URL */
  wxAvatar?: string;
  /** 访客环境来源（wechat/browser/unknown） */
  visitorSource?: string;
  /** 来源类型（1=内容分享 2=工具分享 3=海报分享 4=直接访问） */
  sourceType?: number;
  /** 来源编码 */
  sourceCode?: string;
  /** 关联客户编码（留资建档后回填） */
  clientCode?: string;
  /** 最后互动时间 */
  lastInteractTime?: string;
  /** 最后互动类型（1=内容 2=工具 3=海报） */
  lastInteractType?: TraceType | number;
  /** 互动总次数 */
  interactCount?: number;
  createdAt?: string;
}

// ===== 学习中心 =====

/** 板块来源：1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国（学习中心四板块统一 course_info） */
export enum CourseSource {
  SELF = 1,
  CHANNEL = 2,
  EXTERNAL = 3,
  YANMING = 4,
}

/** 课程大纲章节（courseInfo.courseOutline JSON 解析结果） */
export interface CourseOutlineChapter {
  title: string;
  lessons: { title: string; duration?: number }[];
}

/** 课程讲师简要信息（课程详情聚合） */
export interface CourseLecturerBrief {
  lecturerCode?: string;
  lecturerName?: string;
  avatar?: string;
  title?: string;
  organization?: string;
  introduction?: string;
}

/** 课程（后端 CourseAgentVO，学习中心四板块统一 course_info） */
export interface Course {
  id?: string;
  courseCode: string;
  courseName: string;
  /** 1=线上录播 2=线上直播 3=线下课程 4=混合课程（非平台自研板块为 null） */
  courseType?: number;
  /** 板块来源：1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯 */
  courseSource?: CourseSource | number;
  categoryCode?: string;
  coverImage?: string;
  courseDescription?: string;
  /** 正文（详情页长文，纯文本） */
  courseBody?: string;
  courseOutline?: string;
  targetAudience?: string;
  learningObjectives?: string;
  /** 作者/来源（渠道/外部/资讯用，平台课程走讲师） */
  author?: string;
  /** 时长展示文本（如 28:30 / 约 15 分钟） */
  durationText?: string;
  lecturerCode?: string;
  lecturerName?: string;
  lecturer?: CourseLecturerBrief;
  totalClass?: number;
  totalDuration?: number;
  validDays?: number;
  originalPrice?: number;
  salePrice?: number;
  currentStudents?: number;
  maxStudents?: number;
  viewCount?: number;
  salesCount?: number;
  ratingAvg?: number;
  /** 0=否 1=是 */
  isFree?: number;
  isRecommend?: number;
  courseStartDate?: string;
  courseEndDate?: string;
  /** 角标（热/新/要闻/人物/动态/洞察） */
  badge?: string;
  /** 发布时间（资讯/内容用，课程走开课日期） */
  publishTime?: string;
  sortOrder?: number;
}

/** 课程类型选项（对齐后端 courseType） */
export const COURSE_TYPE_LABELS: Record<number, string> = {
  1: '线上录播',
  2: '线上直播',
  3: '线下课程',
  4: '混合课程',
};

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

/** 权益详情（GET /agent-api/equities/{equityCode}，字段比 EquityCard 更丰富） */
export interface EquityDetail extends EquityCard {
  /** 批次编码 */
  batchCode?: string;
  /** 成本价 */
  costPrice?: number;
  /** 渠道编码 */
  channelCode?: string;
  /** 分配时间 */
  allocateTime?: string;
  /** 出库时间 */
  outboundTime?: string;
  /** 出库渠道 */
  outboundChannelCode?: string;
  /** 出库代理人 */
  outboundAgentCode?: string;
  /** 物流单号 */
  logisticsNo?: string;
  /** 订单编码（关联 order_equity 快照） */
  orderCode?: string;
  /** 商品规格 */
  skuName?: string;
  /** 作废原因 */
  voidReason?: string;
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

/** 权益人构成规则（对齐后端 HolderRule） */
export interface AgentHolderRule {
  self: number;
  spouse: number;
  parent: number;
  designateAtActivation?: boolean;
}

/** 取消退预定金政策档位 */
export interface AgentRefundRule {
  beforeHours: number;
  refundRate: number;
}

/** 单次使用规则（随心住类） */
export interface AgentUsageRule {
  maxDaysPerUse?: number;
  maxNightsPerUse?: number;
  maxRoomsPerUse?: number;
  maxGuestsPerUse?: number;
  requireBeneficiaryCheckIn?: boolean;
  advanceBookDays?: number;
  depositAmount?: number;
  refundPolicy?: AgentRefundRule[];
  blackoutType?: string;
  blackoutDays?: number;
}

/** 服务网络范围（null=业态全部机构） */
export interface AgentNetworkScope {
  mode: 'all' | 'custom';
  parks?: { parkCode: string; roomTypeCodes?: string[] }[];
}

/** 服务项目权益内容（对齐后端 GoodsEquityVO.ServiceItemRelVO） */
export interface AgentServiceItemRel {
  itemCode: string;
  itemName?: string;
  itemSubtype?: number;
  quantity: number;
  /** 1=权益期内总量 2=每年（按激活周年） */
  quotaType?: number;
  admissionGuaranteed?: number;
  admissionPriority?: number;
  admissionDiscount?: number;
  /** 90=门市价9折 */
  discountRate?: number | null;
  usageRule?: AgentUsageRule | null;
  networkScope?: AgentNetworkScope | null;
}

/** 商品权益配置（对齐后端 GoodsEquityVO） */
export interface AgentEquityConfig {
  personCount?: number;
  /** 1=固定天数 2=终身 */
  validityType?: number;
  /** 0=按人独立配额 1=共享池 */
  shareMode?: number;
  validDays?: number;
  /** 可转让次数（0=不可） */
  maxTransferable?: number;
  holderRule?: AgentHolderRule | null;
  serviceItems?: AgentServiceItemRel[];
}

/** 商品展示板块（对齐后端 GoodsDisplayBlockVO，goods_display_block） */
export interface GoodsDisplayBlock {
  id?: number;
  goodsCode?: string;
  /** 板块类型（product_intro/rights_detail/service_flow/faq/purchase_terms/custom） */
  blockType: string;
  /** 板块标题（详情页 tab 名） */
  blockTitle?: string;
  /** 富文本内容（HTML） */
  content?: string;
  /** 图片key列表（JSON数组字符串） */
  images?: string;
  /** 图片描述列表（JSON数组字符串，与 images 一一对应） */
  imageDescriptions?: string;
  sortOrder?: number;
  status?: number;
}

/** 页面展示配置（goods_info.display_config JSON：banners/thumbnail） */
export interface GoodsDisplayConfig {
  banners: string[];
  thumbnail: string;
}

/** 商城权益商品（对齐后端 AgentGoodsVO：基础信息 + 权益配置） */
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
  /** 权益配置（null=未配置） */
  equity?: AgentEquityConfig | null;
  /** 页面展示配置（JSON字符串：{"banners":[...],"thumbnail":"..."}；空=回退封面图） */
  displayConfig?: string;
  /** 详情页展示板块（仅显示态，按 sortOrder 升序；列表接口不携带） */
  displayBlocks?: GoodsDisplayBlock[];
}

// ===== 权益内容展示工具 =====

/** 服务项目简称（按子类）：旅游短居→旅居 / 活力长居→长居 / 照护长居→照护 */
export function itemShortName(item: AgentServiceItemRel): string {
  if (item.itemSubtype === 2) return '长居';
  if (item.itemSubtype === 3) return '照护';
  if (item.itemSubtype === 1) return '旅居';
  return item.itemName || '服务';
}

/** 次数与周期文案：如 "旅居6次/年" / "长居3次/权益期" */
export function quotaText(item: AgentServiceItemRel): string {
  const unit = item.quotaType === 1 ? '次/权益期' : '次/年';
  return `${itemShortName(item)}${item.quantity}${unit}`;
}

/** 入住权标签列表：保证入住 / 优先入住 / 优惠(9折) */
export function rightTags(item: AgentServiceItemRel): string[] {
  const tags: string[] = [];
  if (item.admissionGuaranteed === 1) tags.push('保证入住');
  if (item.admissionPriority === 1) tags.push('优先入住');
  if (item.admissionDiscount === 1) {
    tags.push(item.discountRate ? `优惠${item.discountRate / 10}折` : '优惠入住');
  }
  return tags;
}

/** 权益人构成文案：如 "本人+配偶+父母2席" / "本人" */
export function holderText(equity: AgentEquityConfig): string {
  const rule = equity.holderRule;
  if (!rule) return `${equity.personCount ?? 1}人`;
  const parts = ['本人'];
  if (rule.spouse === 1) parts.push('配偶');
  if (rule.parent > 0) parts.push(`父母${rule.parent}席`);
  return parts.join('+');
}

/** 期限文案：终身有效 / 365天 */
export function validityText(equity: AgentEquityConfig): string {
  return equity.validityType === 2 ? '终身有效' : `${equity.validDays ?? 365}天`;
}

/** 网络范围摘要：全部机构 / N家机构·M个房型 */
export function networkText(item: AgentServiceItemRel): string {
  const scope = item.networkScope;
  if (!scope || scope.mode !== 'custom' || !scope.parks?.length) return '业态全部机构';
  const rooms = scope.parks.reduce((s, p) => s + (p.roomTypeCodes?.length || 0), 0);
  const base = `${scope.parks.length}家机构`;
  return rooms > 0 ? `${base}·${rooms}个房型` : base;
}

/** 随心住使用规则一行摘要 */
export function usageBrief(item: AgentServiceItemRel): string {
  const u = item.usageRule;
  if (!u) return '';
  const parts: string[] = [];
  if (u.maxDaysPerUse) parts.push(`每次${u.maxDaysPerUse}天${u.maxNightsPerUse || 0}晚`);
  if (u.maxRoomsPerUse) parts.push(`${u.maxRoomsPerUse}间房`);
  if (u.maxGuestsPerUse) parts.push(`每间可住${u.maxGuestsPerUse}人`);
  if (u.advanceBookDays) parts.push(`提前${u.advanceBookDays}天预订`);
  if (u.depositAmount) parts.push(`预定金${u.depositAmount}元`);
  if (u.blackoutType === 'spring_festival' && u.blackoutDays) parts.push(`春节${u.blackoutDays}天不可住`);
  if (u.requireBeneficiaryCheckIn) parts.push('本人到场');
  return parts.join(' · ');
}

// ===== 页面展示板块工具 =====

/** 板块类型选项（与 admin GOODS_DISPLAY_BLOCK_TYPE_OPTIONS 一致） */
export const GOODS_BLOCK_TYPE_OPTIONS: { label: string; value: string }[] = [
  { label: '产品介绍', value: 'product_intro' },
  { label: '权益详解', value: 'rights_detail' },
  { label: '服务流程', value: 'service_flow' },
  { label: '常见问题', value: 'faq' },
  { label: '购买须知', value: 'purchase_terms' },
  { label: '自定义', value: 'custom' }
];

/** 板块标题：未自定义标题时按类型映射默认名 */
export function blockTitleOf(block: GoodsDisplayBlock): string {
  if (block.blockTitle) return block.blockTitle;
  const hit = GOODS_BLOCK_TYPE_OPTIONS.find((o) => o.value === block.blockType);
  return hit ? hit.label : (block.blockType || '详情');
}

/** 解析 JSON 数组字符串 → string[]（容错非数组） */
export function parseImagesArr(s?: string): string[] {
  if (!s) return [];
  try {
    const arr = JSON.parse(s);
    return Array.isArray(arr) ? arr.map(String).filter(Boolean) : [];
  } catch {
    return [];
  }
}

/** 解析展示配置 JSON 字符串 → {banners, thumbnail}（容错） */
export function parseDisplayConfig(raw?: string): GoodsDisplayConfig {
  if (!raw) return { banners: [], thumbnail: '' };
  try {
    const parsed = JSON.parse(raw);
    return {
      banners: Array.isArray(parsed.banners) ? parsed.banners.map(String).filter(Boolean) : [],
      thumbnail: parsed.thumbnail ? String(parsed.thumbnail) : ''
    };
  } catch {
    return { banners: [], thumbnail: '' };
  }
}

/** 富文本去标签纯文本（板块正文摘要用） */
export function stripHtml(html?: string): string {
  if (!html) return '';
  return html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim();
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

// ===== 工具实例 =====

/** 工具实例（对齐后端 ToolInfoVO，GET /agent-api/tools） */
export interface ToolInfo {
  id?: number;
  toolCode: string;
  toolName: string;
  /** pension/gap/ai_creator/ai_qa */
  toolType?: string;
  toolDesc?: string;
  icon?: string;
  entryPath: string;
  /** 工具实例基础配置 JSON */
  configJson?: string;
  visibleScope?: string;
  sortOrder?: number;
  status?: number;
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

// ===== AI 问答 =====

/** 引用出处（对齐后端 QaCitation） */
export interface QaCitation {
  text: string;
  score?: number;
  repoId?: number;
  repoName?: string;
  docId?: string;
  docName?: string;
}

/** 会话消息（对齐后端 QaMessage） */
export interface QaMessage {
  id: number;
  sessionCode: string;
  role: 'user' | 'assistant';
  content: string;
  citations?: QaCitation[];
}

/** AI 问答会话（对齐后端 QaSession） */
export interface QaSession {
  id: number;
  sessionCode: string;
  configId: number;
  configCode: string;
  personaName: string;
  title: string;
  messageCount: number;
  lastMessageAt?: string;
  createdAt?: string;
}

/** AI 问答助手人物（对齐后端 QaConfig） */
export interface QaConfig {
  id: number;
  configCode: string;
  personaName: string;
  icon?: string;
  iconColor?: string;
  systemPrompt: string;
  welcomeMsg?: string;
  recommendQuestions?: string[];
  repoIds?: number[];
}

/** AI 问答单轮结果（POST /agent-api/tools/qa/chat） */
export interface QaChatResult {
  answer: string;
  citations?: QaCitation[];
  sessionCode: string;
}
