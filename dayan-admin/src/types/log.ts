/**
 * 系统日志相关类型（四端分表）。
 *
 * 字段逐字对齐后端 com.dayan.system.entity.SystemLogEntry（含 BaseEntity 的 createdAt 等）。
 * 后端按 source（organ/channel/agent/client）路由到 system_log_* 四张表，
 * 登录/登出事件以 module='auth', action='login'/'logout' 入表。
 */

/** 日志来源（四端） */
export type LogSource = 'organ' | 'channel' | 'agent' | 'client'

/** 日志来源选项（四端系统命名：大雁核心/渠道核心/养老宝典/雁栖康养） */
export const LOG_SOURCE_OPTIONS: { label: string; value: LogSource }[] = [
  { label: '大雁核心', value: 'organ' },
  { label: '渠道核心', value: 'channel' },
  { label: '养老宝典', value: 'agent' },
  { label: '雁栖康养', value: 'client' }
]

/** 日志来源标签文案 */
export function logSourceLabel(source?: string): string {
  return LOG_SOURCE_OPTIONS.find(o => o.value === source)?.label ?? String(source ?? '')
}

/**
 * 操作模块英文 → 中文映射。
 * 存量日志 module 中英文混杂（切面早期写英文、现写中文），展示层统一为中文；
 * 未覆盖的英文值回退原样显示。
 */
const MODULE_ALIASES: Record<string, string> = {
  auth: '认证',
  equity: '权益管理',
  order: '订单管理',
  goods: '商品管理',
  park: '机构管理',
  scene: '场景管理',
  content: '内容管理',
  course: '课程管理',
  channel: '渠道管理',
  client: '客户管理',
  agent: '代理人管理',
  finance: '财务管理',
  supplier: '供应商管理',
  distributor: '分销商管理',
  lead: '线索管理',
  butler: '管家管理',
  session: '服务会话',
  knowledge: '知识库',
  message: '消息管理',
  dict: '字典管理',
  menu: '菜单管理',
  role: '角色管理',
  account: '账号管理',
  employee: '员工管理',
  organ: '机构管理',
  config: '系统配置',
  log: '日志管理',
  statemachine: '状态机',
  asset: '素材管理',
  upload: '文件上传'
}

/** 模块展示：中文原样返回，英文按映射转中文，未知回退原值 */
export function logModuleLabel(module?: string): string {
  if (!module) return '—'
  return MODULE_ALIASES[module] ?? module
}

/**
 * 操作动作英文 → 中文映射（同 module，展示层统一）。
 */
const ACTION_ALIASES: Record<string, string> = {
  login: '登录',
  logout: '登出',
  create: '新增',
  update: '修改',
  delete: '删除',
  audit: '审核',
  export: '导出',
  import: '导入',
  shelf: '上架',
  unshelf: '下架',
  submit: '提交审核',
  assign: '分配',
  save: '保存',
  authorize: '授权',
  reset: '重置',
  cancel: '取消',
  confirm: '确认',
  upload: '上传',
  download: '下载',
  activate: '激活',
  outbound: '出库',
  stockin: '入库',
  change: '变更',
  rollback: '回滚',
  sync: '同步',
  publish: '发布',
  revoke: '撤回',
  bind: '绑定',
  unbind: '解绑'
}

/** 动作展示：中文原样返回，英文按映射转中文，未知回退原值 */
export function logActionLabel(action?: string): string {
  if (!action) return '—'
  return ACTION_ALIASES[action] ?? action
}

/**
 * 账号类型英文 → 中文映射（详情"账号类型"展示统一，四端按系统命名）。
 */
const ACCOUNT_TYPE_ALIASES: Record<string, string> = {
  admin: '大雁核心',
  system: '系统',
  supplier: '供应商',
  distributor: '分销商',
  channel: '渠道核心',
  agent: '养老宝典',
  client: '雁栖康养',
  unknown: '未知'
}

/** 账号类型展示：未知回退原值 */
export function logAccountTypeLabel(type?: string): string {
  if (!type) return '—'
  return ACCOUNT_TYPE_ALIASES[type] ?? type
}

/**
 * 操作对象类型英文 → 中文映射（详情"操作对象"展示统一）。
 */
const TARGET_TYPE_ALIASES: Record<string, string> = {
  account: '账号',
  channel_info: '渠道',
  client_info: '客户',
  agent_info: '代理人',
  equity_depot: '权益仓库',
  equity_batch: '权益批次',
  order_equity: '养老权益订单',
  order_scene: '场景营销订单',
  order_course: '培训课程订单',
  order_sojourn: '旅游短居订单',
  goods_info: '商品',
  park_info: '养老机构',
  scene_info: '场景',
  content_info: '内容',
  course_info: '课程',
  supplier_info: '供应商',
  distributor_info: '分销商',
  butler_info: '管家',
  service_session: '服务会话',
  system_dict: '字典',
  system_message: '消息',
  system_config: '系统配置'
}

/** 操作对象类型展示：未知回退原值 */
export function logTargetTypeLabel(type?: string): string {
  if (!type) return '—'
  return TARGET_TYPE_ALIASES[type] ?? type
}

/**
 * 系统日志条目（后端 SystemLogEntry 实体）。
 *
 * 字段语义：
 * - accountType / accountCode / accountName：操作账号类型/编码/姓名
 * - module / action：操作模块 / 操作动作（登录登出为 auth/login、auth/logout）
 * - requestMethod / requestUrl / requestParams：请求方法 / URL / 参数（JSON 字符串，已脱敏）
 * - responseResult：响应结果（JSON 字符串，超长截断）
 * - resultStatus：结果状态，1=成功 / 0=失败
 * - duration：执行耗时（毫秒）
 * - ipAddress / userAgent / deviceType / os / browser：终端审计信息
 */
export interface SystemLog {
  id?: number
  /** 链路追踪 ID */
  traceId?: string
  /** 账号类型（admin/channel/agent/client/supplier/distributor） */
  accountType?: string
  /** 操作账号编码 */
  accountCode?: string
  /** 操作人姓名 */
  accountName?: string
  /** 操作模块 */
  module?: string
  /** 操作动作 */
  action?: string
  /** 操作描述 */
  actionDescription?: string
  /** 操作对象类型 */
  targetType?: string
  /** 操作对象编码 */
  targetCode?: string
  /** 操作对象描述 */
  targetDescription?: string
  /** 请求 URL */
  requestUrl?: string
  /** 请求方法（GET/POST/PUT/DELETE） */
  requestMethod?: string
  /** 请求参数（JSON 字符串，已脱敏；登录事件为 {"loginType","identity"}） */
  requestParams?: string
  /** 响应结果（JSON 字符串，超长截断） */
  responseResult?: string
  /** 响应状态码（0=成功，1=失败，与 resultStatus 同义但为 Int） */
  responseCode?: number
  /** 操作 IP 地址 */
  ipAddress?: string
  /** IP 归属地 */
  ipLocation?: string
  /** 浏览器 User-Agent */
  userAgent?: string
  /** 设备类型（pc/mobile/tablet） */
  deviceType?: string
  /** 操作系统 */
  os?: string
  /** 浏览器 */
  browser?: string
  /** 结果状态：1=成功 / 0=失败 */
  resultStatus?: number
  /** 错误信息（失败时） */
  errorMsg?: string
  /** 执行耗时（毫秒） */
  duration?: number
  /** 创建时间（即操作时间） */
  createdAt?: string
}

/** 系统日志分页查询参数 */
export interface SystemLogQuery {
  /** 日志来源（四端分表，必填） */
  source: LogSource
  /** 模块筛选（模糊匹配） */
  module?: string
  /** 操作账号编码筛选（精确匹配） */
  accountCode?: string
  /** 结果状态筛选：1=成功 / 0=失败 */
  resultStatus?: number
  /** 操作时间范围起始（ISO 字符串 yyyy-MM-ddTHH:mm:ss） */
  startTime?: string
  /** 操作时间范围结束（ISO 字符串 yyyy-MM-ddTHH:mm:ss） */
  endTime?: string
  current: number
  size: number
}

/** 结果状态选项 */
export const RESULT_STATUS_OPTIONS = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 }
] as const

/** 结果状态标签文案 */
export function resultStatusLabel(status?: number): string {
  if (status === 1) return '成功'
  if (status === 0) return '失败'
  return String(status ?? '')
}
