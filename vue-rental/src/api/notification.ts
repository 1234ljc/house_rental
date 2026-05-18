import request from '@/utils/request'

// 获取未读消息数量
export const getUnreadCountApi = () => {
  return request.get('/notification/unread-count')
}

// 获取消息列表
export const getNotificationListApi = (params: {
  notifyType?: number
  isRead?: number
  page?: number
  size?: number
}) => {
  return request.get('/notification/list', { params })
}

// 获取消息统计
export const getNotificationStatsApi = () => {
  return request.get('/notification/stats')
}

// 标记单条消息为已读
export const markAsReadApi = (notifyId: number) => {
  return request.put(`/notification/read/${notifyId}`)
}

// 标记所有消息为已读
export const markAllAsReadApi = () => {
  return request.put('/notification/read-all')
}

// 删除消息
export const deleteNotificationApi = (notifyId: number) => {
  return request.delete(`/notification/${notifyId}`)
}

// 清除已读消息
export const clearReadNotificationsApi = () => {
  return request.delete('/notification/clear-read')
}

// 获取最新未读消息（用于Dashboard展示）
export const getRecentMessagesApi = (size: number = 5) => {
  return request.get('/notification/list', { params: { isRead: 0, page: 1, size } })
}
