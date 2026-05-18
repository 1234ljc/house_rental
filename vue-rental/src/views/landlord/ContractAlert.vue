<template>
  <div class="contract-alert">
    <div class="stats-row">
      <div class="stat-card" @click="filter.alertType = undefined; loadList()">
        <div class="stat-icon total"><el-icon><Document /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.totalActive }}</div><div class="stat-label">生效中合同</div></div>
      </div>
      <div class="stat-card danger" @click="filter.alertType = 1; loadList()">
        <div class="stat-icon danger"><el-icon><Warning /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.expiring7Days }}</div><div class="stat-label">7天内到期</div></div>
      </div>
      <div class="stat-card warning" @click="filter.alertType = 2; loadList()">
        <div class="stat-icon warning"><el-icon><Bell /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.expiring30Days }}</div><div class="stat-label">30天内到期</div></div>
      </div>
      <div class="stat-card" @click="filter.alertType = 3; loadList()">
        <div class="stat-icon info"><el-icon><Clock /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.expiring90Days }}</div><div class="stat-label">90天内到期</div></div>
      </div>
      <div class="stat-card expired" @click="filter.alertType = 4; loadList()">
        <div class="stat-icon expired"><el-icon><CircleClose /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ stats.expired }}</div><div class="stat-label">已过期</div></div>
      </div>
    </div>
    <div class="charts-row">
      <el-card class="chart-card"><template #header><span>📈 到期趋势（未来6个月）</span></template><div class="chart-box" ref="chartRef"></div></el-card>
      <el-card class="calendar-card">
        <template #header>
          <div class="calendar-header">
            <span>📅 到期日历</span>
            <div class="calendar-nav">
              <el-button link @click="prevMonth"><el-icon><ArrowLeft /></el-icon></el-button>
              <span class="calendar-title">{{ calendarYear }}年{{ calendarMonth }}月</span>
              <el-button link @click="nextMonth"><el-icon><ArrowRight /></el-icon></el-button>
            </div>
          </div>
        </template>
        <div class="calendar-weekdays"><span v-for="d in ['日','一','二','三','四','五','六']" :key="d">{{ d }}</span></div>
        <div class="calendar-days">
          <div v-for="(day, idx) in calendarDays" :key="idx" class="calendar-day" 
            :class="{ empty: !day, today: isToday(day), hasData: calendarData[day]?.length }"
            @click="day && calendarData[day]?.length && showDayContracts(day)">
            <span class="day-num">{{ day }}</span>
            <span v-if="calendarData[day]?.length" class="day-count">{{ calendarData[day].length }}</span>
          </div>
        </div>
      </el-card>
    </div>
    <el-card class="list-card">
      <template #header><span>📋 到期合同列表</span></template>
      <div class="filter-row">
        <el-select v-model="filter.alertType" placeholder="预警类型" clearable style="width: 140px" @change="loadList">
          <el-option label="7天内到期" :value="1" /><el-option label="30天内到期" :value="2" />
          <el-option label="90天内到期" :value="3" /><el-option label="已过期" :value="4" />
        </el-select>
        <el-input v-model="filter.keyword" placeholder="搜索合同号/房源/租客" clearable style="width: 220px" @keyup.enter="loadList">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadList">搜索</el-button>
      </div>
      <el-table :data="list" v-loading="loading" style="margin-top: 15px">
        <el-table-column label="合同信息" min-width="180">
          <template #default="{ row }">
            <div class="contract-no">{{ row.contractNo }}</div>
            <div class="house-info"><el-icon><House /></el-icon><span>{{ row.houseTitle }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="租客" width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.tenantAvatar">{{ row.tenantName?.[0] }}</el-avatar>
              <div class="user-info"><div class="user-name">{{ row.tenantName }}</div><div class="user-phone">{{ row.tenantPhone }}</div></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="租期" width="200">
          <template #default="{ row }">
            <div>{{ row.rentStartDate }} ~ {{ row.rentEndDate }}</div>
            <div class="sub-text">共{{ row.rentMonths }}个月</div>
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="120">
          <template #default="{ row }">
            <el-tag :type="getAlertType(row.alertLevel)" size="large">
              <span v-if="row.daysRemaining < 0">已过期{{ Math.abs(row.daysRemaining) }}天</span>
              <span v-else-if="row.daysRemaining === 0">今天到期</span>
              <span v-else>{{ row.daysRemaining }}天</span>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="月租金" width="100">
          <template #default="{ row }"><span class="rent-value">¥{{ row.monthlyRent }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleNotify(row)">发送提醒</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="filter.size"
          v-model:current-page="filter.page" @current-change="loadList" />
      </div>
    </el-card>
    <el-dialog v-model="dayDialogVisible" :title="`${calendarYear}年${calendarMonth}月${selectedDay}日到期合同`" width="600px">
      <el-table :data="dayContracts" size="small">
        <el-table-column label="合同号" prop="contractNo" width="140" />
        <el-table-column label="房源" prop="houseTitle" />
        <el-table-column label="租客" prop="tenantName" width="100" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }"><el-button type="primary" link size="small" @click="handleNotify(row)">提醒</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Warning, Bell, Clock, CircleClose, Search, House, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getAlertStatsApi, getAlertListApi, getAlertCalendarApi, getAlertTrendApi, sendNotifyApi } from '@/api/landlordContractAlert'

const stats = reactive({ totalActive: 0, expiring7Days: 0, expiring30Days: 0, expiring90Days: 0, expired: 0 })
const filter = reactive({ alertType: undefined as number | undefined, keyword: '', page: 1, size: 10 })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const calendarYear = ref(new Date().getFullYear())
const calendarMonth = ref(new Date().getMonth() + 1)
const calendarData = ref<Record<number, any[]>>({})
const dayDialogVisible = ref(false)
const selectedDay = ref(0)
const dayContracts = ref<any[]>([])
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const getAlertType = (level: string): any => ({ expired: 'danger', danger: 'danger', warning: 'warning', info: 'info' }[level] || 'info')

const calendarDays = computed(() => {
  const firstDay = new Date(calendarYear.value, calendarMonth.value - 1, 1).getDay()
  const daysInMonth = new Date(calendarYear.value, calendarMonth.value, 0).getDate()
  const days: (number | null)[] = []
  for (let i = 0; i < firstDay; i++) days.push(null)
  for (let i = 1; i <= daysInMonth; i++) days.push(i)
  return days
})

const isToday = (day: number | null) => {
  if (!day) return false
  const t = new Date()
  return t.getFullYear() === calendarYear.value && t.getMonth() + 1 === calendarMonth.value && t.getDate() === day
}

const prevMonth = () => { if (calendarMonth.value === 1) { calendarYear.value--; calendarMonth.value = 12 } else { calendarMonth.value-- } loadCalendar() }
const nextMonth = () => { if (calendarMonth.value === 12) { calendarYear.value++; calendarMonth.value = 1 } else { calendarMonth.value++ } loadCalendar() }
const showDayContracts = (day: number) => { selectedDay.value = day; dayContracts.value = calendarData.value[day] || []; dayDialogVisible.value = true }

const loadStats = async () => { try { Object.assign(stats, await getAlertStatsApi()) } catch (e) { console.error(e) } }
const loadList = async () => {
  loading.value = true
  try { const res: any = await getAlertListApi(filter); list.value = res.records || []; total.value = res.total || 0 } finally { loading.value = false }
}
const loadCalendar = async () => { try { const res: any = await getAlertCalendarApi(calendarYear.value, calendarMonth.value); calendarData.value = res.data || {} } catch (e) { console.error(e) } }
const initChart = async () => {
  if (!chartRef.value) return
  try {
    const data: any = (await getAlertTrendApi()) || []
    chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: data.map((d: any) => d.monthLabel) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', data: data.map((d: any) => d.count), itemStyle: { color: '#f56c6c', borderRadius: [4, 4, 0, 0] }, label: { show: true, position: 'top' } }]
    })
  } catch (e) { console.error(e) }
}

const handleNotify = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认发送到期提醒通知给租客吗？', '发送提醒')
    await sendNotifyApi(row.contractId)
    ElMessage.success('已发送到期提醒')
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

const resize = () => { chart?.resize() }
onMounted(() => { loadStats(); loadList(); loadCalendar(); initChart(); window.addEventListener('resize', resize) })
onUnmounted(() => { window.removeEventListener('resize', resize); chart?.dispose() })
</script>

<style scoped>
.contract-alert { padding: 0; }
.stats-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 15px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 18px; display: flex; align-items: center; gap: 12px; cursor: pointer; transition: all 0.3s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-card.danger { border-left: 3px solid #f56c6c; }
.stat-card.warning { border-left: 3px solid #e6a23c; }
.stat-card.expired { border-left: 3px solid #909399; }
.stat-icon { width: 45px; height: 45px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff; }
.stat-icon.total { background: linear-gradient(135deg, #409eff, #79bbff); }
.stat-icon.danger { background: linear-gradient(135deg, #f56c6c, #fab6b6); }
.stat-icon.warning { background: linear-gradient(135deg, #e6a23c, #f7ba2a); }
.stat-icon.info { background: linear-gradient(135deg, #909399, #c0c4cc); }
.stat-icon.expired { background: linear-gradient(135deg, #606266, #909399); }
.stat-value { font-size: 24px; font-weight: bold; }
.stat-label { font-size: 13px; color: #999; margin-top: 2px; }
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px; }
.chart-card, .calendar-card, .list-card { border-radius: 8px; }
.chart-box { height: 280px; }
.calendar-header { display: flex; justify-content: space-between; align-items: center; }
.calendar-nav { display: flex; align-items: center; gap: 10px; }
.calendar-title { font-size: 14px; min-width: 100px; text-align: center; }
.calendar-weekdays { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; font-size: 12px; color: #999; padding: 10px 0; border-bottom: 1px solid #eee; }
.calendar-days { display: grid; grid-template-columns: repeat(7, 1fr); gap: 5px; padding: 10px 0; }
.calendar-day { aspect-ratio: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; border-radius: 6px; cursor: pointer; position: relative; }
.calendar-day:not(.empty):hover { background: #f5f7fa; }
.calendar-day.today { background: #e6f7ff; }
.calendar-day.today .day-num { color: #409eff; font-weight: bold; }
.calendar-day.hasData { background: #fef0f0; }
.calendar-day.hasData:hover { background: #fde2e2; }
.day-num { font-size: 14px; }
.day-count { position: absolute; top: 2px; right: 2px; background: #f56c6c; color: #fff; font-size: 10px; padding: 1px 4px; border-radius: 8px; }
.filter-row { display: flex; gap: 10px; flex-wrap: wrap; }
.contract-no { font-weight: 500; margin-bottom: 4px; }
.house-info { display: flex; align-items: center; gap: 4px; font-size: 13px; color: #666; }
.user-cell { display: flex; align-items: center; gap: 8px; }
.user-name { font-weight: 500; font-size: 13px; }
.user-phone { font-size: 12px; color: #999; }
.sub-text { font-size: 12px; color: #999; }
.rent-value { font-weight: 600; color: #f56c6c; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }
@media (max-width: 1200px) { .stats-row { grid-template-columns: repeat(3, 1fr); } .charts-row { grid-template-columns: 1fr; } }
</style>
