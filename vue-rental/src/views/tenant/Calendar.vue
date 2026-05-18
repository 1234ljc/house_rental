<template>
  <div class="tenant-calendar">
    <div class="stats-row">
      <div class="stat-card" v-for="s in statCards" :key="s.label">
        <div class="stat-icon" :style="{background: s.bg}"><el-icon><component :is="s.icon" /></el-icon></div>
        <div class="stat-info"><div class="stat-value">{{ s.value }}</div><div class="stat-label">{{ s.label }}</div></div>
      </div>
    </div>
    <div class="main-row">
      <el-card class="calendar-card">
        <template #header><span>📅 租房日历</span></template>
        <el-calendar v-model="currentDate">
          <template #date-cell="{ data }">
            <div class="calendar-cell" @click="showDayEvents(data.day)">
              <span class="day-num">{{ data.day.split('-')[2] }}</span>
              <div class="event-dots">
                <span v-for="e in getEventsForDay(data.day)" :key="e.id" class="dot" :style="{background: e.color}" :title="e.title"></span>
              </div>
            </div>
          </template>
        </el-calendar>
      </el-card>
      <div class="side-panel">
        <el-card class="reminder-card">
          <template #header>
            <div class="card-header">
              <span>⏰ 待办提醒</span>
              <el-button type="primary" link @click="reminderCollapsed = !reminderCollapsed">
                <el-icon><component :is="reminderCollapsed ? ArrowDown : ArrowUp" /></el-icon>
                {{ reminderCollapsed ? '展开' : '收起' }}
              </el-button>
            </div>
          </template>
          <div class="reminder-list" v-if="reminders.length" v-show="!reminderCollapsed">
            <div class="reminder-item" v-for="r in displayedReminders" :key="r.type + r.content" :class="r.level">
              <el-icon class="r-icon"><component :is="r.icon" /></el-icon>
              <div class="r-content"><div class="r-title">{{ r.title }}</div><div class="r-desc">{{ r.content }}</div></div>
              <el-tag size="small" :type="r.level === 'danger' ? 'danger' : (r.level === 'warning' ? 'warning' : 'info')">
                {{ r.level === 'danger' ? '紧急' : (r.level === 'warning' ? '注意' : '提醒') }}
              </el-tag>
            </div>
            <div class="tl-toggle" v-if="reminders.length > 3">
              <el-button type="primary" link @click="reminderExpanded = !reminderExpanded">
                {{ reminderExpanded ? '收起' : `查看更多 (${reminders.length - 3})` }}
                <el-icon><component :is="reminderExpanded ? ArrowUp : ArrowDown" /></el-icon>
              </el-button>
            </div>
          </div>
          <el-empty v-else-if="!reminderCollapsed" description="暂无待办" :image-size="60" />
        </el-card>
        <el-card class="timeline-card">
          <template #header><span>🏠 租房时间线</span></template>
          <el-timeline v-if="timeline.length">
            <el-timeline-item v-for="t in displayedTimeline" :key="t.id" :color="getTimelineColor(t.status)" :timestamp="t.startDate + ' ~ ' + t.endDate" placement="top">
              <div class="tl-content">
                <div class="tl-title">{{ t.houseTitle }}</div>
                <div class="tl-info">
                  <span>¥{{ t.monthlyRent }}/月</span>
                  <el-tag size="small" :type="getStatusType(t.status)">{{ t.statusText }}</el-tag>
                </div>
                <div class="tl-days" v-if="t.daysLeft !== undefined">
                  <el-tag size="small" :type="t.daysLeft <= 7 ? 'danger' : 'warning'">剩余 {{ t.daysLeft }} 天</el-tag>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <div class="tl-toggle" v-if="timeline.length > 3">
            <el-button type="primary" link @click="timelineExpanded = !timelineExpanded">
              {{ timelineExpanded ? '收起' : `查看更多 (${timeline.length - 3})` }}
              <el-icon><component :is="timelineExpanded ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
            </el-button>
          </div>
          <el-empty v-if="!timeline.length" description="暂无租房记录" :image-size="60" />
        </el-card>
      </div>
    </div>
    <el-dialog v-model="dayDialogVisible" :title="'事件详情 - ' + selectedDay" width="500px">
      <div class="day-events" v-if="selectedDayEvents.length">
        <div class="day-event" v-for="e in selectedDayEvents" :key="e.id">
          <div class="de-dot" :style="{background: e.color}"></div>
          <div class="de-info">
            <div class="de-title">{{ e.title }}</div>
            <div class="de-time" v-if="e.time">时间: {{ e.time }}</div>
            <div class="de-amount" v-if="e.amount">金额: ¥{{ e.amount }}</div>
          </div>
        </div>
      </div>
      <el-empty v-else description="当日无事件" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Document, Calendar, Money, Warning, Clock, ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import { getCalendarEventsApi, getContractTimelineApi, getRemindersApi, getCalendarStatsApi } from '@/api/tenantCalendar'

const currentDate = ref(new Date())
const events = ref<any[]>([])
const timeline = ref<any[]>([])
const reminders = ref<any[]>([])
const stats = ref({ activeContracts: 0, pendingPayments: 0, expiringContracts: 0, totalRentals: 0 })
const dayDialogVisible = ref(false)
const selectedDay = ref('')
const selectedDayEvents = ref<any[]>([])
const timelineExpanded = ref(false)
const reminderCollapsed = ref(false)
const reminderExpanded = ref(false)

const displayedTimeline = computed(() => {
  if (timelineExpanded.value) return timeline.value
  return timeline.value.slice(0, 3)
})

const displayedReminders = computed(() => {
  if (reminderExpanded.value) return reminders.value
  return reminders.value.slice(0, 3)
})

const statCards = computed(() => [
  { icon: Document, bg: '#667eea', value: stats.value.activeContracts, label: '生效合同' },
  { icon: Money, bg: '#e6a23c', value: stats.value.pendingPayments, label: '待付款' },
  { icon: Warning, bg: '#f56c6c', value: stats.value.expiringContracts, label: '即将到期' },
  { icon: Clock, bg: '#43e97b', value: stats.value.totalRentals, label: '累计租房' }
])

const getEventsForDay = (day: string) => events.value.filter(e => e.date === day)

const showDayEvents = (day: string) => {
  selectedDay.value = day
  selectedDayEvents.value = getEventsForDay(day)
  if (selectedDayEvents.value.length) dayDialogVisible.value = true
}

const getTimelineColor = (status: number) => {
  if (status === 2) return '#67c23a'
  if (status === 3) return '#909399'
  if (status === 4) return '#f56c6c'
  return '#409eff'
}

const getStatusType = (status: number): any => {
  if (status === 2) return 'success'
  if (status === 3) return 'info'
  if (status === 4) return 'danger'
  return 'warning'
}

const loadData = async () => {
  try { stats.value = await getCalendarStatsApi() as any } catch (e) { console.error(e) }
  try { events.value = (await getCalendarEventsApi() as any) || [] } catch (e) { console.error(e) }
  try { timeline.value = (await getContractTimelineApi() as any) || [] } catch (e) { console.error(e) }
  try { reminders.value = (await getRemindersApi() as any) || [] } catch (e) { console.error(e) }
}

onMounted(loadData)
</script>

<style scoped>
.tenant-calendar { padding: 20px; background: #f5f7fa; min-height: calc(100vh - 140px); }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 20px; display: flex; align-items: center; gap: 15px; }
.stat-icon { width: 45px; height: 45px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; color: #fff; }
.stat-value { font-size: 24px; font-weight: 600; }
.stat-label { font-size: 13px; color: #909399; }
.main-row { display: grid; grid-template-columns: 1fr 380px; gap: 20px; }
.calendar-card { border-radius: 8px; }
.side-panel { display: flex; flex-direction: column; gap: 15px; }
.reminder-card, .timeline-card { border-radius: 8px; }
.calendar-cell { height: 100%; display: flex; flex-direction: column; align-items: center; cursor: pointer; }
.day-num { font-size: 14px; }
.event-dots { display: flex; gap: 3px; margin-top: 4px; flex-wrap: wrap; justify-content: center; }
.dot { width: 6px; height: 6px; border-radius: 50%; }
.reminder-list { display: flex; flex-direction: column; gap: 12px; }
.reminder-item { display: flex; align-items: center; gap: 12px; padding: 12px; background: #f5f7fa; border-radius: 6px; border-left: 3px solid #909399; }
.reminder-item.danger { border-left-color: #f56c6c; background: #fef0f0; }
.reminder-item.warning { border-left-color: #e6a23c; background: #fdf6ec; }
.reminder-item.info { border-left-color: #409eff; background: #ecf5ff; }
.r-icon { font-size: 20px; color: #606266; }
.r-content { flex: 1; }
.r-title { font-weight: 500; font-size: 14px; }
.r-desc { font-size: 12px; color: #909399; margin-top: 2px; }
.tl-content { padding: 8px 0; }
.tl-title { font-weight: 500; margin-bottom: 6px; }
.tl-info { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #606266; }
.tl-days { margin-top: 6px; }
.tl-toggle { text-align: center; padding: 8px 0 4px; }
.day-events { display: flex; flex-direction: column; gap: 12px; }
.day-event { display: flex; align-items: flex-start; gap: 12px; padding: 12px; background: #f5f7fa; border-radius: 6px; }
.de-dot { width: 10px; height: 10px; border-radius: 50%; margin-top: 4px; }
.de-info { flex: 1; }
.de-title { font-weight: 500; }
.de-time, .de-amount { font-size: 13px; color: #909399; margin-top: 4px; }
@media (max-width: 1200px) { .stats-row { grid-template-columns: repeat(3, 1fr); } .main-row { grid-template-columns: 1fr; } }
</style>
