<template>
  <div class="landlord-layout">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-left">
        <div class="logo" @click="goHome" style="cursor: pointer;">
          <img src="@/assets/xt.png" alt="Logo" style="width: 32px; height: 32px; object-fit: contain;" />
          <span class="title">房屋租赁系统</span>
        </div>
        <!-- 顶部导航菜单 -->
        <nav class="nav-menu">
          <router-link 
            v-for="item in navItems" 
            :key="item.path" 
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <el-badge v-if="item.path === '/landlord/chat' && chatUnreadCount > 0" :value="chatUnreadCount" :max="99" class="chat-badge">
              <span>{{ item.name }}</span>
            </el-badge>
            <span v-else>{{ item.name }}</span>
          </router-link>
        </nav>
      </div>
      
      <div class="header-right">
        <el-tooltip content="人工客服" placement="bottom">
          <div class="cs-btn" @click="goCustomerService">
            <el-icon :size="20"><Service /></el-icon>
          </div>
        </el-tooltip>

        <el-badge :value="unreadCount" :max="99" class="notify-badge" :hidden="unreadCount === 0">
          <el-icon :size="20" @click="goToMessage" style="cursor: pointer;"><Bell /></el-icon>
        </el-badge>
        
        <!-- 用户头像下拉菜单 -->
        <el-dropdown trigger="hover" @command="handleCommand">
          <div class="user-dropdown">
            <el-avatar 
              :size="36" 
              :src="userStore.userInfo?.avatar || defaultAvatar"
            >
              <el-icon :size="20"><User /></el-icon>
            </el-avatar>
            <span class="username">{{ userStore.userInfo?.username }}</span>
            <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item command="contract-alert">
                <el-icon><Warning /></el-icon>合同到期
              </el-dropdown-item>
              <el-dropdown-item command="message">
                <el-icon><Bell /></el-icon>消息通知
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { 
  House, Bell, User, SwitchButton, ArrowDown,
  HomeFilled, Calendar, Document, Money, Service, Star, ChatLineSquare, ChatDotRound, TrendCharts, Warning, OfficeBuilding
} from '@element-plus/icons-vue'
import { getUnreadCountApi } from '@/api/notification'
import { getChatUnreadCountApi } from '@/api/chat'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const unreadCount = ref(0)
const chatUnreadCount = ref(0)

// 获取未读消息数
const loadUnreadCount = async () => {
  try {
    const res: any = await getUnreadCountApi()
    unreadCount.value = res || 0
  } catch (e) { console.error(e) }
}

const loadChatUnreadCount = async () => {
  try {
    const res: any = await getChatUnreadCountApi()
    chatUnreadCount.value = res || 0
  } catch (e) { console.error(e) }
}

const goToMessage = () => {
  router.push('/landlord/message')
}

const goCustomerService = () => {
  router.push({ path: '/landlord/chat', query: { customerService: '1' } })
}

// 返回首页
const goHome = () => {
  router.push('/landlord')
}

onMounted(() => {
  // 刷新页面时重新获取用户信息
  if (!userStore.userInfo) {
    userStore.getCurrentUser()
  }
  loadUnreadCount()
  loadChatUnreadCount()
  // 每60秒刷新一次未读数
  setInterval(loadUnreadCount, 60000)
  setInterval(loadChatUnreadCount, 30000)
})

// 导航菜单项
const navItems = [
  { name: '工作台', path: '/landlord', icon: 'HomeFilled' },
  { name: '房源管理', path: '/landlord/house', icon: 'House' },
  { name: '合同管理', path: '/landlord/rental', icon: 'Document' },
  { name: '财务管理', path: '/landlord/finance', icon: 'Money' },
  { name: '租后服务', path: '/landlord/after', icon: 'Service' },
  { name: '收入分析', path: '/landlord/statistics', icon: 'TrendCharts' },
  { name: '聊天中心', path: '/landlord/chat', icon: 'ChatDotRound' }
]

// 判断是否激活
const isActive = (path: string) => {
  if (path === '/landlord') {
    return route.path === '/landlord'
  }
  return route.path.startsWith(path)
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/landlord/profile')
      break
    case 'contract-alert':
      router.push('/landlord/contract-alert')
      break
    case 'message':
      router.push('/landlord/message')
      break
    case 'logout':
      userStore.logout()
      break
  }
}
</script>

<style scoped>
.landlord-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 40px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo .title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
  text-decoration: none;
  transition: all 0.3s;
}

.nav-item:hover {
  color: #1890ff;
  background: #e6f7ff;
}

.nav-item.active {
  color: #1890ff;
  background: #e6f7ff;
  font-weight: 500;
}

.chat-badge :deep(.el-badge__content) {
  top: -4px;
  right: -8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.cs-btn {
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;
  display: flex;
  align-items: center;
}
.cs-btn:hover { color: #1890ff; }

.notify-badge {
  cursor: pointer;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.3s;
}

.user-dropdown:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #606266;
  margin-left: 8px;
}

.dropdown-icon {
  margin-left: 4px;
  color: #909399;
  font-size: 12px;
}

.main-content {
  margin-top: 60px;
  min-height: calc(100vh - 60px);
  padding: 24px;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
