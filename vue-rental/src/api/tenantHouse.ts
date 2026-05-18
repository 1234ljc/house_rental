import request from '@/utils/request'

export interface HouseSearchParams {
  keyword?: string
  province?: string
  city?: string
  district?: string
  minPrice?: number
  maxPrice?: number
  houseType?: string
  minArea?: number
  maxArea?: number
  orientation?: string
  sort?: string
  page?: number
  size?: number
}

export interface HouseInfo {
  houseId: number
  title: string
  description?: string
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
  viewCount: number
  collectCount: number
  createTime: string
  landlordId?: number
}

// 搜索房源
export const searchHousesApi = (params: HouseSearchParams) => {
  return request.get('/tenant/house/search', { params })
}

// 获取房源详情
export const getHouseDetailApi = (houseId: number) => {
  return request.get(`/tenant/house/${houseId}`)
}

// 获取热门城市
export const getHotCitiesApi = () => {
  return request.get('/tenant/house/hot-cities')
}

// 获取推荐房源
export const getRecommendHousesApi = (limit?: number) => {
  return request.get('/tenant/house/recommend', { params: { limit } })
}

// 获取热门房源
export const getHotHousesApi = (limit?: number) => {
  return request.get('/tenant/house/hot', { params: { limit } })
}

// 收藏房源
export const addFavoriteApi = (houseId: number) => {
  return request.post(`/tenant/house/favorite/${houseId}`)
}

// 取消收藏
export const removeFavoriteApi = (houseId: number) => {
  return request.delete(`/tenant/house/favorite/${houseId}`)
}

// 检查是否已收藏
export const checkFavoriteApi = (houseId: number) => {
  return request.get(`/tenant/house/favorite/check/${houseId}`)
}

// 获取收藏列表
export const getFavoriteListApi = (params: { page?: number; size?: number }) => {
  return request.get('/tenant/house/favorite/list', { params })
}
