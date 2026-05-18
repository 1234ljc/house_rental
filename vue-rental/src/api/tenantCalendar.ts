import request from '@/utils/request'

// 获取日历事件
export const getCalendarEventsApi = (params?: { startDate?: string; endDate?: string }) => {
  return request.get('/tenant/calendar/events', { params })
}

// 获取合同时间线
export const getContractTimelineApi = () => {
  return request.get('/tenant/calendar/timeline')
}

// 获取提醒列表
export const getRemindersApi = () => {
  return request.get('/tenant/calendar/reminders')
}

// 获取日历统计
export const getCalendarStatsApi = () => {
  return request.get('/tenant/calendar/stats')
}
