import request from '@/utils/request'

// 获取合同到期预警统计
export const getAlertStatsApi = () => request.get('/admin/contract-alert/stats')

// 获取到期合同列表
export const getAlertListApi = (params: any) => request.get('/admin/contract-alert/list', { params })

// 获取到期日历数据
export const getAlertCalendarApi = (year?: number, month?: number) => 
  request.get('/admin/contract-alert/calendar', { params: { year, month } })

// 获取到期趋势
export const getAlertTrendApi = () => request.get('/admin/contract-alert/trend')

// 发送到期提醒通知
export const sendNotifyApi = (contractId: number) => 
  request.post(`/admin/contract-alert/notify/${contractId}`)

// 批量发送到期提醒
export const batchNotifyApi = (alertType: number) => 
  request.post('/admin/contract-alert/batch-notify', { alertType })

// 标记合同为已到期
export const markExpiredApi = (contractId: number) => 
  request.post(`/admin/contract-alert/mark-expired/${contractId}`)

// 批量标记已过期合同
export const batchMarkExpiredApi = () => request.post('/admin/contract-alert/batch-mark-expired')
