/**
 * 通用格式化工具（跨页面共享）。
 * 偿还增量3 技术债 #2：statusTagType/formatAmount 在多页重复。
 */

/** 订单/通用状态 → el-tag type 映射 */
export function statusTagType(status: number | undefined | null): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (status === undefined || status === null) return 'info'
  const map: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'warning',   // 待处理/待支付
    1: '',          // 进行中/已激活
    2: 'success',   // 完成/已通过
    3: 'success',   // 完成
    4: 'info',      // 关闭/下线
    5: 'danger',    // 取消
    6: 'warning'    // 退款中/挂起
  }
  return map[status] ?? 'info'
}

/** 金额格式化：分/元 → 2 位小数字符串 */
export function formatAmount(val: number | string | null | undefined): string {
  if (val == null) return '--'
  return Number(val).toFixed(2)
}

/** 金额格式化：千分位 + ¥ 前缀（与 admin 端 formatMoney 对齐，用于表格金额列） */
export function formatMoney(val: number | string | null | undefined): string {
  if (val == null || val === '') return '--'
  const n = Number(val)
  if (Number.isNaN(n)) return '--'
  return `¥${n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/**
 * 将后端 ISO 时间字符串格式化为 "YYYY-MM-DD HH:mm:ss"。
 *
 * 兼容 ISO 含 T（2026-08-05T18:59:27）、带毫秒/时区、已是目标格式三种输入；
 * 解析失败或空值返回 '--'。
 */
export function formatDateTime(value: unknown): string {
  if (value == null || value === '') return '--'
  const str = String(value).trim()
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(str)) return str
  if (/^\d{4}-\d{2}-\d{2}$/.test(str)) return str
  const d = new Date(str)
  if (Number.isNaN(d.getTime())) return '--'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 通用空值显示 */
export function dashIfEmpty(val: unknown): string {
  return val == null || val === '' ? '--' : String(val)
}
