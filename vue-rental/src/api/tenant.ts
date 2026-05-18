import request from '@/utils/request'
import type { HouseInfo, SearchParams } from '@/types/tenant'

// 获取热门房源
export const getHotHousesApi = (city?: string) => {
  return request.get<HouseInfo[]>('/tenant/dashboard/hot-houses', { params: { city } })
}

// 获取低价房源
export const getCheapHousesApi = (city?: string) => {
  return request.get<HouseInfo[]>('/tenant/dashboard/cheap-houses', { params: { city } })
}

// 获取推荐房源
export const getRecommendHousesApi = (city?: string) => {
  return request.get<HouseInfo[]>('/tenant/dashboard/recommend-houses', { params: { city } })
}

// 搜索房源
export const searchHousesApi = (params: SearchParams) => {
  return request.get<HouseInfo[]>('/tenant/dashboard/search', { params })
}
