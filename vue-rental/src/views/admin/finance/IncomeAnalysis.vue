<template>
  <div class="income-analysis">
    <!-- 概览卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card total">
          <div class="stat-value">¥{{ formatAmount(overview.totalIncome) }}</div>
          <div class="stat-label">累计收入</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card month">
          <div class="stat-value">¥{{ formatAmount(overview.monthIncome) }}</div>
          <div class="stat-label">本月收入</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card today">
          <div class="stat-value">¥{{ formatAmount(overview.todayIncome) }}</div>
          <div class="stat-label">今日收入</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ overview.totalOrders || 0 }}</div>
          <div class="stat-label">累计订单</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ overview.monthOrders || 0 }}</div>
          <div class="stat-label">本月订单</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ overview.todayOrders || 0 }}</div>
          <div class="stat-label">今日订单</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 收入趋势图 -->
    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>收入趋势</span>
          <el-radio-group v-model="trendType" size="small" @change="loadTrend">
            <el-radio-button value="day">按日</el-radio-button>
            <el-radio-button value="month">按月</el-radio-button>
            <el-radio-button value="year">按年</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendChartRef" class="chart-container"></div>
    </el-card>

    <el-row :gutter="16">
      <!-- 收入构成 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header><span>收入构成</span></template>
          <div ref="compositionChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <!-- 支付方式分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header><span>支付方式分布</span></template>
          <div ref="paymentChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 导出报表 -->
    <el-card class="export-card">
      <template #header><span>财务报表导出</span></template>
      <el-form :inline="true">
        <el-form-item label="时间范围">
          <el-date-picker v-model="exportRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleExport" :loading="exporting">导出报表</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="exportData" v-if="exportData.length" max-height="300" style="margin-top: 16px">
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="houseTitle" label="房源" min-width="150" show-overflow-tooltip />
        <el-table-column prop="tenantName" label="租客" width="100" />
        <el-table-column prop="payAmount" label="金额" width="100">
          <template #default="{ row }">¥{{ row.payAmount }}</template>
        </el-table-column>
        <el-table-column prop="paymentTime" label="支付时间" width="170" />
      </el-table>
      <div v-if="exportData.length" class="export-summary">
        共 {{ exportTotal }} 条记录，总金额：<span class="amount">¥{{ formatAmount(exportAmount) }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getIncomeOverview, getIncomeTrend, getIncomeComposition, getPaymentMethodDistribution, exportFinanceReport } from '@/api/adminFinance'

const overview = ref<any>({})
const trendType = ref('day')
const trendData = ref<any[]>([])
const compositionData = ref<any[]>([])
const paymentData = ref<any[]>([])

const exportRange = ref<string[]>([])
const exporting = ref(false)
const exportData = ref<any[]>([])
const exportTotal = ref(0)
const exportAmount = ref(0)

const trendChartRef = ref<HTMLElement>()
const compositionChartRef = ref<HTMLElement>()
const paymentChartRef = ref<HTMLElement>()

let trendChart: echarts.ECharts | null = null
let compositionChart: echarts.ECharts | null = null
let paymentChart: echarts.ECharts | null = null

const formatAmount = (amount: any) => {
  if (!amount) return '0'
  return Number(amount).toLocaleString()
}

const loadOverview = async () => {
  try {
    const res: any = await getIncomeOverview()
    overview.value = res
  } catch (e) { console.error(e) }
}

const loadTrend = async () => {
  try {
    const res: any = await getIncomeTrend(trendType.value, 30)
    trendData.value = res
    renderTrendChart()
  } catch (e) { console.error(e) }
}

const loadComposition = async () => {
  try {
    const res: any = await getIncomeComposition()
    compositionData.value = res
    renderCompositionChart()
  } catch (e) { console.error(e) }
}

const loadPaymentMethod = async () => {
  try {
    const res: any = await getPaymentMethodDistribution()
    paymentData.value = res
    renderPaymentChart()
  } catch (e) { console.error(e) }
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  trendChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br/>收入: ¥{c}' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: trendData.value.map(i => i.date), boundaryGap: false },
    yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
    series: [{
      name: '收入',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.3 },
      data: trendData.value.map(i => i.amount),
      itemStyle: { color: '#409eff' }
    }]
  })
}

const renderCompositionChart = () => {
  if (!compositionChartRef.value) return
  if (!compositionChart) {
    compositionChart = echarts.init(compositionChartRef.value)
  }
  compositionChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: '5%' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: compositionData.value.map(i => ({ name: i.name, value: i.value }))
    }]
  })
}

const renderPaymentChart = () => {
  if (!paymentChartRef.value) return
  if (!paymentChart) {
    paymentChart = echarts.init(paymentChartRef.value)
  }
  paymentChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: '5%' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: paymentData.value.map(i => ({ name: i.name, value: i.value })),
      color: ['#409eff', '#67c23a', '#e6a23c']
    }]
  })
}

const handleExport = async () => {
  exporting.value = true
  try {
    const params: any = {}
    if (exportRange.value?.[0]) params.startDate = exportRange.value[0]
    if (exportRange.value?.[1]) params.endDate = exportRange.value[1]
    
    const res: any = await exportFinanceReport(params)
    exportData.value = res.records
    exportTotal.value = res.total
    exportAmount.value = res.totalAmount
    
    if (res.records.length === 0) {
      ElMessage.info('该时间段内没有数据')
    }
  } catch (e) {
    console.error(e)
  } finally {
    exporting.value = false
  }
}

const handleResize = () => {
  trendChart?.resize()
  compositionChart?.resize()
  paymentChart?.resize()
}

onMounted(async () => {
  await loadOverview()
  await nextTick()
  loadTrend()
  loadComposition()
  loadPaymentMethod()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  compositionChart?.dispose()
  paymentChart?.dispose()
})
</script>

<style scoped>
.income-analysis { padding: 20px; }
.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 16px 0; }
.stat-value { font-size: 24px; font-weight: bold; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
.stat-card.total .stat-value { color: #409eff; }
.stat-card.month .stat-value { color: #67c23a; }
.stat-card.today .stat-value { color: #e6a23c; }
.chart-card { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart-container { height: 350px; }
.export-card { margin-bottom: 16px; }
.export-summary { margin-top: 16px; text-align: right; color: #606266; }
.export-summary .amount { color: #e6a23c; font-weight: bold; font-size: 18px; }
</style>
