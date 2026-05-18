import request from '@/utils/request'

// 获取会话列表
export const getChatSessionsApi = (params: { page?: number; size?: number }) => {
  return request.get('/chat/sessions', { params })
}

// 获取未读消息总数
export const getChatUnreadCountApi = () => {
  return request.get('/chat/unread-count')
}

// 创建或获取会话
export const createOrGetSessionApi = (data: { houseId: number; tenantId?: number }) => {
  return request.post('/chat/session', data)
}

// 获取会话详情
export const getSessionDetailApi = (sessionId: number) => {
  return request.get(`/chat/session/${sessionId}`)
}

// 获取消息列表
export const getChatMessagesApi = (sessionId: number, params: { page?: number; size?: number }) => {
  return request.get(`/chat/messages/${sessionId}`, { params })
}

// 发送消息（HTTP方式，WebSocket不可用时的备用）
export const sendMessageApi = (data: {
  sessionId: number
  content: string
  messageType?: number
  fileUrl?: string
}) => {
  return request.post('/chat/send', data)
}

// 标记消息已读
export const markChatReadApi = (sessionId: number) => {
  return request.put(`/chat/read/${sessionId}`)
}

// 上传聊天文件
export const uploadChatFileApi = (formData: FormData) => {
  return request.post('/chat/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
