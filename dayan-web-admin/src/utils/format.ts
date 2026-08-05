/**
 * 通用展示格式化工具。
 *
 * - 时间：后端返回 ISO 字符串（如 "2026-08-05T18:59:27"），
 *   统一格式化为 "YYYY-MM-DD HH:mm:ss"；空值返回占位符。
 * - 金额：统一千分位 + ¥ 前缀，避免各页面格式不一致。
 * - 枚举/状态：原始值映射到中文标签的通用查找。
 */

/** 空值占位（统一用 em dash，与表格默认空值视觉一致） */
const EMPTY_PLACEHOLDER = '—'

/**
 * 将后端 ISO 时间字符串格式化为 "YYYY-MM-DD HH:mm:ss"。
 *
 * 兼容三种输入：
 * - 标准 ISO（含 T）：2026-08-05T18:59:27
 * - 带毫秒/时区：2026-08-05T18:59:27.123Z
 * - 已经是 "YYYY-MM-DD HH:mm:ss" 的字符串（直接返回）
 *
 * 解析失败或空值时返回 placeholder（默认 "—"）。
 */
export function formatDateTime(value: unknown, placeholder = EMPTY_PLACEHOLDER): string {
  if (value == null || value === '') return placeholder
  const str = String(value).trim()
  // 已是目标格式直接放行，避免多余处理
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(str)) return str
  // 仅日期
  if (/^\d{4}-\d{2}-\d{2}$/.test(str)) return str
  const d = new Date(str)
  if (Number.isNaN(d.getTime())) return placeholder
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/**
 * 仅日期格式化（YYYY-MM-DD）。用于生日、生产日期等不需要时分的场景。
 */
export function formatDate(value: unknown, placeholder = EMPTY_PLACEHOLDER): string {
  if (value == null || value === '') return placeholder
  const str = String(value).trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(str)) return str
  const d = new Date(str)
  if (Number.isNaN(d.getTime())) return placeholder
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/**
 * 金额格式化：千分位 + ¥ 前缀。
 *
 * - 正数：¥12,000.00
 * - 负数：-¥1,200.00
 * - 空值：返回 placeholder
 *
 * @param value 数字或数字字符串
 * @param withSymbol 是否带 ¥ 前缀（默认 true）
 */
export function formatMoney(value: unknown, withSymbol = true, placeholder = EMPTY_PLACEHOLDER): string {
  if (value == null || value === '') return placeholder
  const n = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(n)) return placeholder
  const sign = n < 0 ? '-' : ''
  // toLocaleString 不一定可靠（依赖宿主 locale），这里手写千分位
  const fixed = Math.abs(n).toFixed(2)
  const [intPart, decPart] = fixed.split('.')
  const intWithSep = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  const body = `${intWithSep}.${decPart}`
  return withSymbol ? `${sign}¥${body}` : `${sign}${body}`
}

/**
 * 枚举值 -> 中文标签通用查找。
 *
 * @param value 原始值（数字或字符串）
 * @param options 枚举选项数组，形如 [{ label: '启用', value: 1 }]
 * @param fallback 找不到时的回显（默认返回原值的字符串形式，避免空）
 */
export function formatOption<T>(
  value: T,
  options: ReadonlyArray<{ label: string; value: T }>,
  fallback: string = String(value ?? EMPTY_PLACEHOLDER)
): string {
  const hit = options.find((o) => o.value === value)
  return hit ? hit.label : fallback
}
