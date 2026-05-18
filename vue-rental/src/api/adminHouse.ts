import request from '@/utils/request'

// 房源信息
export interface HouseAuditInfo {
  houseId: number
  title: string
  description: string
  address: string
  province: string
  city: string
  district: string
  rentPrice: number
  depositType: string // 押付方式
  area: number
  houseType: string
  floor: string
  orientation: string
  facilities: string
  images: string
  rentOption: number
  status: number
  auditReason?: string
  viewCount: number
  collectCount: number
  createTime: string
  updateTime?: string
  landlordId: number
  landlordName: string
  landlordPhone: string
  landlordEmail?: string
  propertyLicenseFront?: string
  propertyLicenseBack?: string
  propertyLicenseOther?: string
}

// 获取待审核房源列表
export const getAuditListApi = (params: {
  status?: number
  keyword?: string
  page?: number
  size?: number
}) => {
  return request.get('/admin/house/audit/list', { params })
}

// 获取房源详情
export const getHouseDetailApi = (houseId: number) => {
  return request.get(`/admin/house/audit/${houseId}`)
}

// 审核通过
export const approveHouseApi = (houseId: number) => {
  return request.put(`/admin/house/audit/${houseId}/approve`)
}

// 审核驳回
export const rejectHouseApi = (houseId: number, reason: string) => {
  return request.put(`/admin/house/audit/${houseId}/reject`, { reason })
}

// 批量审核通过
export const batchApproveApi = (ids: number[]) => {
  return request.put('/admin/house/audit/batch/approve', { ids })
}

// 批量审核驳回
export const batchRejectApi = (ids: number[], reason: string) => {
  return request.put('/admin/house/audit/batch/reject', { ids, reason })
}

// 获取审核统计
export const getAuditStatsApi = () => {
  return request.get('/admin/house/audit/stats')
}

// ==================== 房源监管 ====================

// 获取监管房源列表
export const getMonitorListApi = (params: {
  status?: number
  city?: string
  district?: string
  minPrice?: number
  maxPrice?: number
  keyword?: string
  page?: number
  size?: number
}) => {
  return request.get('/admin/house/monitor/list', { params })
}

// 获取监管统计
export const getMonitorStatsApi = () => {
  return request.get('/admin/house/monitor/stats')
}

// 强制下架房源
export const offlineHouseApi = (houseId: number, reason: string) => {
  return request.put(`/admin/house/monitor/${houseId}/offline`, { reason })
}

// 恢复上架房源
export const onlineHouseApi = (houseId: number) => {
  return request.put(`/admin/house/monitor/${houseId}/online`)
}

// 编辑房源信息
export const editHouseApi = (houseId: number, data: {
  title?: string
  description?: string
  address?: string
  rentPrice?: number
  area?: number
}) => {
  return request.put(`/admin/house/monitor/${houseId}/edit`, data)
}

// 批量下架房源
export const batchOfflineApi = (ids: number[], reason: string) => {
  return request.put('/admin/house/monitor/batch/offline', { ids, reason })
}

// ==================== 数据分析 ====================

// 获取区域分布
export const getAreaDistributionApi = () => {
  return request.get('/admin/house/analysis/area-distribution')
}

// 获取价格分布
export const getPriceDistributionApi = () => {
  return request.get('/admin/house/analysis/price-distribution')
}

// 获取热门房源排行
export const getHotRankingApi = (limit: number = 10) => {
  return request.get('/admin/house/analysis/hot-ranking', { params: { limit } })
}

// 获取发布趋势
export const getPublishTrendApi = (days: number = 30) => {
  return request.get('/admin/house/analysis/publish-trend', { params: { days } })
}

// 获取类型分布
export const getTypeDistributionApi = () => {
  return request.get('/admin/house/analysis/type-distribution')
}

// 获取分析概览
export const getAnalysisOverviewApi = () => {
  return request.get('/admin/house/analysis/overview')
}
