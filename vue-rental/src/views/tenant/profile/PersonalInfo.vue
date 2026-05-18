<template>
  <div class="personal-info">
    <div class="page-header">
      <h3>个人信息管理</h3>
      <p class="desc">管理您的个人资料和账号设置</p>
    </div>

    <!-- 基本信息卡片 -->
    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <el-button type="primary" link @click="showEditDialog">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
        </div>
      </template>
      
      <div class="info-content" v-loading="loading">
        <div class="avatar-section">
          <el-avatar :size="80" :src="userInfo?.avatar || defaultAvatar">
            <el-icon :size="40"><User /></el-icon>
          </el-avatar>
          <el-upload
            class="avatar-upload"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
            accept="image/*"
          >
            <el-button size="small">更换头像</el-button>
          </el-upload>
        </div>
        
        <el-descriptions :column="2" border class="info-desc">
          <el-descriptions-item label="用户名">
            {{ userInfo?.username || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="实名状态">
            <el-tag :type="realnameStatusType">{{ realnameStatusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">
            {{ userInfo?.phone || '-' }}
            <el-button type="primary" link size="small" @click="showPhoneDialog">修改</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">
            {{ userInfo?.email || '未设置' }}
            <el-button type="primary" link size="small" @click="showEmailDialog">修改</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ formatTime(userInfo?.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <!-- 身份证修改卡片 -->
    <el-card class="security-card" v-if="userInfo?.realnameStatus === 1">
      <template #header>
        <span>身份信息</span>
      </template>
      
      <div class="security-item">
        <div class="item-info">
          <el-icon :size="24" color="#52c41a"><Checked /></el-icon>
          <div class="item-text">
            <h4>身份证号</h4>
            <p>{{ maskIdCard(userInfo?.idCard) }}</p>
          </div>
        </div>
        <el-button type="primary" @click="showIdCardDialog">修改身份证</el-button>
      </div>
    </el-card>

    <!-- 编辑基本信息对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑基本信息" width="450px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="请输入用户名" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="editSubmitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改手机号对话框 -->
    <el-dialog v-model="phoneDialogVisible" title="修改手机号" width="450px">
      <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-width="100px">
        <el-form-item label="当前手机号">
          <span class="current-value">{{ userInfo?.phone }}</span>
        </el-form-item>
        <el-form-item label="新手机号" prop="phone">
          <el-input v-model="phoneForm.phone" placeholder="请输入新手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="验证码" prop="verifyCode">
          <div class="verify-code-input">
            <el-input v-model="phoneForm.verifyCode" placeholder="请输入验证码" maxlength="6" />
            <div class="captcha-box" @click="sendPhoneCode">
              <span v-if="!phoneGeneratedCode" class="captcha-btn">点击获取验证码</span>
              <span v-else class="captcha-code">{{ phoneGeneratedCode }}</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="phoneDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePhoneSubmit" :loading="phoneSubmitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改邮箱对话框 -->
    <el-dialog v-model="emailDialogVisible" title="修改邮箱" width="450px">
      <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-width="100px">
        <el-form-item label="当前邮箱">
          <span class="current-value">{{ userInfo?.email || '未设置' }}</span>
        </el-form-item>
        <el-form-item label="新邮箱" prop="email">
          <el-input v-model="emailForm.email" placeholder="请输入新邮箱" />
        </el-form-item>
        <el-form-item label="验证码" prop="verifyCode">
          <div class="verify-code-input">
            <el-input v-model="emailForm.verifyCode" placeholder="请输入验证码" maxlength="6" />
            <div class="captcha-box" @click="sendEmailCode">
              <span v-if="!emailGeneratedCode" class="captcha-btn">点击获取验证码</span>
              <span v-else class="captcha-code">{{ emailGeneratedCode }}</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emailDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEmailSubmit" :loading="emailSubmitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改身份证对话框 -->
    <el-dialog v-model="idCardDialogVisible" title="修改身份证" width="450px">
      <el-alert type="warning" :closable="false" style="margin-bottom: 16px;">
        修改身份证后需要重新进行实名认证审核
      </el-alert>
      <el-form ref="idCardFormRef" :model="idCardForm" :rules="idCardRules" label-width="100px">
        <el-form-item label="当前身份证">
          <span class="current-value">{{ maskIdCard(userInfo?.idCard) }}</span>
        </el-form-item>
        <el-form-item label="新身份证号" prop="idCard">
          <el-input v-model="idCardForm.idCard" placeholder="请输入新身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item label="验证码" prop="verifyCode">
          <div class="verify-code-input">
            <el-input v-model="idCardForm.verifyCode" placeholder="请输入验证码" maxlength="6" />
            <div class="captcha-box" @click="sendIdCardCode">
              <span v-if="!idCardGeneratedCode" class="captcha-btn">点击获取验证码</span>
              <span v-else class="captcha-code">{{ idCardGeneratedCode }}</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="idCardDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleIdCardSubmit" :loading="idCardSubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { User, Edit, Lock, Checked } from '@element-plus/icons-vue'
import { 
  getPersonalInfoApi, 
  updatePersonalInfoApi, 
  updatePhoneApi, 
  updateEmailApi,
  updateIdCardApi,
  type PersonalInfo 
} from '@/api/profile'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const loading = ref(false)
const userInfo = ref<PersonalInfo | null>(null)

// 编辑基本信息
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editSubmitting = ref(false)
const editForm = ref({ username: '' })
const editRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为2-20个字符', trigger: 'blur' }
  ]
}

// 生成6位随机验证码（字母+数字）
const generateVerifyCode = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let code = ''
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return code
}

// 修改手机号
const phoneDialogVisible = ref(false)
const phoneFormRef = ref<FormInstance>()
const phoneSubmitting = ref(false)
const phoneCooldown = ref(0)
const phoneForm = ref({ phone: '', verifyCode: '' })
const phoneGeneratedCode = ref('') // 存储生成的验证码

// 验证手机验证码
const validatePhoneCode = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入验证码'))
  } else if (!phoneGeneratedCode.value) {
    callback(new Error('请先获取验证码'))
  } else if (value !== phoneGeneratedCode.value) {
    callback(new Error('验证码错误'))
  } else {
    callback()
  }
}

const phoneRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, validator: validatePhoneCode, trigger: 'blur' }
  ]
}

// 修改邮箱
const emailDialogVisible = ref(false)
const emailFormRef = ref<FormInstance>()
const emailSubmitting = ref(false)
const emailCooldown = ref(0)
const emailForm = ref({ email: '', verifyCode: '' })
const emailGeneratedCode = ref('') // 存储生成的验证码

// 验证邮箱验证码
const validateEmailCode = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入验证码'))
  } else if (!emailGeneratedCode.value) {
    callback(new Error('请先获取验证码'))
  } else if (value !== emailGeneratedCode.value) {
    callback(new Error('验证码错误'))
  } else {
    callback()
  }
}

const emailRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, validator: validateEmailCode, trigger: 'blur' }
  ]
}

// 修改身份证
const idCardDialogVisible = ref(false)
const idCardFormRef = ref<FormInstance>()
const idCardSubmitting = ref(false)
const idCardForm = ref({ idCard: '', verifyCode: '' })
const idCardGeneratedCode = ref('')

// 验证身份证验证码
const validateIdCardCode = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入验证码'))
  } else if (!idCardGeneratedCode.value) {
    callback(new Error('请先获取验证码'))
  } else if (value !== idCardGeneratedCode.value) {
    callback(new Error('验证码错误'))
  } else {
    callback()
  }
}

// 验证身份证号格式
const validateIdCard = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入身份证号'))
  } else if (!/^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(value)) {
    callback(new Error('请输入正确的身份证号'))
  } else {
    callback()
  }
}

const idCardRules: FormRules = {
  idCard: [
    { required: true, validator: validateIdCard, trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, validator: validateIdCardCode, trigger: 'blur' }
  ]
}

// 脱敏显示身份证
const maskIdCard = (idCard?: string) => {
  if (!idCard) return '-'
  return idCard.replace(/^(.{6})(.*)(.{4})$/, '$1********$3')
}

// 实名状态
const realnameStatusType = computed(() => {
  const status = userInfo.value?.realnameStatus
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  if (status === 0) return 'warning'
  return 'info'
})

const realnameStatusText = computed(() => {
  const status = userInfo.value?.realnameStatus
  if (status === 1) return '已认证'
  if (status === 2) return '认证被驳回'
  if (status === 0) return '审核中'
  return '未认证'
})

// 获取个人信息
const fetchUserInfo = async () => {
  loading.value = true
  try {
    const res = await getPersonalInfoApi()
    userInfo.value = res as unknown as PersonalInfo
  } catch (error) {
    console.error('获取个人信息失败', error)
  } finally {
    loading.value = false
  }
}

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 头像上传
const beforeAvatarUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB')
    return false
  }
  return true
}

// 压缩图片
const compressImage = (file: File, maxWidth: number = 200, quality: number = 0.8): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height
        
        // 等比例缩放
        if (width > maxWidth) {
          height = (maxWidth / width) * height
          width = maxWidth
        }
        
        canvas.width = width
        canvas.height = height
        
        const ctx = canvas.getContext('2d')
        ctx?.drawImage(img, 0, 0, width, height)
        
        // 转为 base64，使用 jpeg 格式压缩
        const base64 = canvas.toDataURL('image/jpeg', quality)
        resolve(base64)
      }
      img.onerror = reject
      img.src = e.target?.result as string
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const handleAvatarUpload = async (options: UploadRequestOptions) => {
  const file = options.file
  try {
    // 压缩图片到 200px 宽度
    const compressedBase64 = await compressImage(file, 200, 0.8)
    await updatePersonalInfoApi({ avatar: compressedBase64 })
    ElMessage.success('头像更新成功')
    fetchUserInfo()
    userStore.getCurrentUser()
  } catch (error: any) {
    ElMessage.error(error.message || '头像更新失败')
  }
}

// 显示编辑对话框
const showEditDialog = () => {
  editForm.value.username = userInfo.value?.username || ''
  editDialogVisible.value = true
}

// 提交编辑
const handleEditSubmit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      await updatePersonalInfoApi({ username: editForm.value.username })
      ElMessage.success('信息更新成功')
      editDialogVisible.value = false
      fetchUserInfo()
      userStore.getCurrentUser()
    } catch (error: any) {
      ElMessage.error(error.message || '更新失败')
    } finally {
      editSubmitting.value = false
    }
  })
}

// 显示手机号对话框
const showPhoneDialog = () => {
  phoneForm.value = { phone: '', verifyCode: '' }
  phoneGeneratedCode.value = ''
  phoneCooldown.value = 0
  phoneDialogVisible.value = true
}

// 发送手机验证码
const sendPhoneCode = () => {
  if (!phoneForm.value.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }
  // 验证手机号格式
  if (!/^1[3-9]\d{9}$/.test(phoneForm.value.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  // 生成验证码
  phoneGeneratedCode.value = generateVerifyCode()
}

// 提交手机号修改
const handlePhoneSubmit = async () => {
  if (!phoneFormRef.value) return
  await phoneFormRef.value.validate(async (valid) => {
    if (!valid) return
    phoneSubmitting.value = true
    try {
      await updatePhoneApi(phoneForm.value)
      ElMessage.success('手机号修改成功')
      phoneDialogVisible.value = false
      fetchUserInfo()
    } catch (error: any) {
      ElMessage.error(error.message || '修改失败')
    } finally {
      phoneSubmitting.value = false
    }
  })
}

// 显示邮箱对话框
const showEmailDialog = () => {
  emailForm.value = { email: '', verifyCode: '' }
  emailGeneratedCode.value = ''
  emailCooldown.value = 0
  emailDialogVisible.value = true
}

// 发送邮箱验证码
const sendEmailCode = () => {
  if (!emailForm.value.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  // 验证邮箱格式
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailReg.test(emailForm.value.email)) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }
  // 生成验证码
  emailGeneratedCode.value = generateVerifyCode()
}

// 提交邮箱修改
const handleEmailSubmit = async () => {
  if (!emailFormRef.value) return
  await emailFormRef.value.validate(async (valid) => {
    if (!valid) return
    emailSubmitting.value = true
    try {
      await updateEmailApi(emailForm.value)
      ElMessage.success('邮箱修改成功')
      emailDialogVisible.value = false
      fetchUserInfo()
    } catch (error: any) {
      ElMessage.error(error.message || '修改失败')
    } finally {
      emailSubmitting.value = false
    }
  })
}

// 显示身份证对话框
const showIdCardDialog = () => {
  idCardForm.value = { idCard: '', verifyCode: '' }
  idCardGeneratedCode.value = ''
  idCardDialogVisible.value = true
}

// 发送身份证验证码
const sendIdCardCode = () => {
  if (!idCardForm.value.idCard) {
    ElMessage.warning('请先输入身份证号')
    return
  }
  // 验证身份证格式
  if (!/^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(idCardForm.value.idCard)) {
    ElMessage.warning('请输入正确的身份证号')
    return
  }
  // 生成验证码
  idCardGeneratedCode.value = generateVerifyCode()
}

// 提交身份证修改
const handleIdCardSubmit = async () => {
  if (!idCardFormRef.value) return
  await idCardFormRef.value.validate(async (valid) => {
    if (!valid) return
    idCardSubmitting.value = true
    try {
      await updateIdCardApi({ idCard: idCardForm.value.idCard })
      ElMessage.success('身份证修改成功，需要重新审核')
      idCardDialogVisible.value = false
      fetchUserInfo()
    } catch (error: any) {
      ElMessage.error(error.message || '修改失败')
    } finally {
      idCardSubmitting.value = false
    }
  })
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.personal-info {
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

.info-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-content {
  display: flex;
  gap: 40px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-upload {
  display: inline-block;
}

.info-desc {
  flex: 1;
}

.info-desc :deep(.el-descriptions__cell) {
  padding: 12px 16px;
}

.security-card {
  margin-bottom: 20px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.security-item:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-text h4 {
  font-size: 14px;
  color: #303133;
  margin: 0 0 4px 0;
}

.item-text p {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

.current-value {
  color: #909399;
}

.verify-code-input {
  display: flex;
  gap: 12px;
}

.verify-code-input .el-input {
  flex: 1;
}

.captcha-box {
  width: 120px;
  height: 32px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}

.captcha-btn {
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
  color: #fff;
  font-size: 12px;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.captcha-btn:hover {
  opacity: 0.9;
}

.captcha-code {
  background: linear-gradient(135deg, #36cfc9 0%, #40a9ff 100%);
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  font-family: 'Courier New', monospace;
  letter-spacing: 4px;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.3);
  position: relative;
}

.captcha-code::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    linear-gradient(45deg, transparent 45%, rgba(255,255,255,0.1) 50%, transparent 55%),
    linear-gradient(-45deg, transparent 45%, rgba(255,255,255,0.1) 50%, transparent 55%);
  pointer-events: none;
}

/* 信用评分卡片 */
.credit-card {
  margin-bottom: 20px;
}

.credit-content {
  min-height: 200px;
}

.credit-score-display {
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  margin-bottom: 24px;
}

.score-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.score-number {
  font-size: 36px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 4px;
}

.score-number.excellent {
  color: #52c41a;
}

.score-number.good {
  color: #1890ff;
}

.score-number.normal {
  color: #13c2c2;
}

.score-number.fair {
  color: #faad14;
}

.score-number.poor {
  color: #f5222d;
}

.score-label {
  font-size: 14px;
  color: #666;
}

.score-level {
  flex: 1;
  color: #fff;
}

.score-level .el-tag {
  font-size: 18px;
  padding: 8px 20px;
}

.credit-details {
  margin-bottom: 24px;
}

.credit-details h4 {
  font-size: 14px;
  color: #303133;
  margin: 0 0 12px 0;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  color: #606266;
  font-size: 14px;
}

.detail-value {
  color: #303133;
  font-weight: 500;
}

.detail-value.success {
  color: #67c23a;
}

.credit-suggestions h4 {
  font-size: 14px;
  color: #303133;
  margin: 0 0 12px 0;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px;
  background: #f0f9ff;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.suggestion-item .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}
</style>
