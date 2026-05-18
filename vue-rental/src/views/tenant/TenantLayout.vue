<template>
  <div class="tenant-layout">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-left">
        <div class="logo" @click="goHome" style="cursor: pointer;">
          <img src="@/assets/xt.png" alt="Logo" style="width: 32px; height: 32px; object-fit: contain;" />
          <span class="title">房屋租赁系统</span>
        </div>
        <!-- 城市选择器 -->
        <el-popover
          placement="bottom-start"
          :width="480"
          trigger="click"
          v-model:visible="cityPopoverVisible"
        >
          <template #reference>
            <div class="city-selector">
              <el-icon><Location /></el-icon>
              <span class="city-name">{{ selectedCity || '选择城市' }}</span>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </div>
          </template>
          <div class="city-popover">
            <div class="city-tabs">
              <span 
                v-for="(province, index) in hotProvinces" 
                :key="index"
                :class="['province-tab', activeProvince === province ? 'active' : '']"
                @click="activeProvince = province"
              >
                {{ province }}
              </span>
            </div>
            <div class="city-list">
              <span 
                v-for="city in currentCities" 
                :key="city"
                :class="['city-item', selectedCity === city ? 'active' : '']"
                @click="selectCity(city)"
              >
                {{ city }}
              </span>
            </div>
          </div>
        </el-popover>
        <!-- 顶部导航菜单 -->
        <nav class="nav-menu">
          <router-link 
            v-for="item in navItems" 
            :key="item.path" 
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
          >
            <el-badge v-if="item.path === '/tenant/chat' && chatUnreadCount > 0" :value="chatUnreadCount" :max="99" class="chat-badge">
              {{ item.name }}
            </el-badge>
            <template v-else>{{ item.name }}</template>
          </router-link>
        </nav>
      </div>
      
      <div class="header-right">
        <el-tooltip content="人工客服" placement="bottom">
          <div class="cs-btn" @click="goCustomerService">
            <el-icon :size="20"><Service /></el-icon>
          </div>
        </el-tooltip>

        <el-badge :value="unreadCount" :max="99" class="notify-badge" :hidden="unreadCount === 0" @click="handleNotify">
          <el-icon :size="20"><Bell /></el-icon>
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
              <el-dropdown-item command="favorite">
                <el-icon><Star /></el-icon>我的收藏
              </el-dropdown-item>
              <el-dropdown-item command="contract">
                <el-icon><Document /></el-icon>我的合同
              </el-dropdown-item>
              <el-dropdown-item command="history">
                <el-icon><Clock /></el-icon>历史记录
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

    <!-- 底部 -->
    <footer class="footer">
      <p>© 2025 房屋租赁系统 版权所有</p>
    </footer>

    <!-- 历史记录抽屉 -->
    <el-drawer v-model="historyDrawerVisible" title="浏览历史" direction="rtl" size="380px">
      <div class="history-drawer-content">
        <div class="history-actions" v-if="recentViewed.length > 0">
          <span class="history-count">共 {{ recentViewed.length }} 条记录</span>
          <el-button type="danger" link size="small" @click="clearRecentViewed">清空记录</el-button>
        </div>
        <el-empty v-if="recentViewed.length === 0" description="暂无浏览记录" />
        <div v-else class="history-list">
          <div class="history-item" v-for="house in recentViewed" :key="house.houseId" @click="goHouseDetail(house.houseId)">
            <el-image :src="getFirstImage(house.images)" fit="cover" class="history-img">
              <template #error><div class="history-img-placeholder"><el-icon><Picture /></el-icon></div></template>
            </el-image>
            <div class="history-info">
              <div class="history-title">{{ house.title }}</div>
              <div class="history-address">{{ house.address }}</div>
              <div class="history-meta">
                <span>{{ house.houseType }}</span>
                <span>{{ house.area }}㎡</span>
              </div>
              <div class="history-price">¥{{ house.rentPrice }}<span class="price-unit">/月</span></div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { 
  House, Bell, User, Star, Calendar, Money, SwitchButton, ArrowDown, ChatLineSquare, ChatDotRound, OfficeBuilding, Location, CreditCard, Document, Service, Clock, Picture
} from '@element-plus/icons-vue'
import { getUnreadCountApi } from '@/api/notification'
import { getChatUnreadCountApi } from '@/api/chat'
import { regionData } from '@/utils/regionData'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 城市选择器
const cityPopoverVisible = ref(false)
const selectedCity = ref(localStorage.getItem('selectedCity') || '')
const activeProvince = ref('热门城市')

// 聊天未读数
const chatUnreadCount = ref(0)

// 历史记录抽屉
const historyDrawerVisible = ref(false)
const RECENT_KEY = 'tenant_recent_viewed'
const recentViewed = ref<any[]>([])

const loadRecentViewed = () => {
  try {
    const data = localStorage.getItem(RECENT_KEY)
    recentViewed.value = data ? JSON.parse(data) : []
  } catch { recentViewed.value = [] }
}

const clearRecentViewed = () => {
  localStorage.removeItem(RECENT_KEY)
  recentViewed.value = []
}

const getFirstImage = (images: string) => {
  if (!images) return ''
  try { return JSON.parse(images)[0] || '' } catch { return images.split(',')[0] || '' }
}

const goHouseDetail = (houseId: number) => {
  historyDrawerVisible.value = false
  router.push(`/tenant/house/${houseId}`)
}

// 热门省份/城市分类
const hotProvinces = ['热门城市', '北京市', '上海市', '广东省', '江苏省', '浙江省', '四川省', '湖北省', '河南省']

// 热门城市列表
const hotCities = ['北京', '上海', '广州', '深圳', '杭州', '南京', '成都', '武汉', '重庆', '西安', '苏州', '天津', '郑州', '长沙', '东莞', '佛山', '宁波', '青岛', '沈阳', '无锡']

// 当前显示的城市列表
const currentCities = computed(() => {
  if (activeProvince.value === '热门城市') {
    return hotCities
  }
  const cities = Object.keys(regionData[activeProvince.value] || {})
  // 直辖市特殊处理
  if (['北京市', '上海市', '天津市', '重庆市'].includes(activeProvince.value)) {
    return [activeProvince.value.replace('市', '')]
  }
  return cities.map(c => c.replace('市', ''))
})

// 选择城市
const selectCity = (city: string) => {
  selectedCity.value = city
  localStorage.setItem('selectedCity', city)
  cityPopoverVisible.value = false
  // 触发自定义事件通知其他组件城市已变化
  window.dispatchEvent(new CustomEvent('cityChanged', { detail: city }))
}

// 返回首页
const goHome = () => {
  router.push('/tenant')
}

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const unreadCount = ref(0)

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
  { name: '首页', path: '/tenant' },
  { name: '我要租房', path: '/tenant/search' },
  { name: '支付中心', path: '/tenant/payment' },
  { name: '租后管理', path: '/tenant/after' },
  { name: '聊天中心', path: '/tenant/chat' },
  { name: '支出分析', path: '/tenant/statistics' },
  { name: '租房日历', path: '/tenant/calendar' }
]

// 判断是否激活
const isActive = (path: string) => {
  if (path === '/tenant') {
    return route.path === '/tenant'
  }
  return route.path.startsWith(path)
}

const handleNotify = () => {
  router.push('/tenant/message')
}

const goCustomerService = () => {
  router.push({ path: '/tenant/chat', query: { customerService: '1' } })
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/tenant/profile')
      break
    case 'favorite':
      router.push('/tenant/favorite')
      break
    case 'payment':
      router.push('/tenant/payment')
      break
    case 'contract':
      router.push('/tenant/contract')
      break
    case 'history':
      loadRecentViewed()
      historyDrawerVisible.value = true
      break
    case 'logout':
      userStore.logout()
      break
  }
}
</script>

<style scoped>
.tenant-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
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
  gap: 24px;
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

/* 城市选择器 */
.city-selector {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e4e7ed;
}

.city-selector:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.city-name {
  font-size: 14px;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 12px;
  color: #909399;
}

.city-popover {
  padding: 8px 0;
}

.city-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 12px;
}

.province-tab {
  padding: 4px 12px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s;
}

.province-tab:hover {
  color: #1890ff;
}

.province-tab.active {
  color: #1890ff;
  background: #e6f7ff;
}

.city-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.city-item {
  padding: 6px 16px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s;
}

.city-item:hover {
  color: #1890ff;
  background: #f5f7fa;
}

.city-item.active {
  color: #fff;
  background: #1890ff;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-item {
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
  flex: 1;
  margin-top: 60px;
}

.footer {
  background: #303133;
  color: #909399;
  text-align: center;
  padding: 20px;
  font-size: 14px;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 历史记录抽屉 */
.history-drawer-content { padding: 0 4px; }
.history-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.history-count { font-size: 13px; color: #909399; }
.history-list { display: flex; flex-direction: column; gap: 12px; }
.history-item { display: flex; gap: 12px; padding: 12px; background: #fafafa; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
.history-item:hover { background: #f0f7ff; transform: translateX(2px); }
.history-img { width: 90px; height: 68px; border-radius: 6px; flex-shrink: 0; }
.history-img-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f0f0f0; color: #ccc; }
.history-info { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: space-between; }
.history-title { font-size: 14px; font-weight: 500; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-address { font-size: 12px; color: #909399; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-meta { display: flex; gap: 8px; font-size: 12px; color: #606266; }
.history-meta span { background: #f0f0f0; padding: 1px 6px; border-radius: 3px; }
.history-price { font-size: 16px; font-weight: bold; color: #ff4d4f; }
.history-price .price-unit { font-size: 12px; font-weight: normal; color: #999; }
</style>
