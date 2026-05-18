import request from '@/utils/request'

// ==================== 业务数据统计 ====================

export function getUserStats() {
  return request.get('/admin/statistics/business/user')
}

export function getHouseStats() {
  return request.get('/admin/statistics/business/house')
}

export function getRentalStats() {
  return request.get('/admin/statistics/business/rental')
}

export function getFinanceStats() {
  return request.get('/admin/statistics/business/finance')
}

// ==================== 数据分析报表 ====================

export function getUserProfile() {
  return request.get('/admin/statistics/report/user-profile')
}

export function getHouseQuality() {
  return request.get('/admin/statistics/report/house-quality')
}

export function getRentalBehavior() {
  return request.get('/admin/statistics/report/rental-behavior')
}

export function getIncomeForecast() {
  return request.get('/admin/statistics/report/income-forecast')
}

export function exportData(params: { type: string; startDate?: string; endDate?: string }) {
  return request.get('/admin/statistics/report/export', { params })
}
