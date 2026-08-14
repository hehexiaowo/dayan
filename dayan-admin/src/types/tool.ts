/**
 * 工具域相关类型（获客工具）。
 *
 * 字段对齐后端 com.dayan.tool.entity.ToolInfo / ToolInfoVO。
 * - Integer → number
 * - LocalDateTime → string
 * - config 为 JSON 字符串（原样透传，端上自行解析）
 */
import type { PageQuery } from '@/types/common'

/**
 * 工具类型（tool_info.tool_type）。
 * 1=计算器 / 2=测评 / 3=表单 / 4=其他。
 */
export const TOOL_TYPE_OPTIONS = [
  { label: '计算器', value: 1 },
  { label: '测评', value: 2 },
  { label: '表单', value: 3 },
  { label: '其他', value: 4 }
] as const

/** 工具类型标签文本。 */
export function toolTypeLabel(v?: number): string {
  const found = TOOL_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : v != null ? String(v) : '--'
}

/**
 * 可见端（tool_info.visible_scope，逗号分隔多值）。
 */
export const TOOL_END_OPTIONS = [
  { label: '代理人端', value: 'agent' },
  { label: '客户端', value: 'client' }
] as const

/**
 * 工具实体（后端 ToolInfoVO）。
 */
export interface ToolInfo {
  id?: number
  /** 工具编码（服务端生成，TL 前缀） */
  toolCode?: string
  /** 工具名称（必填） */
  toolName: string
  /** 工具类型：1计算器/2测评/3表单/4其他 */
  toolType?: number
  /** 工具简介 */
  toolDesc?: string
  /** 图标（文字或图标名） */
  icon?: string
  /** 入口路径（端上页面路径，必填） */
  entryPath: string
  /** 工具配置（JSON 字符串，如 {"color":"orange"}） */
  config?: string
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
  toolType?: number
  status?: number
}
