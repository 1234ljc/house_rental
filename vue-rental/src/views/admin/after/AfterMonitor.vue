<template>
  <div class="after-monitor">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon total"><el-icon><Tickets /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总工单数</div>
        </div>
      </div>
      <div class="stat-card warning" @click="filter.status = 0; loadList()">
        <div class="stat-icon pending"><el-icon><Clock /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待处理</div>
        </div>
      </div>
      <div class="stat-card" @click="filter.status = 1; loadList()">
        <div class="stat-icon processing"><el-icon><Loading /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.processing }}</div>
          <div class="stat-label">处理中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon completed"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.completeRate }}%</div>
          <div class="stat-label">完成率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon time"><el-icon><Timer /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.avgProcessHours }}h</div>
          <div class="stat-label">平均处理时长</div>
        </div>
      </div>
      <div class="stat-card danger" @click="showOvertimeList">
        <div class="stat-icon overtime"><el-icon><Warning /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.overtime }}</div>
          <div class="stat-label">超时未处理</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <el-card class="chart-card">
        <template #header>
          <span>工单趋势（近12个月）</span>
        </template>
        <div class="chart-container" ref="trendChartRef"></div>
      </el-card>
      <el-card class="chart-card small">
        <template #header>
          <span>工单类型分布</span>
        </template>
        <div class="chart-container" ref="typeChartRef"></div>
      </el-card>
    </div>

    <!-- 工单列表 -->
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span>工单列表</span>
        </div>
      </template>

      <!-- 筛选 -->
      <div class="filter-row">
        <el-select v-model="filter.manageType" placeholder="工单类型" clearable style="width: 130px" @change="loadList">
          <el-option label="维修申请" :value="0" />
          <el-option label="其他问题" :value="1" />
        </el-select>
        <el-select v-model="filter.status" placeholder="状态" clearable style="width: 120px" @change="loadList">
          <el-option label="待处理" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已完成" :value="2" />
        </el-select>
        <el-checkbox v-model="filter.overtime" @change="loadList">仅超时工单</el-checkbox>
        <el-input v-model="filter.keyword" placeholder="搜索问题描述" clearable style="width: 200px" @keyup.enter="loadList">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadList">搜索</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" style="margin-top: 15px">
        <el-table-column label="工单信息" min-width="250">
          <template #default="{ row }">
            <div class="order-cell">
              <el-tag size="small" :type="row.manageType === 0 ? 'warning' : 'info'">
                {{ row.manageTypeName }}
              </el-tag>
              <div class="order-content">{{ row.content }}</div>
              <div class="order-house" v-if="row.houseTitle">
                <el-icon><House /></el-icon>
                <span>{{ row.houseTitle }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提交人" width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.userAvatar">{{ row.userRealName?.[0] }}</el-avatar>
              <div class="user-info">
                <div class="user-name">{{ row.userRealName || row.username }}</div>
                <div class="user-phone">{{ row.userPhone }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="房东" width="160">
          <template #default="{ row }">
            <div class="user-cell" v-if="row.landlordName">
              <el-avatar :size="32" :src="row.landlordAvatar">{{ row.landlordName?.[0] }}</el-avatar>
              <div class="user-info">
                <div class="user-name">{{ row.landlordName }}</div>
                <div class="user-phone">{{ row.landlordPhone }}</div>
              </div>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <div>
              <el-tag :type="getStatusType(row.status)">{{ row.statusName }}</el-tag>
              <div class="overtime-tag" v-if="row.isOvertime">
                <el-icon color="#f56c6c"><Warning /></el-icon>
                <span>超时{{ row.waitingHours }}h</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="处理时长" width="100">
          <template #default="{ row }">
            <span v-if="row.processHours !== undefined">{{ row.processHours }}小时</span>
            <span v-else-if="row.waitingHours !== undefined" class="waiting-time">
              等待{{ row.waitingHours }}h
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="100">
          <template #default="{ row }">
            <div v-if="row.rating" class="rating-cell">
              <span>{{ row.rating }}</span>
              <el-icon color="#f7ba2a"><Star /></el-icon>
            </div>
            <span v-else class="text-muted">未评价</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            <div>{{ formatTime(row.createTime) }}</div>
            <div class="sub-text" v-if="row.completeTime">完成: {{ formatTime(row.completeTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="warning" link @click="handleUrge(row)">催促</el-button>
            <el-button v-if="row.status !== 2" type="danger" link @click="handleForceComplete(row)">强制完成</el-button>
            <el-button type="info" link @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="filter.size"
          v-model:current-page="filter.page" @current-change="loadList" />
      </div>
    </el-card>

    <!-- 房东服务排行 -->
    <el-card class="ranking-card">
      <template #header>
        <div class="card-header">
          <span>房东服务排行</span>
          <el-radio-group v-model="rankingSortBy" size="small" @change="loadRanking">
            <el-radio-button value="response">响应速度</el-radio-button>
            <el-radio-button value="complete">完成率</el-radio-button>
            <el-radio-button value="rating">满意度</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="ranking" v-loading="rankingLoading">
        <el-table-column label="排名" width="70">
          <template #default="{ $index }">
            <span class="rank-num" :class="{ top: $index < 3 }">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="房东" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :src="row.avatar">{{ row.realName?.[0] }}</el-avatar>
              <div class="user-info">
                <div class="user-name">{{ row.realName || row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="工单数" prop="totalCount" width="80" />
        <el-table-column label="完成率" width="100">
          <template #default="{ row }">
            <span :class="{ 'good-rate': row.completeRate >= 80 }">{{ row.completeRate }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="平均响应" width="100">
          <template #default="{ row }">
            <span :class="{ 'fast-time': row.avgHours <= 24 }">{{ row.avgHours }}h</span>
          </template>
        </el-table-column>
        <el-table-column label="满意度" width="100">
          <template #default="{ row }">
            <span class="score-value" v-if="row.avgRating > 0">{{ row.avgRating }}</span>
            <span v-else class="text-muted">-</span>
            <el-icon v-if="row.avgRating > 0" color="#f7ba2a"><Star /></el-icon>
          </template>
        </el-table-column>
        <el-table-column label="待处理" width="80">
          <template #default="{ row }">
            <el-badge :value="row.pendingCount" :hidden="row.pendingCount === 0" type="danger">
              <span>{{ row.pendingCount }}</span>
            </el-badge>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="650px">
      <div class="detail-content" v-if="currentOrder">
        <div class="detail-section">
          <div class="section-title">工单信息</div>
          <div class="detail-row">
            <span class="label">工单类型：</span>
            <el-tag :type="currentOrder.manageType === 0 ? 'warning' : 'info'">{{ currentOrder.manageTypeName }}</el-tag>
          </div>
          <div class="detail-row">
            <span class="label">问题描述：</span>
            <span>{{ currentOrder.content }}</span>
          </div>
          <div class="detail-row" v-if="currentOrder.images">
            <span class="label">图片：</span>
            <div class="image-list">
              <el-image v-for="(img, idx) in parseImages(currentOrder.images)" :key="idx" 
                :src="img" :preview-src-list="parseImages(currentOrder.images)" fit="cover" class="detail-image" />
            </div>
          </div>
          <div class="detail-row">
            <span class="label">状态：</span>
            <el-tag :type="getStatusType(currentOrder.status)">{{ currentOrder.statusName }}</el-tag>
            <span v-if="currentOrder.isOvertime" class="overtime-text">（已超时{{ currentOrder.waitingHours }}小时）</span>
          </div>
          <div class="detail-row" v-if="currentOrder.responseContent">
            <span class="label">处理反馈：</span>
            <span>{{ currentOrder.responseContent }}</span>
          </div>
          <div class="detail-row" v-if="currentOrder.rating">
            <span class="label">满意度评分：</span>
            <el-rate v-model="currentOrder.rating" disabled />
          </div>
        </div>
        <div class="detail-section">
          <div class="section-title">提交人</div>
          <div class="user-detail">
            <el-avatar :size="48" :src="currentOrder.userAvatar">{{ currentOrder.userRealName?.[0] }}</el-avatar>
            <div>
              <div class="user-name">{{ currentOrder.userRealName || currentOrder.username }}</div>
              <div class="user-phone">{{ currentOrder.userPhone }}</div>
            </div>
          </div>
        </div>
        <div class="detail-section" v-if="currentOrder.landlordName">
          <div class="section-title">房东</div>
          <div class="user-detail">
            <el-avatar :size="48" :src="currentOrder.landlordAvatar">{{ currentOrder.landlordName?.[0] }}</el-avatar>
            <div>
              <div class="user-name">{{ currentOrder.landlordName }}</div>
              <div class="user-phone">{{ currentOrder.landlordPhone }}</div>
            </div>
          </div>
        </div>
        <div class="detail-section" v-if="currentOrder.houseTitle">
          <div class="section-title">关联房源</div>
          <div class="detail-row">
            <span class="label">房源：</span>
            <span>{{ currentOrder.houseTitle }}</span>
          </div>
          <div class="detail-row">
            <span class="label">地址：</span>
            <span>{{ currentOrder.houseAddress }}</span>
          </div>
        </div>
        <div class="detail-section">
          <div class="section-title">时间信息</div>
          <div class="detail-row">
            <span class="label">提交时间：</span>
            <span>{{ formatTime(currentOrder.createTime) }}</span>
          </div>
          <div class="detail-row" v-if="currentOrder.completeTime">
            <span class="label">完成时间：</span>
            <span>{{ formatTime(currentOrder.completeTime) }}</span>
          </div>
          <div class="detail-row" v-if="currentOrder.processHours !== undefined">
            <span class="label">处理时长：</span>
            <span>{{ currentOrder.processHours }} 小时</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 超时工单预警弹窗 -->
    <el-dialog v-model="overtimeVisible" title="超时工单预警" width="800px">
      <el-table :data="overtimeList" v-loading="overtimeLoading">
        <el-table-column label="工单内容" min-width="200">
          <template #default="{ row }">
            <div>{{ row.content }}</div>
            <div class="sub-text">{{ row.houseTitle }}</div>
          </template>
        </el-table-column>
        <el-table-column label="房东" width="120" prop="landlordName" />
        <el-table-column label="超时时长" width="100">
          <template #default="{ row }">
            <span class="overtime-value">{{ row.overtimeHours }}小时</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="warning" link @click="handleUrge(row)">催促</el-button>
            <el-button type="danger" link @click="handleForceComplete(row)">强制完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 强制完成弹窗 -->
    <el-dialog v-model="forceCompleteVisible" title="强制完成工单" width="500px">
      <el-form>
        <el-form-item label="处理说明" required>
          <el-input v-model="forceCompleteReason" type="textarea" :rows="3" placeholder="请填写处理说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forceCompleteVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmForceComplete" :loading="forceCompleteLoading">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Tickets, Clock, Loading, CircleCheck, Timer, Warning, Search, House, Star } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getAfterStatsApi, getAfterTrendApi, getTypeDistributionApi,
  getAfterListApi, urgeAfterApi, forceCompleteApi,
  getLandlordRankingApi, getOvertimeListApi
} from '@/api/adminAfter'

const stats = reactive({ total: 0, pending: 0, processing: 0, completed: 0, completeRate: 0, avgProcessHours: 0, avgRating: 0, overtime: 0 })
const filter = reactive({ manageType: undefined as number | undefined, status: undefined as number | undefined, overtime: false, keyword: '', page: 1, size: 10 })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

const ranking = ref<any[]>([])
const rankingLoading = ref(false)
const rankingSortBy = ref('response')

const detailVisible = ref(false)
const currentOrder = ref<any>(null)

const overtimeVisible = ref(false)
const overtimeList = ref<any[]>([])
const overtimeLoading = ref(false)

const forceCompleteVisible = ref(false)
const forceCompleteReason = ref('')
const forceCompleteLoading = ref(false)
const forceCompleteTarget = ref<any>(null)

const trendChartRef = ref<HTMLElement>()
const typeChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let typeChart: echarts.ECharts | null = null

const formatTime = (time: string) => time ? new Date(time).toLocaleString('zh-CN') : ''
const getStatusType = (status: number) => ({ 0: 'warning', 1: 'primary', 2: 'success' }[status] || 'info')
const parseImages = (images: string) => {
  if (!images) return []
  try { return JSON.parse(images) } catch { return images.split(',') }
}

const loadStats = async () => {
  try {
    const res: any = await getAfterStatsApi()
    Object.assign(stats, res)
  } catch (e) { console.error(e) }
}

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getAfterListApi(filter)
    list.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const loadRanking = async () => {
  rankingLoading.value = true
  try {
    const res: any = await getLandlordRankingApi(rankingSortBy.value)
    ranking.value = res || []
  } finally { rankingLoading.value = false }
}

const showOvertimeList = async () => {
  overtimeVisible.value = true
  overtimeLoading.value = true
  try {
    const res: any = await getOvertimeListApi()
    overtimeList.value = res || []
  } finally { overtimeLoading.value = false }
}

const handleUrge = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认发送催促通知给房东吗？', '催促处理')
    await urgeAfterApi(row.manageId)
    ElMessage.success('已发送催促通知')
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

const handleForceComplete = (row: any) => {
  forceCompleteTarget.value = row
  forceCompleteReason.value = ''
  forceCompleteVisible.value = true
}

const confirmForceComplete = async () => {
  if (!forceCompleteReason.value.trim()) {
    ElMessage.warning('请填写处理说明')
    return
  }
  forceCompleteLoading.value = true
  try {
    await forceCompleteApi(forceCompleteTarget.value.manageId, forceCompleteReason.value)
    ElMessage.success('工单已强制完成')
    forceCompleteVisible.value = false
    loadList()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally { forceCompleteLoading.value = false }
}

const handleDetail = (row: any) => {
  currentOrder.value = row
  detailVisible.value = true
}

const initTrendChart = async () => {
  if (!trendChartRef.value) return
  try {
    const res: any = await getAfterTrendApi()
    const data = res || []
    
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['新增工单', '完成工单'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: data.map((d: any) => d.monthLabel) },
      yAxis: { type: 'value' },
      series: [
        { name: '新增工单', type: 'bar', data: data.map((d: any) => d.newCount), itemStyle: { color: '#409eff' } },
        { name: '完成工单', type: 'bar', data: data.map((d: any) => d.completeCount), itemStyle: { color: '#67c23a' } }
      ]
    })
  } catch (e) { console.error(e) }
}

const initTypeChart = async () => {
  if (!typeChartRef.value) return
  try {
    const res: any = await getTypeDistributionApi()
    const data = res || []
    
    typeChart = echarts.init(typeChartRef.value)
    typeChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: data.map((d: any, i: number) => ({
          name: d.name,
          value: d.value,
          itemStyle: { color: ['#e6a23c', '#909399'][i] }
        }))
      }]
    })
  } catch (e) { console.error(e) }
}

const handleResize = () => {
  trendChart?.resize()
  typeChart?.resize()
}

onMounted(() => {
  loadStats()
  loadList()
  loadRanking()
  initTrendChart()
  initTypeChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  typeChart?.dispose()
})
</script>


<style scoped>
.after-monitor { padding: 0; }

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
.stat-card.warning { border-left: 3px solid #e6a23c; }
.stat-card.danger { border-left: 3px solid #f56c6c; }

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

.stat-icon.total { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.pending { background: linear-gradient(135deg, #e6a23c, #f7ba2a); }
.stat-icon.processing { background: linear-gradient(135deg, #409eff, #79bbff); }
.stat-icon.completed { background: linear-gradient(135deg, #67c23a, #95d475); }
.stat-icon.time { background: linear-gradient(135deg, #909399, #c0c4cc); }
.stat-icon.overtime { background: linear-gradient(135deg, #f56c6c, #fab6b6); }

.stat-value { font-size: 24px; font-weight: bold; color: #333; }
.stat-label { font-size: 13px; color: #999; margin-top: 2px; }

/* 图表 */
.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card { border-radius: 8px; }
.chart-container { height: 280px; }

/* 列表 */
.list-card { border-radius: 8px; margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }

.filter-row { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }

.order-cell { }
.order-content { margin: 5px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.order-house { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #999; }

.user-cell { display: flex; align-items: center; gap: 10px; }
.user-info { }
.user-name { font-weight: 500; }
.user-phone { font-size: 12px; color: #999; }

.overtime-tag { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #f56c6c; margin-top: 4px; }
.waiting-time { color: #e6a23c; }
.rating-cell { display: flex; align-items: center; gap: 4px; }
.sub-text { font-size: 12px; color: #999; }
.text-muted { color: #999; }

.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }

/* 排行 */
.ranking-card { border-radius: 8px; }
.rank-num { font-size: 16px; font-weight: bold; color: #999; }
.rank-num.top { color: #f7ba2a; }
.score-value { font-size: 16px; font-weight: bold; color: #f7ba2a; margin-right: 4px; }
.good-rate { color: #67c23a; font-weight: 500; }
.fast-time { color: #67c23a; font-weight: 500; }

/* 详情弹窗 */
.detail-content { }
.detail-section { margin-bottom: 20px; padding-bottom: 15px; border-bottom: 1px solid #eee; }
.detail-section:last-child { border-bottom: none; }
.section-title { font-weight: 600; margin-bottom: 12px; color: #333; }
.detail-row { display: flex; margin-bottom: 8px; flex-wrap: wrap; }
.detail-row .label { color: #999; width: 100px; flex-shrink: 0; }
.user-detail { display: flex; align-items: center; gap: 12px; }
.user-detail .user-name { font-weight: 500; }
.user-detail .user-phone { font-size: 12px; color: #999; }
.image-list { display: flex; gap: 10px; flex-wrap: wrap; }
.detail-image { width: 80px; height: 80px; border-radius: 4px; }
.overtime-text { color: #f56c6c; margin-left: 10px; }
.overtime-value { color: #f56c6c; font-weight: 500; }

@media (max-width: 1400px) {
  .stats-row { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 1200px) {
  .charts-row { grid-template-columns: 1fr; }
}
</style>
