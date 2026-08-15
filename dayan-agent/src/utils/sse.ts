/**
 * H5 端 SSE 流式读取（fetch + ReadableStream）。
 * 仅 H5 可用（小程序无 fetch 流，调用方条件编译降级非流式接口）。
 */
export interface SseHandlers {
  onEvent: (name: string, data: string) => void
}

/** POST JSON 并逐事件回调 SSE（event:name\ndata:json 格式，Spring SseEmitter 输出） */
export async function postSseStream(url: string, body: Record<string, unknown>, handlers: SseHandlers): Promise<void> {
  const token = uni.getStorageSync('agent_token')
  const resp = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Agent-Token': token },
    body: JSON.stringify(body)
  })
  if (!resp.ok || !resp.body) {
    throw new Error(`HTTP ${resp.status}`)
  }
  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    // SSE 事件以空行分帧；一个事件可含多个 data: 行（Spring 发送长 JSON 时按行拆分），
    // 须聚合完整帧后再触发一次回调，否则逐行触发会把长 JSON 拆碎导致解析失败
    const frames = buffer.split('\n\n')
    buffer = frames.pop() ?? ''
    for (const frame of frames) {
      if (!frame.trim()) continue
      let eventName = ''
      const dataLines: string[] = []
      for (const line of frame.split('\n')) {
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).replace(/^ /, ''))
        }
      }
      if (dataLines.length) {
        handlers.onEvent(eventName || 'message', dataLines.join('\n'))
      }
    }
  }
}
