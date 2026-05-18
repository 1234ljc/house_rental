<template>
  <div class="message-page">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card" :class="{ active: currentType === undefined }" @click="filterByType(undefined)">
        <div class="stat-num">{{ stats.unread }}</div>
        <div class="stat-label">全部未读</div>
      </div>
      <div class="stat-card" :class="{ active: currentType === 1 }" @click="filterByType(1)">
        <div class="stat-num">{{ stats.type1 || 0 }}</div>
        <div class="stat-label">系统通知</div>
      </div>
      <div class="stat-card" :class="{ active: currentType === 4 }" @click="filterByType(4)">
        <div class="stat-num">{{ stats.type4 || 0 }}</div>
        <div class="stat-label">合同通知</div>
      </div>
      <div class="stat-card" :class="{ active: currentType === 5 }" @click="filterByType(5)">
        <div class="stat-num">{{ stats.type5 || 0 }}</div>
        <div class="stat-label">支付通知</div>
      </div>
      <div class="stat-card" :class="{ active: currentType === 6 }" @click="filterByType(6)">
        <div class="stat-num">{{ stats.type6 || 0 }}</div>
        <div class="stat-label">问题反馈</div>
      </div>
    </div>

    <!-- 操作栏 -->
    <el-card class="action-card">
      <div class="action-row">
        <el-radio-group v-model="readFilter" @change="loadList">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="0">未读</el-radio-button>
          <el-radio-button :value="1">已读</el-radio-button>
        </el-radio-group>
        <div class="action-btns">
          <el-button type="primary" link @click="handleMarkAllRead" :disabled="stats.unread === 0">
            <el-icon><Check /></el-icon> 全部已读
          </el-button>
          <el-button type="danger" link @click="handleClearRead">
            <el-icon><Delete /></el-icon> 清除已读
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 消息列表 -->
    <div class="message-list" v-loading="loading">
      <el-empty v-if="!loading && messages.length === 0" description="暂无消息" />
      <div v-else class="message-item" v-for="item in messages" :key="item.notifyId"
           :class="{ unread: item.isRead === 0 }" @click="handleClickMessage(item)">
        <div class="message-icon" :class="'type-' + item.notifyType">
          <el-icon><component :is="getTypeIcon(item.notifyType)" /></el-icon>
        </div>
        <div class="message-content">
          <div class="message-title">
            <span>{{ item.title }}</span>
            <el-tag v-if="item.isRead === 0" type="danger" size="small">未读</el-tag>
            <el-tag :type="getTypeTagType(item.notifyType)" size="small">{{ getTypeName(item.notifyType) }}</el-tag>
          </div>
          <div class="message-text">{{ item.content }}</div>
          <div class="message-footer">
            <span class="message-time">{{ formatTime(item.createTime) }}</span>
            <span v-if="item.relatedId" class="message-link">点击查看详情 →</span>
          </div>
        </div>
        <div class="message-actions">
          <el-button type="danger" link size="small" @click.stop="handleDelete(item)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > 10">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="10"
        v-model:current-page="page" @current-change="loadList" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Bell, Calendar, Document, Tickets, Money, ChatDotRound, Warning } from '@element-plus/icons-vue'
import {
  getNotificationListApi, getNotificationStatsApi, markAsReadApi,
  markAllAsReadApi, deleteNotificationApi, clearReadNotificationsApi
} from '@/api/notification'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const messages = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const currentType = ref<number | undefined>(undefined)
const readFilter = ref<number | undefined>(undefined)
const stats = reactive<any>({ unread: 0 })

// 判断是租客还是房东
const isLandlord = computed(() => route.path.startsWith('/landlord'))
const baseUrl = computed(() => isLandlord.value ? '/landlord' : '/tenant')

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return date.toLocaleDateString('zh-CN')
}

const getTypeIcon = (type: number) => {
  const icons: any = { 1: Warning, 2: Calendar, 3: Document, 4: Tickets, 5: Money, 6: ChatDotRound }
  return icons[type] || Bell
}

const getTypeName = (type: number) => {
  const names: any = { 1: '系统', 2: '预约', 3: '申请', 4: '合同', 5: '支付', 6: '反馈' }
  return names[type] || '通知'
}

const getTypeTagType = (type: number) => {
  const types: any = { 1: 'info', 2: 'success', 3: 'warning', 4: 'primary', 5: 'danger', 6: '' }
  return types[type] || 'info'
}

// 根据通知类型跳转到相关页面
const getTargetRoute = (item: any) => {
  const type = item.notifyType
  const relatedId = item.relatedId
  
  if (isLandlord.value) {
    // 房东端路由
    switch (type) {
      case 1: // 系统通知 - 房源审核
        if (item.title?.includes('房源')) return '/landlord/house/list'
        return null
      case 4: // 合同通知
        return '/landlord/rental'
      case 5: // 支付通知
        return '/landlord/finance'
      case 6: // 问题反馈
        return '/landlord/after'
      default:
        return null
    }
  } else {
    // 租客端路由
    switch (type) {
      case 1: // 系统通知 - 实名认证
        if (item.title?.includes('实名')) return '/tenant/profile/realname'
        return null
      case 4: // 合同通知
        return '/tenant/contract'
      case 5: // 支付通知
        return '/tenant/payment'
      case 6: // 问题反馈
        return '/tenant/after'
      default:
        return null
    }
  }
}

const loadStats = async () => {
  try {
    const res: any = await getNotificationStatsApi()
    Object.assign(stats, res)
  } catch (e) { console.error(e) }
}

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getNotificationListApi({
      notifyType: currentType.value,
      isRead: readFilter.value,
      page: page.value,
      size: 10
    })
    messages.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const filterByType = (type: number | undefined) => {
  currentType.value = type
  page.value = 1
  loadList()
}

const handleClickMessage = async (item: any) => {
  // 标记为已读
  if (item.isRead === 0) {
    try {
      await markAsReadApi(item.notifyId)
      item.isRead = 1
      loadStats()
    } catch (e) { console.error(e) }
  }
  
  // 跳转到相关页面
  const targetRoute = getTargetRoute(item)
  if (targetRoute) {
    router.push(targetRoute)
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllAsReadApi()
    ElMessage.success('已全部标记为已读')
    loadStats()
    loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const handleDelete = async (item: any) => {
  try {
    await ElMessageBox.confirm('确定删除该消息吗？', '删除确认', { type: 'warning' })
    await deleteNotificationApi(item.notifyId)
    ElMessage.success('已删除')
    loadStats()
    loadList()
  } catch (e) { /* cancel */ }
}

const handleClearRead = async () => {
  try {
    await ElMessageBox.confirm('确定清除所有已读消息吗？', '清除确认', { type: 'warning' })
    await clearReadNotificationsApi()
    ElMessage.success('已清除')
    loadStats()
    loadList()
  } catch (e) { /* cancel */ }
}

onMounted(() => {
  loadStats()
  loadList()
})
</script>

<style scoped>
.message-page { padding: 20px; background: #f5f5f5; min-height: calc(100vh - 60px); }

.stats-cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 15px; text-align: center; cursor: pointer; 
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); transition: all 0.3s; border: 2px solid transparent; }
.stat-card:hover { transform: translateY(-2px); }
.stat-card.active { border-color: #409eff; background: #ecf5ff; }
.stat-num { font-size: 24px; font-weight: bold; color: #409eff; margin-bottom: 5px; }
.stat-label { font-size: 13px; color: #666; }

.action-card { margin-bottom: 20px; }
.action-row { display: flex; justify-content: space-between; align-items: center; }
.action-btns { display: flex; gap: 15px; }

.message-list { display: flex; flex-direction: column; gap: 10px; }
.message-item { display: flex; align-items: flex-start; gap: 15px; background: #fff; border-radius: 8px; 
  padding: 15px 20px; cursor: pointer; transition: all 0.3s; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.message-item:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.message-item.unread { background: #fef0f0; border-left: 3px solid #f56c6c; }

.message-icon { width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; 
  justify-content: center; font-size: 18px; color: #fff; flex-shrink: 0; }
.message-icon.type-1 { background: #909399; }
.message-icon.type-2 { background: #67c23a; }
.message-icon.type-3 { background: #e6a23c; }
.message-icon.type-4 { background: #409eff; }
.message-icon.type-5 { background: #f56c6c; }
.message-icon.type-6 { background: #9c27b0; }

.message-content { flex: 1; min-width: 0; }
.message-title { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; flex-wrap: wrap; }
.message-title span { font-size: 15px; font-weight: 500; }
.message-text { font-size: 14px; color: #666; line-height: 1.5; margin-bottom: 8px; }
.message-footer { display: flex; justify-content: space-between; align-items: center; }
.message-time { font-size: 12px; color: #999; }
.message-link { font-size: 12px; color: #409eff; }

.message-actions { flex-shrink: 0; }

.pagination-wrap { margin-top: 20px; display: flex; justify-content: center; }

@media (max-width: 1200px) {
  .stats-cards { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 768px) {
  .stats-cards { grid-template-columns: repeat(2, 1fr); }
}
</style>
