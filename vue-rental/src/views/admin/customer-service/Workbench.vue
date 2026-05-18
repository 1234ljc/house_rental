<template>
  <div class="cs-workbench">
    <div class="workbench-container">
      <!-- 左侧会话列表 -->
      <div class="session-panel">
        <!-- 统计卡片 -->
        <div class="stats-cards">
          <div class="stat-card pending" @click="currentTab = 'pending'">
            <div class="stat-value">{{ stats.pending || 0 }}</div>
            <div class="stat-label">待接入</div>
          </div>
          <div class="stat-card serving" @click="currentTab = 'serving'">
            <div class="stat-value">{{ stats.serving || 0 }}</div>
            <div class="stat-label">服务中</div>
          </div>
          <div class="stat-card closed" @click="currentTab = 'closed'">
            <div class="stat-value">{{ stats.closed || 0 }}</div>
            <div class="stat-label">已结束</div>
          </div>
        </div>

        <!-- 会话标签页 -->
        <el-tabs v-model="currentTab" class="session-tabs">
          <el-tab-pane label="待接入" name="pending">
            <div class="session-list" v-loading="sessionsLoading">
              <el-empty v-if="sessions.length === 0" description="暂无待接入会话" :image-size="60" />
              <div 
                v-for="session in sessions" 
                :key="session.session_id"
                class="session-item"
                :class="{ active: currentSessionId === session.session_id }"
                @click="selectSession(session)"
              >
                <el-avatar :size="40">
                  {{ session.customer_username?.charAt(0) || '用' }}
                </el-avatar>
                <div class="session-info">
                  <div class="session-top">
                    <span class="session-name">{{ session.customer_username || '用户' }}</span>
                    <el-tag size="small" :type="session.customer_type === 1 ? 'success' : 'warning'">
                      {{ session.customer_type === 1 ? '租客' : '房东' }}
                    </el-tag>
                  </div>
                  <div class="session-preview">{{ session.last_message || '新会话' }}</div>
                  <div class="session-time">{{ formatTime(session.last_message_time) }}</div>
                  <div class="session-wait-time">
                    <el-icon><Clock /></el-icon>
                    <span>等待 {{ getWaitTime(session.create_time) }}</span>
                  </div>
                </div>
                <el-badge v-if="session.unread_count > 0" :value="session.unread_count" :max="99" />
              </div>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="服务中" name="serving">
            <div class="session-list" v-loading="sessionsLoading">
              <el-empty v-if="sessions.length === 0" description="暂无服务中会话" :image-size="60" />
              <div 
                v-for="session in sessions" 
                :key="session.session_id"
                class="session-item"
                :class="{ active: currentSessionId === session.session_id }"
                @click="selectSession(session)"
              >
                <el-avatar :size="40">
                  {{ session.customer_username?.charAt(0) || '用' }}
                </el-avatar>
                <div class="session-info">
                  <div class="session-top">
                    <span class="session-name">{{ session.customer_username || '用户' }}</span>
                    <el-tag size="small" :type="session.customer_type === 1 ? 'success' : 'warning'">
                      {{ session.customer_type === 1 ? '租客' : '房东' }}
                    </el-tag>
                  </div>
                  <div class="session-preview">{{ session.last_message || '暂无消息' }}</div>
                  <div class="session-time">{{ formatTime(session.last_message_time) }}</div>
                </div>
                <el-badge v-if="session.unread_count > 0" :value="session.unread_count" :max="99" />
              </div>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="已结束" name="closed">
            <div class="session-list" v-loading="sessionsLoading">
              <el-empty v-if="sessions.length === 0" description="暂无已结束会话" :image-size="60" />
              <div 
                v-for="session in sessions" 
                :key="session.session_id"
                class="session-item"
                :class="{ active: currentSessionId === session.session_id }"
                @click="selectSession(session)"
              >
                <el-avatar :size="40">
                  {{ session.customer_username?.charAt(0) || '用' }}
                </el-avatar>
                <div class="session-info">
                  <div class="session-top">
                    <span class="session-name">{{ session.customer_username || '用户' }}</span>
                    <el-tag size="small" type="info">已结束</el-tag>
                  </div>
                  <div class="session-preview">{{ session.last_message || '暂无消息' }}</div>
                  <div class="session-time">{{ formatTime(session.last_message_time) }}</div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 右侧聊天窗口 -->
      <div class="chat-panel" v-if="currentSessionId">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="chat-title">
            <el-avatar :size="36">
              {{ currentSession?.customer_username?.charAt(0) || '用' }}
            </el-avatar>
            <div class="title-info">
              <span class="name">{{ currentSession?.customer_username || '用户' }}</span>
              <el-tag size="small" :type="currentSession?.customer_type === 1 ? 'success' : 'warning'">
                {{ currentSession?.customer_type === 1 ? '租客' : '房东' }}
              </el-tag>
            </div>
          </div>
          <div class="chat-actions">
            <el-button 
              v-if="currentTab === 'pending'" 
              type="primary" 
              size="small"
              @click="acceptSession"
            >
              接入会话
            </el-button>
            <el-button 
              v-if="currentTab === 'serving'" 
              type="danger" 
              size="small"
              @click="closeSession"
            >
              结束会话
            </el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="message-list" ref="messageListRef">
          <div v-if="hasMoreMessages" class="load-more">
            <el-button link :loading="messagesLoading" @click="loadMoreMessages">加载更多</el-button>
          </div>
          
          <div 
            v-for="msg in messages" 
            :key="msg.message_id"
            class="message-item"
            :class="{ 
              'is-self': msg.sender_type === 3,
              'is-system': msg.message_type === 3
            }"
          >
            <template v-if="msg.message_type === 3">
              <div class="system-message">{{ msg.content }}</div>
            </template>
            <template v-else>
              <el-avatar :size="36">
                {{ msg.sender_type === 3 ? '客' : (msg.sender_name?.charAt(0) || '用') }}
              </el-avatar>
              <div class="message-content">
                <div class="message-meta">
                  <span class="sender-name">{{ msg.sender_type === 3 ? '客服' : msg.sender_name }}</span>
                  <span class="message-time">{{ formatMessageTime(msg.create_time) }}</span>
                </div>
                <div class="message-bubble">
                  <!-- 文本消息 -->
                  <template v-if="msg.message_type === 0 || msg.message_type === '0' || !msg.message_type">
                    {{ msg.content }}
                  </template>
                  <!-- 图片消息 -->
                  <template v-else-if="msg.message_type === 1 || msg.message_type === '1'">
                    <el-image 
                      :src="getFileUrl(msg.file_url)" 
                      fit="cover" 
                      style="max-width: 200px; max-height: 200px; border-radius: 4px;" 
                      :preview-src-list="[getFileUrl(msg.file_url)]" 
                    />
                  </template>
                  <!-- 文件消息 -->
                  <template v-else-if="msg.message_type === 2 || msg.message_type === '2'">
                    <a :href="getFileUrl(msg.file_url)" target="_blank" download class="file-link">
                      <el-icon><Document /></el-icon>
                      {{ msg.content }}
                    </a>
                  </template>
                  <!-- 其他类型 -->
                  <template v-else>
                    {{ msg.content }}
                  </template>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- 快捷回复面板 -->
        <div class="quick-reply-panel" v-if="currentTab !== 'closed'">
          <div class="quick-reply-header" @click="showQuickReply = !showQuickReply">
            <span>💬 快捷回复</span>
            <el-icon :class="{ 'is-rotate': showQuickReply }"><ArrowDown /></el-icon>
          </div>
          <div class="quick-reply-list" v-show="showQuickReply">
            <div
              v-for="(reply, index) in quickReplies"
              :key="index"
              class="quick-reply-item"
              @click="useQuickReply(reply)"
            >
              {{ reply }}
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input" v-if="currentTab !== 'closed'">
          <div class="input-area">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="输入消息，按Enter发送"
              resize="none"
              @keydown.enter.exact.prevent="sendMessage"
            />
          </div>
          <div class="input-actions">
            <span class="char-count">{{ inputMessage.length }}/500</span>
            <el-button type="primary" :disabled="!inputMessage.trim()" @click="sendMessage">
              发送
            </el-button>
          </div>
        </div>
      </div>

      <!-- 未选择会话时的占位 -->
      <div class="chat-placeholder" v-else>
        <el-empty description="选择一个会话开始服务" :image-size="120" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { Clock, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { 
  getAdminCustomerServiceSessionsApi, 
  getAdminCustomerServiceStatsApi,
  acceptCustomerServiceApi,
  getAdminCustomerServiceMessagesApi,
  sendAdminCustomerServiceMessageApi,
  closeCustomerServiceApi
} from '@/api/customerService'
import { wsService } from '@/utils/websocket'
import dayjs from 'dayjs'

const userStore = useUserStore()

// 统计数据
const stats = ref<any>({})

// 会话相关
const currentTab = ref('pending')
const sessions = ref<any[]>([])
const sessionsLoading = ref(false)
const currentSessionId = ref<number | null>(null)

// 消息相关
const messages = ref<any[]>([])
const messagesLoading = ref(false)
const inputMessage = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const currentPage = ref(1)
const hasMoreMessages = ref(false)

// 快捷回复
const showQuickReply = ref(false)
const quickReplies = [
  '您好，我是平台客服，很高兴为您服务！',
  '请问您遇到了什么问题？',
  '好的，我来帮您查看一下。',
  '请您稍等，正在为您处理中。',
  '您的问题已记录，我们会尽快处理。',
  '请提供一下您的合同编号，方便我查询。',
  '关于押金退还，一般在合同结束后7个工作日内处理。',
  '建议您与房东协商解决，如需平台介入请告知。',
  '感谢您的耐心等待，问题已解决。',
  '还有其他问题吗？如果没有，祝您生活愉快！'
]

const useQuickReply = (reply: string) => {
  inputMessage.value = reply
}

// 计算等待时长
const getWaitTime = (createTime: string) => {
  if (!createTime) return '未知'
  const now = dayjs()
  const created = dayjs(createTime)
  const diffMinutes = now.diff(created, 'minute')
  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟`
  const diffHours = now.diff(created, 'hour')
  if (diffHours < 24) return `${diffHours}小时${diffMinutes % 60}分钟`
  const diffDays = now.diff(created, 'day')
  return `${diffDays}天${diffHours % 24}小时`
}

// 当前会话
const currentSession = computed(() => {
  return sessions.value.find(s => s.session_id === currentSessionId.value)
})

// 加载统计数据
const loadStats = async () => {
  try {
    const res: any = await getAdminCustomerServiceStatsApi()
    stats.value = res || {}
  } catch (e) {
    console.error('加载统计数据失败:', e)
  }
}

// 加载会话列表
const loadSessions = async () => {
  sessionsLoading.value = true
  try {
    const statusMap: Record<string, number> = {
      'pending': -1,
      'serving': 0,
      'closed': 1
    }
    const res: any = await getAdminCustomerServiceSessionsApi({
      status: statusMap[currentTab.value],
      page: 1,
      size: 100
    })
    sessions.value = res.records || []
  } catch (e) {
    console.error('加载会话列表失败:', e)
  } finally {
    sessionsLoading.value = false
  }
}

// 选择会话
const selectSession = async (session: any) => {
  if (currentSessionId.value === session.session_id) return
  
  currentSessionId.value = session.session_id
  currentPage.value = 1
  messages.value = []
  await loadMessages()
}

// 加载消息
const loadMessages = async () => {
  if (!currentSessionId.value) return
  
  messagesLoading.value = true
  try {
    const res: any = await getAdminCustomerServiceMessagesApi(currentSessionId.value, {
      page: currentPage.value,
      size: 50
    })
    const newMessages = res.records || []
    
    if (currentPage.value === 1) {
      messages.value = newMessages
    } else {
      messages.value = [...newMessages, ...messages.value]
    }
    
    hasMoreMessages.value = newMessages.length === 50
    
    if (currentPage.value === 1) {
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    console.error('加载消息失败:', e)
  } finally {
    messagesLoading.value = false
  }
}

// 加载更多消息
const loadMoreMessages = () => {
  currentPage.value++
  loadMessages()
}

// 接入会话
const acceptSession = async () => {
  if (!currentSessionId.value) return
  
  try {
    await acceptCustomerServiceApi(currentSessionId.value)
    ElMessage.success('接入成功')
    currentTab.value = 'serving'
    await loadStats()
  } catch (e: any) {
    ElMessage.error(e.message || '接入失败')
  }
}

// 结束会话
const closeSession = async () => {
  if (!currentSessionId.value) return
  
  try {
    await ElMessageBox.confirm('确定要结束该会话吗？', '提示', {
      type: 'warning'
    })
    await closeCustomerServiceApi(currentSessionId.value)
    ElMessage.success('会话已结束')
    currentSessionId.value = null
    await loadStats()
    await loadSessions()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

// 发送消息
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || !currentSessionId.value) return
  
  if (content.length > 500) {
    ElMessage.warning('消息内容不能超过500字')
    return
  }
  
  try {
    const res: any = await sendAdminCustomerServiceMessageApi({
      sessionId: currentSessionId.value,
      content,
      messageType: 0
    })
    if (!messages.value.find(m => m.message_id === res.message_id)) {
      messages.value.push(res)
    }
    inputMessage.value = ''
    await nextTick()
    scrollToBottom()
    
    // 如果是待接入状态，自动切换到服务中
    if (currentTab.value === 'pending') {
      currentTab.value = 'serving'
      await loadStats()
    }
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 处理客服WebSocket消息
const handleCustomerServiceMessage = (msg: any) => {
  console.log('[客服工作台] 收到消息:', msg)
  
  // 如果消息属于当前会话，添加到消息列表
  if (msg.session_id === currentSessionId.value) {
    if (!messages.value.find(m => m.message_id === msg.message_id)) {
      messages.value.push(msg)
      nextTick(() => scrollToBottom())
    }
  }
  
  // 更新会话列表中的最后消息
  const session = sessions.value.find(s => s.session_id === msg.session_id)
  if (session) {
    session.last_message = msg.content?.length > 50 ? msg.content.substring(0, 50) + '...' : msg.content
    session.last_message_time = msg.create_time
    
    // 如果不是当前会话且不是自己发的消息，增加未读数
    if (msg.session_id !== currentSessionId.value && msg.sender_type !== 3) {
      session.unread_count = (session.unread_count || 0) + 1
    }
  } else {
    // 可能是新会话，刷新列表
    loadSessions()
    loadStats()
  }
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = dayjs(time)
  const now = dayjs()
  
  if (date.isSame(now, 'day')) {
    return date.format('HH:mm')
  } else if (date.isSame(now.subtract(1, 'day'), 'day')) {
    return '昨天'
  } else {
    return date.format('MM-DD')
  }
}

const formatMessageTime = (time: string) => {
  if (!time) return ''
  return dayjs(time).format('MM-DD HH:mm')
}

// 获取文件完整URL
const getFileUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  // 将静态文件路径转换为下载接口路径
  if (url.startsWith('/uploads/chat/')) {
    const token = localStorage.getItem('token_admin')
    const separator = url.includes('?') ? '&' : '?'
    return `http://localhost:8080/api/chat/download${url.replace('/uploads/chat', '')}${separator}token=${token}`
  }
  return `http://localhost:8080${url}`
}

// 监听标签页切换
watch(currentTab, () => {
  currentSessionId.value = null
  loadSessions()
})

// 初始化
onMounted(async () => {
  await loadStats()
  await loadSessions()
  
  // 连接WebSocket
  try {
    await wsService.connect()
    wsService.onCustomerServiceMessage(handleCustomerServiceMessage)
    console.log('[客服工作台] WebSocket已连接')
  } catch (e) {
    console.error('[客服工作台] WebSocket连接失败:', e)
  }
})

// 清理
onUnmounted(() => {
  // 不断开WebSocket
})
</script>

<style scoped>
.cs-workbench {
  height: calc(100vh - 120px);
  padding: 20px;
}

.workbench-container {
  display: flex;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 左侧会话面板 */
.session-panel {
  width: 360px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.stats-cards {
  display: flex;
  padding: 16px;
  gap: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.stat-card {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card.pending {
  background: #fef0f0;
  color: #f56c6c;
}

.stat-card.serving {
  background: #f0f9eb;
  color: #67c23a;
}

.stat-card.closed {
  background: #f4f4f5;
  color: #909399;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}

.stat-label {
  font-size: 12px;
  margin-top: 4px;
}

.session-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.session-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
}

.session-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.session-list {
  height: 100%;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 8px;
}

.session-item:hover {
  background: #f5f7fa;
}

.session-item.active {
  background: #ecf5ff;
}

.session-info {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
}

.session-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.session-name {
  font-weight: 500;
  color: #303133;
}

.session-preview {
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

/* 右侧聊天面板 */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-info .name {
  font-weight: 500;
  color: #303133;
}

.message-list {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #f5f7fa;
}

.load-more {
  text-align: center;
  margin-bottom: 16px;
}

.message-item {
  display: flex;
  margin-bottom: 16px;
}

.message-item.is-self {
  flex-direction: row-reverse;
}

.message-item.is-system {
  justify-content: center;
}

.system-message {
  padding: 8px 16px;
  background: #e4e7ed;
  border-radius: 16px;
  font-size: 12px;
  color: #909399;
}

.message-content {
  max-width: 60%;
  margin: 0 12px;
}

.message-item.is-self .message-content {
  text-align: right;
}

.message-meta {
  margin-bottom: 4px;
}

.sender-name {
  font-size: 12px;
  color: #909399;
  margin-right: 8px;
}

.message-time {
  font-size: 12px;
  color: #c0c4cc;
}

.message-bubble {
  display: inline-block;
  padding: 10px 14px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-break: break-word;
  text-align: left;
}

.message-item.is-self .message-bubble {
  background: #409eff;
  color: #fff;
}

.file-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #409eff;
  text-decoration: none;
}

.message-item.is-self .file-link {
  color: #fff;
}

.chat-input {
  border-top: 1px solid #e4e7ed;
  padding: 12px 16px;
}

.input-area {
  margin-bottom: 8px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
}

.char-count {
  font-size: 12px;
  color: #909399;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 等待时长 */
.session-wait-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}

/* 快捷回复面板 */
.quick-reply-panel {
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
}

.quick-reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  user-select: none;
}

.quick-reply-header:hover {
  background: #f0f0f0;
}

.quick-reply-header .is-rotate {
  transform: rotate(180deg);
  transition: transform 0.3s;
}

.quick-reply-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 16px 10px;
  max-height: 120px;
  overflow-y: auto;
}

.quick-reply-item {
  padding: 4px 12px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 16px;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.quick-reply-item:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}
</style>
