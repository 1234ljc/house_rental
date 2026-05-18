import request from '@/utils/request'

// 获取总览统计
export const getOverviewApi = () => request.get('/landlord/statistics/overview')

// 获取收入趋势（近12个月）
export const getIncomeTrendApi = () => request.get('/landlord/statistics/income/trend')

// 获取房源热度排行
export const getHouseRankingApi = () => request.get('/landlord/statistics/house/ranking')


// 获取租客画像分析
export const getTenantAnalysisApi = () => request.get('/landlord/statistics/tenant/analysis')

// 获取收入来源分布
export const getIncomeDistributionApi = () => request.get('/landlord/statistics/income/distribution')

// 获取合同到期预警
export const getExpiringContractsApi = () => request.get('/landlord/statistics/contract/expiring')
