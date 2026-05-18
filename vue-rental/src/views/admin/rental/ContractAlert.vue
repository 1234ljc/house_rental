<template>
  <div class="contract-alert">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" @click="filter.alertType = undefined; loadList()">
        <div class="stat-icon total"><el-icon><Document /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalActive }}</div>
          <div class="stat-label">生效中合同</div>
        </div>
      </div>
      <div class="stat-card danger" @click="filter.alertType = 1; loadList()">
        <div class="stat-icon danger"><el-icon><Warning /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.expiring7Days }}</div>
          <div class="stat-label">7天内到期</div>
        </div>
      </div>
      <div class="stat-card warning" @click="filter.alertType = 2; loadList()">
        <div class="stat-icon warning"><el-icon><Bell /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.expiring30Days }}</div>
          <div class="stat-label">30天内到期</div>
        </div>
      </div>
      <div class="stat-card" @click="filter.alertType = 3; loadList()">
        <div class="stat-icon info"><el-icon><Clock /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.expiring90Days }}</div>
          <div class="stat-label">90天内到期</div>
        </div>
      </div>
      <div class="stat-card expired" @click="filter.alertType = 4; loadList()">
        <div class="stat-icon expired"><el-icon><CircleClose /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.expired }}</div>
          <div class="stat-label">已过期未处理</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon renew"><el-icon><Refresh /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.renewRate }}%</div>
          <div class="stat-label">续租率</div>
        </div>
      </div>
    </div>

    <!-- 图表和日历 -->
    <div class="charts-row">
      <el-card class="chart-card">
        <template #header>
          <span>到期趋势（未来6个月）</span>
        </template>
        <div class="chart-container" ref="trendChartRef"></div>
      </el-card>
      <el-card class="calendar-card">
        <template #header>
          <div class="calendar-header">
            <span>到期日历</span>
            <div class="calendar-nav">
              <el-button link @click="prevMonth"><el-icon><ArrowLeft /></el-icon></el-button>
              <span class="calendar-title">{{ calendarYear }}年{{ calendarMonth }}月</span>
              <el-button link @click="nextMonth"><el-icon><ArrowRight /></el-icon></el-button>
            </div>
          </div>
        </template>
        <div class="calendar-grid">
          <div class="calendar-weekdays">
            <span v-for="d in ['日', '一', '二', '三', '四', '五', '六']" :key="d">{{ d }}</span>
          </div>
          <div class="calendar-days">
            <div v-for="(day, idx) in calendarDays" :key="idx" 
              class="calendar-day" :class="{ empty: !day, today: isToday(day), hasData: calendarData[day]?.length }"
              @click="day && calendarData[day]?.length && showDayContracts(day)">
              <span class="day-num">{{ day }}</span>
              <span v-if="calendarData[day]?.length" class="day-count">{{ calendarData[day].length }}</span>
            </div>
          </div>
        </div>
        <div class="calendar-legend">
          <span><i class="dot today"></i>今天</span>
          <span><i class="dot has-data"></i>有到期合同</span>
        </div>
      </el-card>
    </div>

    <!-- 合同列表 -->
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span>到期合同列表</span>
          <div class="header-actions">
            <el-button type="warning" size="small" @click="handleBatchNotify(1)" :disabled="stats.expiring7Days === 0">
              提醒7天内到期
            </el-button>
            <el-button type="danger" size="small" @click="handleBatchMarkExpired" :disabled="stats.expired === 0">
              处理已过期合同
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-row">
        <el-select v-model="filter.alertType" placeholder="预警类型" clearable style="width: 140px" @change="loadList">
          <el-option label="7天内到期" :value="1" />
          <el-option label="30天内到期" :value="2" />
          <el-option label="90天内到期" :value="3" />
          <el-option label="已过期" :value="4" />
        </el-select>
        <el-input v-model="filter.keyword" placeholder="搜索合同号/房源/租客/房东" clearable style="width: 250px" @keyup.enter="loadList">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadList">搜索</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" style="margin-top: 15px">
        <el-table-column label="合同信息" min-width="200">
          <template #default="{ row }">
            <div class="contract-cell">
              <div class="contract-no">{{ row.contractNo }}</div>
              <div class="house-info">
                <el-icon><House /></el-icon>
                <span>{{ row.houseTitle }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="租客" width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.tenantAvatar">{{ row.tenantName?.[0] }}</el-avatar>
              <div class="user-info">
                <div class="user-name">{{ row.tenantName }}</div>
                <div class="user-phone">{{ row.tenantPhone }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="房东" width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.landlordAvatar">{{ row.landlordName?.[0] }}</el-avatar>
              <div class="user-info">
                <div class="user-name">{{ row.landlordName }}</div>
                <div class="user-phone">{{ row.landlordPhone }}</div>
              </div>
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
          <template #default="{ row }">
            <span class="rent-value">¥{{ row.monthlyRent }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleNotify(row)">发送提醒</el-button>
            <el-button v-if="row.daysRemaining < 0" type="danger" link @click="handleMarkExpired(row)">标记到期</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="filter.size"
          v-model:current-page="filter.page" @current-change="loadList" />
      </div>
    </el-card>

    <!-- 日期详情弹窗 -->
    <el-dialog v-model="dayDetailVisible" :title="`${calendarYear}年${calendarMonth}月${selectedDay}日到期合同`" width="700px">
      <el-table :data="dayContracts" size="small">
        <el-table-column label="合同号" prop="contractNo" width="150" />
        <el-table-column label="房源" prop="houseTitle" />
        <el-table-column label="租客" prop="tenantName" width="100" />
        <el-table-column label="房东" prop="landlordName" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleNotify(row)">提醒</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Warning, Bell, Clock, CircleClose, Refresh, Search, House, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getAlertStatsApi, getAlertListApi, getAlertCalendarApi, getAlertTrendApi,
  sendNotifyApi, batchNotifyApi, markExpiredApi, batchMarkExpiredApi
} from '@/api/adminContractAlert'

const stats = reactive({ totalActive: 0, expiring7Days: 0, expiring30Days: 0, expiring90Days: 0, expired: 0, renewRate: 0 })
const filter = reactive({ alertType: undefined as number | undefined, keyword: '', page: 1, size: 10 })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

const calendarYear = ref(new Date().getFullYear())
const calendarMonth = ref(new Date().getMonth() + 1)
const calendarData = ref<Record<number, any[]>>({})

const dayDetailVisible = ref(false)
const selectedDay = ref(0)
const dayContracts = ref<any[]>([])

const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const getAlertType = (level: string) => ({ expired: 'danger', danger: 'danger', warning: 'warning', info: 'info' }[level] || 'info')

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
  const today = new Date()
  return today.getFullYear() === calendarYear.value && 
         today.getMonth() + 1 === calendarMonth.value && 
         today.getDate() === day
}

const prevMonth = () => {
  if (calendarMonth.value === 1) {
    calendarYear.value--
    calendarMonth.value = 12
  } else {
    calendarMonth.value--
  }
  loadCalendar()
}

const nextMonth = () => {
  if (calendarMonth.value === 12) {
    calendarYear.value++
    calendarMonth.value = 1
  } else {
    calendarMonth.value++
  }
  loadCalendar()
}

const showDayContracts = (day: number) => {
  selectedDay.value = day
  dayContracts.value = calendarData.value[day] || []
  dayDetailVisible.value = true
}

const loadStats = async () => {
  try {
    const res: any = await getAlertStatsApi()
    Object.assign(stats, res)
  } catch (e) { console.error(e) }
}

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getAlertListApi(filter)
    list.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const loadCalendar = async () => {
  try {
    const res: any = await getAlertCalendarApi(calendarYear.value, calendarMonth.value)
    calendarData.value = res.data || {}
  } catch (e) { console.error(e) }
}

const initTrendChart = async () => {
  if (!trendChartRef.value) return
  try {
    const res: any = await getAlertTrendApi()
    const data = res || []
    
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: data.map((d: any) => d.monthLabel) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        type: 'bar',
        data: data.map((d: any) => d.count),
        itemStyle: {
          color: (params: any) => {
            const colors = ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399', '#909399']
            return colors[params.dataIndex] || '#409eff'
          },
          borderRadius: [4, 4, 0, 0]
        },
        label: { show: true, position: 'top' }
      }]
    })
  } catch (e) { console.error(e) }
}

const handleNotify = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认发送到期提醒通知给租客和房东吗？', '发送提醒')
    await sendNotifyApi(row.contractId)
    ElMessage.success('已发送到期提醒')
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

const handleBatchNotify = async (alertType: number) => {
  const typeText = alertType === 1 ? '7天内' : '30天内'
  try {
    await ElMessageBox.confirm(`确认向所有${typeText}到期的合同双方发送提醒通知吗？`, '批量提醒')
    const res: any = await batchNotifyApi(alertType)
    ElMessage.success(res || '发送成功')
    loadStats()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

const handleMarkExpired = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认将该合同标记为已到期吗？房源将恢复为可出租状态。', '标记到期', { type: 'warning' })
    await markExpiredApi(row.contractId)
    ElMessage.success('已标记为到期')
    loadList()
    loadStats()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

const handleBatchMarkExpired = async () => {
  try {
    await ElMessageBox.confirm('确认批量处理所有已过期合同吗？这些合同将被标记为已到期，对应房源将恢复为可出租状态。', '批量处理', { type: 'warning' })
    const res: any = await batchMarkExpiredApi()
    ElMessage.success(res || '处理成功')
    loadList()
    loadStats()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

const handleResize = () => { trendChart?.resize() }

onMounted(() => {
  loadStats()
  loadList()
  loadCalendar()
  initTrendChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>


<style scoped>
.contract-alert { padding: 0; }

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); }
.stat-card.danger { border-left: 3px solid #f56c6c; }
.stat-card.warning { border-left: 3px solid #e6a23c; }
.stat-card.expired { border-left: 3px solid #909399; }

.stat-icon {
  width: 45px;
  height: 45px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
}

.stat-icon.total { background: linear-gradient(135deg, #409eff, #79bbff); }
.stat-icon.danger { background: linear-gradient(135deg, #f56c6c, #fab6b6); }
.stat-icon.warning { background: linear-gradient(135deg, #e6a23c, #f7ba2a); }
.stat-icon.info { background: linear-gradient(135deg, #909399, #c0c4cc); }
.stat-icon.expired { background: linear-gradient(135deg, #606266, #909399); }
.stat-icon.renew { background: linear-gradient(135deg, #67c23a, #95d475); }

.stat-value { font-size: 24px; font-weight: bold; color: #333; }
.stat-label { font-size: 13px; color: #999; margin-top: 2px; }

/* 图表和日历 */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card, .calendar-card { border-radius: 8px; }
.chart-container { height: 280px; }

/* 日历 */
.calendar-header { display: flex; justify-content: space-between; align-items: center; }
.calendar-nav { display: flex; align-items: center; gap: 10px; }
.calendar-title { font-size: 14px; min-width: 100px; text-align: center; }

.calendar-grid { }
.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-size: 12px;
  color: #999;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 5px;
  padding: 10px 0;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.calendar-day:not(.empty):hover { background: #f5f7fa; }
.calendar-day.today { background: #e6f7ff; }
.calendar-day.today .day-num { color: #409eff; font-weight: bold; }
.calendar-day.hasData { background: #fef0f0; cursor: pointer; }
.calendar-day.hasData:hover { background: #fde2e2; }

.day-num { font-size: 14px; }
.day-count {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 8px;
  min-width: 14px;
  text-align: center;
}

.calendar-legend {
  display: flex;
  gap: 20px;
  justify-content: center;
  padding-top: 10px;
  border-top: 1px solid #eee;
  font-size: 12px;
  color: #666;
}

.calendar-legend .dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 5px;
}

.calendar-legend .dot.today { background: #e6f7ff; border: 1px solid #409eff; }
.calendar-legend .dot.has-data { background: #fef0f0; border: 1px solid #f56c6c; }

/* 列表 */
.list-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 10px; }

.filter-row { display: flex; gap: 10px; flex-wrap: wrap; }

.contract-cell { }
.contract-no { font-weight: 500; margin-bottom: 4px; }
.house-info { display: flex; align-items: center; gap: 4px; font-size: 13px; color: #666; }

.user-cell { display: flex; align-items: center; gap: 8px; }
.user-info { }
.user-name { font-weight: 500; font-size: 13px; }
.user-phone { font-size: 12px; color: #999; }

.sub-text { font-size: 12px; color: #999; }
.rent-value { font-weight: 600; color: #f56c6c; }

.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }

@media (max-width: 1400px) {
  .stats-row { grid-template-columns: repeat(3, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
}
</style>
