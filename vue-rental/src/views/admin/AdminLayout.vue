<template>
  <div class="admin-layout">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="logo" @click="router.push('/admin')" style="cursor: pointer;">
        <img src="@/assets/xt.png" alt="Logo" style="width: 32px; height: 32px; object-fit: contain;" />
        <span class="title">房屋租赁系统</span>
      </div>
      <div class="header-right">
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
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="main-container">
      <!-- 左侧菜单 -->
      <aside class="sidebar" :class="{ collapsed: isCollapsed }">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapsed"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#fff"
          :unique-opened="true"
          router
        >
          <template v-for="menu in adminStore.menus" :key="menu.permId">
            <!-- 无子菜单 -->
            <el-menu-item v-if="!menu.children?.length" :index="menu.path">
              <el-icon><component :is="menu.icon" /></el-icon>
              <template #title>{{ menu.permName }}</template>
            </el-menu-item>

            <!-- 有子菜单 -->
            <el-sub-menu v-else :index="menu.path">
              <template #title>
                <el-icon><component :is="menu.icon" /></el-icon>
                <span>{{ menu.permName }}</span>
              </template>

              <template v-for="child in menu.children" :key="child.permId">
                <!-- 二级无子菜单 -->
                <el-menu-item v-if="!child.children?.length" :index="child.path">
                  <el-icon><component :is="child.icon" /></el-icon>
                  <template #title>{{ child.permName }}</template>
                </el-menu-item>

                <!-- 三级菜单 -->
                <el-sub-menu v-else :index="child.path">
                  <template #title>
                    <el-icon><component :is="child.icon" /></el-icon>
                    <span>{{ child.permName }}</span>
                  </template>
                  <el-menu-item
                    v-for="subChild in child.children"
                    :key="subChild.permId"
                    :index="subChild.path"
                  >
                    <el-icon><component :is="subChild.icon" /></el-icon>
                    <template #title>{{ subChild.permName }}</template>
                  </el-menu-item>
                </el-sub-menu>
              </template>
            </el-sub-menu>
          </template>
        </el-menu>
      </aside>

      <!-- 主内容区 -->
      <main class="main-content">
        <!-- 面包屑 -->
        <div class="breadcrumb-container">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- 页面内容 -->
        <div class="page-content">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAdminStore } from '@/stores/admin'
import { HomeFilled, User, SwitchButton, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const adminStore = useAdminStore()

const isCollapsed = ref(false)

// 默认头像
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 当前激活菜单
const activeMenu = computed(() => route.path)

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta?.title)
  return matched.map(item => ({
    path: item.path,
    title: item.meta?.title as string
  }))
})

// 下拉菜单命令处理
const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/admin/profile')
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 退出登录
const handleLogout = () => {
  userStore.logout()
}

// 加载菜单
onMounted(async () => {
  // 刷新页面时重新获取用户信息
  if (!userStore.userInfo) {
    await userStore.getCurrentUser()
  }
  await adminStore.loadMenus()
})
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
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

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1890ff;
}

.logo .title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  font-size: 14px;
  color: #606266;
  margin-left: 8px;
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

.dropdown-icon {
  margin-left: 4px;
  color: #909399;
  font-size: 12px;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

.main-container {
  display: flex;
  margin-top: 60px;
  min-height: calc(100vh - 60px);
}

.sidebar {
  width: 200px;
  background: #304156;
  position: fixed;
  top: 60px;
  left: 0;
  bottom: 0;
  overflow-y: auto;
  transition: width 0.3s;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar::-webkit-scrollbar {
  width: 6px;
}

.sidebar::-webkit-scrollbar-thumb {
  background: #5a6a7a;
  border-radius: 3px;
}

.el-menu {
  border-right: none;
}

:deep(.el-menu-item.is-active) {
  background: #1890ff !important;
  border-left: 3px solid #1890ff;
}

:deep(.el-menu-item:hover) {
  background: #263445 !important;
}

:deep(.el-sub-menu__title:hover) {
  background: #263445 !important;
}

.main-content {
  flex: 1;
  margin-left: 200px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.breadcrumb-container {
  height: 50px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.page-content {
  padding: 24px;
  overflow-y: auto;
}

.page-content::-webkit-scrollbar {
  width: 6px;
}

.page-content::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}
</style>
