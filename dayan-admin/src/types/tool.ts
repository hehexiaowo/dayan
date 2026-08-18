/**
 * 工具域相关类型（工具配置）。
 *
 * 字段对齐后端 com.dayan.tool.entity.ToolInfo / ToolInfoVO。
 * - Integer → number
 * - LocalDateTime → string
 * - configJson 为 JSON 字符串（原样透传，端上自行解析）
 */
import type { PageQuery } from '@/types/common'

/**
 * 工具实例类型（tool_info.tool_type）。
 * 四类固定：pension 社保养老计算器 / gap 养老缺口计算器 / ai_creator AI 创作 / ai_qa 你问我答。
 */
export const TOOL_TYPE_OPTIONS = [
  { label: '社保养老计算器', value: 'pension' },
  { label: '养老缺口计算器', value: 'gap' },
  { label: 'AI 创作', value: 'ai_creator' },
  { label: '你问我答', value: 'ai_qa' }
] as const

/** 工具类型标签文本。 */
export function toolTypeLabel(v?: string): string {
  const found = TOOL_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? v : '--'
}

/**
 * 可见端（tool_info.visible_scope，逗号分隔多值）。
 */
export const TOOL_END_OPTIONS = [
  { label: '代理人端', value: 'agent' },
  { label: '客户端', value: 'client' }
] as const

/**
 * 工具实例实体（后端 ToolInfoVO）。
 */
export interface ToolInfo {
  id?: number
  /** 工具编码（服务端生成，TL 前缀） */
  toolCode?: string
  /** 工具名称（必填） */
  toolName: string
  /** 工具类型：pension/gap/ai_creator/ai_qa */
  toolType?: string
  /** 工具简介 */
  toolDesc?: string
  /** 图标（文字或图标名） */
  icon?: string
  /** 入口路径（端上页面路径，必填） */
  entryPath: string
  /** 工具配置 JSON（按类型承载提示词/默认值等） */
  configJson?: string
  /** 可见端（逗号分隔：agent/client） */
  visibleScope?: string
  /** 排序号 */
  sortOrder?: number
  /** 状态：1启用 0禁用 */
  status?: number
  /** 备注 */
  remark?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 工具分页查询参数。
 */
export interface ToolInfoQuery extends PageQuery {
  toolCode?: string
  toolName?: string
  toolType?: string
  status?: number
}
