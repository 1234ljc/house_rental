<template>
  <div class="profile-layout">
    <div class="profile-container">
      <!-- 左侧菜单 -->
      <aside class="profile-sidebar">
        <!-- 用户头像区域 -->
        <div class="user-card">
          <el-avatar :size="80" :src="userStore.userInfo?.avatar || defaultAvatar">
            <el-icon :size="40"><User /></el-icon>
          </el-avatar>
          <p class="welcome-text">欢迎你，{{ userStore.userInfo?.username }}</p>
          <el-tag type="warning" size="small">房东</el-tag>
        </div>

        <!-- 菜单列表 -->
        <div class="menu-list">
          <div 
            class="menu-item" 
            :class="{ active: activeMenu === 'realname' }"
            @click="handleMenuClick('realname')"
          >
            <el-icon><Checked /></el-icon>
            <span>实名认证</span>
          </div>
          <div 
            class="menu-item" 
            :class="{ active: activeMenu === 'info' }"
            @click="handleMenuClick('info')"
          >
            <el-icon><Setting /></el-icon>
            <span>个人信息管理</span>
          </div>
          <div 
            class="menu-item" 
            :class="{ active: activeMenu === 'card' }"
            @click="handleMenuClick('card')"
          >
            <el-icon><Postcard /></el-icon>
            <span>个人名片</span>
          </div>
          <div 
            class="menu-item" 
            :class="{ active: activeMenu === 'credit' }"
            @click="handleMenuClick('credit')"
          >
            <el-icon><TrophyBase /></el-icon>
            <span>信用评分</span>
          </div>
          <div 
            class="menu-item" 
            :class="{ active: activeMenu === 'password' }"
            @click="handleMenuClick('password')"
          >
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </div>
        </div>
      </aside>

      <!-- 右侧内容区 -->
      <main class="profile-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { User, Checked, Setting, Lock, Postcard, TrophyBase } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 当前激活菜单
const activeMenu = computed(() => {
  if (route.path.includes('realname')) return 'realname'
  if (route.path.includes('password')) return 'password'
  if (route.path.includes('card')) return 'card'
  if (route.path.includes('credit')) return 'credit'
  if (route.path.includes('info')) return 'info'
  return 'realname'
})

// 菜单点击
const handleMenuClick = (menu: string) => {
  if (menu === 'realname') {
    router.push('/landlord/profile/realname')
  } else if (menu === 'info') {
    router.push('/landlord/profile/info')
  } else if (menu === 'card') {
    router.push('/landlord/profile/card')
  } else if (menu === 'credit') {
    router.push('/landlord/profile/credit')
  } else if (menu === 'password') {
    router.push('/landlord/profile/password')
  }
}
</script>

<style scoped>
.profile-layout {
  min-height: calc(100vh - 60px - 48px);
  background: #f5f7fa;
  padding: 24px;
}

.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  gap: 24px;
}

.profile-sidebar {
  width: 240px;
  flex-shrink: 0;
}

.user-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}

.welcome-text {
  margin: 16px 0 8px 0;
  font-size: 14px;
  color: #606266;
}

.menu-list {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 1px solid #f0f0f0;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:hover {
  background: #f5f7fa;
  color: #1890ff;
}

.menu-item.active {
  background: #e6f7ff;
  color: #1890ff;
  border-left: 3px solid #1890ff;
}

.profile-content {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  min-height: 500px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}
</style>
