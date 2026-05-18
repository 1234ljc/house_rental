<template>
  <div class="statistics-page">
    <!-- 概览卡片 -->
    <div class="overview-cards">
      <div class="overview-card beans">
        <div class="card-icon">🫘</div>
        <div class="card-content">
          <div class="card-value">{{ overview.beans }}</div>
          <div class="card-label">支付豆余额</div>
          <div class="card-sub">≈ ¥{{ overview.beansValue }} 可抵扣</div>
        </div>
      </div>
      <div class="overview-card expense">
        <div class="card-icon">💰</div>
        <div class="card-content">
          <div class="card-value">¥{{ formatMoney(overview.totalPaid) }}</div>
          <div class="card-label">累计支出</div>
          <div class="card-sub">本月 ¥{{ formatMoney(overview.monthPaid) }}</div>
        </div>
      </div>
      <div class="overview-card time">
        <div class="card-icon">📅</div>
        <div class="card-content">
          <div class="card-value">{{ overview.totalRentMonths }}</div>
          <div class="card-label">累计租房(月)</div>
          <div class="card-sub">约 {{ overview.totalRentDays }} 天</div>
        </div>
      </div>
      <div class="overview-card contract">
        <div class="card-icon">📝</div>
        <div class="card-content">
          <div class="card-value">{{ overview.activeContracts }}</div>
          <div class="card-label">当前租约</div>
          <div class="card-sub">历史 {{ overview.completedContracts }} 份</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <!-- 支出趋势 -->
      <el-card class="chart-card trend-card">
        <template #header>
          <div class="card-header">
            <span>月度支出趋势</span>
            <span class="header-sub">近12个月</span>
          </div>
        </template>
        <div class="chart-container" ref="trendChartRef"></div>
      </el-card>

      <!-- 支出分类 -->
      <el-card class="chart-card category-card">
        <template #header>
          <div class="card-header">
            <span>支出分类</span>
          </div>
        </template>
        <div class="chart-container" ref="categoryChartRef"></div>
      </el-card>
    </div>

    <!-- 行为分析 -->
    <el-card class="behavior-card">
      <template #header>
        <div class="card-header">
          <span>租房行为分析</span>
        </div>
      </template>
      <div class="behavior-content">
        <div class="behavior-funnel">
          <div class="funnel-item">
            <div class="funnel-bar" :style="{ width: '100%' }">
              <span class="funnel-label">收藏房源</span>
              <span class="funnel-value">{{ behavior.totalFavorites }}</span>
            </div>
          </div>
          <div class="funnel-arrow">↓ 转化率 {{ behavior.contractRate }}%</div>
          <div class="funnel-item">
            <div class="funnel-bar success" :style="{ width: getFunnelWidth(behavior.successApplications, behavior.totalFavorites) }">
              <span class="funnel-label">成功签约</span>
              <span class="funnel-value">{{ behavior.successApplications }}</span>
            </div>
          </div>
        </div>
        <div class="behavior-stats">
          <div class="stat-item">
            <div class="stat-icon">❤️</div>
            <div class="stat-info">
              <div class="stat-value">{{ behavior.totalFavorites }}</div>
              <div class="stat-label">收藏房源</div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">✅</div>
            <div class="stat-info">
              <div class="stat-value">{{ behavior.successApplications }}</div>
              <div class="stat-label">成功签约</div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon">📝</div>
            <div class="stat-info">
              <div class="stat-value">{{ behavior.totalApplications }}</div>
              <div class="stat-label">总合同数</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 租房历史 -->
    <el-card class="history-card">
      <template #header>
        <div class="card-header">
          <span>租房历史</span>
          <div style="display:flex;gap:10px;align-items:center">
            <span class="header-sub">共 {{ rentHistory.length }} 份合同</span>
            <el-button type="primary" size="small" @click="exportData">
              <el-icon><Download /></el-icon> 导出报表
            </el-button>
          </div>
        </div>
      </template>
      <el-empty v-if="rentHistory.length === 0" description="暂无租房记录" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in rentHistory"
          :key="item.contractId"
          :type="getTimelineType(item.status)"
          :timestamp="formatDate(item.startDate) + ' ~ ' + formatDate(item.endDate)"
          placement="top"
        >
          <el-card shadow="hover" class="history-item">
            <div class="history-header">
              <span class="house-title">{{ item.houseTitle }}</span>
              <el-tag :type="getStatusType(item.status)" size="small">{{ item.statusName }}</el-tag>
            </div>
            <div class="history-info">
              <span>📍 {{ item.houseAddress }}</span>
            </div>
            <div class="history-detail">
              <span>月租: ¥{{ item.monthlyRent }}</span>
              <span>押金: ¥{{ item.depositAmount }}</span>
              <span v-if="item.rentMonths">租期: {{ item.rentMonths }}个月</span>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { Download } from '@element-plus/icons-vue'
import { getOverviewApi, getExpenseTrendApi, getExpenseCategoryApi, getRentHistoryApi, getBehaviorStatsApi } from '@/api/tenantStatistics'

const overview = reactive({
  beans: 0,
  beansValue: '0.00',
  totalPaid: 0,
  monthPaid: 0,
  totalRentDays: 0,
  totalRentMonths: 0,
  activeContracts: 0,
  completedContracts: 0,
  favoriteCount: 0
})

const behavior = reactive({
  totalFavorites: 0,
  totalApplications: 0,
  successApplications: 0,
  contractRate: 0
})

const rentHistory = ref<any[]>([])

const trendChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null

const formatMoney = (val: any) => {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toFixed(2)
}

const formatDate = (date: string) => {
  if (!date) return '-'
  return date.substring(0, 10)
}

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: '', 4: 'danger', 5: 'danger' }
  return types[status] || 'info'
}

const getTimelineType = (status: number) => {
  const types: Record<number, string> = { 2: 'success', 3: 'info', 4: 'danger', 5: 'danger' }
  return types[status] || 'primary'
}

const getFunnelWidth = (value: number, total: number) => {
  if (!total || total === 0) return '30%'
  const percent = Math.max(30, (value / total) * 100)
  return percent + '%'
}

const loadOverview = async () => {
  try {
    const res: any = await getOverviewApi()
    Object.assign(overview, res)
  } catch (e) { console.error(e) }
}

const loadBehavior = async () => {
  try {
    const res: any = await getBehaviorStatsApi()
    Object.assign(behavior, res)
  } catch (e) { console.error(e) }
}

const loadRentHistory = async () => {
  try {
    const res: any = await getRentHistoryApi()
    rentHistory.value = res || []
  } catch (e) { console.error(e) }
}

const initTrendChart = async () => {
  if (!trendChartRef.value) return
  
  try {
    const res: any = await getExpenseTrendApi()
    const data = res || []
    
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const p = params[0]
          return `${p.name}<br/>支出: ¥${Number(p.value).toFixed(2)}`
        }
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.map((d: any) => d.monthLabel),
        axisLine: { lineStyle: { color: '#ddd' } },
        axisLabel: { color: '#666' }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#eee' } },
        axisLabel: { color: '#666', formatter: '¥{value}' }
      },
      series: [{
        type: 'bar',
        data: data.map((d: any) => d.amount),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '50%'
      }]
    })
  } catch (e) { console.error(e) }
}

const initCategoryChart = async () => {
  if (!categoryChartRef.value) return
  
  try {
    const res: any = await getExpenseCategoryApi()
    const data = res || []
    
    categoryChart = echarts.init(categoryChartRef.value)
    categoryChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{b}: ¥{c} ({d}%)'
      },
      legend: {
        bottom: '5%',
        left: 'center'
      },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold',
            formatter: '{b}\n¥{c}'
          }
        },
        labelLine: { show: false },
        data: data.map((d: any, i: number) => ({
          name: d.name,
          value: d.value,
          itemStyle: { color: ['#667eea', '#f093fb'][i] }
        }))
      }]
    })
  } catch (e) { console.error(e) }
}

const handleResize = () => {
  trendChart?.resize()
  categoryChart?.resize()
}

const exportData = () => {
  const BOM = '\uFEFF'
  let csv = BOM + '租房支出报表\n\n'
  csv += '概览\n'
  csv += `累计支出,¥${formatMoney(overview.totalPaid)}\n`
  csv += `本月支出,¥${formatMoney(overview.monthPaid)}\n`
  csv += `累计租房,${overview.totalRentMonths}个月\n`
  csv += `当前租约,${overview.activeContracts}份\n\n`
  csv += '租房历史\n'
  csv += '房源,地址,月租金,押金,起租日期,到期日期,状态\n'
  rentHistory.value.forEach((item: any) => {
    csv += `"${item.houseTitle}","${item.houseAddress}",${item.monthlyRent},${item.depositAmount},${formatDate(item.startDate)},${formatDate(item.endDate)},${item.statusName}\n`
  })
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `租房支出报表_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

onMounted(() => {
  loadOverview()
  loadBehavior()
  loadRentHistory()
  initTrendChart()
  initCategoryChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  categoryChart?.dispose()
})
</script>


<style scoped>
.statistics-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}

/* 概览卡片 */
.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.overview-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.3s, box-shadow 0.3s;
}

.overview-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.card-icon {
  font-size: 40px;
}

.card-content {
  flex: 1;
}

.card-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.card-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.card-sub {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.overview-card.beans {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.overview-card.beans .card-value,
.overview-card.beans .card-label,
.overview-card.beans .card-sub {
  color: #fff;
}
.overview-card.beans .card-sub {
  opacity: 0.8;
}

/* 图表区域 */
.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.header-sub {
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

.chart-container {
  height: 300px;
}

/* 行为分析 */
.behavior-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.behavior-content {
  display: flex;
  gap: 40px;
}

.behavior-funnel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.funnel-item {
  display: flex;
  justify-content: center;
}

.funnel-bar {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 6px;
  padding: 12px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  min-width: 150px;
  transition: width 0.5s;
}

.funnel-bar.success {
  background: linear-gradient(90deg, #67c23a 0%, #95d475 100%);
}

.funnel-label {
  font-size: 14px;
}

.funnel-value {
  font-size: 18px;
  font-weight: bold;
}

.funnel-arrow {
  text-align: center;
  color: #999;
  font-size: 12px;
  padding: 5px 0;
}

.behavior-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  flex: 1;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
}

.stat-icon {
  font-size: 28px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #666;
}

/* 租房历史 */
.history-card {
  border-radius: 12px;
}

.history-item {
  margin-bottom: 0;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.house-title {
  font-weight: 600;
  font-size: 15px;
}

.history-info {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.history-detail {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #999;
}

/* 响应式 */
@media (max-width: 1200px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
  .behavior-content {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .overview-cards {
    grid-template-columns: 1fr;
  }
  .behavior-stats {
    grid-template-columns: 1fr;
  }
}
</style>
