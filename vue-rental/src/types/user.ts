// 登录表单
export interface LoginForm {
  username: string
  password: string
  userType: 1 | 2 | 3 | null
}

// 注册表单
export interface RegisterForm {
  username: string
  password: string
  confirmPassword: string
  phone: string
  email?: string
  userType: 1 | 2 // 1租客 2房东
}

// 用户信息
export interface UserInfo {
  userId: number
  username: string
  phone: string
  email?: string
  realName?: string
  idCard?: string
  userType: 1 | 2 | 3 // 1租客 2房东 3管理员
  avatar?: string
  realnameStatus: number
  status: number
}

// 登录/注册响应
export interface AuthResponse {
  token: string
  userInfo: UserInfo
}

// API通用响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}