import request from './request'
export const aiChat = (data) => request.post('/ai/chat', data)
export const getConversations = () => request.get('/ai/conversations')
export const getConversationDetail = (id) => request.get(`/ai/conversations/${id}`)
export const deleteConversation = (id) => request.delete(`/ai/conversations/${id}`)

/**
 * 流式对话（SSE）：POST /ai/chat/stream，用 fetch + ReadableStream 逐段读取。
 * 后端事件协议（data: JSON）：
 *   {"type":"meta","conversationId":"..."}  会话开始
 *   {"type":"delta","content":"..."}        增量内容
 *   {"type":"done"}                          完成（已落库）
 *   {"type":"error","message":"..."}         服务异常
 * 通过回调透出事件，返回 Promise（流读取完毕时 resolve）。
 */
export const aiChatStream = (data, { onMeta, onDelta, onDone, onError } = {}) => {
  const token = localStorage.getItem('token')
  return fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(data)
  }).then(async (response) => {
    if (!response.ok) {
      const message = response.status === 401 ? '登录已过期，请重新登录' : `请求失败（${response.status}）`
      onError?.(message)
      throw new Error(message)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // SSE 事件以空行分隔，末尾可能是不完整的半包，留到下一轮
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''
      for (const event of events) {
        const dataLine = event.split('\n').find((line) => line.startsWith('data:'))
        if (!dataLine) continue
        let payload
        try {
          payload = JSON.parse(dataLine.slice(5).trim())
        } catch {
          continue
        }
        if (payload.type === 'meta') {
          onMeta?.(payload.conversationId)
        } else if (payload.type === 'delta') {
          onDelta?.(payload.content)
        } else if (payload.type === 'done') {
          onDone?.()
        } else if (payload.type === 'error') {
          onError?.(payload.message || 'AI服务调用失败')
        }
      }
    }
  })
}
