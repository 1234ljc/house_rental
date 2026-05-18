<template>
  <div class="business-stats">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 用户统计 -->
      <el-tab-pane label="用户统计" name="user">
        <el-row :gutter="16" class="stats-row">
          <el-col :span="4"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ userStats.total || 0 }}</div><div class="stat-label">总用户数</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card tenant"><div class="stat-value">{{ userStats.tenants || 0 }}</div><div class="stat-label">租客</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card landlord"><div class="stat-value">{{ userStats.landlords || 0 }}</div><div class="stat-label">房东</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ userStats.todayNew || 0 }}</div><div class="stat-label">今日新增</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ userStats.monthNew || 0 }}</div><div class="stat-label">本月新增</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card rate"><div class="stat-value">{{ userStats.verifyRate || 0 }}%</div><div class="stat-label">实名认证率</div></el-card></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="16"><el-card><template #header>注册趋势（近30天）</template><div ref="userTrendRef" class="chart-container"></div></el-card></el-col>
          <el-col :span="8"><el-card><template #header>用户类型分布</template><div ref="userTypeRef" class="chart-container"></div></el-card></el-col>
        </el-row>
      </el-tab-pane>

      <!-- 房源统计 -->
      <el-tab-pane label="房源统计" name="house">
        <el-row :gutter="16" class="stats-row">
          <el-col :span="4"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ houseStats.total || 0 }}</div><div class="stat-label">总房源数</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card available"><div class="stat-value">{{ houseStats.available || 0 }}</div><div class="stat-label">可出租</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card rented"><div class="stat-value">{{ houseStats.rented || 0 }}</div><div class="stat-label">已出租</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ houseStats.todayNew || 0 }}</div><div class="stat-label">今日新增</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ houseStats.monthNew || 0 }}</div><div class="stat-label">本月新增</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card rate"><div class="stat-value">{{ houseStats.passRate || 0 }}%</div><div class="stat-label">审核通过率</div></el-card></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="16"><el-card><template #header>发布趋势（近30天）</template><div ref="houseTrendRef" class="chart-container"></div></el-card></el-col>
          <el-col :span="8"><el-card><template #header>区域分布TOP10</template><div ref="houseCityRef" class="chart-container"></div></el-card></el-col>
        </el-row>
      </el-tab-pane>

      <!-- 租赁统计 -->
      <el-tab-pane label="租赁统计" name="rental">
        <el-row :gutter="16" class="stats-row">
          <el-col :span="4"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ rentalStats.totalApply || 0 }}</div><div class="stat-label">申请总数</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card pending"><div class="stat-value">{{ rentalStats.pendingApply || 0 }}</div><div class="stat-label">待处理</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card success"><div class="stat-value">{{ rentalStats.acceptedApply || 0 }}</div><div class="stat-label">已接受</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ rentalStats.activeContract || 0 }}</div><div class="stat-label">生效合同</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card rate"><div class="stat-value">{{ rentalStats.successRate || 0 }}%</div><div class="stat-label">申请成功率</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ rentalStats.avgRentMonth || 0 }}月</div><div class="stat-label">平均租期</div></el-card></el-col>
        </el-row>
        <el-card><template #header>申请趋势（近30天）</template><div ref="rentalTrendRef" class="chart-container"></div></el-card>
      </el-tab-pane>

      <!-- 财务统计 -->
      <el-tab-pane label="财务统计" name="finance">
        <el-row :gutter="16" class="stats-row">
          <el-col :span="4"><el-card shadow="hover" class="stat-card amount"><div class="stat-value">¥{{ formatAmount(financeStats.totalAmount) }}</div><div class="stat-label">总交易额</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card amount"><div class="stat-value">¥{{ formatAmount(financeStats.monthAmount) }}</div><div class="stat-label">本月交易额</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card amount"><div class="stat-value">¥{{ formatAmount(financeStats.todayAmount) }}</div><div class="stat-label">今日交易额</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card"><div class="stat-value">{{ financeStats.paidOrders || 0 }}</div><div class="stat-label">成功订单</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card rate"><div class="stat-value">{{ financeStats.successRate || 0 }}%</div><div class="stat-label">成功率</div></el-card></el-col>
          <el-col :span="4"><el-card shadow="hover" class="stat-card fail"><div class="stat-value">{{ financeStats.failRate || 0 }}%</div><div class="stat-label">失败率</div></el-card></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="16"><el-card><template #header>收入趋势（近30天）</template><div ref="financeTrendRef" class="chart-container"></div></el-card></el-col>
          <el-col :span="8"><el-card><template #header>订单类型分布</template><div ref="financeTypeRef" class="chart-container"></div></el-card></el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getUserStats, getHouseStats, getRentalStats, getFinanceStats } from '@/api/adminStatistics'

const activeTab = ref('user')
const userStats = ref<any>({})
const houseStats = ref<any>({})
const rentalStats = ref<any>({})
const financeStats = ref<any>({})

const userTrendRef = ref<HTMLElement>()
const userTypeRef = ref<HTMLElement>()
const houseTrendRef = ref<HTMLElement>()
const houseCityRef = ref<HTMLElement>()
const rentalTrendRef = ref<HTMLElement>()
const financeTrendRef = ref<HTMLElement>()
const financeTypeRef = ref<HTMLElement>()

let charts: echarts.ECharts[] = []

const formatAmount = (v: any) => v ? Number(v).toLocaleString() : '0'

const loadUserStats = async () => {
  const res: any = await getUserStats()
  userStats.value = res
  await nextTick()
  renderLineChart(userTrendRef.value, res.trend, 'count', '注册量')
  renderPieChart(userTypeRef.value, res.typeDistribution)
}

const loadHouseStats = async () => {
  const res: any = await getHouseStats()
  houseStats.value = res
  await nextTick()
  renderLineChart(houseTrendRef.value, res.trend, 'count', '发布量')
  renderBarChart(houseCityRef.value, res.cityDistribution)
}

const loadRentalStats = async () => {
  const res: any = await getRentalStats()
  rentalStats.value = res
  await nextTick()
  renderLineChart(rentalTrendRef.value, res.trend, 'count', '申请量')
}

const loadFinanceStats = async () => {
  const res: any = await getFinanceStats()
  financeStats.value = res
  await nextTick()
  renderLineChart(financeTrendRef.value, res.trend, 'amount', '交易额', true)
  renderPieChart(financeTypeRef.value, res.typeDistribution)
}

const handleTabChange = (tab: string) => {
  nextTick(() => {
    if (tab === 'user') loadUserStats()
    else if (tab === 'house') loadHouseStats()
    else if (tab === 'rental') loadRentalStats()
    else if (tab === 'finance') loadFinanceStats()
  })
}

const renderLineChart = (el: HTMLElement | undefined, data: any[], key: string, name: string, isMoney = false) => {
  if (!el || !data) return
  const chart = echarts.init(el)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: data.map(i => i.date || i.month), boundaryGap: false },
    yAxis: { type: 'value', axisLabel: isMoney ? { formatter: '¥{value}' } : {} },
    series: [{ name, type: 'line', smooth: true, areaStyle: { opacity: 0.3 }, data: data.map(i => i[key]), itemStyle: { color: '#409eff' } }]
  })
}

const renderPieChart = (el: HTMLElement | undefined, data: any[]) => {
  if (!el || !data) return
  const chart = echarts.init(el)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: '5%' },
    series: [{ type: 'pie', radius: ['40%', '70%'], data: data.map(i => ({ name: i.name, value: i.value })), itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 } }]
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
    yAxis: { type: 'category', data: data.map(i => i.city || i.name).reverse() },
    series: [{ type: 'bar', data: data.map(i => i.count || i.value).reverse(), itemStyle: { color: '#409eff' } }]
  })
}

const handleResize = () => charts.forEach(c => c.resize())

onMounted(() => {
  loadUserStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
})
</script>

<style scoped>
.business-stats { padding: 20px; }
.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 16px 0; }
.stat-value { font-size: 24px; font-weight: bold; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
.stat-card.tenant .stat-value { color: #409eff; }
.stat-card.landlord .stat-value { color: #67c23a; }
.stat-card.available .stat-value { color: #67c23a; }
.stat-card.rented .stat-value { color: #e6a23c; }
.stat-card.pending .stat-value { color: #e6a23c; }
.stat-card.success .stat-value { color: #67c23a; }
.stat-card.rate .stat-value { color: #409eff; }
.stat-card.amount .stat-value { color: #e6a23c; font-size: 20px; }
.stat-card.fail .stat-value { color: #f56c6c; }
.chart-container { height: 320px; }
</style>
