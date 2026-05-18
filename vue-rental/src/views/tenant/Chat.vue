<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- 左侧会话列表 -->
      <div class="session-list">
        <div class="session-header">
          <h3>消息列表</h3>
          <el-badge :value="totalUnread + customerServiceUnread" :hidden="totalUnread + customerServiceUnread === 0" :max="99">
            <el-icon><ChatDotRound /></el-icon>
          </el-badge>
        </div>
        
        <!-- 客服入口卡片 -->
        <div class="customer-service-entry" @click="openCustomerService">
          <div class="cs-icon">
            <el-icon :size="24"><Headset /></el-icon>
          </div>
          <div class="cs-info">
            <span class="cs-title">联系平台客服</span>
            <span class="cs-desc">有问题？点击咨询</span>
          </div>
          <el-badge v-if="customerServiceUnread > 0" :value="customerServiceUnread" :max="99" class="cs-badge" />
          <el-icon class="cs-arrow"><ArrowRight /></el-icon>
        </div>
        
        <div class="session-search">
          <el-input v-model="searchKeyword" placeholder="搜索聊天记录" prefix-icon="Search" clearable />
        </div>
        
        <div class="session-items" v-loading="sessionsLoading">
          <el-empty v-if="!sessionsLoading && sessions.length === 0" description="暂无聊天记录" :image-size="80" />
          
          <div 
            v-for="session in filteredSessions" 
            :key="session.session_id"
            class="session-item"
            :class="{ active: currentSessionId === session.session_id }"
            @click="selectSession(session)"
          >
            <el-avatar :size="48" :src="session.other_avatar || defaultAvatar">
              {{ session.other_username?.charAt(0) }}
            </el-avatar>
            <div class="session-info">
              <div class="session-top">
                <span class="session-name">{{ session.other_username }}</span>
                <span class="session-time">{{ formatTime(session.last_message_time) }}</span>
              </div>
              <div class="session-bottom">
                <span class="session-preview">{{ session.last_message || '暂无消息' }}</span>
                <el-badge v-if="session.unread_count > 0" :value="session.unread_count" :max="99" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天窗口 -->
      <div class="chat-window" v-if="currentSessionId">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="chat-title">
            <el-avatar 
              :size="36" 
              :src="isCustomerServiceMode ? undefined : (currentSession?.other_avatar || defaultAvatar)"
              class="clickable-avatar"
              @click="!isCustomerServiceMode && showUserCard(currentSession?.other_user_id)"
            >
              <template v-if="isCustomerServiceMode">
                <el-icon><Headset /></el-icon>
              </template>
              <template v-else>
                {{ currentSession?.other_username?.charAt(0) }}
              </template>
            </el-avatar>
            <div class="title-info">
              <span class="name">{{ isCustomerServiceMode ? '平台客服' : currentSession?.other_username }}</span>
              <span class="house" v-if="!isCustomerServiceMode">{{ currentSession?.house_title }}</span>
              <span class="house" v-else>在线为您服务</span>
            </div>
          </div>
          <div class="chat-actions">
            <el-button v-if="!isCustomerServiceMode" link @click="viewHouse">查看房源</el-button>
            <el-button v-if="isCustomerServiceMode" type="danger" size="small" @click="closeCustomerServiceSession">结束会话</el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="message-list" ref="messageListRef" @scroll="handleScroll">
          <!-- 客服模式：顶部常见问题标签 -->
          <div v-if="isCustomerServiceMode" class="faq-tags-area">
            <div class="faq-tags-header">💡 常见问题，点击快速提问：</div>
            <div class="faq-tags">
              <span
                v-for="(item, index) in faqList"
                :key="index"
                class="faq-tag"
                @click="sendFAQ(item)"
              >{{ item.question }}</span>
            </div>
          </div>

          <!-- 排队等待状态 -->
          <div v-if="isQueueWaiting" class="queue-inline-status">
            <div class="queue-inline-spinner"></div>
            <div class="queue-inline-info">
              <span v-if="queuePosition > 1">您前面还有 <b>{{ queuePosition - 1 }}</b> 人排队，请耐心等待</span>
              <span v-else-if="queuePosition === 1">您是下一位，即将为您接入</span>
              <span v-else>正在等待客服接入...</span>
            </div>
            <el-button size="small" @click="cancelQueueWaiting">取消排队</el-button>
          </div>

          <div v-if="hasMoreMessages" class="load-more">
            <el-button link :loading="messagesLoading" @click="loadMoreMessages">加载更多</el-button>
          </div>
          
          <div 
            v-for="msg in messages" 
            :key="msg.message_id"
            class="message-item"
            :class="{ 'is-self': msg.sender_id === userStore.userInfo?.userId }"
          >
            <el-avatar 
              :size="36" 
              :src="msg.sender_avatar || defaultAvatar"
              class="clickable-avatar"
              @click="showUserCard(msg.sender_id)"
            >
              {{ msg.sender_name?.charAt(0) }}
            </el-avatar>
            <div class="message-content">
              <div class="message-meta">
                <span class="sender-name">{{ msg.sender_name }}</span>
                <span class="message-time">{{ formatMessageTime(msg.create_time) }}</span>
              </div>
              <div class="message-bubble">
                <!-- 文本消息 -->
                <template v-if="msg.message_type === 0 || msg.message_type === '0'">
                  {{ msg.content }}
                </template>
                <!-- 图片消息 -->
                <template v-else-if="msg.message_type === 1 || msg.message_type === '1'">
                  <el-image :src="getFileUrl(msg.file_url)" fit="cover" style="max-width: 200px; max-height: 200px;" :preview-src-list="[getFileUrl(msg.file_url)]" />
                </template>
                <!-- 文件消息 -->
                <template v-else-if="msg.message_type === 2 || msg.message_type === '2'">
                  <a :href="getFileUrl(msg.file_url)" target="_blank" download class="file-link">
                    <el-icon><Document /></el-icon>
                    {{ msg.content }}
                  </a>
                </template>
                <!-- 系统消息 -->
                <template v-else-if="msg.message_type === 3 || msg.message_type === '3'">
                  <span class="system-msg">{{ msg.content }}</span>
                </template>
                <!-- 未知类型，显示内容 -->
                <template v-else>
                  {{ msg.content || '[未知消息类型]' }}
                </template>
              </div>
            </div>
          </div>


        </div>

        <!-- 输入区域 -->
        <div class="chat-input">
          <div class="input-toolbar">
            <!-- 表情 -->
            <el-popover placement="top" :width="320" trigger="click">
              <template #reference>
                <el-button link title="表情"><el-icon><ChatLineSquare /></el-icon></el-button>
              </template>
              <div class="emoji-panel">
                <span 
                  v-for="emoji in emojiList" 
                  :key="emoji" 
                  class="emoji-item"
                  @click="insertEmoji(emoji)"
                >{{ emoji }}</span>
              </div>
            </el-popover>
            <!-- 图片 -->
            <el-upload
              :show-file-list="false"
              :before-upload="handleImageUpload"
              accept="image/jpeg,image/png,image/gif,image/webp"
            >
              <el-button link title="图片"><el-icon><Picture /></el-icon></el-button>
            </el-upload>
            <!-- 文件 -->
            <el-upload
              :show-file-list="false"
              :before-upload="handleFileUpload"
            >
              <el-button link title="文件"><el-icon><Folder /></el-icon></el-button>
            </el-upload>
          </div>
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
            <div class="right-actions">
              <span class="char-count">{{ inputMessage.length }}/500</span>
              <el-button type="primary" :disabled="!inputMessage.trim() && !uploading" :loading="uploading" @click="sendMessage">
                发送
              </el-button>
            </div>
          </div>
        </div>

      </div>

      <!-- 未选择会话时的占位 -->
      <div class="chat-placeholder" v-else>
        <el-empty description="选择一个会话开始聊天" :image-size="120" />
      </div>
    </div>

    <!-- 用户名片弹窗 -->
    <UserCardDialog v-model="userCardVisible" :userId="selectedUserId" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Close, Document, Picture, Folder, ChatLineSquare, Check, Edit, Headset, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getChatSessionsApi, getChatMessagesApi, sendMessageApi, markChatReadApi, uploadChatFileApi, createOrGetSessionApi } from '@/api/chat'
import { createCustomerServiceSessionApi, getCustomerServiceMessagesApi, sendCustomerServiceMessageApi, getCustomerServiceUnreadApi, getQueuePositionApi, closeCustomerServiceByUserApi } from '@/api/customerService'
import { wsService } from '@/utils/websocket'
import UserCardDialog from '@/components/UserCardDialog.vue'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 会话相关
const sessions = ref<any[]>([])
const sessionsLoading = ref(false)
const searchKeyword = ref('')
const currentSessionId = ref<number | null>(null)
const totalUnread = ref(0)

// 客服会话相关
const isCustomerServiceMode = ref(false)
const customerServiceSessionId = ref<number | null>(null)
const customerServiceUnread = ref(0)

// 消息相关
const messages = ref<any[]>([])
const messagesLoading = ref(false)
const inputMessage = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const currentPage = ref(1)
const hasMoreMessages = ref(false)
const uploading = ref(false)

// 用户名片
const userCardVisible = ref(false)
const selectedUserId = ref<number | undefined>(undefined)

const showUserCard = (userId?: number) => {
  if (!userId) return
  selectedUserId.value = userId
  userCardVisible.value = true
}

// 表情列表
const emojiList = [
  '😀', '😁', '😂', '🤣', '😃', '😄', '😅', '😆', '😉', '😊',
  '😋', '😎', '😍', '😘', '🥰', '😗', '😙', '😚', '🙂', '🤗',
  '🤩', '🤔', '🤨', '😐', '😑', '😶', '🙄', '😏', '😣', '😥',
  '😮', '🤐', '😯', '😪', '😫', '🥱', '😴', '😌', '😛', '😜',
  '😝', '🤤', '😒', '😓', '😔', '😕', '🙃', '🤑', '😲', '🙁',
  '😖', '😞', '😟', '😤', '😢', '😭', '😦', '😧', '😨', '😩',
  '👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙', '👋', '🤚',
  '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '💔', '💕', '💖'
]

// 当前会话
const currentSession = computed(() => {
  return sessions.value.find(s => s.session_id === currentSessionId.value)
})

// 过滤会话
const filteredSessions = computed(() => {
  if (!searchKeyword.value) return sessions.value
  const keyword = searchKeyword.value.toLowerCase()
  return sessions.value.filter(s => 
    s.other_username?.toLowerCase().includes(keyword) ||
    s.house_title?.toLowerCase().includes(keyword) ||
    s.last_message?.toLowerCase().includes(keyword)
  )
})

// 加载会话列表
const loadSessions = async () => {
  sessionsLoading.value = true
  try {
    const res: any = await getChatSessionsApi({ page: 1, size: 100 })
    sessions.value = res.records || []
    totalUnread.value = sessions.value.reduce((sum: number, s: any) => sum + (s.unread_count || 0), 0)
    
    // 加载客服未读数
    try {
      const csRes: any = await getCustomerServiceUnreadApi()
      customerServiceUnread.value = csRes || 0
    } catch (e) {
      console.error('加载客服未读数失败:', e)
    }
  } catch (e) {
    console.error('加载会话列表失败:', e)
  } finally {
    sessionsLoading.value = false
  }
}

// 常见问题列表
const faqList = [
  { question: '如何租房？', answer: '您可以在首页搜索心仪的房源，查看房源详情后点击"联系房东"进行沟通。双方达成一致后，房东会上传租赁合同，您确认合同即可完成租房流程。' },
  { question: '押金怎么退？', answer: '合同到期或提前解约后，房东会在系统中发起押金退还操作。退还金额会根据合同约定和房屋检查情况确定。您可以在"支付中心"查看押金退还进度。' },
  { question: '合同怎么签？', answer: '房东会在系统中上传合同文件（PDF或图片），您在"我的合同"页面可以查看合同内容。确认无误后点击"确认合同"即可完成签署。' },
  { question: '如何支付租金？', answer: '您可以在"支付中心"查看待支付的租金账单，支持在线支付。系统会在租金到期前提醒您按时缴纳。' },
  { question: '如何申请续租？', answer: '在"我的合同"页面，找到即将到期的合同，点击"申请续租"按钮。房东审核通过后会上传新的合同，您确认即可完成续租。' },
  { question: '房屋维修问题？', answer: '您可以通过"聊天中心"直接联系房东沟通维修事宜。如果房东未及时处理，可以联系客服由平台协调解决。' },
  { question: '如何修改个人信息？', answer: '点击右上角头像进入"个人中心"，可以修改头像、昵称、手机号等个人信息，也可以进行实名认证。' },
  { question: '如何投诉？', answer: '如果遇到纠纷，请直接在此描述您的问题，客服人员会介入处理。请准备好相关证据（聊天记录、合同、照片等）。' }
]

const isQueueWaiting = ref(false)
const queuePosition = ref(0)
let queueTimer: ReturnType<typeof setInterval> | null = null

// 打开客服 — 直接创建会话
const openCustomerService = async () => {
  try {
    const res: any = await createCustomerServiceSessionApi()
    if (res && res.sessionId) {
      customerServiceSessionId.value = res.sessionId
      currentSessionId.value = res.sessionId
      isCustomerServiceMode.value = true
      currentPage.value = 1
      messages.value = []
      await loadCustomerServiceMessages()
      customerServiceUnread.value = 0
      
      // 如果还没有客服接入，开始排队轮询
      if (res.session && !(res.session.serviceAdminId || res.session.service_admin_id)) {
        isQueueWaiting.value = true
        startQueuePolling()
      }
    }
  } catch (e) {
    console.error('创建客服会话失败:', e)
    ElMessage.error('联系客服失败，请稍后重试')
  }
}

// 点击常见问题标签，自动发送问题并显示回答
const sendFAQ = (item: typeof faqList[0]) => {
  // 以本地气泡形式添加问答到消息列表
  const now = new Date().toISOString()
  messages.value.push({
    message_id: `faq-q-${Date.now()}`,
    sender_id: userStore.userInfo?.userId,
    sender_name: userStore.userInfo?.username,
    sender_avatar: userStore.userInfo?.avatar,
    content: item.question,
    message_type: 0,
    create_time: now
  })
  messages.value.push({
    message_id: `faq-a-${Date.now()}`,
    sender_id: 0,
    sender_name: '智能客服',
    sender_type: 3,
    content: item.answer,
    message_type: 0,
    create_time: now
  })
  nextTick(() => scrollToBottom())
}

const startQueuePolling = () => {
  pollQueuePosition()
  queueTimer = setInterval(pollQueuePosition, 5000)
}

const pollQueuePosition = async () => {
  try {
    const res: any = await getQueuePositionApi()
    if (res.status === 'serving') {
      stopQueuePolling()
      isQueueWaiting.value = false
      ElMessage.success('客服已接入，正在为您服务')
    } else if (res.status === 'waiting') {
      queuePosition.value = res.position || 0
    }
  } catch (e) {
    console.error('查询排队位置失败:', e)
  }
}

const stopQueuePolling = () => {
  if (queueTimer) {
    clearInterval(queueTimer)
    queueTimer = null
  }
}

const cancelQueueWaiting = () => {
  stopQueuePolling()
  isQueueWaiting.value = false
  ElMessage.info('已取消排队')
}

// 用户主动结束客服会话
const closeCustomerServiceSession = async () => {
  if (!currentSessionId.value) return
  try {
    await ElMessageBox.confirm('确定要结束本次客服会话吗？', '提示', { type: 'warning' })
    await closeCustomerServiceByUserApi(currentSessionId.value)
    ElMessage.success('会话已结束')
    isCustomerServiceMode.value = false
    currentSessionId.value = null
    messages.value = []
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 加载客服消息
const loadCustomerServiceMessages = async () => {
  if (!currentSessionId.value) return
  
  messagesLoading.value = true
  try {
    const res: any = await getCustomerServiceMessagesApi(currentSessionId.value, {
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
    console.error('加载客服消息失败:', e)
  } finally {
    messagesLoading.value = false
  }
}

// 选择会话
const selectSession = async (session: any) => {
  if (currentSessionId.value === session.session_id) return
  
  // 切换回普通会话模式
  isCustomerServiceMode.value = false
  stopQueuePolling()
  isQueueWaiting.value = false
  
  currentSessionId.value = session.session_id
  currentPage.value = 1
  messages.value = []
  await loadMessages()
  
  // 标记已读
  if (session.unread_count > 0) {
    await markChatReadApi(session.session_id)
    session.unread_count = 0
    totalUnread.value = sessions.value.reduce((sum: number, s: any) => sum + (s.unread_count || 0), 0)
  }
}

// 加载消息
const loadMessages = async () => {
  if (!currentSessionId.value) return
  
  messagesLoading.value = true
  try {
    const res: any = await getChatMessagesApi(currentSessionId.value, { 
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
    
    // 滚动到底部
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

// 发送消息
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || !currentSessionId.value) return
  
  if (content.length > 500) {
    ElMessage.warning('消息内容不能超过500字')
    return
  }
  
  try {
    let res: any
    if (isCustomerServiceMode.value) {
      // 客服模式
      res = await sendCustomerServiceMessageApi({
        sessionId: currentSessionId.value,
        content,
        messageType: 0
      })
    } else {
      // 普通模式
      res = await sendMessageApi({
        sessionId: currentSessionId.value,
        content,
        messageType: 0
      })
    }
    // 添加到消息列表
    if (!messages.value.find(m => m.message_id === res.message_id)) {
      messages.value.push(res)
    }
    if (!isCustomerServiceMode.value) {
      updateSessionLastMessage(content)
    }
    inputMessage.value = ''
    await nextTick()
    scrollToBottom()
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

// 处理图片上传
const handleImageUpload = async (file: File) => {
  if (!currentSessionId.value) {
    ElMessage.warning('请先选择会话')
    return false
  }
  
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过5MB')
    return false
  }
  
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('sessionId', currentSessionId.value.toString())
    formData.append('type', 'image')
    
    const res: any = await uploadChatFileApi(formData)
    if (!messages.value.find(m => m.message_id === res.message_id)) {
      messages.value.push(res)
    }
    updateSessionLastMessage('[图片]')
    await nextTick()
    scrollToBottom()
  } catch (e) {
    ElMessage.error('图片上传失败')
  } finally {
    uploading.value = false
  }
  return false
}

// 处理文件上传
const handleFileUpload = async (file: File) => {
  if (!currentSessionId.value) {
    ElMessage.warning('请先选择会话')
    return false
  }
  
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过20MB')
    return false
  }
  
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('sessionId', currentSessionId.value.toString())
    formData.append('type', 'file')
    
    const res: any = await uploadChatFileApi(formData)
    if (!messages.value.find(m => m.message_id === res.message_id)) {
      messages.value.push(res)
    }
    updateSessionLastMessage('[文件] ' + file.name)
    await nextTick()
    scrollToBottom()
  } catch (e) {
    ElMessage.error('文件上传失败')
  } finally {
    uploading.value = false
  }
  return false
}

// 插入表情
const insertEmoji = (emoji: string) => {
  inputMessage.value += emoji
}

// 更新会话最后消息
const updateSessionLastMessage = (content: string) => {
  const session = sessions.value.find(s => s.session_id === currentSessionId.value)
  if (session) {
    session.last_message = content.length > 50 ? content.substring(0, 50) + '...' : content
    session.last_message_time = new Date().toISOString()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 处理滚动
const handleScroll = () => {
  // 可以在这里实现滚动加载更多
}

// 查看房源
const viewHouse = () => {
  if (currentSession.value?.house_id) {
    router.push(`/house/${currentSession.value.house_id}`)
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
  } else if (date.isSame(now, 'year')) {
    return date.format('MM-DD')
  } else {
    return date.format('YYYY-MM-DD')
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
    const token = localStorage.getItem('token_tenant')
    const separator = url.includes('?') ? '&' : '?'
    return `http://localhost:8080/api/chat/download${url.replace('/uploads/chat', '')}${separator}token=${token}`
  }
  return `http://localhost:8080${url}`
}

// WebSocket消息处理
const handleWsMessage = (msg: any) => {
  // 如果是当前会话的消息，添加到消息列表
  if (msg.session_id === currentSessionId.value) {
    // 避免重复添加自己发送的消息
    if (!messages.value.find(m => m.message_id === msg.message_id)) {
      messages.value.push(msg)
      nextTick(() => scrollToBottom())
    }
  }
  
  // 更新会话列表
  const session = sessions.value.find(s => s.session_id === msg.session_id)
  if (session) {
    session.last_message = msg.content?.length > 50 ? msg.content.substring(0, 50) + '...' : msg.content
    session.last_message_time = msg.create_time
    
    // 如果不是当前会话，增加未读数
    if (msg.session_id !== currentSessionId.value && msg.sender_id !== userStore.userInfo?.userId) {
      session.unread_count = (session.unread_count || 0) + 1
      totalUnread.value++
    }
    
    // 将会话移到顶部
    const index = sessions.value.indexOf(session)
    if (index > 0) {
      sessions.value.splice(index, 1)
      sessions.value.unshift(session)
    }
  } else {
    // 新会话，重新加载列表
    loadSessions()
  }
}

// 初始化
onMounted(async () => {
  await loadSessions()
  
  // 处理 URL 参数，自动创建或选中会话
  const sessionId = route.query.sessionId
  const houseId = route.query.houseId
  
  if (sessionId) {
    // 如果传入了sessionId，直接选中该会话
    const session = sessions.value.find(s => s.session_id === Number(sessionId))
    if (session) {
      await selectSession(session)
    } else {
      // 如果列表中没有，直接设置当前会话ID并加载消息
      currentSessionId.value = Number(sessionId)
      await loadMessages()
    }
    // 清除 URL 参数
    router.replace({ path: '/tenant/chat' })
  } else if (route.query.customerService) {
    // 从导航栏点击人工客服进来，直接打开客服会话
    await openCustomerService()
    router.replace({ path: '/tenant/chat' })
  } else if (houseId) {
    // 如果传入了houseId，创建或获取会话
    try {
      const res: any = await createOrGetSessionApi({ houseId: Number(houseId) })
      if (res && res.sessionId) {
        // 重新加载会话列表
        await loadSessions()
        // 选中该会话
        const session = sessions.value.find(s => s.session_id === res.sessionId)
        if (session) {
          await selectSession(session)
        } else {
          // 如果列表中没有，直接设置当前会话ID并加载消息
          currentSessionId.value = res.sessionId
          await loadMessages()
        }
      }
      // 清除 URL 参数
      router.replace({ path: '/tenant/chat' })
    } catch (e) {
      console.error('创建会话失败:', e)
    }
  }
  
  // 连接WebSocket
  try {
    await wsService.connect()
    wsService.onMessage(handleWsMessage)
    // 监听客服消息
    wsService.onCustomerServiceMessage(handleCustomerServiceMessage)
  } catch (e) {
    console.error('WebSocket连接失败:', e)
  }
})

// 处理客服WebSocket消息
const handleCustomerServiceMessage = (msg: any) => {
  // 如果当前是客服会话模式且消息属于当前会话
  if (isCustomerServiceMode.value && msg.session_id === currentSessionId.value) {
    if (!messages.value.find(m => m.message_id === msg.message_id)) {
      messages.value.push(msg)
      nextTick(() => scrollToBottom())
    }
  }
  // 更新客服未读数
  if (msg.sender_type === 3 && msg.sender_id !== userStore.userInfo?.userId) {
    if (!isCustomerServiceMode.value || msg.session_id !== currentSessionId.value) {
      customerServiceUnread.value++
    }
  }
}

// 清理
onUnmounted(() => {
  stopQueuePolling()
  // 不断开WebSocket，因为其他页面可能还需要
})
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 120px);
  padding: 20px;
}

.chat-container {
  display: flex;
  height: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 会话列表 */
.session-list {
  width: 320px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.session-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-header h3 {
  margin: 0;
  font-size: 16px;
}

/* 客服入口卡片 */
.customer-service-entry {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin: 8px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
  color: #fff;
}

.customer-service-entry:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.cs-icon {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.cs-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.cs-title {
  font-size: 14px;
  font-weight: 500;
}

.cs-desc {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 2px;
}

.cs-badge {
  margin-right: 8px;
}

.cs-arrow {
  opacity: 0.8;
}

.session-search {
  padding: 12px 16px;
}

.session-items {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  display: flex;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
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
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.session-name {
  font-weight: 500;
  color: #303133;
}

.session-time {
  font-size: 12px;
  color: #909399;
}

.session-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-preview {
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

/* 聊天窗口 */
.chat-window {
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
  flex-direction: column;
}

.title-info .name {
  font-weight: 500;
  color: #303133;
}

.title-info .house {
  font-size: 12px;
  color: #909399;
}

/* 消息列表 */
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

.system-msg {
  color: #909399;
  font-size: 12px;
}

/* 输入区域 */
.chat-input {
  border-top: 1px solid #e4e7ed;
  padding: 12px 16px;
}

.input-toolbar {
  margin-bottom: 8px;
  display: flex;
  gap: 8px;
}

.input-area {
  margin-bottom: 8px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.right-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.char-count {
  font-size: 12px;
  color: #909399;
}

/* 表情面板 */
.emoji-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.emoji-item {
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
}

.emoji-item:hover {
  background: #f0f0f0;
}

/* 占位 */
.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 可点击头像 */
.clickable-avatar {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.clickable-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* AI优化消息气泡 */
.optimized-bubbles-container {
  padding: 16px;
  background: #f0f9ff;
  border-radius: 8px;
  margin: 16px 0;
  border: 2px dashed #409eff;
}

.optimized-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #d9ecff;
}

.header-text {
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.optimized-bubble {
  background: #fff;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.optimized-bubble:last-child {
  margin-bottom: 0;
}

.optimized-bubble:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.bubble-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.bubble-style {
  font-size: 12px;
  color: #909399;
}

.bubble-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 8px;
}

.bubble-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #409eff;
  opacity: 0;
  transition: opacity 0.3s;
}

.optimized-bubble:hover .bubble-hint {
  opacity: 1;
}

/* 内嵌常见问题标签 */
.faq-tags-area {
  margin-bottom: 16px;
  padding: 12px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebeef5;
}

.faq-tags-header {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.faq-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.faq-tag {
  padding: 6px 14px;
  background: #f0edff;
  color: #667eea;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s;
  border: 1px solid transparent;
  white-space: nowrap;
}

.faq-tag:hover {
  background: #667eea;
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

/* 排队等待内嵌样式 */
.queue-inline-status {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  margin: 12px 0;
  background: #fff7e6;
  border: 1px solid #ffe58f;
  border-radius: 10px;
}

.queue-inline-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #ffe58f;
  border-top-color: #e6a23c;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.queue-inline-info {
  font-size: 14px;
  color: #606266;
}

.queue-inline-info b {
  color: #e6a23c;
  font-size: 18px;
}


</style>
