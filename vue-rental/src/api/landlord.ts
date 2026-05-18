import request from '@/utils/request'
import type { LandlordStats, LandlordTodos, HouseIncomeRank, IncomeTrend } from '@/types/landlord'

// 获取统计卡片数据
export const getLandlordStatsApi = () => {
  return request.get<LandlordStats>('/landlord/dashboard/stats')
}

// 获取待办事项
export const getLandlordTodosApi = () => {
  return request.get<LandlordTodos>('/landlord/dashboard/todos')
}

// 获取近30天收入趋势
export const getIncomeTrendApi = () => {
  return request.get<IncomeTrend[]>('/landlord/dashboard/income-trend')
}

// 获取房源收入排行
export const getHouseIncomeRankApi = () => {
  return request.get<HouseIncomeRank[]>('/landlord/dashboard/house-income-rank')
}

// 获取房源运营数据分析
export const getHouseAnalyticsApi = () => {
  return request.get('/landlord/dashboard/house-analytics')
}
