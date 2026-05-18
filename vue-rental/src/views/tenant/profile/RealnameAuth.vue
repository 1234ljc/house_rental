<template>
  <div class="realname-auth">
    <div class="page-header">
      <h3>实名认证</h3>
      <p class="desc">完成实名认证后，可享受更多平台服务</p>
    </div>

    <!-- 认证状态展示 -->
    <div class="status-card" v-if="realnameInfo">
      <div class="status-icon" :class="statusClass">
        <el-icon :size="48">
          <component :is="statusIcon" />
        </el-icon>
      </div>
      <div class="status-info">
        <h4>{{ statusText }}</h4>
        <p v-if="realnameInfo.realnameStatus === 1" class="success-text">
          认证通过时间：{{ formatTime(realnameInfo.realnameTime) }}
        </p>
        <p v-else-if="realnameInfo.realnameStatus === 2" class="error-text">
          驳回原因：{{ realnameInfo.auditReason || '未说明' }}
        </p>
        <p v-else-if="realnameInfo.realnameStatus === 0" class="warning-text">
          提交时间：{{ formatTime(realnameInfo.authInfo?.createTime) }}
        </p>
      </div>
    </div>

    <!-- 已认证信息展示 -->
    <el-card v-if="realnameInfo?.realnameStatus === 1" class="info-card">
      <template #header>
        <span>认证信息</span>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="真实姓名">
          {{ maskName(realnameInfo.realName) }}
        </el-descriptions-item>
        <el-descriptions-item label="身份证号">
          {{ maskIdCard(realnameInfo.idCard) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 认证表单 -->
    <el-card v-if="showForm" class="form-card">
      <template #header>
        <span>{{ realnameInfo?.realnameStatus === 2 ? '重新提交认证' : '提交实名认证' }}</span>
      </template>
      
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-width="120px"
        class="auth-form"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" maxlength="20" />
        </el-form-item>

        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入18位身份证号" maxlength="18" />
        </el-form-item>

        <el-form-item label="身份证正面" prop="idCardFront">
          <div class="upload-area">
            <el-upload
              class="id-card-upload"
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="(options: any) => handleUpload(options, 'front')"
              accept="image/*"
            >
              <div v-if="form.idCardFront" class="uploaded-image">
                <img :src="form.idCardFront" alt="身份证正面" />
                <div class="mask">
                  <el-icon><Plus /></el-icon>
                  <span>重新上传</span>
                </div>
              </div>
              <div v-else class="upload-placeholder">
                <el-icon :size="32"><Plus /></el-icon>
                <p>上传身份证正面</p>
                <span class="tip">人像面</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="身份证反面" prop="idCardBack">
          <div class="upload-area">
            <el-upload
              class="id-card-upload"
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="(options: any) => handleUpload(options, 'back')"
              accept="image/*"
            >
              <div v-if="form.idCardBack" class="uploaded-image">
                <img :src="form.idCardBack" alt="身份证反面" />
                <div class="mask">
                  <el-icon><Plus /></el-icon>
                  <span>重新上传</span>
                </div>
              </div>
              <div v-else class="upload-placeholder">
                <el-icon :size="32"><Plus /></el-icon>
                <p>上传身份证反面</p>
                <span class="tip">国徽面</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            提交认证
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 审核中提示 -->
    <el-card v-if="realnameInfo?.realnameStatus === 0" class="pending-card">
      <el-result icon="info" title="认证审核中" sub-title="您的实名认证申请正在审核中，请耐心等待">
        <template #extra>
          <el-button type="primary" @click="fetchRealnameInfo">刷新状态</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Plus, CircleCheck, CircleClose, Clock } from '@element-plus/icons-vue'
import { getRealnameInfoApi, submitRealnameAuthApi, type RealnameInfo } from '@/api/profile'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const realnameInfo = ref<RealnameInfo | null>(null)
const submitting = ref(false)
const loading = ref(false)

const form = ref({
  realName: '',
  idCard: '',
  idCardFront: '',
  idCardBack: ''
})

// 身份证号验证
const validateIdCard = (rule: any, value: string, callback: any) => {
  const reg = /^[1-9]\d{5}(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/
  if (!value) {
    callback(new Error('请输入身份证号'))
  } else if (!reg.test(value)) {
    callback(new Error('请输入正确的18位身份证号'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为2-20个字符', trigger: 'blur' }
  ],
  idCard: [
    { required: true, validator: validateIdCard, trigger: 'blur' }
  ],
  idCardFront: [
    { required: true, message: '请上传身份证正面照片', trigger: 'change' }
  ],
  idCardBack: [
    { required: true, message: '请上传身份证反面照片', trigger: 'change' }
  ]
}

// 状态相关计算属性
// realnameStatus: -1=未认证, 0=审核中, 1=已通过, 2=已驳回
const statusClass = computed(() => {
  if (!realnameInfo.value) return 'default'
  const status = realnameInfo.value.realnameStatus
  if (status === 1) return 'success'
  if (status === 2) return 'error'
  if (status === 0) return 'warning'
  return 'default' // -1 未认证
})

const statusIcon = computed(() => {
  if (!realnameInfo.value) return Clock
  const status = realnameInfo.value.realnameStatus
  if (status === 1) return CircleCheck
  if (status === 2) return CircleClose
  if (status === 0) return Clock
  return Clock // -1 未认证
})

const statusText = computed(() => {
  if (!realnameInfo.value) return '未认证'
  const status = realnameInfo.value.realnameStatus
  if (status === 1) return '已认证'
  if (status === 2) return '认证被驳回'
  if (status === 0) return '审核中'
  return '未认证' // -1 未认证
})

const showForm = computed(() => {
  if (!realnameInfo.value) return true
  const status = realnameInfo.value.realnameStatus
  // 未认证(-1)或被驳回(2)时显示表单
  return status === -1 || status === 2 || status === null || status === undefined
})

// 获取实名认证信息
const fetchRealnameInfo = async () => {
  loading.value = true
  try {
    const res = await getRealnameInfoApi()
    realnameInfo.value = res as unknown as RealnameInfo
  } catch (error) {
    console.error('获取实名认证信息失败', error)
  } finally {
    loading.value = false
  }
}

// 上传前校验
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB')
    return false
  }
  return true
}

// 处理上传（模拟，实际项目需要对接文件上传服务）
const handleUpload = (options: UploadRequestOptions, type: 'front' | 'back') => {
  const file = options.file
  const reader = new FileReader()
  reader.onload = (e) => {
    const base64 = e.target?.result as string
    if (type === 'front') {
      form.value.idCardFront = base64
    } else {
      form.value.idCardBack = base64
    }
  }
  reader.readAsDataURL(file)
}

// 提交认证
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      await submitRealnameAuthApi(form.value)
      ElMessage.success('实名认证申请已提交')
      resetForm()
      fetchRealnameInfo()
      // 刷新用户信息以更新实名状态
      userStore.refreshUserInfo()
    } catch (error: any) {
      ElMessage.error(error.message || '提交失败')
    } finally {
      submitting.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  form.value = {
    realName: '',
    idCard: '',
    idCardFront: '',
    idCardBack: ''
  }
  formRef.value?.resetFields()
}

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 脱敏姓名
const maskName = (name?: string) => {
  if (!name) return '-'
  if (name.length <= 1) return name
  return name[0] + '*'.repeat(name.length - 1)
}

// 脱敏身份证号
const maskIdCard = (idCard?: string) => {
  if (!idCard) return '-'
  if (idCard.length < 8) return idCard
  return idCard.substring(0, 4) + '**********' + idCard.substring(idCard.length - 4)
}

onMounted(() => {
  fetchRealnameInfo()
})
</script>

<style scoped>
.realname-auth {
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

.status-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
}

.status-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-icon.success {
  background: #f0f9eb;
  color: #67c23a;
}

.status-icon.error {
  background: #fef0f0;
  color: #f56c6c;
}

.status-icon.warning {
  background: #fdf6ec;
  color: #e6a23c;
}

.status-icon.default {
  background: #f4f4f5;
  color: #909399;
}

.status-info h4 {
  font-size: 18px;
  color: #303133;
  margin: 0 0 8px 0;
}

.status-info p {
  font-size: 14px;
  margin: 0;
}

.success-text {
  color: #67c23a;
}

.error-text {
  color: #f56c6c;
}

.warning-text {
  color: #e6a23c;
}

.info-card,
.form-card,
.pending-card {
  margin-bottom: 20px;
}

.auth-form {
  max-width: 600px;
}

.upload-area {
  width: 200px;
}

.id-card-upload {
  width: 200px;
  height: 130px;
}

.id-card-upload :deep(.el-upload) {
  width: 100%;
  height: 100%;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.3s;
}

.id-card-upload :deep(.el-upload:hover) {
  border-color: #1890ff;
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.upload-placeholder p {
  margin: 8px 0 4px;
  font-size: 14px;
}

.upload-placeholder .tip {
  font-size: 12px;
  color: #c0c4cc;
}

.uploaded-image {
  width: 100%;
  height: 100%;
  position: relative;
}

.uploaded-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.uploaded-image .mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
}

.uploaded-image:hover .mask {
  opacity: 1;
}

.uploaded-image .mask span {
  margin-top: 8px;
  font-size: 12px;
}
</style>
