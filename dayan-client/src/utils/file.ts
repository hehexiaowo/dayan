/**
 * 将 DB 中存的 key（或历史 http URL）转为可访问的 URL。
 * - http/https 开头的值（历史手填数据）：原样返回
 * - 纯 key（如 park/day001/2026/08/08/abc.jpg）：拼接 client preview 前缀
 * - 空值：返回空字符串
 */
export function formatFileUrl(value: string | undefined | null): string {
  if (!value) return '';
  if (value.startsWith('http://') || value.startsWith('https://')) return value;
  return `/client-api/v1/files/preview/${value}`;
}

/**
 * 将 JSON 字符串图片数组（如 '["a.jpg","b.jpg"]'）解析为可访问 URL 数组。
 * 后端 images 字段存的是 JSON 字符串，需解析后逐个 formatFileUrl。
 */
export function parseImageList(value: string | undefined | null): string[] {
  if (!value) return [];
  try {
    const arr = JSON.parse(value);
    if (Array.isArray(arr)) {
      return arr.map((v: string) => formatFileUrl(v)).filter(Boolean);
    }
  } catch {
    // 非 JSON：可能逗号分隔或单值
    return value
      .split(',')
      .map((v) => v.trim())
      .filter(Boolean)
      .map((v) => formatFileUrl(v));
  }
  return [];
}
