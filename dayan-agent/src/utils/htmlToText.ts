/**
 * HTML → 纯文本（复制/编辑展示用；块级标签转换行，li 加 "- " 前缀）。
 */
export function htmlToText(html: string): string {
  return html
    .replace(/<li[^>]*>/gi, '- ')
    .replace(/<\/(p|h1|h2|h3|h4|h5|h6|li|div)>/gi, '\n')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}
