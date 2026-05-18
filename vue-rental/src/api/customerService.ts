import request from '@/utils/request'

// ==================== 用户端接口 ====================

// 创建或获取客服会话
export const createCustomerServiceSessionApi = () => {
  return request.post('/customer-service/session')
}

// 获取客服会话详情
export const getCustomerServiceSessionApi = (sessionId: number) => {
  return request.get(`/customer-service/session/${sessionId}`)
}

// 获取客服消息列表
export const getCustomerServiceMessagesApi = (sessionId: number, params: { page?: number; size?: number }) => {
  return request.get(`/customer-service/messages/${sessionId}`, { params })
}

// 发送消息给客服
export const sendCustomerServiceMessageApi = (data: {
  sessionId: number
  content: string
  messageType?: number
  fileUrl?: string
}) => {
  return request.post('/customer-service/send', data)
}

// 获取客服未读消息数
export const getCustomerServiceUnreadApi = () => {
  return request.get('/customer-service/unread-count')
}

// 获取排队位置
export const getQueuePositionApi = () => {
  return request.get('/customer-service/queue-position')
}

// 用户主动结束客服会话
export const closeCustomerServiceByUserApi = (sessionId: number) => {
  return request.post(`/customer-service/close/${sessionId}`)
}

// ==================== 管理员端接口 ====================

// 获取客服会话列表
export const getAdminCustomerServiceSessionsApi = (params: { status?: number; page?: number; size?: number }) => {
  return request.get('/admin/customer-service/sessions', { params })
}

// 获取客服统计数据
export const getAdminCustomerServiceStatsApi = () => {
  return request.get('/admin/customer-service/statistics')
}

// 接入客服会话
export const acceptCustomerServiceApi = (sessionId: number) => {
  return request.post(`/admin/customer-service/accept/${sessionId}`)
}

// 获取会话消息列表（管理员）
export const getAdminCustomerServiceMessagesApi = (sessionId: number, params: { page?: number; size?: number }) => {
  return request.get(`/admin/customer-service/messages/${sessionId}`, { params })
}

// 发送消息给用户（管理员）
export const sendAdminCustomerServiceMessageApi = (data: {
  sessionId: number
  content: string
  messageType?: number
  fileUrl?: string
}) => {
  return request.post('/admin/customer-service/send', data)
}

// 结束客服会话
export const closeCustomerServiceApi = (sessionId: number) => {
  return request.post(`/admin/customer-service/close/${sessionId}`)
}

// 获取管理员未读消息数
export const getAdminCustomerServiceUnreadApi = () => {
  return request.get('/admin/customer-service/unread-count')
}
