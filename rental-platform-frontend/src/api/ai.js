import request from './request'
export const aiChat = (data) => request.post('/ai/chat', data)
export const getConversations = () => request.get('/ai/conversations')
export const getConversationDetail = (id) => request.get(`/ai/conversations/${id}`)
export const deleteConversation = (id) => request.delete(`/ai/conversations/${id}`)
