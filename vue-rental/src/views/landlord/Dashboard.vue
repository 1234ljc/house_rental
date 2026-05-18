<template>
  <div class="dashboard" v-loading="loading">
    <!-- 第一行：统计卡片，使用 grid 平均分配宽度，避免右侧空白不齐 -->
    <div class="stat-row">
      <div class="stat-card-wrap" v-for="card in statCards" :key="card.title">
        <div class="stat-card">
          <div class="card-icon" :style="{ background: card.color }">
            <el-icon :size="24" color="#fff"><component :is="card.icon" /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">{{ card.title }}</div>
            <div class="card-value">{{ card.value }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二行：待办事项 + 收入趋势 -->
    <el-row :gutter="16" class="section-row">
      <el-col :span="12" :xs="24">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
              <div class="header-actions">
                <el-button type="primary" link @click="refreshTodos">
                  <el-icon><Refresh /></el-icon> 刷新
                </el-button>
                <el-button type="primary" link @click="todoCollapsed = !todoCollapsed">
                  <el-icon><component :is="todoCollapsed ? ArrowDown : ArrowUp" /></el-icon>
                  {{ todoCollapsed ? '展开' : '收起' }}
                </el-button>
              </div>
            </div>
          </template>
          <div class="todo-section" v-show="!todoCollapsed">
            <!-- 紧急事项 -->
            <div class="todo-group">
              <div class="todo-group-title urgent">
                <span class="dot"></span> 今天紧急事项
              </div>
              <div class="todo-list">
                <div class="todo-item" v-for="(item, index) in todos.urgent" :key="'u'+index" 
                  @click="handleTodoClick(item)" style="cursor: pointer;">
                  <span class="todo-line">├─</span>
                  <span class="todo-text">{{ item.title }}</span>
                </div>
                <div class="todo-empty" v-if="!todos.urgent?.length">暂无紧急事项</div>
              </div>
            </div>
            <!-- 普通事项 -->
            <div class="todo-group">
              <div class="todo-group-title warning">
                <span class="dot"></span> 本周待处理
              </div>
              <div class="todo-list">
                <div class="todo-item" v-for="(item, index) in todos.normal" :key="'n'+index"
                  @click="handleTodoClick(item)" style="cursor: pointer;">
                  <span class="todo-line">{{ index === todos.normal.length - 1 ? '└─' : '├─' }}</span>
                  <span class="todo-text">{{ item.title }}</span>
                </div>
                <div class="todo-empty" v-if="!todos.normal?.length">暂无待处理事项</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12" :xs="24">
        <el-card class="section-card">
          <template #header>
            <span>收入趋势图(30天)</span>
          </template>
          <div ref="incomeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行：房源收入排名 + 快捷操作 -->
    <el-row :gutter="16" class="section-row">
      <el-col :span="16" :xs="24">
        <el-card class="section-card rank-card">
          <template #header>
            <span>房源收入排名(本月Top5)</span>
          </template>
          <div class="rank-container">
            <div class="rank-list">
              <div class="rank-item" v-for="(item, index) in houseRank" :key="index">
                <div class="rank-info">
                  <span class="rank-name">{{ item.houseName }}</span>
                  <span class="rank-income">¥{{ formatMoney(item.income) }}</span>
                </div>
                <el-progress 
                  :percentage="getPercentage(item.income)" 
                  :color="rankColors[index]"
                  :stroke-width="12"
                  :show-text="false"
                />
              </div>
              <el-empty v-if="!houseRank.length" description="暂无收入数据" :image-size="80" />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8" :xs="24">
        <el-card class="section-card quick-card">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-list">
            <div 
              class="quick-item" 
              v-for="item in quickActions" 
              :key="item.path"
              @click="handleQuickAction(item)"
            >
              <el-icon :size="18" color="#1890ff"><component :is="item.icon" /></el-icon>
              <span>{{ item.name }}</span>
              <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行半：房源运营分析 -->
    <el-row :gutter="16" class="section-row">
      <el-col :span="8" :xs="24">
        <el-card class="section-card analytics-card">
          <template #header><span>出租率</span></template>
          <div class="analytics-center">
            <div ref="occupancyChartRef" class="mini-chart"></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8" :xs="24">
        <el-card class="section-card analytics-card">
          <template #header><span>流量概览</span></template>
          <div class="traffic-stats">
            <div class="traffic-item">
              <div class="traffic-value">{{ houseAnalytics.totalViews || 0 }}</div>
              <div class="traffic-label">总浏览量</div>
            </div>
            <div class="traffic-item">
              <div class="traffic-value">{{ houseAnalytics.totalCollects || 0 }}</div>
              <div class="traffic-label">总收藏量</div>
            </div>
            <div class="traffic-item">
              <div class="traffic-value">{{ houseAnalytics.contractCount || 0 }}</div>
              <div class="traffic-label">签约数</div>
            </div>
            <div class="traffic-item">
              <div class="traffic-value">{{ houseAnalytics.conversionRate || 0 }}%</div>
              <div class="traffic-label">签约转化率</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8" :xs="24">
        <el-card class="section-card analytics-card">
          <template #header><span>房源浏览排行</span></template>
          <div class="view-rank-list">
            <div class="view-rank-item" v-for="(item, index) in (houseAnalytics.houseViewRank || [])" :key="index">
              <span class="view-rank-no" :class="'top' + (index + 1)">{{ index + 1 }}</span>
              <span class="view-rank-name">{{ item.title }}</span>
              <span class="view-rank-count">{{ item.viewCount }}次</span>
            </div>
            <el-empty v-if="!(houseAnalytics.houseViewRank || []).length" description="暂无数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第四行：房源分布地图 -->
    <el-row :gutter="16" class="section-row">
      <el-col :span="24">
        <el-card class="section-card map-card">
          <template #header>
            <div class="card-header">
              <span>房源分布地图</span>
              <el-button type="primary" link @click="refreshDistMap" v-if="distMapReady">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>
          <div id="dist-map" class="dist-map-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第五行：最新消息（单独一行） -->
    <el-row :gutter="16" class="section-row">
      <el-col :span="24">
        <el-card class="section-card message-card">
          <template #header>
            <div class="card-header">
              <span>最新消息(未读)</span>
              <el-button type="primary" link @click="$router.push('/landlord/message')">
                查看全部 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <div class="message-grid">
            <div class="message-item" v-for="(msg, index) in recentMessages" :key="index">
              <span class="message-dot" :class="msg.type"></span>
              <span class="message-text">{{ msg.text }}</span>
              <span class="message-time">{{ msg.time }}</span>
            </div>
            <el-empty v-if="!recentMessages.length" description="暂无未读消息" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  Refresh, ArrowRight,
  House, HomeFilled, Calendar, Document, Money, 
  Tickets, Star, Setting, Plus, MapLocation
} from '@element-plus/icons-vue'
import {
  getLandlordStatsApi,
  getLandlordTodosApi,
  getIncomeTrendApi,
  getHouseIncomeRankApi,
  getHouseAnalyticsApi
} from '@/api/landlord'
import { getHouseListApi, type HouseInfo } from '@/api/landlordHouse'
import { getRecentMessagesApi } from '@/api/notification'
import type { LandlordStats, LandlordTodos, HouseIncomeRank, IncomeTrend } from '@/types/landlord'

declare const AMap: any

const loading = ref(true)
const router = useRouter()
const incomeChartRef = ref<HTMLElement>()
let incomeChart: echarts.ECharts | null = null
const todoCollapsed = ref(false)
const reminderCollapsed = ref(false)
const reminderExpanded = ref(false)
const ArrowUp = 'ArrowUp'
const ArrowDown = 'ArrowDown'

// 数据
const stats = ref<LandlordStats>({
  totalHouses: 0, availableHouses: 0,
  pendingContracts: 0, pendingRents: 0, monthlyReceived: 0, monthlyExpected: 0
})
const todos = ref<LandlordTodos>({ urgent: [], normal: [] })
const incomeTrend = ref<IncomeTrend[]>([])
const houseRank = ref<HouseIncomeRank[]>([])

// 最新消息（从通知API获取真实未读消息）
const recentMessages = ref<{type: string; text: string; time: string}[]>([])

// 房源运营分析
const houseAnalytics = ref<any>({})
const occupancyChartRef = ref<HTMLElement>()
let occupancyChart: echarts.ECharts | null = null
// 排行颜色
const rankColors = ['#1890ff', '#52c41a', '#faad14', '#ff4d4f', '#722ed1']

// 统计卡片配置
const statCards = computed(() => [
  { title: '房源总数', value: stats.value.totalHouses + '套', icon: 'House', color: '#1890ff' },
  { title: '可租房源', value: stats.value.availableHouses + '套', icon: 'HomeFilled', color: '#52c41a' },
  { title: '待确认合同', value: stats.value.pendingContracts + '份', icon: 'Tickets', color: '#722ed1' },
  { title: '待收取租金', value: stats.value.pendingRents + '笔', icon: 'Money', color: '#eb2f96' },
  { title: '本月已收', value: '¥' + formatMoney(stats.value.monthlyReceived), icon: 'Money', color: '#13c2c2' },
  { title: '本月预计', value: '¥' + formatMoney(stats.value.monthlyExpected), icon: 'Money', color: '#fadb14' }
])

// 快捷操作
const quickActions = [
  { name: '发布房源', icon: 'Plus', path: '/landlord/house/publish' },
  { name: '管理房源', icon: 'House', path: '/landlord/house/list' },
  { name: '合同管理', icon: 'Document', path: '/landlord/rental' },
  { name: '租后服务', icon: 'Tickets', path: '/landlord/after' }
]

// 格式化金额
function formatMoney(value: number): string {
  if (!value) return '0'
  return value.toLocaleString()
}

// 计算进度百分比
function getPercentage(income: number): number {
  if (!houseRank.value.length) return 0
  const max = Math.max(...houseRank.value.map(i => i.income))
  return max > 0 ? Math.round((income / max) * 100) : 0
}

// 加载统计数据
const loadStats = async () => {
  try {
    const data = await getLandlordStatsApi()
    stats.value = data as unknown as LandlordStats
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

// 加载待办事项
const loadTodos = async () => {
  try {
    const data = await getLandlordTodosApi()
    todos.value = data as unknown as LandlordTodos
  } catch (e) {
    console.error('加载待办事项失败', e)
  }
}

const refreshTodos = () => loadTodos()

// 加载收入趋势
const loadIncomeTrend = async () => {
  try {
    const data = await getIncomeTrendApi()
    incomeTrend.value = data as unknown as IncomeTrend[]
    renderIncomeChart()
  } catch (e) {
    console.error('加载收入趋势失败', e)
  }
}

// 加载房源收入排行
const loadHouseRank = async () => {
  try {
    const data = await getHouseIncomeRankApi()
    houseRank.value = data as unknown as HouseIncomeRank[]
  } catch (e) {
    console.error('加载房源排行失败', e)
  }
}

// 加载最新未读消息
const loadRecentMessages = async () => {
  try {
    const data: any = await getRecentMessagesApi(5)
    recentMessages.value = (data.records || []).map((n: any) => {
      const typeMap: Record<number, string> = { 1: 'info', 2: 'info', 3: 'warning', 4: 'info', 5: 'urgent', 6: 'info' }
      const now = Date.now()
      const created = new Date(n.createTime).getTime()
      const diff = now - created
      let time = ''
      if (diff < 3600000) time = Math.max(1, Math.floor(diff / 60000)) + '分钟前'
      else if (diff < 86400000) time = Math.floor(diff / 3600000) + '小时前'
      else time = Math.floor(diff / 86400000) + '天前'
      return { type: typeMap[n.notifyType] || 'info', text: n.title, time }
    })
  } catch (e) {
    console.error('加载最新消息失败', e)
  }
}

// 加载房源运营分析
const loadHouseAnalytics = async () => {
  try {
    const data: any = await getHouseAnalyticsApi()
    houseAnalytics.value = data || {}
    renderOccupancyChart()
  } catch (e) {
    console.error('加载房源运营分析失败', e)
  }
}

// 渲染出租率环形图
const renderOccupancyChart = () => {
  if (!occupancyChartRef.value) return
  if (!occupancyChart) {
    occupancyChart = echarts.init(occupancyChartRef.value)
  }
  const rate = houseAnalytics.value.occupancyRate || 0
  const option: echarts.EChartsOption = {
    series: [{
      type: 'gauge',
      startAngle: 90,
      endAngle: -270,
      pointer: { show: false },
      progress: {
        show: true,
        overlap: false,
        roundCap: true,
        clip: false,
        itemStyle: { color: rate >= 80 ? '#52c41a' : rate >= 50 ? '#faad14' : '#ff4d4f' }
      },
      axisLine: { lineStyle: { width: 18, color: [[1, '#f0f0f0']] } },
      splitLine: { show: false },
      axisTick: { show: false },
      axisLabel: { show: false },
      data: [{ value: rate, detail: { offsetCenter: ['0%', '0%'] } }],
      detail: {
        fontSize: 28,
        fontWeight: 'bold',
        formatter: '{value}%',
        color: 'inherit'
      }
    }]
  }
  occupancyChart.setOption(option)
}

// 渲染收入趋势图
const renderIncomeChart = () => {
  if (!incomeChartRef.value) return

  if (!incomeChart) {
    incomeChart = echarts.init(incomeChartRef.value)
  }

  const dates = incomeTrend.value.map(item => item.date)
  const amounts = incomeTrend.value.map(item => item.amount)

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        return `${data.name}<br/>收入：¥${(data.value || 0).toLocaleString()}`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates.length ? dates : ['暂无数据'],
      axisLabel: {
        formatter: (value: string) => value.slice(5)
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) => '¥' + value
      }
    },
    series: [{
      type: 'line',
      smooth: true,
      data: amounts.length ? amounts : [0],
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
          { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
        ])
      },
      itemStyle: { color: '#1890ff' },
      lineStyle: { width: 2 }
    }]
  }

  incomeChart.setOption(option)
}

// 快捷操作点击
const handleQuickAction = (item: { name: string; path: string }) => {
  router.push(item.path)
}

// 待办事项点击
const handleTodoClick = (item: any) => {
  if (item.type === 'renewal' || item.type === 'contract') {
    router.push('/landlord/rental')
  } else if (item.type === 'overdue') {
    router.push('/landlord/finance')
  }
}

// ========== 房源分布地图 ==========
let distMap: any = null
let distMarkers: any[] = []
let distInfoWindow: any = null
const distMapReady = ref(false)

const loadDistMap = async () => {
  if (typeof AMap === 'undefined') return
  try {
    // 获取所有房源（不分页，取较多数据）
    const res: any = await getHouseListApi({ page: 1, size: 100 })
    const houses: HouseInfo[] = res.records || []
    if (!houses.length) return

    distMap = new AMap.Map('dist-map', { zoom: 11, resizeEnable: true })
    distMap.addControl(new AMap.Scale())
    distInfoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -30), closeWhenClickMap: true })
    distMapReady.value = true

    const geocoder = new AMap.Geocoder({ city: '全国' })
    const statusColors: Record<number, string> = { 0: '#e6a23c', 1: '#67c23a', 2: '#409eff', 3: '#909399', 4: '#f56c6c' }
    const statusTexts: Record<number, string> = { 0: '待审核', 1: '可出租', 2: '已出租', 3: '已下架', 4: '驳回' }

    houses.forEach((house) => {
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
            const content = `
              <div style="width:240px;padding:10px">
                <div style="font-size:14px;font-weight:600;color:#333;margin-bottom:6px">${house.title}</div>
                <div style="font-size:12px;color:#999;margin-bottom:4px">${house.houseType} · ${house.area}㎡ · ${house.orientation || ''}</div>
                <div style="display:flex;justify-content:space-between;align-items:center">
                  <span style="font-size:16px;font-weight:bold;color:#ff6600">¥${house.rentPrice}/月</span>
                  <span style="background:${color};color:#fff;padding:2px 8px;border-radius:4px;font-size:11px">${statusTexts[house.status] || '未知'}</span>
                </div>
              </div>`
            distInfoWindow.setContent(content)
            distInfoWindow.open(distMap, marker.getPosition())
          })

          distMap.add(marker)
          distMarkers.push(marker)

          if (distMarkers.length > 1) {
            distMap.setFitView(distMarkers, false, [40, 40, 40, 40])
          } else {
            distMap.setCenter(lnglat)
            distMap.setZoom(14)
          }
        }
      })
    })
  } catch (e) {
    console.error('加载房源分布地图失败', e)
  }
}

const refreshDistMap = () => {
  if (distMap) { distMap.destroy(); distMap = null }
  distMarkers = []
  distMapReady.value = false
  loadDistMap()
}

// 窗口resize
const handleResize = () => {
  incomeChart?.resize()
  occupancyChart?.resize()
  distMap?.resize()
}

onMounted(async () => {
  loading.value = true
  await Promise.all([
    loadStats(),
    loadTodos(),
    loadIncomeTrend(),
    loadHouseRank(),
    loadRecentMessages(),
    loadHouseAnalytics()
  ])
  loading.value = false
  loadDistMap()

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  incomeChart?.dispose()
  occupancyChart?.dispose()
  if (distMap) { distMap.destroy(); distMap = null }
})
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
  margin: 0 auto;
}

/* 统计卡片行：grid 布局，统一卡片宽度 */
.stat-row {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
}

.stat-card-wrap {
  min-width: 0;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  height: 100px;
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-title {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.card-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin: 4px 0;
  white-space: nowrap;
}

/* 区块行 */
.section-row {
  margin-bottom: 16px;
}

.section-card {
  height: 320px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 待办事项 */
.todo-section {
  height: 240px;
  overflow-y: auto;
}

.todo-group {
  margin-bottom: 16px;
}

.todo-group-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.todo-group-title .dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.todo-group-title.urgent .dot { background: #ff4d4f; }
.todo-group-title.warning .dot { background: #faad14; }

.todo-list {
  padding-left: 18px;
}

.todo-item {
  padding: 6px 0;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
}

.todo-line {
  color: #c0c4cc;
  margin-right: 8px;
  font-family: monospace;
}

.todo-empty {
  color: #909399;
  font-size: 13px;
  padding: 8px 0;
}

/* 图表 */
.chart-container {
  height: 260px;
}

/* 排名卡片 */
.rank-card {
  height: 320px;
}

.rank-container {
  height: 260px;
  overflow-y: auto;
}

.rank-item {
  margin-bottom: 20px;
}

.rank-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.rank-name {
  font-size: 14px;
  color: #303133;
}

.rank-income {
  font-size: 14px;
  font-weight: bold;
  color: #ff4d4f;
}

/* 快捷操作 */
.quick-card {
  height: 320px;
}

.quick-list {
  height: 260px;
  overflow-y: auto;
}

.quick-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
  border-bottom: 1px solid #f0f0f0;
}

.quick-item:hover {
  background: #f5f7fa;
}

.quick-item span {
  flex: 1;
  margin-left: 12px;
  font-size: 14px;
  color: #303133;
}

.quick-item .arrow {
  color: #c0c4cc;
}

/* 消息卡片 */
.message-card {
  height: auto;
  min-height: 120px;
}

.message-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.message-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 12px;
}

.message-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
  transition: background 0.3s;
}

.message-item:hover {
  background: #f0f5ff;
}

.message-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 12px;
  flex-shrink: 0;
}

.message-dot.urgent { background: #ff4d4f; }
.message-dot.warning { background: #faad14; }
.message-dot.info { background: #1890ff; }

.message-text {
  flex: 1;
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-left: 16px;
  flex-shrink: 0;
}

/* 房源分布地图 */
.map-card {
  height: 420px;
}
.dist-map-container {
  height: 350px;
  border-radius: 8px;
  overflow: hidden;
}

/* 房源运营分析 */
.analytics-card {
  height: 280px;
}

.analytics-center {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 210px;
}

.mini-chart {
  width: 100%;
  height: 200px;
}

.traffic-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding: 20px 10px;
  height: 210px;
  align-content: center;
}

.traffic-item {
  text-align: center;
}

.traffic-value {
  font-size: 26px;
  font-weight: bold;
  color: #303133;
}

.traffic-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.view-rank-list {
  height: 210px;
  overflow-y: auto;
}

.view-rank-item {
  display: flex;
  align-items: center;
  padding: 8px 4px;
  border-bottom: 1px solid #f5f5f5;
}

.view-rank-no {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: #e8e8e8;
  color: #666;
  font-size: 12px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  flex-shrink: 0;
}

.view-rank-no.top1 { background: #ff4d4f; color: #fff; }
.view-rank-no.top2 { background: #ff7a45; color: #fff; }
.view-rank-no.top3 { background: #ffa940; color: #fff; }

.view-rank-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.view-rank-count {
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 8px;
}
</style>
