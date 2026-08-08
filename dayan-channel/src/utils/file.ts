/**
 * channel 端 formatFileUrl：将 DB key 转为 channel-api preview URL。
 * http 开头的原样返回（兼容历史数据）。
 */
export function formatFileUrl(value: string | undefined | null): string {
  if (!value) return ''
  if (value.startsWith('http://') || value.startsWith('https://')) return value
  return `/channel-api/v1/files/preview/${value}`
}
