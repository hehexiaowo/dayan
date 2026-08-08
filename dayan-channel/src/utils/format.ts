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

/** 通用空值显示 */
export function dashIfEmpty(val: unknown): string {
  return val == null || val === '' ? '--' : String(val)
}
