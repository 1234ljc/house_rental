import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Permission } from '@/types/admin'
import { getMenusApi } from '@/api/admin'

export const useAdminStore = defineStore('admin', () => {
  const menus = ref<Permission[]>([])
  const isMenuLoaded = ref(false)

  // 加载菜单
  const loadMenus = async () => {
    if (isMenuLoaded.value) return menus.value
    try {
      const data = await getMenusApi()
      menus.value = data as unknown as Permission[]
      isMenuLoaded.value = true
      return menus.value
    } catch (error) {
      console.error('加载菜单失败:', error)
      return []
    }
  }

  // 重置状态
  const reset = () => {
    menus.value = []
    isMenuLoaded.value = false
  }

  return {
    menus,
    isMenuLoaded,
    loadMenus,
    reset
  }
})
