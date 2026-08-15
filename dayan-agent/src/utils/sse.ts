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
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''
    let eventName = ''
    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        const data = line.slice(5).trim()
        if (data) handlers.onEvent(eventName || 'message', data)
        eventName = ''
      }
    }
  }
}
