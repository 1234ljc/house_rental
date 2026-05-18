<template>
  <el-dialog 
    v-model="visible" 
    title="用户名片" 
    width="400px"
    :close-on-click-modal="true"
  >
    <div class="user-card-content" v-loading="loading">
      <div class="card-header">
        <el-avatar :size="80" :src="cardInfo?.avatar || defaultAvatar">
          <el-icon :size="40"><User /></el-icon>
        </el-avatar>
        <div class="header-info">
          <h3 class="username">{{ cardInfo?.username || '-' }}</h3>
          <el-tag :type="cardInfo?.userType === 2 ? 'warning' : 'primary'" size="small">
            {{ cardInfo?.userType === 2 ? '房东' : '租客' }}
          </el-tag>
        </div>
      </div>

      <el-divider />

      <div class="card-body">
        <div class="info-row">
          <span class="label">
            <el-icon><CircleCheck /></el-icon>
            实名状态
          </span>
          <el-tag :type="realnameStatusType" size="small">{{ realnameStatusText }}</el-tag>
        </div>

        <div class="info-row" v-if="cardInfo?.realName && cardInfo?.realnameStatus === 1">
          <span class="label">
            <el-icon><User /></el-icon>
            真实姓名
          </span>
          <span class="value">{{ maskName(cardInfo.realName) }}</span>
        </div>

        <div class="info-row" v-if="cardInfo?.phone">
          <span class="label">
            <el-icon><Phone /></el-icon>
            联系电话
          </span>
          <span class="value">{{ maskPhone(cardInfo.phone) }}</span>
        </div>

        <div class="info-row" v-if="cardInfo?.email">
          <span class="label">
            <el-icon><Message /></el-icon>
            电子邮箱
          </span>
          <span class="value">{{ maskEmail(cardInfo.email) }}</span>
        </div>

        <div class="info-row">
          <span class="label">
            <el-icon><Calendar /></el-icon>
            注册时间
          </span>
          <span class="value">{{ formatDate(cardInfo?.createTime) }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { User, Phone, Message, Calendar, CircleCheck } from '@element-plus/icons-vue'
import { getUserCardApi, type UserCard } from '@/api/profile'

const props = defineProps<{
  modelValue: boolean
  userId?: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const loading = ref(false)
const cardInfo = ref<UserCard | null>(null)

// 实名状态
const realnameStatusType = computed(() => {
  const status = cardInfo.value?.realnameStatus
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  if (status === 0) return 'warning'
  return 'info'
})

const realnameStatusText = computed(() => {
  const status = cardInfo.value?.realnameStatus
  if (status === 1) return '已认证'
  if (status === 2) return '认证被驳回'
  if (status === 0) return '审核中'
  return '未认证'
})

// 脱敏处理
const maskName = (name: string) => {
  if (!name) return '-'
  if (name.length <= 1) return name
  return name[0] + '*'.repeat(name.length - 1)
}

const maskPhone = (phone: string) => {
  if (!phone) return '-'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

const maskEmail = (email: string) => {
  if (!email) return '-'
  const [name, domain] = email.split('@')
  if (!domain) return email
  const maskedName = name.length > 2 
    ? name[0] + '*'.repeat(name.length - 2) + name[name.length - 1]
    : name[0] + '*'
  return maskedName + '@' + domain
}

const formatDate = (time?: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleDateString('zh-CN')
}

const fetchCardInfo = async () => {
  if (!props.userId) return
  loading.value = true
  try {
    const res = await getUserCardApi(props.userId)
    cardInfo.value = res as unknown as UserCard
  } catch (error) {
    console.error('获取用户名片失败', error)
  } finally {
    loading.value = false
  }
}

// 监听弹窗打开
watch(() => props.modelValue, (val) => {
  if (val && props.userId) {
    fetchCardInfo()
  }
})
</script>

<style scoped>
.user-card-content {
  padding: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.username {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-row .label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

.info-row .value {
  color: #303133;
  font-size: 14px;
}
</style>
