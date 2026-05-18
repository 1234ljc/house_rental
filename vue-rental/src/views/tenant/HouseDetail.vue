<template>
  <div class="house-detail-page" v-loading="loading">
    <template v-if="house">
      <!-- 顶部标题区 -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="house-title">{{ house.title }}</h1>
          <div class="house-meta">
            <span class="meta-item">发布时间：{{ formatDate(house.createTime) }}</span>
          </div>
        </div>
        <div class="header-right">
          <div 
            :class="['collect-btn', { active: isFavorite, hover: isHovering }]" 
            @click="handleCollect"
            @mouseenter="isHovering = true"
            @mouseleave="isHovering = false"
          >
            <el-icon class="collect-icon"><StarFilled v-if="isFavorite" /><Star v-else /></el-icon>
            {{ collectBtnText }}
          </div>
        </div>
      </div>

      <!-- 主体内容 -->
      <div class="main-content">
        <!-- 左侧：图片 + 详情 -->
        <div class="left-section">
          <!-- 图片展示区 - 贝壳风格 -->
          <div class="image-section">
            <div class="main-image">
              <el-image :src="currentImage" fit="cover" :preview-src-list="imageList" :initial-index="currentIndex">
                <template #error><div class="image-placeholder"><el-icon :size="60"><Picture /></el-icon></div></template>
              </el-image>
              <div class="nav-btn prev" @click="prevImage" v-show="currentIndex > 0"><el-icon><ArrowLeft /></el-icon></div>
              <div class="nav-btn next" @click="nextImage" v-show="currentIndex < imageList.length - 1"><el-icon><ArrowRight /></el-icon></div>
              <div class="image-index">{{ currentIndex + 1 }}/{{ imageList.length || 1 }}</div>
            </div>
            <div class="thumbnail-list" v-if="imageList.length > 1">
              <div v-for="(img, index) in imageList" :key="index" 
                :class="['thumb-item', { active: currentIndex === index }]" @click="currentIndex = index">
                <el-image :src="img" fit="cover" />
              </div>
            </div>
          </div>

          <!-- Tab导航 - 去掉讨论区 -->
          <div class="info-tabs">
            <span :class="['tab-item', { active: activeTab === 'info' }]" @click="activeTab = 'info'">房屋信息</span>
            <span :class="['tab-item', { active: activeTab === 'desc' }]" @click="activeTab = 'desc'">房源描述</span>
            <span :class="['tab-item', { active: activeTab === 'cost' }]" @click="activeTab = 'cost'">费用详情</span>
            <span :class="['tab-item', { active: activeTab === 'facility' }]" @click="activeTab = 'facility'">配套设施</span>
            <span :class="['tab-item', { active: activeTab === 'map' }]" @click="switchToMap">地址与交通</span>
          </div>

          <!-- 房屋信息 -->
          <div class="info-panel" v-show="activeTab === 'info'">
            <h3 class="panel-title">房屋信息</h3>
            <div class="info-grid">
              <div class="info-row">
                <span class="info-label">面积：</span>
                <span class="info-value">{{ house.area }}㎡</span>
              </div>
              <div class="info-row">
                <span class="info-label">朝向：</span>
                <span class="info-value">{{ house.orientation || '暂无' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">户型：</span>
                <span class="info-value">{{ house.houseType }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">楼层：</span>
                <span class="info-value">{{ house.floor || '暂无' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">租赁方式：</span>
                <span class="info-value">{{ rentOptionText }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">房源状态：</span>
                <span class="info-value">
                  <el-tag :type="statusType" size="small">{{ statusText }}</el-tag>
                </span>
              </div>
              <div class="info-row full">
                <span class="info-label">位置：</span>
                <span class="info-value">{{ house.province }} {{ house.city }} {{ house.district }} {{ house.address }}</span>
              </div>
            </div>
          </div>

          <!-- 房源描述 -->
          <div class="info-panel" v-show="activeTab === 'desc'">
            <h3 class="panel-title">房源描述</h3>
            <div class="landlord-row">
              <el-avatar :size="50" :src="house.landlord?.avatar" @click="showLandlordCard" class="clickable-avatar">
                <el-icon :size="25"><User /></el-icon>
              </el-avatar>
              <div class="landlord-info">
                <div class="landlord-name">
                  {{ house.landlord?.realName || house.landlord?.username || '房东' }}
                  <el-tag size="small" type="success" v-if="house.landlord?.realnameStatus === 1">已实名</el-tag>
                </div>
                <div class="landlord-role">房东</div>
              </div>
            </div>
            <div class="desc-images" v-if="imageList.length > 0">
              <div class="desc-image-grid">
                <el-image v-for="(img, index) in imageList" :key="index" :src="img" fit="cover" 
                  :preview-src-list="imageList" :initial-index="index" class="desc-img" />
              </div>
            </div>
            <div class="desc-text">{{ house.description || '暂无房源描述' }}</div>
          </div>

          <!-- 费用详情 -->
          <div class="info-panel" v-show="activeTab === 'cost'">
            <h3 class="panel-title">费用详情</h3>
            <div class="cost-grid">
              <div class="cost-item">
                <span class="cost-label">月租金</span>
                <span class="cost-value highlight">{{ house.rentPrice }}元/月</span>
              </div>
              <div class="cost-item">
                <span class="cost-label">押付方式</span>
                <span class="cost-value">{{ house.depositType || '-' }}</span>
              </div>
            </div>
          </div>

          <!-- 配套设施 -->
          <div class="info-panel" v-show="activeTab === 'facility'">
            <h3 class="panel-title">配套设施</h3>
            <div class="facility-grid" v-if="facilitiesList.length > 0">
              <div class="facility-item" v-for="item in facilitiesList" :key="item">
                <el-icon class="facility-icon"><Check /></el-icon>
                <span>{{ item }}</span>
              </div>
            </div>
            <div v-else class="no-data">暂无配套设施信息</div>
          </div>

          <!-- 举报弹窗 -->
          <el-dialog v-model="reportDialogVisible" title="举报帖子" width="400px" append-to-body>
            <div class="report-content-preview">{{ reportTarget?.content }}</div>
            <el-input v-model="reportReason" type="textarea" :rows="3" placeholder="请说明举报原因..." maxlength="200" show-word-limit style="margin-top: 12px" />
            <template #footer>
              <el-button @click="reportDialogVisible = false">取消</el-button>
              <el-button type="danger" @click="submitReport" :loading="reportSubmitting">提交举报</el-button>
            </template>
          </el-dialog>

          <!-- 地址与交通 -->
          <div class="info-panel" v-show="activeTab === 'map'">
            <h3 class="panel-title">地址与交通</h3>
            <div class="address-bar">
              <el-icon><Location /></el-icon>
              <span>{{ house.province }} {{ house.city }} {{ house.district }} {{ house.address }}</span>
            </div>
            <!-- 地图容器 -->
            <div id="house-map" class="map-container"></div>
            <!-- 周边配套 -->
            <div class="poi-section">
              <div class="poi-tabs">
                <span v-for="cat in poiCategories" :key="cat.key"
                  :class="['poi-tab', { active: activePoi === cat.key }]"
                  @click="searchPoi(cat.key, cat.keyword)">
                  <el-icon><component :is="cat.icon" /></el-icon>
                  {{ cat.label }}
                </span>
              </div>
              <div class="poi-list" v-loading="poiLoading">
                <div v-if="poiList.length === 0 && !poiLoading" class="no-data">暂无周边数据</div>
                <div v-for="(poi, idx) in poiList" :key="idx" class="poi-item" @click="focusPoi(poi)">
                  <div class="poi-index">{{ idx + 1 }}</div>
                  <div class="poi-info">
                    <div class="poi-name">{{ poi.name }}</div>
                    <div class="poi-addr">{{ poi.address || poi.vicinity }}</div>
                  </div>
                  <div class="poi-dist">{{ formatDist(poi.distance) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：价格卡片 + 讨论区 -->
        <div class="right-section">
          <!-- 价格卡片 -->
          <div class="price-card">
            <div class="price-row">
              <span class="price-num">{{ house.rentPrice }}</span>
              <span class="price-unit">元/月</span>
            </div>
            <div class="deposit-info">{{ house.depositType || '-' }}</div>
            
            <!-- 房源简要信息 -->
            <div class="house-brief">
              <div class="brief-item">
                <span class="brief-val">{{ house.houseType }}</span>
                <span class="brief-label">户型</span>
              </div>
              <div class="brief-item">
                <span class="brief-val">{{ house.area }}㎡</span>
                <span class="brief-label">面积</span>
              </div>
              <div class="brief-item">
                <span class="brief-val">{{ house.orientation || '-' }}</span>
                <span class="brief-label">朝向</span>
              </div>
              <div class="brief-item">
                <span class="brief-val">{{ house.floor || '-' }}</span>
                <span class="brief-label">楼层</span>
              </div>
            </div>

            <div class="stats-row">
              <span><el-icon><View /></el-icon> {{ house.viewCount || 0 }}次浏览</span>
              <span><el-icon><Star /></el-icon> {{ house.collectCount || 0 }}人收藏</span>
            </div>

            <!-- 房东信息（紧接 stats-row，无大按钮） -->
            <div class="landlord-card" @click="showLandlordCard">
              <el-avatar :size="45" :src="house.landlord?.avatar" class="landlord-avatar">
                <el-icon :size="22"><User /></el-icon>
              </el-avatar>
              <div class="landlord-detail">
                <div class="name">{{ house.landlord?.realName || house.landlord?.username || '房东' }}</div>
                <div class="verify" v-if="house.landlord?.realnameStatus === 1">
                  <el-icon color="#52c41a"><CircleCheck /></el-icon> 已实名认证
                </div>
              </div>
              <el-button type="warning" size="small" @click.stop="handleChat" :disabled="house.status === 2">
                <el-icon><ChatDotRound /></el-icon> {{ house.status === 2 ? '已出租' : '联系房东' }}
              </el-button>
            </div>

            <el-alert v-if="house.status === 2" title="该房源已出租，暂不可租" type="warning" :closable="false" show-icon style="margin-top: 12px" />
          </div>

          <!-- 讨论区（右侧独立卡片） -->
          <div class="review-card">
            <div class="review-card-header">
              <span class="review-title">讨论区</span>
              <span class="review-count" v-if="commentsData.total > 0">{{ commentsData.total }} 条</span>
            </div>
            <!-- 发帖框 -->
            <div class="post-box">
              <el-input v-model="newComment" type="textarea" :rows="3"
                :placeholder="userStore.userInfo?.userType === 2 ? '作为房东，在此回复租客的问题...' : '分享你对这个房源的看法...'"
                maxlength="500" show-word-limit />
              <div class="post-actions">
                <span class="post-tip" v-if="userStore.userInfo?.userType !== 2">系统会自动标注是否租过该房源</span>
                <span class="post-tip" v-else>房东身份发帖将显示「房东」标签</span>
                <el-button type="primary" size="small" @click="submitComment" :loading="commentSubmitting">发表</el-button>
              </div>
            </div>
            <!-- 帖子列表 -->
            <div class="comment-list" v-loading="commentsLoading">
              <div v-if="commentsData.list.length === 0 && !commentsLoading" class="no-data">暂无讨论，来发表第一条吧</div>
              <div v-for="item in commentsData.list" :key="item.commentId" class="comment-item">
                <!-- 帖子头部 -->
                <div class="comment-user">
                  <el-avatar :size="36" :src="item.avatar"><el-icon><User /></el-icon></el-avatar>
                  <div class="comment-user-info">
                    <div class="comment-user-name">
                      {{ item.realName || item.username || '匿名用户' }}
                      <el-tag v-if="item.hasRented" type="success" size="small" class="rented-tag">已租过</el-tag>
                      <el-tag v-else type="info" size="small" class="rented-tag">未租过</el-tag>
                      <el-tag v-if="item.userType === 2" type="warning" size="small" class="rented-tag">房东</el-tag>
                    </div>
                    <span class="comment-time">{{ formatDate(item.createTime) }}</span>
                  </div>
                  <!-- 操作菜单 -->
                  <el-dropdown trigger="click">
                    <el-icon class="comment-more"><MoreFilled /></el-icon>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item v-if="item.isOwn" @click="handleDeleteComment(item)">
                          <el-icon><Delete /></el-icon> 删除
                        </el-dropdown-item>
                        <el-dropdown-item v-if="!item.isOwn" @click="openReportDialog(item)">
                          <el-icon><WarningFilled /></el-icon> 举报
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
                <!-- 帖子内容 -->
                <div class="comment-content">{{ item.content }}</div>
                <!-- 底部操作栏 -->
                <div class="comment-footer">
                  <span class="reply-btn" @click="toggleReply(item)">
                    <el-icon><ChatLineRound /></el-icon> 回复
                  </span>
                  <span class="expand-btn" v-if="item.replies?.length > 0" @click="expandedReplies[item.commentId] = !expandedReplies[item.commentId]">
                    {{ expandedReplies[item.commentId] ? '收起' : `展开 ${item.replies.length} 条回复` }}
                  </span>
                </div>
                <!-- 回复输入框 -->
                <div class="reply-input-box" v-if="replyStates[item.commentId]?.visible">
                  <el-input v-model="replyStates[item.commentId].content" type="textarea" :rows="2"
                    :placeholder="`回复 ${item.realName || item.username}...`" maxlength="200" show-word-limit />
                  <div class="reply-input-actions">
                    <el-button size="small" @click="replyStates[item.commentId].visible = false">取消</el-button>
                    <el-button type="primary" size="small" @click="submitReply(item)"
                      :loading="replyStates[item.commentId]?.submitting">回复</el-button>
                  </div>
                </div>
                <!-- 回复列表 -->
                <div class="replies-wrap" v-if="item.replies?.length > 0 && expandedReplies[item.commentId]">
                  <div v-for="reply in item.replies" :key="reply.commentId" class="reply-item">
                    <el-avatar :size="28" :src="reply.avatar"><el-icon><User /></el-icon></el-avatar>
                    <div class="reply-body">
                      <div class="reply-header">
                        <span class="reply-username">{{ reply.realName || reply.username }}</span>
                        <span v-if="reply.replyToUsername" class="reply-to">回复 <b>{{ reply.replyToUsername }}</b></span>
                        <el-tag v-if="reply.hasRented" type="success" size="small" class="rented-tag">已租过</el-tag>
                        <span class="comment-time">{{ formatDate(reply.createTime) }}</span>
                        <el-dropdown trigger="click" style="margin-left:auto">
                          <el-icon class="comment-more"><MoreFilled /></el-icon>
                          <template #dropdown>
                            <el-dropdown-menu>
                              <el-dropdown-item v-if="reply.isOwn" @click="handleDeleteComment(reply)">
                                <el-icon><Delete /></el-icon> 删除
                              </el-dropdown-item>
                              <el-dropdown-item v-if="!reply.isOwn" @click="openReportDialog(reply)">
                                <el-icon><WarningFilled /></el-icon> 举报
                              </el-dropdown-item>
                            </el-dropdown-menu>
                          </template>
                        </el-dropdown>
                      </div>
                      <div class="reply-content">{{ reply.content }}</div>
                      <div class="comment-footer">
                        <span class="reply-btn" @click="toggleReply(item)">
                          <el-icon><ChatLineRound /></el-icon> 回复
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="comment-pagination" v-if="commentsData.total > 20">
              <el-pagination background layout="prev, pager, next" :total="commentsData.total" :page-size="20"
                v-model:current-page="commentPage" @current-change="loadComments" small />
            </div>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="房源不存在或已下架">
      <el-button type="primary" @click="goBack">返回列表</el-button>
    </el-empty>

    <!-- 房东个人卡片弹窗 -->
    <UserCardDialog v-model="landlordCardVisible" :user-id="house?.landlordId" />
  </div>
</template>


<script setup lang="ts">
import { ref, computed, onMounted, reactive, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, Star, View, User, CircleCheck, House, Check, Picture, StarFilled, ChatDotRound, Location, School, Van, ShoppingCart, FirstAidKit, MoreFilled, WarningFilled, Delete, ChatLineRound } from '@element-plus/icons-vue'
import { getHouseDetailApi, addFavoriteApi, removeFavoriteApi, checkFavoriteApi } from '@/api/tenantHouse'
import { getHouseCommentsApi, postCommentApi, deleteCommentApi, reportCommentApi } from '@/api/houseComment'
import { createOrGetSessionApi } from '@/api/chat'
import UserCardDialog from '@/components/UserCardDialog.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

declare const AMap: any

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const house = ref<any>(null)
const activeTab = ref('info')
const currentIndex = ref(0)
const isFavorite = ref(false)
const isHovering = ref(false)
const landlordCardVisible = ref(false)

// 讨论区相关
const commentsLoading = ref(false)
const commentsData = reactive({ list: [] as any[], total: 0 })
const commentPage = ref(1)
const newComment = ref('')
const commentSubmitting = ref(false)
let commentsLoaded = false

// 回复状态：key=commentId，value={visible, content, submitting}
const replyStates = reactive<Record<number, { visible: boolean; content: string; submitting: boolean }>>({})
// 展开回复：key=commentId
const expandedReplies = reactive<Record<number, boolean>>({})

// 举报相关
const reportDialogVisible = ref(false)
const reportTarget = ref<any>(null)
const reportReason = ref('')
const reportSubmitting = ref(false)

// 地图相关
let mapInstance: any = null
let houseMarker: any = null
let poiMarkers: any[] = []
const poiLoading = ref(false)
const poiList = ref<any[]>([])
const activePoi = ref('subway')
const poiCategories = [
  { key: 'subway', label: '地铁', keyword: '地铁站', icon: 'Van' },
  { key: 'bus', label: '公交', keyword: '公交站', icon: 'Van' },
  { key: 'school', label: '学校', keyword: '学校', icon: 'School' },
  { key: 'hospital', label: '医院', keyword: '医院', icon: 'FirstAidKit' },
  { key: 'mall', label: '商场', keyword: '购物中心|超市', icon: 'ShoppingCart' },
]

const collectBtnText = computed(() => {
  if (!isFavorite.value) return '收藏房源'
  return isHovering.value ? '取消收藏' : '已收藏'
})

const imageList = computed(() => {
  if (!house.value?.images) return []
  try { return JSON.parse(house.value.images) } catch { return [] }
})

const currentImage = computed(() => imageList.value[currentIndex.value] || '')

const facilitiesList = computed(() => {
  if (!house.value?.facilities) return []
  try { return JSON.parse(house.value.facilities) } catch { return [] }
})

const rentOptionText = computed(() => {
  const map: Record<number, string> = { 1: '整租', 2: '合租', 3: '整租/合租' }
  return map[house.value?.rentOption] || '整租/合租'
})

const statusText = computed(() => {
  const map: Record<number, string> = { 0: '待审核', 1: '可出租', 2: '已出租', 3: '已下架', 4: '审核驳回' }
  return map[house.value?.status] || '未知'
})

const statusType = computed(() => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'danger' }
  return map[house.value?.status] || 'info'
})

const formatDate = (date: string) => date ? new Date(date).toLocaleDateString('zh-CN') : ''

const prevImage = () => { if (currentIndex.value > 0) currentIndex.value-- }
const nextImage = () => { if (currentIndex.value < imageList.value.length - 1) currentIndex.value++ }
const goBack = () => router.push('/tenant/search')

const showLandlordCard = () => {
  if (house.value?.landlordId) landlordCardVisible.value = true
}

const loadDetail = async () => {
  const houseId = route.params.houseId as string
  if (!houseId) return
  loading.value = true
  try {
    const res: any = await getHouseDetailApi(Number(houseId))
    house.value = res
    checkFavoriteStatus(Number(houseId))
    saveToRecentViewed(res)
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const RECENT_KEY = 'tenant_recent_viewed'
const MAX_RECENT = 8
const saveToRecentViewed = (h: any) => {
  if (!h || !h.houseId) return
  try {
    const stored = localStorage.getItem(RECENT_KEY)
    let list = stored ? JSON.parse(stored) : []
    list = list.filter((item: any) => item.houseId !== h.houseId)
    list.unshift({ houseId: h.houseId, title: h.title, address: h.address, houseType: h.houseType, area: h.area, rentPrice: h.rentPrice, images: h.images })
    localStorage.setItem(RECENT_KEY, JSON.stringify(list.slice(0, MAX_RECENT)))
  } catch {}
}

const checkFavoriteStatus = async (houseId: number) => {
  try { isFavorite.value = (await checkFavoriteApi(houseId)) === true } catch {}
}

const handleCollect = async () => {
  if (!house.value) return
  try {
    if (isFavorite.value) {
      await removeFavoriteApi(house.value.houseId)
      isFavorite.value = false
      house.value.collectCount = Math.max(0, (house.value.collectCount || 1) - 1)
      ElMessage.success('已取消收藏')
    } else {
      await addFavoriteApi(house.value.houseId)
      isFavorite.value = true
      house.value.collectCount = (house.value.collectCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } catch {}
}

const handleChat = async () => {
  if (!house.value) return
  try {
    const session: any = await createOrGetSessionApi({ houseId: house.value.houseId })
    // 跳转到聊天页面并传递会话ID
    router.push({ path: '/tenant/chat', query: { sessionId: session.sessionId } })
  } catch { ElMessage.error('发起聊天失败') }
}

// 加载房源讨论帖子
const loadComments = async () => {
  if (!house.value) return
  commentsLoading.value = true
  try {
    const res: any = await getHouseCommentsApi(house.value.houseId, { page: commentPage.value, size: 20 })
    commentsData.list = res.records || []
    commentsData.total = res.total || 0
    commentsLoaded = true
  } catch (e) { console.error(e) } finally { commentsLoading.value = false }
}

// 发表帖子
const submitComment = async () => {
  if (!newComment.value.trim()) { ElMessage.warning('请输入内容'); return }
  if (!house.value) return
  commentSubmitting.value = true
  try {
    await postCommentApi(house.value.houseId, { content: newComment.value.trim() })
    ElMessage.success('发表成功')
    newComment.value = ''
    commentPage.value = 1
    await loadComments()
  } catch (e: any) { ElMessage.error(e.message || '发表失败') } finally { commentSubmitting.value = false }
}

// 删除帖子
const handleDeleteComment = async (item: any) => {
  try {
    await ElMessageBox.confirm('确定删除这条帖子吗？', '删除确认', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await deleteCommentApi(item.commentId)
    ElMessage.success('已删除')
    await loadComments()
  } catch (e: any) {
    if (e !== 'cancel' && e !== undefined) ElMessage.error(e.message || '删除失败')
  }
}

// 切换回复框
const toggleReply = (item: any) => {
  if (!userStore.userInfo) { ElMessage.warning('请先登录'); return }
  const id = item.commentId
  if (!replyStates[id]) replyStates[id] = { visible: false, content: '', submitting: false }
  replyStates[id].visible = !replyStates[id].visible
}

// 提交回复
const submitReply = async (parentItem: any, replyToItem?: any) => {
  const id = parentItem.commentId
  if (!replyStates[id]?.content?.trim()) { ElMessage.warning('请输入回复内容'); return }
  if (!house.value) return
  replyStates[id].submitting = true
  try {
    await postCommentApi(house.value.houseId, {
      content: replyStates[id].content.trim(),
      parentId: parentItem.commentId,
      replyToUserId: replyToItem?.userId ?? parentItem.userId
    })
    ElMessage.success('回复成功')
    replyStates[id].content = ''
    replyStates[id].visible = false
    expandedReplies[id] = true
    await loadComments()
  } catch (e: any) { ElMessage.error(e.message || '回复失败') } finally { replyStates[id].submitting = false }
}

// 举报帖子
const openReportDialog = (item: any) => {
  reportTarget.value = item
  reportReason.value = ''
  reportDialogVisible.value = true
}

const submitReport = async () => {
  if (!reportReason.value.trim()) { ElMessage.warning('请填写举报原因'); return }
  reportSubmitting.value = true
  try {
    await reportCommentApi(reportTarget.value.commentId, reportReason.value.trim())
    ElMessage.success('举报成功，等待管理员审核')
    reportDialogVisible.value = false
    await loadComments()
  } catch (e: any) { ElMessage.error(e.message || '举报失败') } finally { reportSubmitting.value = false }
}

// 切换到地图 tab，初始化地图
const switchToMap = async () => {
  activeTab.value = 'map'
  await nextTick()
  if (!mapInstance) initMap()
}

const initMap = () => {
  if (typeof AMap === 'undefined') {
    ElMessage.warning('地图加载中，请稍后重试')
    return
  }
  const h = house.value
  if (!h) return

  mapInstance = new AMap.Map('house-map', {
    zoom: 15,
    resizeEnable: true,
  })
  mapInstance.addControl(new AMap.Scale())
  mapInstance.addControl(new AMap.ToolBar({ position: 'RB' }))

  // 地理编码：地址 → 坐标
  const geocoder = new AMap.Geocoder({ city: h.city || '全国' })
  const fullAddr = `${h.province || ''}${h.city || ''}${h.district || ''}${h.address || ''}`
  geocoder.getLocation(fullAddr, (status: string, result: any) => {
    if (status === 'complete' && result.geocodes.length > 0) {
      const lnglat = result.geocodes[0].location
      mapInstance.setCenter(lnglat)
      houseMarker = new AMap.Marker({
        position: lnglat,
        title: h.title,
        label: {
          content: `<div class="map-house-label">${h.title}</div>`,
          offset: new AMap.Pixel(-60, -40)
        }
      })
      mapInstance.add(houseMarker)
      // 默认搜索地铁
      searchPoi('subway', '地铁站', lnglat)
    } else {
      ElMessage.warning('地址解析失败，无法定位房源位置')
    }
  })
}

const searchPoi = (key: string, keyword: string, center?: any) => {
  if (typeof AMap === 'undefined' || !mapInstance) return
  activePoi.value = key
  poiLoading.value = true
  poiList.value = []
  if (poiMarkers.length) { mapInstance.remove(poiMarkers); poiMarkers = [] }

  const searchCenter = center || mapInstance.getCenter()
  const placeSearch = new AMap.PlaceSearch({ pageSize: 8, pageIndex: 1 })
  placeSearch.searchNearBy(keyword, searchCenter, 1500, (status: string, result: any) => {
    poiLoading.value = false
    if (status === 'complete' && result.poiList?.pois?.length) {
      poiList.value = result.poiList.pois
      result.poiList.pois.forEach((poi: any, idx: number) => {
        const marker = new AMap.Marker({
          position: [poi.location.lng, poi.location.lat],
          label: {
            content: `<div class="map-poi-num">${idx + 1}</div>`,
            offset: new AMap.Pixel(-10, -30)
          }
        })
        mapInstance.add(marker)
        poiMarkers.push(marker)
      })
    }
  })
}

const focusPoi = (poi: any) => {
  if (!mapInstance || !poi.location) return
  mapInstance.setCenter([poi.location.lng, poi.location.lat])
  mapInstance.setZoom(16)
}

const formatDist = (dist: number) => {
  if (!dist && dist !== 0) return ''
  return dist >= 1000 ? `${(dist / 1000).toFixed(1)}km` : `${Math.round(dist)}m`
}

onMounted(() => { loadDetail().then(() => loadComments()) })

onBeforeUnmount(() => {
  if (mapInstance) { mapInstance.destroy(); mapInstance = null }
})
</script>


<style scoped>
.house-detail-page {
  padding: 20px 40px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}

/* 顶部标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e8e8e8;
}

.house-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0 0 10px 0;
}

.house-meta {
  color: #999;
  font-size: 13px;
}

/* 收藏按钮 */
.collect-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #666;
  cursor: pointer;
  padding: 8px 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  transition: all 0.3s;
}

.collect-btn:hover { color: #ff6600; border-color: #ff6600; }
.collect-btn.active { color: #ff6600; border-color: #ff6600; background: #fff7e6; }
.collect-btn.active.hover { color: #ff4d4f; border-color: #ff4d4f; background: #fff1f0; }
.collect-btn.active .collect-icon { animation: heartBeat 0.3s ease; }
@keyframes heartBeat { 0% { transform: scale(1); } 50% { transform: scale(1.3); } 100% { transform: scale(1); } }

/* 主体布局 - 6:4比例 */
.main-content {
  display: flex;
  gap: 25px;
}

.left-section {
  flex: 6;
  min-width: 0;
}

.right-section {
  flex: 4;
}

/* 图片展示 - 贝壳风格 */
.image-section {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.main-image {
  position: relative;
  height: 380px;
  background: #000;
}

.main-image .el-image {
  width: 100%;
  height: 100%;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #ccc;
}

.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 18px;
}

.nav-btn:hover { background: rgba(0, 0, 0, 0.7); }
.nav-btn.prev { left: 15px; }
.nav-btn.next { right: 15px; }

.image-index {
  position: absolute;
  bottom: 12px;
  right: 15px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
}

.thumbnail-list {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #fafafa;
  overflow-x: auto;
}

.thumb-item {
  width: 100px;
  height: 75px;
  flex-shrink: 0;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.thumb-item.active { border-color: #409eff; }
.thumb-item .el-image { width: 100%; height: 100%; }

/* Tab导航 */
.info-tabs {
  display: flex;
  gap: 30px;
  padding: 15px 20px;
  background: #fff;
  margin-top: 20px;
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid #e8e8e8;
}

.tab-item {
  color: #666;
  cursor: pointer;
  padding: 5px 0;
  position: relative;
  transition: all 0.3s;
}

.tab-item:hover { color: #409eff; }
.tab-item.active { color: #409eff; font-weight: 500; }
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -16px;
  left: 0;
  right: 0;
  height: 2px;
  background: #409eff;
}

/* 信息面板 */
.info-panel {
  background: #fff;
  padding: 20px;
  border-radius: 0 0 8px 8px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.info-row { display: flex; }
.info-row.full { grid-column: span 2; }
.info-label { color: #999; width: 80px; flex-shrink: 0; }
.info-value { color: #333; }

/* 房源描述 */
.landlord-row {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.clickable-avatar { cursor: pointer; transition: transform 0.3s; }
.clickable-avatar:hover { transform: scale(1.1); }

.landlord-info .landlord-name {
  font-size: 15px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.landlord-info .landlord-role {
  color: #999;
  font-size: 13px;
  margin-top: 5px;
}

.desc-images { margin-bottom: 20px; }
.desc-image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.desc-img {
  width: 100%;
  height: 150px;
  border-radius: 4px;
}

.desc-text {
  color: #666;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* 费用详情 */
.cost-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.cost-item {
  text-align: center;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.cost-label {
  display: block;
  color: #999;
  font-size: 13px;
  margin-bottom: 10px;
}

.cost-value { font-size: 16px; color: #333; }
.cost-value.highlight { color: #ff6600; font-weight: 600; font-size: 18px; }

/* 配套设施 */
.facility-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

.facility-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 13px;
}

.facility-icon { font-size: 24px; color: #52c41a; }
.no-data { color: #999; text-align: center; padding: 30px; }

/* 讨论区 */
.tab-badge { font-size: 12px; color: #999; margin-left: 2px; }
.post-box { background: #fafafa; border-radius: 8px; padding: 15px; margin-bottom: 20px; }
.post-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.post-tip { font-size: 12px; color: #999; }
.comment-list { display: flex; flex-direction: column; gap: 16px; }
.comment-item { padding: 16px 0; border-bottom: 1px solid #f0f0f0; }
.comment-item:last-child { border-bottom: none; }
.comment-user { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.comment-user-info { flex: 1; display: flex; flex-direction: column; }
.comment-user-name { font-size: 14px; font-weight: 500; color: #333; display: flex; align-items: center; gap: 6px; }
.comment-time { font-size: 12px; color: #999; }
.rented-tag { transform: scale(0.85); }
.comment-content { font-size: 14px; color: #555; line-height: 1.6; padding-left: 46px; }
.comment-actions-right { flex-shrink: 0; }
.comment-more { cursor: pointer; color: #999; font-size: 16px; }
.comment-more:hover { color: #409eff; }
.comment-pagination { margin-top: 20px; display: flex; justify-content: center; }
.report-content-preview { background: #f5f5f5; padding: 10px; border-radius: 6px; font-size: 13px; color: #666; max-height: 80px; overflow: hidden; }

/* 地址与交通 */
.address-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #555;
  font-size: 14px;
  margin-bottom: 12px;
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 6px;
}
.map-container {
  width: 100%;
  height: 360px;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}
.poi-section { margin-top: 4px; }
.poi-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.poi-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 20px;
  border: 1px solid #dcdfe6;
  font-size: 13px;
  cursor: pointer;
  color: #606266;
  transition: all 0.2s;
}
.poi-tab:hover { border-color: #409eff; color: #409eff; }
.poi-tab.active { background: #409eff; color: #fff; border-color: #409eff; }
.poi-list { min-height: 60px; }
.poi-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}
.poi-item:hover { background: #f5f7fa; padding-left: 6px; border-radius: 4px; }
.poi-index {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.poi-info { flex: 1; min-width: 0; }
.poi-name { font-size: 14px; color: #303133; font-weight: 500; }
.poi-addr { font-size: 12px; color: #909399; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.poi-dist { font-size: 13px; color: #409eff; flex-shrink: 0; }

/* 右侧价格卡片 */
.price-card {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 5px;
  margin-bottom: 10px;
}

.price-num { font-size: 36px; font-weight: bold; color: #ff6600; }
.price-unit { font-size: 16px; color: #ff6600; }

.deposit-info {
  color: #666;
  font-size: 14px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

/* 房源简要信息 */
.house-brief {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
}

.brief-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brief-val {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.brief-label {
  font-size: 12px;
  color: #999;
}

.stats-row {
  display: flex;
  gap: 25px;
  padding: 15px 0;
  color: #999;
  font-size: 14px;
  border-bottom: 1px solid #f0f0f0;
}

.stats-row span { display: flex; align-items: center; gap: 5px; }

.landlord-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  cursor: pointer;
  transition: all 0.3s;
}
.landlord-avatar { cursor: pointer; transition: transform 0.3s; }
.landlord-avatar:hover { transform: scale(1.1); }
.landlord-detail { flex: 1; }
.landlord-detail .name { font-size: 16px; font-weight: 500; color: #333; }
.landlord-detail .verify { display: flex; align-items: center; gap: 4px; color: #52c41a; font-size: 13px; margin-top: 6px; }

/* 弹窗样式 */
.dialog-house-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
}

.dialog-house-img {
  width: 100px;
  height: 75px;
  border-radius: 6px;
  flex-shrink: 0;
}

.dialog-house-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.dialog-house-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dialog-house-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #999;
}

.dialog-house-price {
  font-size: 18px;
  font-weight: bold;
  color: #ff6600;
}

.dialog-house-price small {
  font-size: 12px;
  font-weight: normal;
}

.dialog-house-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: #f5f5f5;
  border-radius: 4px;
}

.dialog-house-info .price { color: #ff6600; font-weight: 500; }

.cost-preview {
  background: #f5f5f5;
  border-radius: 6px;
  padding: 15px;
  width: 100%;
}

.cost-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  color: #666;
  font-size: 14px;
}

.cost-row.total {
  border-top: 1px dashed #ddd;
  margin-top: 8px;
  padding-top: 12px;
  font-weight: 500;
  color: #333;
}

.cost-row .total-price { color: #ff6600; font-size: 18px; font-weight: bold; }

/* 右侧讨论区卡片 */
.review-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-top: 16px;
}
.review-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.review-title { font-size: 15px; font-weight: 600; color: #303133; }
.review-count { font-size: 13px; color: #909399; }

/* 帖子底部操作栏 */
.comment-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-left: 46px;
  margin-top: 8px;
}
.like-btn,
.reply-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}
.reply-btn:hover { color: #409eff; }
.expand-btn {
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
}
.expand-btn:hover { text-decoration: underline; }

/* 回复输入框 */
.reply-input-box {
  margin: 10px 0 0 46px;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px;
}
.reply-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

/* 回复列表 */
.replies-wrap {
  margin: 10px 0 0 46px;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.reply-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.reply-body { flex: 1; min-width: 0; }
.reply-header {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.reply-username { font-size: 13px; font-weight: 500; color: #333; }
.reply-to { font-size: 12px; color: #909399; }
.reply-to b { color: #606266; }
.reply-content { font-size: 13px; color: #555; line-height: 1.6; }
.reply-item .comment-footer { padding-left: 0; margin-top: 4px; }
</style>
