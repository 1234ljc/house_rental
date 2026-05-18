import request from '@/utils/request'

// 获取租房概览数据
export const getOverviewApi = () => request.get('/tenant/statistics/overview')

// 获取月度支出趋势
export const getExpenseTrendApi = () => request.get('/tenant/statistics/expense-trend')

// 获取支出分类统计
export const getExpenseCategoryApi = () => request.get('/tenant/statistics/expense-category')

// 获取租房历史
export const getRentHistoryApi = () => request.get('/tenant/statistics/rent-history')

// 获取行为统计
export const getBehaviorStatsApi = () => request.get('/tenant/statistics/behavior')
