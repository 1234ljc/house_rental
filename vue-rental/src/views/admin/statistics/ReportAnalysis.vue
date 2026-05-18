<template>
  <div class="report-analysis">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 用户画像分析 -->
      <el-tab-pane label="用户画像分析" name="user">
        <el-row :gutter="16" class="stats-row">
          <el-col :span="6"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ userProfile.tenantCount || 0 }}</div><div class="stat-label">租客总数</div></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ userProfile.tenantVerifyRate || 0 }}%</div><div class="stat-label">租客认证率</div></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ userProfile.landlordCount || 0 }}</div><div class="stat-label">房东总数</div></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ userProfile.landlordVerifyRate || 0 }}%</div><div class="stat-label">房东认证率</div></el-card></el-col>
        </el-row>
        <el-row :gutter="16" class="stats-row">
          <el-col :span="6"><el-card shadow="hover" class="stat-card active"><div class="stat-value">{{ userProfile.activeTenants || 0 }}</div><div class="stat-label">活跃租客</div></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover" class="stat-card active"><div class="stat-value">{{ userProfile.activeLandlords || 0 }}</div><div class="stat-label">活跃房东</div></el-card></el-col>
        </el-row>
        <el-card><template #header>用户注册趋势（近12个月）</template><div ref="userRegisterRef" class="chart-container"></div></el-card>
      </el-tab-pane>

      <!-- 房源质量分析 -->
      <el-tab-pane label="房源质量分析" name="house">
        <el-row :gutter="16" class="stats-row">
          <el-col :span="6"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ houseQuality.total || 0 }}</div><div class="stat-label">房源总数</div></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover" class="stat-card rate"><div class="stat-value">{{ houseQuality.descriptionRate || 0 }}%</div><div class="stat-label">描述完整率</div></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover" class="stat-card rate"><div class="stat-value">{{ houseQuality.facilitiesRate || 0 }}%</div><div class="stat-label">设施完整率</div></el-card></el-col>
          <el-col :span="6"><el-card shadow="hover" class="stat-card rate"><div class="stat-value">{{ houseQuality.imagesRate || 0 }}%</div><div class="stat-label">图片完整率</div></el-card></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-card><template #header>价格区间分布</template><div ref="housePriceRef" class="chart-container"></div></el-card></el-col>
          <el-col :span="12"><el-card><template #header>户型分布TOP10</template><div ref="houseTypeRef" class="chart-container"></div></el-card></el-col>
        </el-row>
      </el-tab-pane>

      <!-- 租赁行为分析 -->
      <el-tab-pane label="租赁行为分析" name="rental">
        <el-card style="margin-bottom: 16px">
          <template #header>热门房源TOP10</template>
          <el-table :data="rentalBehavior.hotHouses" stripe>
            <el-table-column prop="rank" label="排名" width="60" />
            <el-table-column prop="title" label="房源标题" min-width="200" show-overflow-tooltip />
            <el-table-column prop="city" label="城市" width="100" />
            <el-table-column prop="rentPrice" label="月租金" width="100"><template #default="{ row }">¥{{ row.rentPrice }}</template></el-table-column>
            <el-table-column prop="viewCount" label="浏览量" width="100" />
            <el-table-column prop="collectCount" label="收藏数" width="100" />
          </el-table>
        </el-card>
        <el-row :gutter="16">
          <el-col :span="12"><el-card><template #header>租期偏好分布</template><div ref="rentMonthRef" class="chart-container-sm"></div></el-card></el-col>
        </el-row>
      </el-tab-pane>

      <!-- 收入趋势预测 -->
      <el-tab-pane label="收入趋势预测" name="forecast">
        <el-row :gutter="16" class="stats-row">
          <el-col :span="8"><el-card shadow="hover" class="stat-card amount"><div class="stat-value">¥{{ formatAmount(incomeForecast.avgMonthlyIncome) }}</div><div class="stat-label">月均收入</div></el-card></el-col>
          <el-col :span="8"><el-card shadow="hover" class="stat-card forecast"><div class="stat-value">¥{{ formatAmount(incomeForecast.predictedNextMonth) }}</div><div class="stat-label">下月预测收入</div></el-card></el-col>
        </el-row>
        <el-card><template #header>收入趋势与预测</template><div ref="forecastRef" class="chart-container"></div></el-card>
      </el-tab-pane>

      <!-- 数据导出 -->
      <el-tab-pane label="数据导出" name="export">
        <el-card>
          <el-form :inline="true" :model="exportForm">
            <el-form-item label="数据类型">
              <el-select v-model="exportForm.type" style="width: 150px">
                <el-option label="用户数据" value="user" />
                <el-option label="房源数据" value="house" />
                <el-option label="订单数据" value="order" />
                <el-option label="合同数据" value="contract" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间范围">
              <el-date-picker v-model="exportRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleExport" :loading="exporting">查询数据</el-button>
              <el-button type="success" @click="downloadCSV" :disabled="!exportData.records?.length">导出CSV</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        <el-card v-if="exportData.records?.length" style="margin-top: 16px">
          <template #header>
            <div class="export-header">
              <span>查询结果（共 {{ exportData.total }} 条）</span>
              <span v-if="exportData.totalAmount">总金额：¥{{ formatAmount(exportData.totalAmount) }}</span>
            </div>
          </template>
          <el-table :data="exportData.records" max-height="400" stripe>
            <el-table-column v-for="col in exportColumns" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { getUserProfile, getHouseQuality, getRentalBehavior, getIncomeForecast, exportData as exportDataApi } from '@/api/adminStatistics'

const activeTab = ref('user')
const userProfile = ref<any>({})
const houseQuality = ref<any>({})
const rentalBehavior = ref<any>({ hotHouses: [] })
const incomeForecast = ref<any>({})

const userRegisterRef = ref<HTMLElement>()
const housePriceRef = ref<HTMLElement>()
const houseTypeRef = ref<HTMLElement>()
const rentMonthRef = ref<HTMLElement>()
const forecastRef = ref<HTMLElement>()

let charts: echarts.ECharts[] = []

const exportForm = reactive({ type: 'user' })
const exportRange = ref<string[]>([])
const exporting = ref(false)
const exportData = ref<any>({})

const exportColumns = computed(() => {
  const cols: Record<string, any[]> = {
    user: [{ prop: 'userId', label: 'ID', width: 80 }, { prop: 'username', label: '用户名', width: 120 }, { prop: 'phone', label: '手机号', width: 130 }, { prop: 'userType', label: '类型', width: 80 }, { prop: 'realnameStatus', label: '认证', width: 80 }, { prop: 'createTime', label: '注册时间', width: 170 }],
    house: [{ prop: 'houseId', label: 'ID', width: 80 }, { prop: 'title', label: '标题', width: 200 }, { prop: 'city', label: '城市', width: 100 }, { prop: 'rentPrice', label: '租金', width: 100 }, { prop: 'status', label: '状态', width: 80 }, { prop: 'createTime', label: '创建时间', width: 170 }],
    order: [{ prop: 'orderId', label: 'ID', width: 80 }, { prop: 'orderNo', label: '订单号', width: 180 }, { prop: 'payAmount', label: '金额', width: 100 }, { prop: 'paymentStatus', label: '状态', width: 80 }, { prop: 'paymentTime', label: '支付时间', width: 170 }, { prop: 'createTime', label: '创建时间', width: 170 }],
    contract: [{ prop: 'contractId', label: 'ID', width: 80 }, { prop: 'contractNo', label: '合同号', width: 180 }, { prop: 'monthlyRent', label: '月租金', width: 100 }, { prop: 'status', label: '状态', width: 80 }, { prop: 'rentStartDate', label: '开始日期', width: 120 }, { prop: 'rentEndDate', label: '结束日期', width: 120 }]
  }
  return cols[exportForm.type] || []
})

const formatAmount = (v: any) => v ? Number(v).toLocaleString() : '0'

const loadUserProfile = async () => {
  const res: any = await getUserProfile()
  userProfile.value = res
  await nextTick()
  renderLineChart(userRegisterRef.value, res.registerTrend, 'count', '注册量', 'month')
}

const loadHouseQuality = async () => {
  const res: any = await getHouseQuality()
  houseQuality.value = res
  await nextTick()
  renderPieChart(housePriceRef.value, res.priceDistribution)
  renderBarChart(houseTypeRef.value, res.houseTypeDistribution)
}

const loadRentalBehavior = async () => {
  const res: any = await getRentalBehavior()
  rentalBehavior.value = res
  await nextTick()
  renderPieChart(rentMonthRef.value, res.rentMonthDistribution)
}

const loadIncomeForecast = async () => {
  const res: any = await getIncomeForecast()
  incomeForecast.value = res
  await nextTick()
  renderForecastChart(forecastRef.value, res.history, res.forecast)
}

const handleTabChange = (tab: string) => {
  nextTick(() => {
    if (tab === 'user') loadUserProfile()
    else if (tab === 'house') loadHouseQuality()
    else if (tab === 'rental') loadRentalBehavior()
    else if (tab === 'forecast') loadIncomeForecast()
  })
}

const handleExport = async () => {
  exporting.value = true
  try {
    const params: any = { type: exportForm.type }
    if (exportRange.value?.[0]) params.startDate = exportRange.value[0]
    if (exportRange.value?.[1]) params.endDate = exportRange.value[1]
    const res: any = await exportDataApi(params)
    exportData.value = res
  } catch (e) { console.error(e) }
  finally { exporting.value = false }
}

const downloadCSV = () => {
  if (!exportData.value.records?.length) return
  const cols = exportColumns.value
  const header = cols.map(c => c.label).join(',')
  const rows = exportData.value.records.map((r: any) => cols.map(c => `"${r[c.prop] || ''}"`).join(','))
  const csv = [header, ...rows].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${exportForm.type}_export_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

const renderLineChart = (el: HTMLElement | undefined, data: any[], key: string, name: string, xKey = 'date') => {
  if (!el || !data) return
  const chart = echarts.init(el)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.map(i => i[xKey]), boundaryGap: false },
    yAxis: { type: 'value' },
    series: [{ name, type: 'line', smooth: true, areaStyle: { opacity: 0.3 }, data: data.map(i => i[key]), itemStyle: { color: '#409eff' } }]
  })
}

const renderPieChart = (el: HTMLElement | undefined, data: any[]) => {
  if (!el || !data) return
  const chart = echarts.init(el)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: '5%', type: 'scroll' },
    series: [{ type: 'pie', radius: ['35%', '65%'], data: data.map(i => ({ name: i.name, value: i.value })), itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 }, label: { show: false } }]
  })
}

const renderBarChart = (el: HTMLElement | undefined, data: any[]) => {
  if (!el || !data) return
  const chart = echarts.init(el)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(i => i.name).reverse() },
    series: [{ type: 'bar', data: data.map(i => i.value).reverse(), itemStyle: { color: '#409eff' } }]
  })
}

const renderForecastChart = (el: HTMLElement | undefined, history: any[], forecast: any[]) => {
  if (!el) return
  const chart = echarts.init(el)
  charts.push(chart)
  const allData = [...(history || []), ...(forecast || [])]
  chart.setOption({
    tooltip: { trigger: 'axis', formatter: (p: any) => p.map((i: any) => `${i.seriesName}: ¥${i.value}`).join('<br/>') },
    legend: { data: ['历史收入', '预测收入'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: allData.map(i => i.month), boundaryGap: false },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: [
      { name: '历史收入', type: 'line', smooth: true, data: history?.map(i => i.amount) || [], itemStyle: { color: '#409eff' }, areaStyle: { opacity: 0.3 } },
      { name: '预测收入', type: 'line', smooth: true, data: [...Array(history?.length || 0).fill(null), ...(forecast?.map(i => i.amount) || [])], itemStyle: { color: '#67c23a' }, lineStyle: { type: 'dashed' } }
    ]
  })
}

const handleResize = () => charts.forEach(c => c.resize())

onMounted(() => {
  loadUserProfile()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
})
</script>

<style scoped>
.report-analysis { padding: 20px; }
.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 16px 0; }
.stat-value { font-size: 24px; font-weight: bold; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
.stat-card.rate .stat-value { color: #409eff; }
.stat-card.active .stat-value { color: #67c23a; }
.stat-card.amount .stat-value { color: #e6a23c; }
.stat-card.forecast .stat-value { color: #67c23a; }
.chart-container { height: 350px; }
.chart-container-sm { height: 280px; }
.export-header { display: flex; justify-content: space-between; }
</style>
