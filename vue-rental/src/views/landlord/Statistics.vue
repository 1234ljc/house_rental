<template>
  <div class="landlord-statistics">
    <div class="overview-row">
      <div class="stat-card" v-for="card in overviewCards" :key="card.label">
        <div class="stat-icon" :class="card.iconClass"><el-icon><component :is="card.icon" /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ card.prefix }}{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>
    <div class="charts-row">
      <el-card class="chart-card"><template #header><div class="card-header"><span>📈 收入趋势</span><el-button type="primary" size="small" @click="exportData"><el-icon><Download /></el-icon> 导出报表</el-button></div></template><div class="chart-box" ref="chartRef1"></div></el-card>
      <el-card class="chart-card"><template #header><span>💰 收入分布</span></template><div class="chart-box" ref="chartRef2"></div></el-card>
    </div>
    <div class="bottom-row">
      <el-card><template #header><span>🏠 房源排行</span></template>
        <el-table :data="ranking" size="small">
          <el-table-column label="#" width="50"><template #default="{$index}">{{ $index + 1 }}</template></el-table-column>
          <el-table-column label="房源" prop="title" />
          <el-table-column label="浏览" prop="viewCount" width="80" />
          <el-table-column label="收藏" prop="collectCount" width="80" />
        </el-table>
      </el-card>
      <el-card><template #header><span>⏰ 合同到期</span></template>
        <el-table :data="expList" size="small">
          <el-table-column label="房源" prop="houseTitle" />
          <el-table-column label="租客" prop="tenantName" width="100" />
          <el-table-column label="剩余" width="80"><template #default="{row}"><el-tag size="small" type="warning">{{ row.daysLeft }}天</el-tag></template></el-table-column>
        </el-table>
        <el-empty v-if="!expList.length" description="暂无" :image-size="60" />
      </el-card>
    </div>
  </div>
</template>


<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { House, Document, Money, Coin, User, Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getOverviewApi, getIncomeTrendApi, getHouseRankingApi, getIncomeDistributionApi, getExpiringContractsApi } from '@/api/landlordStatistics'

const overview = reactive({ totalHouse: 0, availableHouse: 0, rentedHouse: 0, activeContracts: 0, monthlyIncome: 0, totalIncome: 0, tenantCount: 0 })
const ranking = ref<any[]>([])
const expList = ref<any[]>([])
const chartRef1 = ref<HTMLElement>()
const chartRef2 = ref<HTMLElement>()
let c1: echarts.ECharts | null = null
let c2: echarts.ECharts | null = null

const overviewCards = computed(() => [
  { icon: House, iconClass: 'house', value: overview.totalHouse, label: '总房源', prefix: '' },
  { icon: Document, iconClass: 'contract', value: overview.activeContracts, label: '生效合同', prefix: '' },
  { icon: Money, iconClass: 'income', value: overview.monthlyIncome, label: '本月收入', prefix: '¥' },
  { icon: Coin, iconClass: 'total', value: overview.totalIncome, label: '累计收入', prefix: '¥' },
  { icon: User, iconClass: 'tenant', value: overview.tenantCount, label: '当前租客', prefix: '' }
])

const load = async () => {
  try { Object.assign(overview, await getOverviewApi()) } catch (e) { console.error(e) }
  try { ranking.value = (await getHouseRankingApi() as any) || [] } catch (e) { console.error(e) }
  try { const r: any = await getExpiringContractsApi(); expList.value = r.expiringList || [] } catch (e) { console.error(e) }
}

const initCharts = async () => {
  if (chartRef1.value) {
    const d: any = (await getIncomeTrendApi()) || []
    c1 = echarts.init(chartRef1.value)
    c1.setOption({ tooltip: { trigger: 'axis' }, xAxis: { type: 'category', data: d.map((x: any) => x.month) }, yAxis: { type: 'value' }, series: [{ type: 'bar', data: d.map((x: any) => x.income) }] })
  }
  if (chartRef2.value) {
    const d: any = (await getIncomeDistributionApi()) || []
    c2 = echarts.init(chartRef2.value)
    c2.setOption({ tooltip: { trigger: 'item' }, series: [{ type: 'pie', radius: '60%', data: d }] })
  }
}

const resize = () => { c1?.resize(); c2?.resize() }

const exportData = () => {
  const BOM = '\uFEFF'
  let csv = BOM + '房东收支报表\n\n'
  csv += '概览\n'
  csv += `总房源,${overview.totalHouse}\n`
  csv += `生效合同,${overview.activeContracts}\n`
  csv += `本月收入,¥${overview.monthlyIncome}\n`
  csv += `累计收入,¥${overview.totalIncome}\n`
  csv += `当前租客,${overview.tenantCount}\n\n`
  csv += '房源排行\n'
  csv += '房源,浏览量,收藏量\n'
  ranking.value.forEach((item: any) => {
    csv += `"${item.title}",${item.viewCount},${item.collectCount}\n`
  })
  csv += '\n即将到期合同\n'
  csv += '房源,租客,剩余天数\n'
  expList.value.forEach((item: any) => {
    csv += `"${item.houseTitle}","${item.tenantName}",${item.daysLeft}\n`
  })
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `房东收支报表_${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

onMounted(() => { load(); initCharts(); window.addEventListener('resize', resize) })
onUnmounted(() => { window.removeEventListener('resize', resize); c1?.dispose(); c2?.dispose() })
</script>


<style scoped>
.landlord-statistics { padding: 20px; background: #f5f7fa; }
.overview-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 15px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 20px; display: flex; align-items: center; gap: 15px; }
.stat-icon { width: 45px; height: 45px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; color: #fff; }
.stat-icon.house { background: #667eea; }
.stat-icon.contract { background: #4facfe; }
.stat-icon.income { background: #43e97b; }
.stat-icon.total { background: #fa709a; }
.stat-icon.tenant { background: #a8edea; color: #333; }
.stat-value { font-size: 24px; font-weight: 600; }
.stat-label { font-size: 13px; color: #909399; }
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px; }
.chart-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.chart-box { height: 280px; }
.funnel-box { padding: 20px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.funnel-bar { height: 45px; border-radius: 4px; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 500; }
.bottom-row { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
@media (max-width: 1200px) { .overview-row { grid-template-columns: repeat(3, 1fr); } .charts-row, .bottom-row { grid-template-columns: 1fr; } }
</style>
