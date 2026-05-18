<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2 class="register-title">房屋租赁系统 - 注册</h2>
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" size="large" class="register-form">
        <el-form-item label="用户类型" prop="userType">
          <el-radio-group v-model="registerForm.userType" class="user-type-group">
            <el-radio-button :value="1">
              <el-icon><House /></el-icon>
              <span>我是租客</span>
            </el-radio-button>
            <el-radio-button :value="2">
              <el-icon><OfficeBuilding /></el-icon>
              <span>我是房东</span>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="用户名" prop="username" class="form-col">
            <el-input v-model="registerForm.username" placeholder="请输入用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone" class="form-col">
            <el-input v-model="registerForm.phone" placeholder="请输入手机号" :prefix-icon="Phone" />
          </el-form-item>
        </div>
        <el-form-item label="邮箱（选填）" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" :prefix-icon="Message" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="密码" prop="password" class="form-col">
            <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword" class="form-col">
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" :prefix-icon="Lock" show-password />
          </el-form-item>
        </div>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="register-btn" @click="handleRegister">立即注册</el-button>
        </el-form-item>
        <div class="form-links">
          <el-link type="primary" @click="$router.push('/login')">已有账号？立即登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { User, Lock, Phone, Message, House, OfficeBuilding } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { registerApi } from '@/api/auth'
import type { RegisterForm } from '@/types/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

const registerFormRef = ref<FormInstance>()
const loading = ref(false)
const registerForm = reactive<RegisterForm>({ username: '', password: '', confirmPassword: '', phone: '', email: '', userType: 1 })

const validateConfirmPassword = (rule: any, value: string, callback: Function) => {
  if (value !== registerForm.password) callback(new Error('两次输入密码不一致'))
  else callback()
}

const registerRules: FormRules = {
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await registerApi(registerForm)
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch { ElMessage.error('注册失败') } finally { loading.value = false }
    }
  })
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: url('@/assets/zk01.jpg') center/cover no-repeat;
}

.register-card {
  width: 580px;
  padding: 40px 50px;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.register-title {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
  font-size: 26px;
  font-weight: 600;
}

.register-form { width: 100%; }

.user-type-group { width: 100%; display: flex; }
.user-type-group :deep(.el-radio-button) { flex: 1; }
.user-type-group :deep(.el-radio-button__inner) { 
  width: 100%; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  gap: 8px; 
  padding: 14px 20px; 
  font-size: 15px; 
}

.form-row { display: flex; gap: 20px; }
.form-col { flex: 1; }

.register-btn { width: 100%; height: 48px; font-size: 16px; margin-top: 10px; }

.form-links { text-align: center; margin-top: 20px; }

@media (max-width: 650px) {
  .register-container { padding: 20px; }
  .register-card { width: 100%; padding: 30px 25px; }
  .form-row { flex-direction: column; gap: 0; }
}
</style>
