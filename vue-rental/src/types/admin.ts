// 权限菜单
export interface Permission {
  permId: number
  permCode: string
  permName: string
  permType: number
  parentId: number
  path: string
  component: string
  icon: string
  orderNum: number
  children?: Permission[]
}

// 仪表盘统计数据
export interface DashboardStats {
  tenantCount: number
  landlordCount: number
  adminCount: number
  totalUserCount: number
  totalHouseCount: number
  pendingHouseCount: number
  availableHouseCount: number
  rentedHouseCount: number
  todayNewUsers: number
  todayNewHouses: number
  todayOrders: number
  todayAmount: number
  totalAmount: number
}

// 实时监控数据
export interface MonitorData {
  onlineUsers: number
  todayNewHouses: number
  alertCount: number
  cpuUsage: number
  memoryUsage: number
}

// 待办事项
export interface TodoItem {
  type: 'urgent' | 'important' | 'normal'
  content: string
  count: number
}

// 趋势数据
export interface TrendData {
  userTrend: { date: string; count: number }[]
  houseTrend: { date: string; count: number }[]
  amountTrend: { date: string; amount: number }[]
}

// 热门城市
export interface HotCity {
  city: string
  count: number
}
