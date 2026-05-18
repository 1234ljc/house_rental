<template>
  <div class="personal-card">
    <div class="page-header">
      <h3>个人名片</h3>
      <p class="desc">您的公开信息，租客可以通过点击头像查看</p>
    </div>

    <el-card class="card-preview" v-loading="loading">
      <div class="card-content">
        <div class="avatar-area">
          <el-avatar :size="100" :src="cardInfo?.avatar || defaultAvatar">
            <el-icon :size="50"><User /></el-icon>
          </el-avatar>
          <div class="user-type-tag">
            <el-tag type="warning" size="small">房东</el-tag>
          </div>
        </div>

        <div class="info-area">
          <h2 class="username">{{ cardInfo?.username || '-' }}</h2>
          
          <div class="info-item">
            <el-icon><CircleCheck /></el-icon>
            <span class="label">实名状态：</span>
            <el-tag :type="realnameStatusType" size="small">{{ realnameStatusText }}</el-tag>
          </div>

          <div class="info-item" v-if="cardInfo?.realName && cardInfo?.realnameStatus === 1">
            <el-icon><User /></el-icon>
            <span class="label">真实姓名：</span>
            <span class="value">{{ maskName(cardInfo.realName) }}</span>
          </div>

          <div class="info-item" v-if="cardInfo?.phone">
            <el-icon><Phone /></el-icon>
            <span class="label">联系电话：</span>
            <span class="value">{{ maskPhone(cardInfo.phone) }}</span>
          </div>

          <div class="info-item" v-if="cardInfo?.email">
            <el-icon><Message /></el-icon>
            <span class="label">电子邮箱：</span>
            <span class="value">{{ maskEmail(cardInfo.email) }}</span>
          </div>

          <div class="info-item">
            <el-icon><Calendar /></el-icon>
            <span class="label">注册时间：</span>
            <span class="value">{{ formatDate(cardInfo?.createTime) }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="privacy-tips">
      <template #header><span>隐私说明</span></template>
      <ul class="tips-list">
        <li>您的手机号和邮箱将以脱敏形式展示给租客</li>
        <li>真实姓名仅在完成实名认证后显示，且会进行脱敏处理</li>
        <li>租客可以在聊天界面点击您的头像查看此名片</li>
        <li>如需修改展示信息，请前往"个人信息管理"页面</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { User, Phone, Message, Calendar, CircleCheck } from '@element-plus/icons-vue'
import { getMyCardApi, type UserCard } from '@/api/profile'

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const loading = ref(false)
const cardInfo = ref<UserCard | null>(null)

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

const maskName = (name: string) => name ? name[0] + '*'.repeat(name.length - 1) : '-'
const maskPhone = (phone: string) => phone ? phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '-'
const maskEmail = (email: string) => {
  if (!email) return '-'
  const [name, domain] = email.split('@')
  if (!domain) return email
  const maskedName = name.length > 2 ? name[0] + '*'.repeat(name.length - 2) + name[name.length - 1] : name[0] + '*'
  return maskedName + '@' + domain
}
const formatDate = (time?: string) => time ? new Date(time).toLocaleDateString('zh-CN') : '-'

const fetchCardInfo = async () => {
  loading.value = true
  try {
    const res = await getMyCardApi()
    cardInfo.value = res as unknown as UserCard
  } catch (error) { console.error('获取名片信息失败', error) }
  finally { loading.value = false }
}

onMounted(() => { fetchCardInfo() })
</script>

<style scoped>
.personal-card { padding: 8px; }
.page-header { margin-bottom: 24px; }
.page-header h3 { font-size: 18px; color: #303133; margin: 0 0 8px 0; }
.page-header .desc { font-size: 14px; color: #909399; margin: 0; }
.card-preview { margin-bottom: 20px; }
.card-content { display: flex; gap: 40px; padding: 20px; }
.avatar-area { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.user-type-tag { margin-top: 8px; }
.info-area { flex: 1; }
.username { font-size: 24px; color: #303133; margin: 0 0 20px 0; }
.info-item { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; font-size: 14px; color: #606266; }
.info-item .el-icon { color: #909399; }
.info-item .label { color: #909399; min-width: 80px; }
.info-item .value { color: #303133; }
.privacy-tips { background: #fafafa; }
.tips-list { margin: 0; padding-left: 20px; color: #909399; font-size: 14px; line-height: 2; }
</style>
