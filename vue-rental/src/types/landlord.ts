// 房东统计数据
export interface LandlordStats {
  totalHouses: number
  availableHouses: number
  pendingContracts: number
  pendingRents: number
  monthlyReceived: number
  monthlyExpected: number
}

// 待办事项
export interface TodoItem {
  title: string
  subTitle: string
  type: string
}

export interface LandlordTodos {
  urgent: TodoItem[]
  normal: TodoItem[]
}

// 收入趋势
export interface IncomeTrend {
  date: string
  amount: number
}

// 房源收入排行
export interface HouseIncomeRank {
  houseName: string
  income: number
}
