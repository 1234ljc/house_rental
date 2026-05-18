<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-main">
        <!-- 左侧头像区域 -->
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <el-avatar :size="100" :src="avatarUrl" :class="{ 'avatar-visible': showAvatar }" class="user-avatar">
              <el-icon :size="50"><User /></el-icon>
            </el-avatar>
          </div>
          <p class="avatar-tip">{{ showAvatar ? '验证成功' : '请输入登录信息' }}</p>
        </div>

        <!-- 右侧表单区域 -->
        <div class="form-section">
          <h2 class="form-title">房屋租赁系统</h2>
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0" size="large" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" clearable @input="checkUserInfo" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password @input="checkUserInfo" @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item prop="userType">
              <el-select v-model="loginForm.userType" placeholder="请选择角色" style="width: 100%" clearable @change="checkUserInfo">
                <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value">
                  <template #default><el-icon style="margin-right: 8px"><component :is="item.icon" /></el-icon>{{ item.label }}</template>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">登录</el-button>
            </el-form-item>
            <div class="form-links">
              <el-link type="primary" @click="$router.push('/register')">还没有账号？立即注册</el-link>
            </div>
          </el-form>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { User, Lock, UserFilled, OfficeBuilding, House } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { preCheckUserApi } from '@/api/auth'
import type { LoginForm } from '@/types/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const loginForm = reactive<LoginForm>({ username: '', password: '', userType: null })
const roleOptions = [
  { label: '租客', value: 1, icon: House },
  { label: '房东', value: 2, icon: OfficeBuilding },
  { label: '管理员', value: 3, icon: UserFilled }
]
const avatarUrl = ref('')
const showAvatar = ref(false)

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

let debounceTimer: number | null = null
const checkUserInfo = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (loginForm.username && loginForm.password && loginForm.userType) {
    debounceTimer = setTimeout(async () => {
      try {
        const response = await preCheckUserApi(loginForm)
        if (response && (response as any).avatar) { avatarUrl.value = (response as any).avatar; showAvatar.value = true }
        else { showAvatar.value = false; avatarUrl.value = '' }
      } catch { showAvatar.value = false; avatarUrl.value = '' }
    }, 500)
  } else { showAvatar.value = false; avatarUrl.value = '' }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        if (!loginForm.userType) { ElMessage.error('请选择角色'); loading.value = false; return }
        await userStore.login(loginForm)
      } catch { } finally { loading.value = false }
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: url('@/assets/zk01.jpg') center/cover no-repeat;
}

.login-card {
  width: 600px;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.login-main {
  display: flex;
  min-height: 400px;
}

.avatar-section {
  width: 200px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 20px;
  color: white;
}

.avatar-wrapper { margin-bottom: 20px; }

.user-avatar {
  opacity: 0.6;
  transform: scale(0.9);
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  border: 3px solid rgba(255, 255, 255, 0.3);
}

.user-avatar.avatar-visible {
  opacity: 1;
  transform: scale(1);
  border-color: rgba(255, 255, 255, 0.8);
}

.avatar-tip { font-size: 14px; color: rgba(255, 255, 255, 0.8); text-align: center; }

.form-section { flex: 1; padding: 40px 30px; }

.form-title { text-align: center; margin-bottom: 30px; color: #303133; font-size: 24px; font-weight: 600; }

.login-form { width: 100%; }

.login-btn { width: 100%; height: 44px; font-size: 16px; margin-top: 10px; }

.form-links { text-align: center; margin-top: 20px; }

@media (max-width: 768px) {
  .login-container { padding: 20px; }
  .login-card { width: 100%; max-width: 400px; }
  .login-main { flex-direction: column; }
  .avatar-section { width: 100%; min-height: 120px; }
}
</style>
