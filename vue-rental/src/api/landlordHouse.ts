import request from '@/utils/request'

// 房源发布DTO
export interface HousePublishDTO {
  title: string
  description: string
  address: string
  province: string
  city: string
  district: string
  longitude?: number
  latitude?: number
  rentPrice: number
  depositType: string // 押付方式：押一付一、押一付二、押一付三等
  area: number
  houseType: string
  floor: string
  orientation: string
  facilities: string // JSON字符串
  images: string // JSON字符串
  rentOption: number
  propertyLicenseFront?: string
  propertyLicenseBack?: string
  propertyLicenseOther?: string
}

// 房源信息
export interface HouseInfo {
  houseId: number
  landlordId: number
  title: string
  description: string
  address: string
  province: string
  city: string
  district: string
  longitude?: number
  latitude?: number
  rentPrice: number
  depositType: string // 押付方式
  area: number
  houseType: string
  floor: string
  orientation: string
  facilities: string
  images: string
  rentOption: number
  status: number // 0待审核 1可出租 2已出租 3已下架 4审核驳回
  auditReason?: string
  viewCount: number
  collectCount: number
  createTime: string
  updateTime?: string
  propertyLicenseFront?: string
  propertyLicenseBack?: string
  propertyLicenseOther?: string
}

// 发布房源
export const publishHouseApi = (data: HousePublishDTO) => {
  return request.post('/landlord/house/publish', data)
}

// 获取房源列表
export const getHouseListApi = (params: {
  status?: number
  keyword?: string
  page?: number
  size?: number
}) => {
  return request.get('/landlord/house/list', { params })
}

// 获取房源详情
export const getHouseDetailApi = (houseId: number) => {
  return request.get(`/landlord/house/${houseId}`)
}

// 更新房源
export const updateHouseApi = (houseId: number, data: HousePublishDTO) => {
  return request.put(`/landlord/house/${houseId}`, data)
}

// 下架房源
export const offlineHouseApi = (houseId: number) => {
  return request.put(`/landlord/house/${houseId}/offline`)
}

// 重新上架房源
export const onlineHouseApi = (houseId: number) => {
  return request.put(`/landlord/house/${houseId}/online`)
}

// 删除房源
export const deleteHouseApi = (houseId: number) => {
  return request.delete(`/landlord/house/${houseId}`)
}

// 获取房源统计
export const getHouseStatsApi = () => {
  return request.get('/landlord/house/stats')
}

// 批量操作房源
export const batchHouseOperateApi = (data: {
  houseIds: number[]
  action: 'offline' | 'online' | 'price'
  price?: number
  adjustType?: 'fixed' | 'percent'
}) => {
  return request.post('/landlord/house/batch', data)
}

// 获取单个房源数据看板
export const getHouseDashboardApi = (houseId: number) => {
  return request.get(`/landlord/statistics/house/${houseId}/dashboard`)
}
