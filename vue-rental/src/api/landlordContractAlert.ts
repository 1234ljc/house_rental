import request from '@/utils/request'

// 获取合同到期统计
export const getAlertStatsApi = () => {
  return request.get('/landlord/contract-alert/stats')
}

// 获取到期合同列表
export const getAlertListApi = (params: any) => {
  return request.get('/landlord/contract-alert/list', { params })
}

// 获取到期日历数据
export const getAlertCalendarApi = (year: number, month: number) => {
  return request.get('/landlord/contract-alert/calendar', { params: { year, month } })
}

// 获取到期趋势
export const getAlertTrendApi = () => {
  return request.get('/landlord/contract-alert/trend')
}

// 发送到期提醒
export const sendNotifyApi = (contractId: number) => {
  return request.post(`/landlord/contract-alert/notify/${contractId}`)
}
