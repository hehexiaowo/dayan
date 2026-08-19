import DOMPurify from 'dompurify'

/**
 * 对 HTML 内容进行 XSS 消毒，用于 <rich-text> 等需要渲染 HTML 的场景。
 * 移除所有潜在的恶意脚本、事件处理器等。
 */
export function sanitizeHtml(html: string | undefined | null): string {
  if (!html) return ''
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'em', 'u', 's', 'span', 'div',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'ul', 'ol', 'li', 'a', 'img',
      'table', 'thead', 'tbody', 'tr', 'td', 'th',
      'blockquote', 'pre', 'code',
    ],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class', 'style', 'width', 'height'],
  })
}
