<template>
  <div class="dashboard" v-loading="loading">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card-wrap" v-for="card in statCards" :key="card.title">
        <div class="stat-card">
          <div class="card-icon" :style="{ background: card.color }">
            <el-icon :size="24" color="#fff"><component :is="card.icon" /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-title">{{ card.title }}</div>
            <div class="card-value">{{ card.value }}</div>
            <div class="card-change" :class="card.changeType">
              <span v-if="card.changeType === 'up'">↑</span>
              <span v-else-if="card.changeType === 'down'">↓</span>
              {{ card.change }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 监控面板 + 趋势图 -->
    <el-row :gutter="16" class="section-row">
      <el-col :span="12" :xs="24">
        <el-card class="monitor-card">
          <template #header>
            <div class="card-header">
              <span>实时监控面板</span>
              <el-button type="primary" link @click="refreshMonitor">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>
          <div class="monitor-list">
            <div class="monitor-item">
              <span class="label">在线用户：</span>
              <span class="value">{{ monitorData.onlineUsers }}</span>
            </div>
            <div class="monitor-item">
              <span class="label">今日新增房源：</span>
              <span class="value success">+{{ monitorData.todayNewHouses }}</span>
            </div>
            <div class="monitor-item">
              <span class="label">待审核房源：</span>
              <span class="value warning">{{ stats.pendingHouseCount }}</span>
            </div>
            <div class="monitor-item">
              <span class="label">今日交易额：</span>
              <span class="value">¥{{ (stats.todayAmount || 0).toLocaleString() }}</span>
            </div>
          </div>
          <div class="monitor-date">{{ currentDate }}</div>
        </el-card>
      </el-col>

      <el-col :span="12" :xs="24">
        <el-card class="chart-card">
          <template #header>
            <span>近7天用户增长趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门城市 + 待办事项 -->
    <el-row :gutter="16" class="section-row">
      <el-col :span="12" :xs="24">
        <el-card class="city-card">
          <template #header>
            <span>热门城市TOP10</span>
          </template>
          <div class="city-list">
            <div class="city-item" v-for="(city, index) in hotCities" :key="city.city">
              <span class="city-rank">{{ index + 1 }}</span>
              <span class="city-name">{{ city.city }}</span>
              <span class="city-count">{{ city.count }}套</span>
            </div>
            <el-empty v-if="!hotCities.length" description="暂无数据" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="12" :xs="24">
        <el-card class="todo-card">
          <template #header>
            <div class="card-header">
              <span>待处理消息</span>
              <el-button type="primary" link @click="refreshTodos">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>
          <div class="todo-list">
            <div
              class="todo-item"
              v-for="(todo, index) in todos"
              :key="index"
              :class="todo.type"
            >
              <span class="todo-icon">
                {{ todo.type === 'urgent' ? '🔴' : todo.type === 'important' ? '🟡' : '🔵' }}
              </span>
              <span class="todo-content">{{ todo.content }}</span>
              <span class="todo-time">{{ todo.time }}</span>
            </div>
            <el-empty v-if="!todos.length" description="暂无待处理消息" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Refresh, User, House, Money, Document, DataAnalysis } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getDashboardStatsApi,
  getMonitorDataApi,
  getTrendsApi,
  getHotCitiesApi,
  getTodosApi
} from '@/api/admin'
import type { DashboardStats, MonitorData, TodoItem, HotCity, TrendData } from '@/types/admin'
import dayjs from 'dayjs'

const loading = ref(true)
const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let refreshTimer: number | null = null

// 数据
const stats = ref<DashboardStats>({
  tenantCount: 0, landlordCount: 0, adminCount: 0, totalUserCount: 0,
  totalHouseCount: 0, pendingHouseCount: 0, availableHouseCount: 0, rentedHouseCount: 0,
  todayNewUsers: 0, todayNewHouses: 0, todayOrders: 0, todayAmount: 0, totalAmount: 0
})
const monitorData = ref<MonitorData>({
  onlineUsers: 0, todayNewHouses: 0, alertCount: 0, cpuUsage: 0, memoryUsage: 0
})
const trends = ref<TrendData>({ userTrend: [], houseTrend: [], amountTrend: [] })
const hotCities = ref<HotCity[]>([])
const todos = ref<TodoItem[]>([])

// 当前日期
const currentDate = computed(() => dayjs().format('YYYY-MM-DD'))

// 统计卡片配置
const statCards = computed(() => [
  { title: '注册用户总数', value: stats.value.totalUserCount, icon: 'User', color: '#1890ff', change: '较昨日', changeType: 'up' },
  { title: '房源总数', value: stats.value.totalHouseCount, icon: 'House', color: '#52c41a', change: '较昨日', changeType: 'up' },
  { title: '今日新增用户', value: stats.value.todayNewUsers, icon: 'UserFilled', color: '#faad14', change: '+' + stats.value.todayNewUsers, changeType: 'up' },
  { title: '今日订单数', value: stats.value.todayOrders, icon: 'Document', color: '#ff4d4f', change: stats.value.todayOrders + '笔', changeType: 'normal' },
  { title: '平台总交易额', value: '¥' + (stats.value.totalAmount || 0).toLocaleString(), icon: 'Money', color: '#722ed1', change: '累计', changeType: 'normal' }
])

// 加载统计数据
const loadStats = async () => {
  try {
    const data = await getDashboardStatsApi()
    stats.value = data as unknown as DashboardStats
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

// 加载监控数据
const loadMonitor = async () => {
  try {
    const data = await getMonitorDataApi()
    monitorData.value = data as unknown as MonitorData
  } catch (e) {
    console.error('加载监控数据失败', e)
  }
}

// 刷新监控
const refreshMonitor = () => {
  loadMonitor()
}

// 加载趋势数据
const loadTrends = async () => {
  try {
    const data = await getTrendsApi()
    trends.value = data as unknown as TrendData
    renderTrendChart()
  } catch (e) {
    console.error('加载趋势数据失败', e)
  }
}

// 加载热门城市
const loadHotCities = async () => {
  try {
    const data = await getHotCitiesApi()
    hotCities.value = data as unknown as HotCity[]
  } catch (e) {
    console.error('加载热门城市失败', e)
  }
}

// 加载待办事项
const loadTodos = async () => {
  try {
    const data = await getTodosApi()
    todos.value = data as unknown as TodoItem[]
  } catch (e) {
    console.error('加载待办事项失败', e)
  }
}

// 刷新待办
const refreshTodos = () => {
  loadTodos()
}

// 渲染趋势图
const renderTrendChart = () => {
  if (!trendChartRef.value) return

  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const userDates = trends.value.userTrend.map(item => item.date)
  const userCounts = trends.value.userTrend.map(item => item.count)
  const houseCounts = trends.value.houseTrend.map(item => item.count)

  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['用户', '房源'], top: 10 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: userDates },
    yAxis: { type: 'value' },
    series: [
      {
        name: '用户',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.3 },
        itemStyle: { color: '#1890ff' },
        data: userCounts
      },
      {
        name: '房源',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.3 },
        itemStyle: { color: '#52c41a' },
        data: houseCounts
      }
    ]
  }

  trendChart.setOption(option)
}

// 窗口resize处理
const handleResize = () => {
  trendChart?.resize()
}

onMounted(async () => {
  loading.value = true
  await Promise.all([
    loadStats(),
    loadMonitor(),
    loadTrends(),
    loadHotCities(),
    loadTodos()
  ])
  loading.value = false

  window.addEventListener('resize', handleResize)

  // 30秒自动刷新监控数据
  refreshTimer = window.setInterval(() => {
    loadMonitor()
    loadStats()
  }, 30000)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.dashboard {
  min-height: 100%;
}

.stat-cards {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.stat-card-wrap {
  min-width: 0;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  height: 120px;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.card-info {
  flex: 1;
}

.card-title {
  font-size: 14px;
  color: #606266;
}

.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin: 4px 0;
}

.card-change {
  font-size: 12px;
  color: #909399;
}

.card-change.up {
  color: #52c41a;
}

.card-change.down {
  color: #ff4d4f;
}

.section-row {
  margin-bottom: 16px;
}

.monitor-card,
.chart-card,
.city-card,
.todo-card {
  height: 350px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.monitor-list {
  padding: 10px 0;
}

.monitor-item {
  height: 60px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
}

.monitor-item .label {
  font-size: 14px;
  color: #606266;
}

.monitor-item .value {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-left: 8px;
}

.monitor-item .value.warning {
  color: #ff4d4f;
}

.monitor-item .value.success {
  color: #52c41a;
}

.monitor-date {
  text-align: right;
  font-size: 14px;
  color: #909399;
  margin-top: 16px;
}

.chart-container {
  height: 280px;
}

.city-list {
  max-height: 280px;
  overflow-y: auto;
}

.city-item {
  height: 40px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  transition: background 0.3s;
}

.city-item:hover {
  background: #f5f7fa;
}

.city-rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #1890ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  margin-right: 12px;
}

.city-name {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.city-count {
  font-size: 14px;
  color: #303133;
}

.todo-list {
  max-height: 280px;
  overflow-y: auto;
}

.todo-item {
  padding: 12px;
  display: flex;
  align-items: center;
  border-radius: 4px;
  margin-bottom: 8px;
}

.todo-item.urgent {
  background: #fef0f0;
}

.todo-item.important {
  background: #fff7e6;
}

.todo-item.normal {
  background: #e6f7ff;
}

.todo-icon {
  margin-right: 8px;
  font-size: 16px;
  flex-shrink: 0;
}

.todo-content {
  font-size: 14px;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-time {
  font-size: 12px;
  color: #909399;
  margin-left: 12px;
  flex-shrink: 0;
}
</style>
