// 房源信息
export interface HouseInfo {
  houseId: number
  title: string
  address: string
  city: string
  rentPrice: number
  area: number
  houseType: string
  images: string
  viewCount: number
  collectCount: number
}

// 搜索参数
export interface SearchParams {
  keyword?: string
  city?: string
  minPrice?: number
  maxPrice?: number
}
