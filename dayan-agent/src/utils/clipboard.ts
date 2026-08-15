/**
 * 跨端复制文本（H5 clipboard API + execCommand 降级；小程序 uni.setClipboardData）。
 */
export function copyText(text: string): Promise<void> {
  return new Promise((resolve, reject) => {
    // #ifdef H5
    const fallback = () => {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      try {
        if (document.execCommand('copy')) resolve()
        else reject(new Error('copy failed'))
      } catch (e) {
        reject(e)
      } finally {
        document.body.removeChild(ta)
      }
    }
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text).then(resolve, fallback)
    } else {
      fallback()
    }
    // #endif
    // #ifndef H5
    uni.setClipboardData({
      data: text,
      success: () => resolve(),
      fail: (e) => reject(e)
    })
    // #endif
  })
}
