<template>
  <div class="house-list-page">
    <!-- 实名认证提示 -->
    <RealnameAlert :require-realname="true" feature-name="房源管理功能" />

    <!-- 已认证用户显示内容 -->
    <template v-if="isRealnameVerified">
      <!-- 统计卡片 -->
      <div class="stats-row">
        <el-card class="stat-card" shadow="hover" @click="handleFilterByStatus(null)">
          <div class="stat-content">
            <div class="stat-icon total"><el-icon :size="28"><House /></el-icon></div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.total || 0 }}</p>
              <p class="stat-label">全部房源</p>
            </div>
          </div>
        </el-card>
        <el-card class="stat-card" shadow="hover" @click="handleFilterByStatus(0)">
          <div class="stat-content">
            <div class="stat-icon pending"><el-icon :size="28"><Clock /></el-icon></div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.pending || 0 }}</p>
              <p class="stat-label">待审核</p>
            </div>
          </div>
        </el-card>
        <el-card class="stat-card" shadow="hover" @click="handleFilterByStatus(1)">
          <div class="stat-content">
            <div class="stat-icon available"><el-icon :size="28"><CircleCheck /></el-icon></div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.available || 0 }}</p>
              <p class="stat-label">可出租</p>
            </div>
          </div>
        </el-card>
        <el-card class="stat-card" shadow="hover" @click="handleFilterByStatus(2)">
          <div class="stat-content">
            <div class="stat-icon rented"><el-icon :size="28"><Finished /></el-icon></div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.rented || 0 }}</p>
              <p class="stat-label">已出租</p>
            </div>
          </div>
        </el-card>
        <el-card class="stat-card" shadow="hover" @click="handleFilterByStatus(3)">
          <div class="stat-content">
            <div class="stat-icon offline"><el-icon :size="28"><Remove /></el-icon></div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.offline || 0 }}</p>
              <p class="stat-label">已下架</p>
            </div>
          </div>
        </el-card>
        <el-card class="stat-card" shadow="hover" @click="handleFilterByStatus(4)">
          <div class="stat-content">
            <div class="stat-icon rejected"><el-icon :size="28"><CircleClose /></el-icon></div>
            <div class="stat-info">
              <p class="stat-value">{{ stats.rejected || 0 }}</p>
              <p class="stat-label">审核驳回</p>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 筛选和操作 -->
      <el-card class="filter-card">
        <el-form :inline="true" :model="filterForm" class="filter-form">
          <el-form-item label="房源状态">
            <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
              <el-option label="待审核" :value="0" />
              <el-option label="可出租" :value="1" />
              <el-option label="已出租" :value="2" />
              <el-option label="已下架" :value="3" />
              <el-option label="审核驳回" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="filterForm.keyword" placeholder="标题/地址" clearable style="width: 180px" 
              @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
          </el-form-item>
          <el-form-item style="margin-left: auto;">
            <div class="view-switch">
              <span :class="['switch-btn', { active: viewMode === 'list' }]" @click="viewMode = 'list'" title="列表模式">
                <el-icon><List /></el-icon>
              </span>
              <span :class="['switch-btn', { active: viewMode === 'map' }]" @click="switchToMapView" title="地图模式">
                <el-icon><MapLocation /></el-icon>
              </span>
            </div>
            <el-button type="primary" @click="handlePublish" style="margin-left:12px">
              <el-icon><Plus /></el-icon> 发布房源
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 房源列表 -->
      <el-card class="table-card">
        <!-- 列表视图 -->
        <template v-if="viewMode === 'list'">
          <!-- 批量操作工具栏 -->
          <div v-if="selectedIds.length > 0" class="batch-toolbar">
            <span class="batch-info">已选 <b>{{ selectedIds.length }}</b> 套</span>
            <el-button size="small" @click="batchOnline" :loading="batchOperating">批量上架</el-button>
            <el-button size="small" type="warning" @click="batchOffline" :loading="batchOperating">批量下架</el-button>
            <el-button size="small" type="primary" @click="openBatchPrice">批量改价</el-button>
            <el-button size="small" @click="clearSelection">取消选择</el-button>
          </div>

          <div class="house-list" v-loading="loading">
            <!-- 全选行 -->
            <div v-if="houses.length > 0" class="select-all-row">
              <el-checkbox
                :model-value="selectedIds.length === houses.length && houses.length > 0"
                :indeterminate="selectedIds.length > 0 && selectedIds.length < houses.length"
                @change="toggleSelectAll"
              >全选本页</el-checkbox>
            </div>

            <div class="house-item" v-for="house in houses" :key="house.houseId"
              :class="{ selected: selectedIds.includes(house.houseId) }">
              <div class="house-checkbox">
                <el-checkbox
                  :model-value="selectedIds.includes(house.houseId)"
                  @change="toggleSelect(house.houseId)"
                />
              </div>
              <div class="house-image">
                <img :src="getFirstImage(house.images)" alt="房源图片" />
                <div class="house-status" :class="getStatusClass(house.status)">
                  {{ getStatusText(house.status) }}
                </div>
              </div>
              <div class="house-info">
                <h3 class="house-title">{{ house.title }}</h3>
                <p class="house-address">
                  <el-icon><Location /></el-icon>
                  {{ house.province }}{{ house.city }}{{ house.district }} {{ house.address }}
                </p>
                <div class="house-meta">
                  <span><el-icon><HomeFilled /></el-icon> {{ house.houseType }}</span>
                  <span><el-icon><Expand /></el-icon> {{ house.area }}㎡</span>
                  <span><el-icon><Compass /></el-icon> {{ house.orientation }}</span>
                  <span><el-icon><OfficeBuilding /></el-icon> {{ house.floor }}</span>
                </div>
                <div class="house-stats">
                  <span><el-icon><View /></el-icon> 浏览 {{ house.viewCount || 0 }}</span>
                  <span><el-icon><Star /></el-icon> 收藏 {{ house.collectCount || 0 }}</span>
                  <span class="create-time">发布于 {{ formatTime(house.createTime) }}</span>
                </div>
                <div v-if="house.status === 4 && house.auditReason" class="reject-reason">
                  <el-icon><Warning /></el-icon> 驳回原因：{{ house.auditReason }}
                </div>
              </div>
              <div class="house-price">
                <span class="price">¥{{ house.rentPrice }}</span>
                <span class="unit">/月</span>
                <p class="deposit">{{ house.depositType || '-' }}</p>
              </div>
              <div class="house-actions">
                <el-button type="primary" link @click="handleView(house)">查看详情</el-button>
                <el-button type="success" link @click="router.push(`/tenant/house/${house.houseId}`)">讨论区</el-button>
                <el-button type="info" link @click="openDashboard(house)"><el-icon><DataLine /></el-icon> 数据看板</el-button>
                <el-button v-if="house.status !== 2" type="warning" link @click="handleEdit(house)">编辑</el-button>
                <el-button v-if="house.status === 1" type="danger" link @click="handleOffline(house)">下架</el-button>
                <el-button v-if="house.status === 3" type="success" link @click="handleOnline(house)">重新上架</el-button>
                <el-button v-if="house.status !== 2" type="danger" link @click="handleDelete(house)">删除</el-button>
              </div>
            </div>

            <el-empty v-if="!houses.length && !loading" description="暂无房源，快去发布吧">
              <el-button type="primary" @click="handlePublish">发布房源</el-button>
            </el-empty>
          </div>
          <div class="pagination-container" v-if="pagination.total > 0">
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.size"
              :page-sizes="[10, 20, 50]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="fetchHouses"
              @current-change="fetchHouses"
            />
          </div>
        </template>

        <!-- 地图视图 -->
        <template v-if="viewMode === 'map'">
          <div class="list-map-container">
            <div id="house-list-map" class="house-list-map"></div>
          </div>
        </template>
      </el-card>
    </template>

    <!-- 未认证用户显示占位 -->
    <template v-else>
      <el-card class="page-card">
        <el-empty description="完成实名认证后即可管理房源">
          <template #image>
            <el-icon :size="80" color="#909399"><House /></el-icon>
          </template>
        </el-empty>
      </el-card>
    </template>

    <!-- 批量改价弹窗 -->
  <el-dialog v-model="batchPriceVisible" title="批量修改价格" width="420px" :close-on-click-modal="false">
    <el-form :model="batchPriceForm" label-width="100px">
      <el-form-item label="调整方式">
        <el-radio-group v-model="batchPriceForm.adjustType">
          <el-radio value="fixed">设置固定价格</el-radio>
          <el-radio value="percent">按百分比调整</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="batchPriceForm.adjustType === 'fixed' ? '新价格(元)' : '调整幅度(%)'">
        <el-input-number
          v-model="batchPriceForm.price"
          :min="batchPriceForm.adjustType === 'percent' ? -50 : 1"
          :max="batchPriceForm.adjustType === 'percent' ? 100 : 99999"
          :precision="0"
          style="width: 100%"
        />
        <div class="price-tip" v-if="batchPriceForm.adjustType === 'percent'">
          正数涨价，负数降价，如 10 表示涨价10%，-10 表示降价10%
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="batchPriceVisible = false">取消</el-button>
      <el-button type="primary" @click="submitBatchPrice" :loading="batchOperating">确认修改</el-button>
    </template>
  </el-dialog>

  <!-- 数据看板抽屉 -->
  <el-drawer v-model="dashboardVisible" title="房源数据看板" size="520px" @close="onDashboardClose">
    <div v-loading="dashboardLoading" class="dashboard-drawer">
      <template v-if="dashboardData">
        <!-- 基础指标 -->
        <div class="db-metrics">
          <div class="db-metric">
            <div class="db-metric-val">{{ dashboardData.viewCount }}</div>
            <div class="db-metric-label">总浏览量</div>
          </div>
          <div class="db-metric">
            <div class="db-metric-val">{{ dashboardData.collectCount }}</div>
            <div class="db-metric-label">收藏数</div>
          </div>
          <div class="db-metric">
            <div class="db-metric-val">{{ dashboardData.consultCount }}</div>
            <div class="db-metric-label">咨询量</div>
          </div>
          <div class="db-metric">
            <div class="db-metric-val">{{ dashboardData.occupancyRate }}%</div>
            <div class="db-metric-label">出租率</div>
          </div>
        </div>

        <!-- 合同统计 -->
        <div class="db-section">
          <div class="db-section-title">合同概况</div>
          <div class="db-contract-info">
            <span>历史合同：{{ dashboardData.totalContracts }} 份</span>
            <span>已签约：{{ dashboardData.signedContracts }} 份</span>
            <el-tag :type="dashboardData.isRented ? 'success' : 'info'" size="small">
              {{ dashboardData.isRented ? '出租中' : '空置中' }}
            </el-tag>
          </div>
        </div>

        <!-- 近30天浏览量趋势 -->
        <div class="db-section">
          <div class="db-section-title">近30天浏览量趋势</div>
          <div id="view-trend-chart" style="height: 180px;"></div>
        </div>

        <!-- 近6个月收入 -->
        <div class="db-section">
          <div class="db-section-title">近6个月收入</div>
          <div id="income-trend-chart" style="height: 160px;"></div>
        </div>
      </template>
      <el-empty v-else-if="!dashboardLoading" description="暂无数据" />
    </div>
  </el-drawer>

  <!-- 房源详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="房源详情" width="800px" top="5vh">
      <div class="detail-content" v-if="currentHouse">
        <!-- 图片轮播：按房源图片数组动态渲染，展示房源详情图集 -->
        <el-carousel v-if="getImageArray(currentHouse.images).length" height="300px" class="detail-carousel">
          <el-carousel-item v-for="(img, index) in getImageArray(currentHouse.images)" :key="index">
            <img :src="img" alt="房源图片" class="carousel-image" />
          </el-carousel-item>
        </el-carousel>

        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="房源标题" :span="2">{{ currentHouse.title }}</el-descriptions-item>
          <el-descriptions-item label="房源状态">
            <el-tag :type="getStatusType(currentHouse.status)">{{ getStatusText(currentHouse.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="月租金">¥{{ currentHouse.rentPrice }}/月</el-descriptions-item>
          <el-descriptions-item label="押付方式">{{ currentHouse.depositType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="房屋面积">{{ currentHouse.area }}㎡</el-descriptions-item>
          <el-descriptions-item label="房屋户型">{{ currentHouse.houseType }}</el-descriptions-item>
          <el-descriptions-item label="所在楼层">{{ currentHouse.floor }}</el-descriptions-item>
          <el-descriptions-item label="房屋朝向">{{ currentHouse.orientation }}</el-descriptions-item>
          <el-descriptions-item label="租赁方式">{{ getRentOptionText(currentHouse.rentOption) }}</el-descriptions-item>
          <el-descriptions-item label="所在地区" :span="2">
            {{ currentHouse.province }}{{ currentHouse.city }}{{ currentHouse.district }}
          </el-descriptions-item>
          <el-descriptions-item label="详细地址" :span="2">{{ currentHouse.address }}</el-descriptions-item>
          <el-descriptions-item label="配套设施" :span="2">
            <el-tag v-for="f in getFacilitiesArray(currentHouse.facilities)" :key="f" size="small" class="facility-tag">
              {{ f }}
            </el-tag>
            <span v-if="!getFacilitiesArray(currentHouse.facilities).length">暂无</span>
          </el-descriptions-item>
          <el-descriptions-item label="房源描述" :span="2">{{ currentHouse.description }}</el-descriptions-item>
          <el-descriptions-item label="浏览量">{{ currentHouse.viewCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="收藏数">{{ currentHouse.collectCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="发布时间" :span="2">{{ formatTime(currentHouse.createTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentHouse.status === 4 && currentHouse.auditReason" label="驳回原因" :span="2">
            <span class="reject-text">{{ currentHouse.auditReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  House, Clock, CircleCheck, CircleClose, Finished, Remove, Plus, Search, Refresh,
  Location, HomeFilled, Expand, Compass, OfficeBuilding, View, Star, Warning,
  List, MapLocation, DataLine, Select
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import RealnameAlert from '@/components/RealnameAlert.vue'
import { 
  getHouseListApi, getHouseStatsApi, offlineHouseApi, onlineHouseApi, deleteHouseApi,
  batchHouseOperateApi, getHouseDashboardApi,
  type HouseInfo 
} from '@/api/landlordHouse'

declare const AMap: any

const router = useRouter()
const userStore = useUserStore()
const isRealnameVerified = computed(() => userStore.isRealnameVerified)

const loading = ref(false)
const houses = ref<HouseInfo[]>([])
const stats = ref<any>({})
const filterForm = reactive({ status: null as number | null, keyword: '' })
const pagination = reactive({ page: 1, size: 10, total: 0 })

// 地图视图
const viewMode = ref<'list' | 'map'>('list')
let listMap: any = null
let listMapMarkers: any[] = []
let listInfoWindow: any = null

// 详情对话框
const detailDialogVisible = ref(false)
const currentHouse = ref<HouseInfo | null>(null)

// 批量操作
const selectedIds = ref<number[]>([])
const batchPriceVisible = ref(false)
const batchPriceForm = reactive({ adjustType: 'fixed' as 'fixed' | 'percent', price: 0 })
const batchOperating = ref(false)

// 数据看板
const dashboardVisible = ref(false)
const dashboardLoading = ref(false)
const dashboardData = ref<any>(null)
let dashboardChart: any = null
let incomeChart: any = null

// 默认图片
const defaultImage = 'https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg'

// 获取第一张图片
const getFirstImage = (images: string): string => {
  if (!images) return defaultImage
  try {
    const arr = JSON.parse(images)
    return arr[0] || defaultImage
  } catch {
    return defaultImage
  }
}

// 获取图片数组
const getImageArray = (images: string): string[] => {
  if (!images) return []
  try {
    return JSON.parse(images)
  } catch {
    return []
  }
}

// 获取设施数组
const getFacilitiesArray = (facilities: string): string[] => {
  if (!facilities) return []
  try {
    return JSON.parse(facilities)
  } catch {
    return []
  }
}

// 状态相关
const getStatusClass = (status: number) => {
  const map: Record<number, string> = { 0: 'pending', 1: 'available', 2: 'rented', 3: 'offline', 4: 'rejected' }
  return map[status] || ''
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待审核', 1: '可出租', 2: '已出租', 3: '已下架', 4: '审核驳回' }
  return map[status] || '未知'
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info', 3: '', 4: 'danger' }
  return map[status] || ''
}

const getRentOptionText = (option: number) => {
  const map: Record<number, string> = { 1: '整租', 2: '合租', 3: '整租/合租' }
  return map[option] || '整租/合租'
}

const formatTime = (time: string) => time ? new Date(time).toLocaleString('zh-CN') : '-'

// 获取统计数据
const fetchStats = async () => {
  if (!isRealnameVerified.value) return
  try {
    const res = await getHouseStatsApi()
    stats.value = res
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

// 获取房源列表
const fetchHouses = async () => {
  if (!isRealnameVerified.value) return
  loading.value = true
  try {
    const res: any = await getHouseListApi({
      status: filterForm.status ?? undefined,
      keyword: filterForm.keyword || undefined,
      page: pagination.page,
      size: pagination.size
    })
    houses.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    console.error('获取房源列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; fetchHouses() }
const handleReset = () => { filterForm.status = null; filterForm.keyword = ''; handleSearch() }
const handleFilterByStatus = (status: number | null) => { filterForm.status = status; handleSearch() }

const handlePublish = () => router.push('/landlord/house/publish')
const handleView = (house: HouseInfo) => { currentHouse.value = house; detailDialogVisible.value = true }
const handleEdit = (house: HouseInfo) => router.push({ path: '/landlord/house/publish', query: { id: house.houseId } })

const handleOffline = async (house: HouseInfo) => {
  await ElMessageBox.confirm('确定要下架该房源吗？下架后租客将无法看到此房源。', '确认下架', { type: 'warning' })
  try {
    await offlineHouseApi(house.houseId)
    ElMessage.success('房源已下架')
    fetchHouses()
    fetchStats()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleOnline = async (house: HouseInfo) => {
  await ElMessageBox.confirm('确定要重新上架该房源吗？上架后需要重新审核。', '确认上架', { type: 'warning' })
  try {
    await onlineHouseApi(house.houseId)
    ElMessage.success('房源已重新提交审核')
    fetchHouses()
    fetchStats()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (house: HouseInfo) => {
  await ElMessageBox.confirm('确定要删除该房源吗？删除后无法恢复。', '确认删除', { type: 'warning' })
  try {
    await deleteHouseApi(house.houseId)
    ElMessage.success('房源已删除')
    fetchHouses()
    fetchStats()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// ========== 批量操作 ==========
const toggleSelect = (houseId: number) => {
  const idx = selectedIds.value.indexOf(houseId)
  if (idx === -1) selectedIds.value.push(houseId)
  else selectedIds.value.splice(idx, 1)
}
const toggleSelectAll = () => {
  if (selectedIds.value.length === houses.value.length) {
    selectedIds.value = []
  } else {
    selectedIds.value = houses.value.map(h => h.houseId)
  }
}
const clearSelection = () => { selectedIds.value = [] }

const batchOffline = async () => {
  await ElMessageBox.confirm(`确定批量下架选中的 ${selectedIds.value.length} 套房源吗？`, '批量下架', { type: 'warning' })
  batchOperating.value = true
  try {
    const res: any = await batchHouseOperateApi({ houseIds: selectedIds.value, action: 'offline' })
    ElMessage.success(res || '操作完成')
    selectedIds.value = []
    fetchHouses(); fetchStats()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') } finally { batchOperating.value = false }
}

const batchOnline = async () => {
  await ElMessageBox.confirm(`确定批量上架选中的 ${selectedIds.value.length} 套房源吗？上架后需重新审核。`, '批量上架', { type: 'warning' })
  batchOperating.value = true
  try {
    const res: any = await batchHouseOperateApi({ houseIds: selectedIds.value, action: 'online' })
    ElMessage.success(res || '操作完成')
    selectedIds.value = []
    fetchHouses(); fetchStats()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') } finally { batchOperating.value = false }
}

const openBatchPrice = () => {
  batchPriceForm.adjustType = 'fixed'
  batchPriceForm.price = 0
  batchPriceVisible.value = true
}

const submitBatchPrice = async () => {
  if (!batchPriceForm.price) { ElMessage.warning('请输入价格'); return }
  batchOperating.value = true
  try {
    const res: any = await batchHouseOperateApi({
      houseIds: selectedIds.value,
      action: 'price',
      price: batchPriceForm.price,
      adjustType: batchPriceForm.adjustType
    })
    ElMessage.success(res || '操作完成')
    batchPriceVisible.value = false
    selectedIds.value = []
    fetchHouses(); fetchStats()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') } finally { batchOperating.value = false }
}

// ========== 数据看板 ==========
const openDashboard = async (house: HouseInfo) => {
  dashboardVisible.value = true
  dashboardLoading.value = true
  dashboardData.value = null
  try {
    dashboardData.value = await getHouseDashboardApi(house.houseId)
    await nextTick()
    renderDashboardCharts()
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    dashboardLoading.value = false
  }
}

const renderDashboardCharts = () => {
  const data = dashboardData.value
  if (!data) return

  // 浏览量趋势图
  const viewEl = document.getElementById('view-trend-chart')
  if (viewEl && (window as any).echarts) {
    if (dashboardChart) dashboardChart.dispose()
    dashboardChart = (window as any).echarts.init(viewEl)
    dashboardChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: data.viewTrend?.map((d: any) => d.date) || [], axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ name: '浏览量', type: 'line', smooth: true, data: data.viewTrend?.map((d: any) => d.count) || [],
        areaStyle: { opacity: 0.2 }, itemStyle: { color: '#409eff' } }]
    })
  }

  // 收入趋势图
  const incomeEl = document.getElementById('income-trend-chart')
  if (incomeEl && (window as any).echarts) {
    if (incomeChart) incomeChart.dispose()
    incomeChart = (window as any).echarts.init(incomeEl)
    incomeChart.setOption({
      tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].name}<br/>收入：¥${p[0].value}` },
      grid: { left: 60, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: data.incomeTrend?.map((d: any) => d.month) || [] },
      yAxis: { type: 'value' },
      series: [{ name: '收入', type: 'bar', data: data.incomeTrend?.map((d: any) => d.income) || [],
        itemStyle: { color: '#67c23a' } }]
    })
  }
}

const onDashboardClose = () => {
  if (dashboardChart) { dashboardChart.dispose(); dashboardChart = null }
  if (incomeChart) { incomeChart.dispose(); incomeChart = null }
}

// ========== 地图视图 ==========
const switchToMapView = async () => {
  viewMode.value = 'map'
  await nextTick()
  initListMap()
}

const initListMap = () => {
  if (typeof AMap === 'undefined') {
    ElMessage.warning('地图加载中，请稍后重试')
    return
  }
  if (listMap) { listMap.destroy(); listMap = null }
  listMapMarkers = []

  listMap = new AMap.Map('house-list-map', { zoom: 11, resizeEnable: true })
  listMap.addControl(new AMap.Scale())
  listMap.addControl(new AMap.ToolBar({ position: 'RB' }))
  listInfoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -30), closeWhenClickMap: true })

  plotListHouses()
}

const plotListHouses = () => {
  if (!listMap) return
  if (listMapMarkers.length) { listMap.remove(listMapMarkers); listMapMarkers = [] }
  if (!houses.value.length) return

  const geocoder = new AMap.Geocoder({ city: '全国' })
  const statusColors: Record<number, string> = { 0: '#e6a23c', 1: '#67c23a', 2: '#409eff', 3: '#909399', 4: '#f56c6c' }
  const statusTexts: Record<number, string> = { 0: '待审核', 1: '可出租', 2: '已出租', 3: '已下架', 4: '驳回' }

  houses.value.forEach((house) => {
    const fullAddr = `${house.province || ''}${house.city || ''}${house.district || ''}${house.address || ''}`
    geocoder.getLocation(fullAddr, (status: string, result: any) => {
      if (status === 'complete' && result.geocodes.length > 0) {
        const lnglat = result.geocodes[0].location
        const color = statusColors[house.status] || '#409eff'
        const marker = new AMap.Marker({
          position: lnglat,
          label: {
            content: `<div style="background:${color};color:#fff;padding:2px 8px;border-radius:10px;font-size:11px;white-space:nowrap">¥${house.rentPrice}</div>`,
            offset: new AMap.Pixel(-25, -35),
            direction: 'top'
          }
        })

        marker.on('click', () => {
          const img = getFirstImage(house.images)
          const content = `
            <div style="width:260px;padding:0">
              <img src="${img}" style="width:100%;height:120px;object-fit:cover;border-radius:6px 6px 0 0" onerror="this.style.display='none'" />
              <div style="padding:10px">
                <div style="font-size:14px;font-weight:600;color:#333;margin-bottom:4px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${house.title}</div>
                <div style="font-size:12px;color:#999;margin-bottom:4px">${house.houseType} · ${house.area}㎡ · ${house.orientation || ''}</div>
                <div style="display:flex;justify-content:space-between;align-items:center">
                  <span style="font-size:16px;font-weight:bold;color:#ff6600">¥${house.rentPrice}/月</span>
                  <span style="background:${color};color:#fff;padding:2px 8px;border-radius:4px;font-size:11px">${statusTexts[house.status] || '未知'}</span>
                </div>
              </div>
            </div>`
          listInfoWindow.setContent(content)
          listInfoWindow.open(listMap, marker.getPosition())
        })

        listMap.add(marker)
        listMapMarkers.push(marker)

        if (listMapMarkers.length > 1) {
          listMap.setFitView(listMapMarkers, false, [40, 40, 40, 40])
        } else {
          listMap.setCenter(lnglat)
          listMap.setZoom(14)
        }
      }
    })
  })
}

onMounted(async () => {
  // 确保用户信息已加载
  if (!userStore.userInfo) {
    await userStore.getCurrentUser()
  }
  fetchStats()
  fetchHouses()
})

onBeforeUnmount(() => {
  if (listMap) { listMap.destroy(); listMap = null }
})
</script>

<style scoped>
/* 批量操作 */
.batch-toolbar { display: flex; align-items: center; gap: 10px; padding: 10px 16px; background: #ecf5ff; border-radius: 6px; margin-bottom: 12px; }
.batch-info { font-size: 14px; color: #409eff; margin-right: 4px; }
.select-all-row { padding: 8px 16px; border-bottom: 1px solid #f0f0f0; }
.house-item.selected { background: #f0f7ff; }
.house-checkbox { display: flex; align-items: center; padding-right: 12px; flex-shrink: 0; }
.price-tip { font-size: 12px; color: #909399; margin-top: 4px; }

/* 数据看板 */
.dashboard-drawer { padding: 0 4px; }
.db-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.db-metric { text-align: center; background: #f5f7fa; border-radius: 8px; padding: 14px 8px; }
.db-metric-val { font-size: 22px; font-weight: bold; color: #303133; }
.db-metric-label { font-size: 12px; color: #909399; margin-top: 4px; }
.db-section { margin-bottom: 20px; }
.db-section-title { font-size: 14px; font-weight: 500; color: #303133; margin-bottom: 10px; padding-left: 8px; border-left: 3px solid #409eff; }
.db-contract-info { display: flex; gap: 16px; align-items: center; font-size: 13px; color: #606266; }

.house-list-page { padding: 0; }

/* 统计卡片 */
.stats-row { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.stat-card { flex: 1; min-width: 140px; cursor: pointer; transition: all 0.3s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-content { display: flex; align-items: center; gap: 12px; }
.stat-icon { width: 50px; height: 50px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; }
.stat-icon.total { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-icon.pending { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.stat-icon.available { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.stat-icon.rented { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.stat-icon.offline { background: linear-gradient(135deg, #868f96 0%, #596164 100%); }
.stat-icon.rejected { background: linear-gradient(135deg, #ff0844 0%, #ffb199 100%); }
.stat-value { font-size: 24px; font-weight: bold; color: #303133; margin: 0; }
.stat-label { font-size: 12px; color: #909399; margin: 4px 0 0; }

/* 筛选 */
.filter-card { margin-bottom: 16px; }
.filter-form { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }

/* 房源列表 */
.table-card { margin-bottom: 16px; }
.house-list { min-height: 200px; }
.house-item { display: flex; gap: 16px; padding: 16px; border-bottom: 1px solid #f0f0f0; transition: background 0.3s; }
.house-item:hover { background: #fafafa; }
.house-item:last-child { border-bottom: none; }

.house-image { width: 200px; height: 150px; border-radius: 8px; overflow: hidden; position: relative; flex-shrink: 0; }
.house-image img { width: 100%; height: 100%; object-fit: cover; }
.house-status { position: absolute; top: 8px; left: 8px; padding: 4px 8px; border-radius: 4px; font-size: 12px; color: #fff; }
.house-status.pending { background: #e6a23c; }
.house-status.available { background: #67c23a; }
.house-status.rented { background: #409eff; }
.house-status.offline { background: #909399; }
.house-status.rejected { background: #f56c6c; }

.house-info { flex: 1; min-width: 0; }
.house-title { font-size: 16px; font-weight: 500; color: #303133; margin: 0 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.house-address { font-size: 13px; color: #606266; margin: 0 0 8px; display: flex; align-items: center; gap: 4px; }
.house-meta { display: flex; gap: 16px; font-size: 13px; color: #909399; margin-bottom: 8px; }
.house-meta span { display: flex; align-items: center; gap: 4px; }
.house-stats { display: flex; gap: 16px; font-size: 12px; color: #909399; }
.house-stats span { display: flex; align-items: center; gap: 4px; }
.create-time { margin-left: auto; }
.reject-reason { margin-top: 8px; padding: 8px 12px; background: #fef0f0; border-radius: 4px; font-size: 13px; color: #f56c6c; display: flex; align-items: center; gap: 4px; }

.house-price { text-align: right; min-width: 120px; }
.house-price .price { font-size: 24px; font-weight: bold; color: #ff4d4f; }
.house-price .unit { font-size: 14px; color: #909399; }
.house-price .deposit { font-size: 13px; color: #909399; margin: 4px 0 0; }

.house-actions { display: flex; flex-direction: column; gap: 8px; justify-content: center; min-width: 80px; }

.pagination-container { margin-top: 16px; display: flex; justify-content: flex-end; }

/* 详情对话框 */
.detail-carousel { margin-bottom: 20px; border-radius: 8px; overflow: hidden; }
.carousel-image { width: 100%; height: 100%; object-fit: contain; background: #f5f7fa; }
.detail-desc { margin-top: 16px; }
.facility-tag { margin-right: 8px; margin-bottom: 4px; }
.reject-text { color: #f56c6c; }

.page-card { margin-bottom: 16px; }

/* 视图切换 */
.view-switch {
  display: inline-flex;
  gap: 2px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
.switch-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 30px;
  cursor: pointer;
  color: #606266;
  background: #fff;
  transition: all 0.2s;
}
.switch-btn:hover { color: #409eff; }
.switch-btn.active { background: #409eff; color: #fff; }

/* 地图视图 */
.list-map-container {
  height: calc(100vh - 380px);
  min-height: 400px;
  border-radius: 8px;
  overflow: hidden;
}
.house-list-map {
  width: 100%;
  height: 100%;
}
</style>
