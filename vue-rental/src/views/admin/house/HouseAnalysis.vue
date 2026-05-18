<template>
  <div class="house-analysis-page" v-loading="loading">
    <!-- 概览卡片 -->
    <el-row :gutter="16" class="overview-row">
      <el-col :span="4">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-icon" style="background: linear-gradient(135deg, #409eff, #66b1ff)">
            <el-icon :size="24"><House /></el-icon>
          </div>
          <div class="overview-info">
            <div class="overview-value">{{ overview.totalHouses }}</div>
            <div class="overview-label">房源总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-icon" style="background: linear-gradient(135deg, #67c23a, #95d475)">
            <el-icon :size="24"><Check /></el-icon>
          </div>
          <div class="overview-info">
            <div class="overview-value">{{ overview.availableHouses }}</div>
            <div class="overview-label">可出租</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-icon" style="background: linear-gradient(135deg, #e6a23c, #f5c78a)">
            <el-icon :size="24"><Key /></el-icon>
          </div>
          <div class="overview-info">
            <div class="overview-value">{{ overview.rentedHouses }}</div>
            <div class="overview-label">已出租</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-icon" style="background: linear-gradient(135deg, #f56c6c, #fab6b6)">
            <el-icon :size="24"><Plus /></el-icon>
          </div>
          <div class="overview-info">
            <div class="overview-value">{{ overview.todayNew }}</div>
            <div class="overview-label">今日新增</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-icon" style="background: linear-gradient(135deg, #909399, #c0c4cc)">
            <el-icon :size="24"><Calendar /></el-icon>
          </div>
          <div class="overview-info">
            <div class="overview-value">{{ overview.monthNew }}</div>
            <div class="overview-label">本月新增</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="overview-card">
          <div class="overview-icon" style="background: linear-gradient(135deg, #722ed1, #b37feb)">
            <el-icon :size="24"><Money /></el-icon>
          </div>
          <div class="overview-info">
            <div class="overview-value">¥{{ overview.avgPrice }}</div>
            <div class="overview-label">平均租金</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>房源发布趋势</span>
              <el-radio-group v-model="trendDays" size="small" @change="loadPublishTrend">
                <el-radio-button :value="7">7天</el-radio-button>
                <el-radio-button :value="30">30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header><span>租赁方式分布</span></template>
          <div ref="typeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header><span>城市房源分布 TOP10</span></template>
          <div ref="areaChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header><span>价格区间分布</span></template>
          <div ref="priceChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门房源排行 -->
    <el-card class="ranking-card">
      <template #header>
        <div class="card-header">
          <span>热门房源排行 TOP10</span>
          <el-button type="primary" link @click="loadHotRanking">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>
      <el-table :data="hotRanking" stripe>
        <el-table-column label="排名" width="70" align="center">
          <template #default="{ row }">
            <span class="rank-badge" :class="'rank-' + row.rank">{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column label="房源信息" min-width="300">
          <template #default="{ row }">
            <div class="house-info">
              <el-image :src="getFirstImage(row.images)" fit="cover" class="house-thumb">
                <template #error><div class="img-error"><el-icon><Picture /></el-icon></div></template>
              </el-image>
              <div class="house-detail">
                <div class="house-title">{{ row.title }}</div>
                <div class="house-city">{{ row.city }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="月租金" width="120" align="center">
          <template #default="{ row }"><span class="price">¥{{ row.rentPrice }}</span></template>
        </el-table-column>
        <el-table-column label="浏览量" width="120" align="center">
          <template #default="{ row }">
            <span class="view-count">{{ row.viewCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收藏量" width="120" align="center">
          <template #default="{ row }">{{ row.collectCount }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { House, Check, Key, Plus, Calendar, Money, Refresh, Picture } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getAnalysisOverviewApi, getAreaDistributionApi, getPriceDistributionApi,
  getHotRankingApi, getPublishTrendApi, getTypeDistributionApi
} from '@/api/adminHouse'

const loading = ref(true)
const overview = ref({ totalHouses: 0, availableHouses: 0, rentedHouses: 0, todayNew: 0, monthNew: 0, avgPrice: 0, totalViews: 0 })
const hotRanking = ref<any[]>([])
const trendDays = ref(30)

const normalizeArrayData = (data: any) => {
  if (Array.isArray(data)) return data
  if (data && typeof data === 'object') {
    return Object.entries(data).map(([name, value]) => ({ name, value }))
  }
  return []
}

const normalizeTrendData = (data: any) => {
  if (Array.isArray(data)) return data
  if (data && typeof data === 'object') {
    return Object.entries(data).map(([date, count]) => ({ date, count }))
  }
  return []
}

const trendChartRef = ref<HTMLElement>()
const typeChartRef = ref<HTMLElement>()
const areaChartRef = ref<HTMLElement>()
const priceChartRef = ref<HTMLElement>()

let trendChart: echarts.ECharts | null = null
let typeChart: echarts.ECharts | null = null
let areaChart: echarts.ECharts | null = null
let priceChart: echarts.ECharts | null = null

const getFirstImage = (images: string) => {
  try { return JSON.parse(images)[0] || '' } catch { return '' }
}

const loadOverview = async () => {
  try {
    const res: any = await getAnalysisOverviewApi()
    overview.value = {
      totalHouses: res.totalHouses ?? res.total ?? 0,
      availableHouses: res.availableHouses ?? res.available ?? 0,
      rentedHouses: res.rentedHouses ?? res.rented ?? 0,
      todayNew: res.todayNew ?? 0,
      monthNew: res.monthNew ?? 0,
      avgPrice: res.avgPrice ?? 0,
      totalViews: res.totalViews ?? 0
    }
  } catch (e) { console.error(e) }
}

const loadPublishTrend = async () => {
  try {
    const res: any = await getPublishTrendApi(trendDays.value)
    renderTrendChart(normalizeTrendData(res))
  } catch (e) { console.error(e) }
}

const loadTypeDistribution = async () => {
  try {
    const res: any = await getTypeDistributionApi()
    renderTypeChart(normalizeArrayData(res))
  } catch (e) { console.error(e) }
}

const loadAreaDistribution = async () => {
  try {
    const res: any = await getAreaDistributionApi()
    renderAreaChart(normalizeArrayData(res))
  } catch (e) { console.error(e) }
}

const loadPriceDistribution = async () => {
  try {
    const res: any = await getPriceDistributionApi()
    renderPriceChart(normalizeArrayData(res))
  } catch (e) { console.error(e) }
}

const loadHotRanking = async () => {
  try {
    const res: any = await getHotRankingApi(10)
    hotRanking.value = Array.isArray(res) ? res : []
  } catch (e) { console.error(e) }
}

const renderTrendChart = (data: any[]) => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: data.map(d => d.date?.substring(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      name: '新增房源',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      itemStyle: { color: '#409eff' },
      data: data.map(d => d.count)
    }]
  }
  trendChart.setOption(option)
}

const renderTypeChart = (data: any[]) => {
  if (!typeChartRef.value) return
  if (!typeChart) typeChart = echarts.init(typeChartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 10 },
    color: ['#409eff', '#67c23a', '#e6a23c'],
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: data
    }]
  }
  typeChart.setOption(option)
}

const renderAreaChart = (data: any[]) => {
  if (!areaChartRef.value) return
  if (!areaChart) areaChart = echarts.init(areaChartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '10%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(d => d.city).reverse() },
    series: [{
      type: 'bar',
      data: data.map(d => d.count).reverse(),
      itemStyle: { 
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#409eff' },
          { offset: 1, color: '#66b1ff' }
        ])
      },
      label: { show: true, position: 'right', formatter: '{c}套' }
    }]
  }
  areaChart.setOption(option)
}

const renderPriceChart = (data: any[]) => {
  if (!priceChartRef.value) return
  if (!priceChart) priceChart = echarts.init(priceChartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c}套 ({d}%)' },
    legend: { bottom: 10 },
    color: ['#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452'],
    series: [{
      type: 'pie',
      radius: '65%',
      center: ['50%', '45%'],
      data: data,
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
      },
      label: { formatter: '{b}\n{d}%' }
    }]
  }
  priceChart.setOption(option)
}

const handleResize = () => {
  trendChart?.resize()
  typeChart?.resize()
  areaChart?.resize()
  priceChart?.resize()
}

onMounted(async () => {
  loading.value = true
  await loadOverview()
  loading.value = false

  // 初始化图表
  trendChart = echarts.init(trendChartRef.value!)
  typeChart = echarts.init(typeChartRef.value!)
  areaChart = echarts.init(areaChartRef.value!)
  priceChart = echarts.init(priceChartRef.value!)

  await Promise.all([
    loadPublishTrend(),
    loadTypeDistribution(),
    loadAreaDistribution(),
    loadPriceDistribution(),
    loadHotRanking()
  ])

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  typeChart?.dispose()
  areaChart?.dispose()
  priceChart?.dispose()
})
</script>

<style scoped>
.house-analysis-page { padding: 0; }

.overview-row { margin-bottom: 16px; }
.overview-card { }
.overview-card :deep(.el-card__body) { display: flex; align-items: center; padding: 16px; }
.overview-icon { width: 48px; height: 48px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; margin-right: 12px; }
.overview-info { flex: 1; }
.overview-value { font-size: 24px; font-weight: bold; color: #303133; }
.overview-label { font-size: 13px; color: #909399; margin-top: 2px; }

.chart-row { margin-bottom: 16px; }
.chart-card { height: 380px; }
.chart-card .card-header { display: flex; justify-content: space-between; align-items: center; }
.chart-container { height: 300px; }

.ranking-card { }
.ranking-card .card-header { display: flex; justify-content: space-between; align-items: center; }

.rank-badge { display: inline-block; width: 24px; height: 24px; line-height: 24px; text-align: center; border-radius: 4px; font-size: 12px; font-weight: bold; background: #f0f0f0; color: #909399; }
.rank-badge.rank-1 { background: linear-gradient(135deg, #ffd700, #ffec8b); color: #8b6914; }
.rank-badge.rank-2 { background: linear-gradient(135deg, #c0c0c0, #e8e8e8); color: #666; }
.rank-badge.rank-3 { background: linear-gradient(135deg, #cd7f32, #daa06d); color: #5c3317; }

.house-info { display: flex; gap: 10px; align-items: center; }
.house-thumb { width: 60px; height: 45px; border-radius: 4px; flex-shrink: 0; }
.img-error { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f5f5; color: #ccc; }
.house-detail { flex: 1; min-width: 0; }
.house-title { font-weight: 500; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.house-city { font-size: 12px; color: #909399; }
.price { color: #f56c6c; font-weight: bold; }
.view-count { color: #409eff; font-weight: 500; }
</style>
