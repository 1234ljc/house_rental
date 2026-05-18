import request from '@/utils/request'

// 获取租后服务统计概览
export const getAfterStatsApi = () => request.get('/admin/after/stats')

// 获取工单趋势
export const getAfterTrendApi = () => request.get('/admin/after/trend')

// 获取工单类型分布
export const getTypeDistributionApi = () => request.get('/admin/after/type-distribution')

// 获取工单列表
export const getAfterListApi = (params: any) => request.get('/admin/after/list', { params })

// 获取工单详情
export const getAfterDetailApi = (manageId: number) => request.get(`/admin/after/${manageId}`)

// 催促房东
export const urgeAfterApi = (manageId: number) => request.post(`/admin/after/urge/${manageId}`)

// 强制完成工单
export const forceCompleteApi = (manageId: number, reason: string) => 
  request.post(`/admin/after/force-complete/${manageId}`, { reason })

// 获取房东服务排行
export const getLandlordRankingApi = (sortBy?: string) => 
  request.get('/admin/after/landlord-ranking', { params: { sortBy } })

// 获取超时工单预警列表
export const getOvertimeListApi = () => request.get('/admin/after/overtime-list')
