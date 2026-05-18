import request from '@/utils/request'

// 获取财务统计
export const getFinanceStatsApi = () => {
  return request.get('/landlord/finance/stats')
}

// 获取租金收取列表
export const getRentListApi = (params: {
  status?: number
  houseId?: number
  page?: number
  size?: number
}) => {
  return request.get('/landlord/finance/rent/list', { params })
}

// 发送催缴提醒
export const sendReminderApi = (orderId: number) => {
  return request.post(`/landlord/finance/rent/remind/${orderId}`)
}

// 获取押金管理列表
export const getDepositListApi = (params: {
  status?: number
  page?: number
  size?: number
}) => {
  return request.get('/landlord/finance/deposit/list', { params })
}

// 处理押金退还
export const processDepositRefundApi = (orderId: number, data: {
  action: number
  refundAmount?: number
  remark?: string
}) => {
  return request.post(`/landlord/finance/deposit/process/${orderId}`, data)
}

// 获取收入趋势
export const getIncomeTrendApi = () => {
  return request.get('/landlord/finance/income/trend')
}

// 获取房源收入排行
export const getHouseIncomeRankApi = () => {
  return request.get('/landlord/finance/income/rank')
}

// 获取收入统计
export const getIncomeSummaryApi = (type: string = 'month') => {
  return request.get('/landlord/finance/income/summary', { params: { type } })
}

// 导出财务报表
export const exportFinanceReportApi = (params: {
  startDate?: string
  endDate?: string
}) => {
  return request.get('/landlord/finance/export', { params })
}
