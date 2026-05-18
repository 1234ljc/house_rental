<template>
  <div class="change-password">
    <div class="page-header">
      <h3>修改密码</h3>
      <p class="desc">定期修改密码可以提高账号安全性</p>
    </div>

    <el-card class="password-card">
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-width="100px"
        style="max-width: 450px;"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input 
            v-model="form.oldPassword" 
            type="password" 
            placeholder="请输入当前密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="form.newPassword" 
            type="password" 
            placeholder="请输入新密码（6-20位）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            确认修改
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="tips-card">
      <template #header>
        <span>安全提示</span>
      </template>
      <ul class="tips-list">
        <li>密码长度为6-20个字符</li>
        <li>建议使用字母、数字和特殊字符的组合</li>
        <li>请勿使用与其他网站相同的密码</li>
        <li>修改密码后需要重新登录</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { updatePasswordApi } from '@/api/profile'
import { useUserStore } from '@/stores/user'

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 验证确认密码
const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== form.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await updatePasswordApi({
        oldPassword: form.value.oldPassword,
        newPassword: form.value.newPassword
      })
      ElMessage.success('密码修改成功，请重新登录')
      // 使用 userStore 的 logout 方法来正确清除 token
      const userStore = useUserStore()
      userStore.logout()
    } catch (error: any) {
      ElMessage.error(error.message || '密码修改失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleReset = () => {
  formRef.value?.resetFields()
}
</script>

<style scoped>
.change-password {
  padding: 8px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h3 {
  font-size: 18px;
  color: #303133;
  margin: 0 0 8px 0;
}

.page-header .desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.password-card {
  margin-bottom: 20px;
}

.tips-card {
  background: #fafafa;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  color: #909399;
  font-size: 14px;
  line-height: 2;
}

.tips-list li {
  margin-bottom: 4px;
}
</style>
