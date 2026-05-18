import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, LoginForm, AuthResponse } from '@/types/user'
import { loginApi, getCurrentUserApi } from '@/api/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 根据当前路由获取用户类型
const getUserTypeFromPath = (): number | null => {
  const path = window.location.pathname
  if (path.startsWith('/tenant')) return 1
  if (path.startsWith('/landlord')) return 2
  if (path.startsWith('/admin')) return 3
  return null
}

// 根据用户类型获取存储key
const getStorageKey = (userType: number | null): string => {
  if (userType === 1) return 'token_tenant'
  if (userType === 2) return 'token_landlord'
  if (userType === 3) return 'token_admin'
  return 'token'
}

// 获取当前路由对应的token
const getCurrentToken = (): string => {
  const userType = getUserTypeFromPath()
  const key = getStorageKey(userType)
  return localStorage.getItem(key) || ''
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(getCurrentToken())
  const userInfo = ref<UserInfo | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const userType = computed(() => userInfo.value?.userType || null)
  
  // 实名认证状态: -1=未认证, 0=审核中, 1=已通过, 2=已驳回
  const realnameStatus = computed(() => userInfo.value?.realnameStatus ?? -1)
  const isRealnameVerified = computed(() => realnameStatus.value === 1)

  // 登录
  const login = async (loginForm: LoginForm) => {
    try {
      const response = await loginApi(loginForm)
      const data = response as unknown as AuthResponse

      token.value = data.token
      userInfo.value = data.userInfo

      // 根据用户类型存储token
      const storageKey = getStorageKey(data.userInfo.userType)
      localStorage.setItem(storageKey, data.token)

      ElMessage.success('登录成功')

      // 修复：正确处理router.push的Promise
      const route = data.userInfo.userType === 1 ? '/tenant' :
        data.userInfo.userType === 2 ? '/landlord' :
          data.userInfo.userType === 3 ? '/admin' : '/'
      await router.push(route)
    } catch (error: any) {
      throw error
    }
  }

  // 退出登录
  const logout = () => {
    // 清除当前用户类型的token
    const currentUserType = userInfo.value?.userType || getUserTypeFromPath()
    const storageKey = getStorageKey(currentUserType)
    
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(storageKey)
    router.push('/login')
  }

  // 获取当前用户信息
  const getCurrentUser = async () => {
    // 刷新时重新获取token
    const currentToken = getCurrentToken()
    if (currentToken) {
      token.value = currentToken
    }
    
    if (!token.value) return
    try {
      const response = await getCurrentUserApi()
      userInfo.value = response as unknown as UserInfo
    } catch (error) {
      logout()
    }
  }

  // 更新实名认证状态（用于认证提交后刷新）
  const refreshUserInfo = async () => {
    await getCurrentUser()
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    userType,
    realnameStatus,
    isRealnameVerified,
    login,
    logout,
    getCurrentUser,
    refreshUserInfo
  }
})