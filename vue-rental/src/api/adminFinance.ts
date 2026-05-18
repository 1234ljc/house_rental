import request from '@/utils/request'

// ==================== 订单管理 ====================

// 获取订单列表
export function getOrderList(params: {
  paymentStatus?: number
  orderType?: number
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}) {
  return request.get('/admin/finance/order/list', { params })
}

// 获取订单详情
export function getOrderDetail(id: number) {
  return request.get(`/admin/finance/order/${id}`)
}

// 获取订单统计
export function getOrderStats() {
  return request.get('/admin/finance/order/stats')
}

// ==================== 收入分析 ====================

// 获取收入概览
export function getIncomeOverview() {
  return request.get('/admin/finance/income/overview')
}

// 获取收入趋势
export function getIncomeTrend(type: string = 'day', days: number = 30) {
  return request.get('/admin/finance/income/trend', { params: { type, days } })
}

// 获取收入构成
export function getIncomeComposition() {
  return request.get('/admin/finance/income/composition')
}

// 获取支付方式分布
export function getPaymentMethodDistribution() {
  return request.get('/admin/finance/income/payment-method')
}

// 导出财务报表
export function exportFinanceReport(params: { startDate?: string; endDate?: string }) {
  return request.get('/admin/finance/income/export', { params })
}
