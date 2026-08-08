/**
 * 将 DB 中存的 key（或历史 http URL）转为可访问的 URL。
 * - http/https 开头的值（历史手填数据）：原样返回，兼容旧数据
 * - 纯 key（如 goods/day001/2026/08/08/abc.jpg）：拼接 admin preview 前缀
 * - 空值：返回空字符串
 */
export function formatFileUrl(value: string | undefined | null): string {
  if (!value) return ''
  if (value.startsWith('http://') || value.startsWith('https://')) return value
  return `/admin-api/v1/files/preview/${value}`
}
