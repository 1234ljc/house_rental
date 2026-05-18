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
          <el-descriptions-item label="用户名">{{ userInfo?.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户类型">
            <el-tag type="danger">管理员</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ userInfo?.realName || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">
            {{ userInfo?.phone || '-' }}
            <el-button type="primary" link size="small" @click="showPhoneDialog">修改</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">
            {{ userInfo?.email || '未设置' }}
            <el-button type="primary" link size="small" @click="showEmailDialog">修改</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatTime(userInfo?.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <!-- 编辑基本信息对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑基本信息" width="450px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="请输入用户名" maxlength="20" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="editForm.realName" placeholder="请输入真实姓名" maxlength="20" />
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
        <el-form-item label="当前手机号"><span class="current-value">{{ userInfo?.phone }}</span></el-form-item>
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
        <el-form-item label="当前邮箱"><span class="current-value">{{ userInfo?.email || '未设置' }}</span></el-form-item>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { User, Edit } from '@element-plus/icons-vue'
import { getPersonalInfoApi, updatePersonalInfoApi, updatePhoneApi, updateEmailApi, type PersonalInfo } from '@/api/profile'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const loading = ref(false)
const userInfo = ref<PersonalInfo | null>(null)

const generateVerifyCode = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let code = ''
  for (let i = 0; i < 6; i++) code += chars.charAt(Math.floor(Math.random() * chars.length))
  return code
}

// 编辑基本信息
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editSubmitting = ref(false)
const editForm = ref({ username: '', realName: '' })
const editRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 2, max: 20, message: '用户名长度为2-20个字符', trigger: 'blur' }]
}

// 修改手机号
const phoneDialogVisible = ref(false)
const phoneFormRef = ref<FormInstance>()
const phoneSubmitting = ref(false)
const phoneForm = ref({ phone: '', verifyCode: '' })
const phoneGeneratedCode = ref('')
const validatePhoneCode = (rule: any, value: string, callback: any) => {
  if (!value) callback(new Error('请输入验证码'))
  else if (!phoneGeneratedCode.value) callback(new Error('请先获取验证码'))
  else if (value !== phoneGeneratedCode.value) callback(new Error('验证码错误'))
  else callback()
}
const phoneRules: FormRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  verifyCode: [{ required: true, validator: validatePhoneCode, trigger: 'blur' }]
}

// 修改邮箱
const emailDialogVisible = ref(false)
const emailFormRef = ref<FormInstance>()
const emailSubmitting = ref(false)
const emailForm = ref({ email: '', verifyCode: '' })
const emailGeneratedCode = ref('')
const validateEmailCode = (rule: any, value: string, callback: any) => {
  if (!value) callback(new Error('请输入验证码'))
  else if (!emailGeneratedCode.value) callback(new Error('请先获取验证码'))
  else if (value !== emailGeneratedCode.value) callback(new Error('验证码错误'))
  else callback()
}
const emailRules: FormRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  verifyCode: [{ required: true, validator: validateEmailCode, trigger: 'blur' }]
}

const fetchUserInfo = async () => {
  loading.value = true
  try {
    const res = await getPersonalInfoApi()
    userInfo.value = res as unknown as PersonalInfo
  } catch (error) { console.error('获取个人信息失败', error) }
  finally { loading.value = false }
}

const formatTime = (time?: string) => time ? new Date(time).toLocaleString('zh-CN') : '-'

const compressImage = (file: File, maxWidth: number = 200, quality: number = 0.8): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width, height = img.height
        if (width > maxWidth) { height = (maxWidth / width) * height; width = maxWidth }
        canvas.width = width; canvas.height = height
        canvas.getContext('2d')?.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', quality))
      }
      img.onerror = reject
      img.src = e.target?.result as string
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const beforeAvatarUpload = (file: File) => {
  if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片文件'); return false }
  if (file.size / 1024 / 1024 > 2) { ElMessage.error('图片大小不能超过2MB'); return false }
  return true
}

const handleAvatarUpload = async (options: UploadRequestOptions) => {
  try {
    const base64 = await compressImage(options.file, 200, 0.8)
    await updatePersonalInfoApi({ avatar: base64 })
    ElMessage.success('头像更新成功')
    fetchUserInfo(); userStore.getCurrentUser()
  } catch (error: any) { ElMessage.error(error.message || '头像更新失败') }
}

const showEditDialog = () => { 
  editForm.value.username = userInfo.value?.username || ''
  editForm.value.realName = userInfo.value?.realName || ''
  editDialogVisible.value = true 
}
const handleEditSubmit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      await updatePersonalInfoApi({ username: editForm.value.username, realName: editForm.value.realName })
      ElMessage.success('信息更新成功'); editDialogVisible.value = false; fetchUserInfo(); userStore.getCurrentUser()
    } catch (error: any) { ElMessage.error(error.message || '更新失败') }
    finally { editSubmitting.value = false }
  })
}

const showPhoneDialog = () => { phoneForm.value = { phone: '', verifyCode: '' }; phoneGeneratedCode.value = ''; phoneDialogVisible.value = true }
const sendPhoneCode = () => {
  if (!phoneForm.value.phone) { ElMessage.warning('请先输入手机号'); return }
  if (!/^1[3-9]\d{9}$/.test(phoneForm.value.phone)) { ElMessage.warning('请输入正确的手机号'); return }
  phoneGeneratedCode.value = generateVerifyCode()
}
const handlePhoneSubmit = async () => {
  if (!phoneFormRef.value) return
  await phoneFormRef.value.validate(async (valid) => {
    if (!valid) return
    phoneSubmitting.value = true
    try {
      await updatePhoneApi(phoneForm.value)
      ElMessage.success('手机号修改成功'); phoneDialogVisible.value = false; fetchUserInfo()
    } catch (error: any) { ElMessage.error(error.message || '修改失败') }
    finally { phoneSubmitting.value = false }
  })
}

const showEmailDialog = () => { emailForm.value = { email: '', verifyCode: '' }; emailGeneratedCode.value = ''; emailDialogVisible.value = true }
const sendEmailCode = () => {
  if (!emailForm.value.email) { ElMessage.warning('请先输入邮箱'); return }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailForm.value.email)) { ElMessage.warning('请输入正确的邮箱格式'); return }
  emailGeneratedCode.value = generateVerifyCode()
}
const handleEmailSubmit = async () => {
  if (!emailFormRef.value) return
  await emailFormRef.value.validate(async (valid) => {
    if (!valid) return
    emailSubmitting.value = true
    try {
      await updateEmailApi(emailForm.value)
      ElMessage.success('邮箱修改成功'); emailDialogVisible.value = false; fetchUserInfo()
    } catch (error: any) { ElMessage.error(error.message || '修改失败') }
    finally { emailSubmitting.value = false }
  })
}

onMounted(() => { fetchUserInfo() })
</script>

<style scoped>
.personal-info { padding: 8px; }
.page-header { margin-bottom: 24px; }
.page-header h3 { font-size: 18px; color: #303133; margin: 0 0 8px 0; }
.page-header .desc { font-size: 14px; color: #909399; margin: 0; }
.info-card { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.info-content { display: flex; gap: 40px; }
.avatar-section { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.info-desc { flex: 1; }
.info-desc :deep(.el-descriptions__cell) { padding: 12px 16px; }
.current-value { color: #909399; }
.verify-code-input { display: flex; gap: 12px; }
.verify-code-input .el-input { flex: 1; }
.captcha-box { width: 120px; height: 32px; border-radius: 4px; cursor: pointer; display: flex; align-items: center; justify-content: center; user-select: none; }
.captcha-btn { background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%); color: #fff; font-size: 12px; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; border-radius: 4px; }
.captcha-code { background: linear-gradient(135deg, #36cfc9 0%, #40a9ff 100%); color: #fff; font-size: 18px; font-weight: bold; font-family: 'Courier New', monospace; letter-spacing: 4px; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; border-radius: 4px; text-shadow: 1px 1px 2px rgba(0,0,0,0.3); }
</style>
