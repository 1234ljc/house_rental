<template>
  <div class="finance-page">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card pending">
        <div class="stat-num">{{ stats.pendingCount }}</div>
        <div class="stat-label">待收租金</div>
      </div>
      <div class="stat-card danger">
        <div class="stat-num">{{ stats.overdueCount }}</div>
        <div class="stat-label">逾期租金</div>
      </div>
      <div class="stat-card success">
        <div class="stat-num">¥{{ formatMoney(stats.monthlyReceived) }}</div>
        <div class="stat-label">本月已收</div>
      </div>
      <div class="stat-card info">
        <div class="stat-num">¥{{ formatMoney(stats.monthlyExpected) }}</div>
        <div class="stat-label">本月预计</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-num">{{ stats.pendingRefund }}</div>
        <div class="stat-label">待处理退押金</div>
      </div>
    </div>

    <!-- Tab切换 -->
    <el-tabs v-model="activeTab" class="main-tabs">
      <el-tab-pane label="租金收取" name="rent" />
      <el-tab-pane label="押金管理" name="deposit" />
      <el-tab-pane label="收入报表" name="report" />
    </el-tabs>

    <!-- 租金收取 -->
    <div v-show="activeTab === 'rent'" class="tab-content">
      <el-card class="filter-card">
        <div class="filter-row">
          <span class="filter-label">状态：</span>
          <el-radio-group v-model="rentFilter.status" @change="loadRentList">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">待支付</el-radio-button>
            <el-radio-button :value="1">已支付</el-radio-button>
            <el-radio-button :value="2">逾期</el-radio-button>
          </el-radio-group>
        </div>
      </el-card>

      <div class="order-list" v-loading="rentLoading">
        <el-empty v-if="!rentLoading && rentList.length === 0" description="暂无租金记录" />
        <div v-else class="order-item" v-for="order in rentList" :key="order.orderId" 
             :class="{ overdue: order.isOverdue }">
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <div class="header-tags">
              <el-tag v-if="order.isOverdue" type="danger" size="small">逾期</el-tag>
              <el-tag :type="order.paymentStatus === 1 ? 'success' : 'warning'">
                {{ order.paymentStatus === 1 ? '已支付' : '待支付' }}
              </el-tag>
            </div>
          </div>
          <div class="order-body">
            <div class="house-info" v-if="order.house">
              <el-image :src="getFirstImage(order.house.images)" fit="cover" class="house-img">
                <template #error><div class="img-placeholder"><el-icon><Picture /></el-icon></div></template>
              </el-image>
              <div class="house-detail">
                <div class="house-title">{{ order.house.title }}</div>
                <div class="house-address">{{ order.house.address }}</div>
              </div>
            </div>
            <div class="order-info">
              <div class="info-row">
                <span class="label">订单类型：</span>
                <span>{{ getOrderTypeName(order.orderType) }}</span>
              </div>
              <div class="info-row">
                <span class="label">租客：</span>
                <span>{{ order.tenant?.realName || '租客' }} {{ order.tenant?.phone }}</span>
              </div>
              <div class="info-row">
                <span class="label">创建时间：</span>
                <span>{{ formatTime(order.createTime) }}</span>
              </div>
              <div class="info-row" v-if="order.paymentTime">
                <span class="label">支付时间：</span>
                <span>{{ formatTime(order.paymentTime) }}</span>
              </div>
            </div>
            <div class="order-amount">
              <div class="amount-label">{{ order.paymentStatus === 1 ? '已收金额' : '应收金额' }}</div>
              <div class="amount-value" :class="{ success: order.paymentStatus === 1 }">
                ¥{{ formatMoney(order.paymentStatus === 1 ? order.payAmount : order.totalAmount) }}
              </div>
            </div>
          </div>
          <div class="order-footer" v-if="order.paymentStatus === 0">
            <el-button type="warning" @click="handleRemind(order)">发送催缴</el-button>
          </div>
        </div>
      </div>
      <div class="pagination-wrap" v-if="rentTotal > 0">
        <el-pagination background layout="prev, pager, next" :total="rentTotal" :page-size="10"
          v-model:current-page="rentPage" @current-change="loadRentList" />
      </div>
    </div>

    <!-- 押金管理 -->
    <div v-show="activeTab === 'deposit'" class="tab-content">
      <el-card class="filter-card">
        <div class="filter-row">
          <span class="filter-label">状态：</span>
          <el-radio-group v-model="depositFilter.status" @change="loadDepositList">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">待处理</el-radio-button>
            <el-radio-button :value="1">已退还</el-radio-button>
            <el-radio-button :value="2">已扣除</el-radio-button>
          </el-radio-group>
        </div>
      </el-card>

      <div class="order-list" v-loading="depositLoading">
        <el-empty v-if="!depositLoading && depositList.length === 0" description="暂无押金退还申请" />
        <div v-else class="order-item deposit" v-for="order in depositList" :key="order.orderId">
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <el-tag :type="getDepositStatusType(order.paymentStatus)">{{ getDepositStatusName(order.paymentStatus) }}</el-tag>
          </div>
          <div class="order-body">
            <div class="house-info" v-if="order.house">
              <el-image :src="getFirstImage(order.house.images)" fit="cover" class="house-img">
                <template #error><div class="img-placeholder"><el-icon><Picture /></el-icon></div></template>
              </el-image>
              <div class="house-detail">
                <div class="house-title">{{ order.house.title }}</div>
                <div class="house-address">{{ order.house.address }}</div>
              </div>
            </div>
            <div class="order-info">
              <div class="info-row">
                <span class="label">租客：</span>
                <span>{{ order.tenant?.realName || '租客' }} {{ order.tenant?.phone }}</span>
              </div>
              <div class="info-row">
                <span class="label">申请押金：</span>
                <span>¥{{ formatMoney(order.totalAmount) }}</span>
              </div>
              <div class="info-row" v-if="order.paymentStatus !== 0">
                <span class="label">实退金额：</span>
                <span>¥{{ formatMoney(order.payAmount) }}</span>
              </div>
              <div class="info-row">
                <span class="label">申请时间：</span>
                <span>{{ formatTime(order.createTime) }}</span>
              </div>
            </div>
          </div>
          <div class="order-footer" v-if="order.paymentStatus === 0">
            <el-button type="success" @click="handleRefund(order, 1)">同意退还</el-button>
            <el-button type="danger" @click="handleRefund(order, 2)">扣除押金</el-button>
          </div>
        </div>
      </div>
      <div class="pagination-wrap" v-if="depositTotal > 0">
        <el-pagination background layout="prev, pager, next" :total="depositTotal" :page-size="10"
          v-model:current-page="depositPage" @current-change="loadDepositList" />
      </div>
    </div>

    <!-- 收入报表 -->
    <div v-show="activeTab === 'report'" class="tab-content">
      <div class="report-section">
        <el-card class="summary-card">
          <template #header>
            <div class="card-header">
              <span>收入统计</span>
              <el-radio-group v-model="summaryType" size="small" @change="loadSummary">
                <el-radio-button value="month">本月</el-radio-button>
                <el-radio-button value="quarter">本季度</el-radio-button>
                <el-radio-button value="year">本年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="summary-content">
            <div class="summary-item">
              <div class="summary-value">¥{{ formatMoney(summary.totalIncome) }}</div>
              <div class="summary-label">总收入</div>
            </div>
            <div class="summary-item">
              <div class="summary-value">¥{{ formatMoney(summary.rentIncome) }}</div>
              <div class="summary-label">租金收入</div>
            </div>
            <div class="summary-item">
              <div class="summary-value">¥{{ formatMoney(summary.depositIncome) }}</div>
              <div class="summary-label">押金收入</div>
            </div>
            <div class="summary-item">
              <div class="summary-value">{{ summary.orderCount }}</div>
              <div class="summary-label">订单数</div>
            </div>
          </div>
        </el-card>

        <div class="charts-row">
          <el-card class="chart-card">
            <template #header><span>近30天收入趋势</span></template>
            <div class="chart-container" ref="trendChartRef"></div>
          </el-card>
          <el-card class="chart-card">
            <template #header><span>房源收入排行（本月TOP5）</span></template>
            <div class="rank-list" v-if="houseRank.length > 0">
              <div class="rank-item" v-for="(item, index) in houseRank" :key="index">
                <span class="rank-num" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
                <span class="rank-name">{{ item.houseName }}</span>
                <span class="rank-income">¥{{ formatMoney(item.income) }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无数据" :image-size="60" />
          </el-card>
        </div>

        <el-card class="export-card">
          <template #header><span>导出报表</span></template>
          <div class="export-form">
            <el-date-picker v-model="exportRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
            <el-button type="primary" @click="handleExport" :loading="exporting">导出Excel</el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 押金处理弹窗 -->
    <el-dialog v-model="refundVisible" :title="refundAction === 1 ? '同意退还押金' : '扣除押金'" width="450px">
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="申请押金">
          <span class="form-value">¥{{ formatMoney(currentDeposit?.totalAmount) }}</span>
        </el-form-item>
        <el-form-item :label="refundAction === 1 ? '退还金额' : '扣除后退还'">
          <el-input-number v-model="refundForm.refundAmount" :min="0" :max="currentDeposit?.totalAmount" :precision="2" />
        </el-form-item>
        <el-form-item label="备注说明" v-if="refundAction === 2">
          <el-input v-model="refundForm.remark" type="textarea" :rows="3" placeholder="请说明扣除原因（如房屋损坏等）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button :type="refundAction === 1 ? 'success' : 'danger'" @click="confirmRefund" :loading="refunding">
          {{ refundAction === 1 ? '确认退还' : '确认扣除' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getFinanceStatsApi, getRentListApi, sendReminderApi,
  getDepositListApi, processDepositRefundApi,
  getIncomeTrendApi, getHouseIncomeRankApi, getIncomeSummaryApi, exportFinanceReportApi
} from '@/api/landlordFinance'

const activeTab = ref('rent')
const stats = reactive({ pendingCount: 0, overdueCount: 0, monthlyReceived: 0, monthlyExpected: 0, pendingRefund: 0 })

// 租金相关
const rentLoading = ref(false)
const rentList = ref<any[]>([])
const rentFilter = reactive({ status: undefined as number | undefined })
const rentPage = ref(1)
const rentTotal = ref(0)

// 押金相关
const depositLoading = ref(false)
const depositList = ref<any[]>([])
const depositFilter = reactive({ status: undefined as number | undefined })
const depositPage = ref(1)
const depositTotal = ref(0)

// 押金处理
const refundVisible = ref(false)
const currentDeposit = ref<any>(null)
const refundAction = ref(1)
const refundForm = reactive({ refundAmount: 0, remark: '' })
const refunding = ref(false)

// 报表相关
const summaryType = ref('month')
const summary = reactive({ totalIncome: 0, rentIncome: 0, depositIncome: 0, orderCount: 0 })
const trendChartRef = ref<HTMLElement | null>(null)
const houseRank = ref<any[]>([])
const exportRange = ref<string[]>([])
const exporting = ref(false)
let trendChart: echarts.ECharts | null = null

const formatMoney = (val: any) => val === null || val === undefined ? '0.00' : Number(val).toFixed(2)
const formatTime = (time: string) => time ? new Date(time).toLocaleString('zh-CN') : ''
const getFirstImage = (images: string) => {
  if (!images) return ''
  try { return JSON.parse(images)[0] || '' } catch { return images.split(',')[0] || '' }
}
const getOrderTypeName = (type: number) => ({ 0: '首期支付', 1: '租金支付', 2: '押金退还' }[type] || '未知')
const getDepositStatusName = (status: number) => ({ 0: '待处理', 1: '已退还', 2: '已扣除' }[status] || '未知')
const getDepositStatusType = (status: number) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info')

const loadStats = async () => {
  try {
    const res: any = await getFinanceStatsApi()
    Object.assign(stats, res)
  } catch (e) { console.error(e) }
}

const loadRentList = async () => {
  rentLoading.value = true
  try {
    const res: any = await getRentListApi({ status: rentFilter.status, page: rentPage.value, size: 10 })
    rentList.value = res.records || []
    rentTotal.value = res.total || 0
  } finally { rentLoading.value = false }
}

const loadDepositList = async () => {
  depositLoading.value = true
  try {
    const res: any = await getDepositListApi({ status: depositFilter.status, page: depositPage.value, size: 10 })
    depositList.value = res.records || []
    depositTotal.value = res.total || 0
  } finally { depositLoading.value = false }
}

const handleRemind = async (order: any) => {
  try {
    await ElMessageBox.confirm('确定发送催缴提醒给租客吗？', '发送催缴', { type: 'warning' })
    await sendReminderApi(order.orderId)
    ElMessage.success('催缴提醒已发送')
  } catch (e) { /* cancel */ }
}

const handleRefund = (order: any, action: number) => {
  currentDeposit.value = order
  refundAction.value = action
  refundForm.refundAmount = action === 1 ? order.totalAmount : 0
  refundForm.remark = ''
  refundVisible.value = true
}

const confirmRefund = async () => {
  refunding.value = true
  try {
    await processDepositRefundApi(currentDeposit.value.orderId, {
      action: refundAction.value,
      refundAmount: refundForm.refundAmount,
      remark: refundForm.remark
    })
    ElMessage.success(refundAction.value === 1 ? '押金已退还' : '押金已处理')
    refundVisible.value = false
    loadStats()
    loadDepositList()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally { refunding.value = false }
}

const loadSummary = async () => {
  try {
    const res: any = await getIncomeSummaryApi(summaryType.value)
    Object.assign(summary, res)
  } catch (e) { console.error(e) }
}

const loadTrend = async () => {
  try {
    const res: any = await getIncomeTrendApi()
    await nextTick()
    if (trendChartRef.value) {
      if (!trendChart) {
        trendChart = echarts.init(trendChartRef.value)
      }
      trendChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: res.map((i: any) => i.date.substring(5)) },
        yAxis: { type: 'value' },
        series: [{ data: res.map((i: any) => i.amount), type: 'line', smooth: true, areaStyle: { opacity: 0.3 } }],
        grid: { left: 50, right: 20, top: 20, bottom: 30 }
      })
    }
  } catch (e) { console.error(e) }
}

const loadHouseRank = async () => {
  try {
    const res: any = await getHouseIncomeRankApi()
    houseRank.value = res || []
  } catch (e) { console.error(e) }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const params: any = {}
    if (exportRange.value?.length === 2) {
      params.startDate = exportRange.value[0]
      params.endDate = exportRange.value[1]
    }
    const res: any = await exportFinanceReportApi(params)
    // 简单导出为CSV
    if (res && res.length > 0) {
      const headers = ['订单号', '订单类型', '金额', '实付', '支付方式', '支付时间', '合同号', '房源', '租客']
      const rows = res.map((r: any) => [
        r.orderNo, r.orderType, r.totalAmount, r.payAmount, r.paymentMethod, r.paymentTime, r.contractNo, r.houseName, r.tenantName
      ])
      const csv = [headers.join(','), ...rows.map((r: any) => r.join(','))].join('\n')
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `财务报表_${new Date().toISOString().split('T')[0]}.csv`
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    } else {
      ElMessage.warning('暂无数据可导出')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  } finally { exporting.value = false }
}

watch(activeTab, (tab) => {
  if (tab === 'report') {
    loadSummary()
    loadTrend()
    loadHouseRank()
  }
})

onMounted(() => {
  loadStats()
  loadRentList()
  loadDepositList()
})
</script>


<style scoped>
.finance-page { padding: 20px; background: #f5f5f5; min-height: calc(100vh - 60px); }

.stats-cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 15px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 18px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.stat-num { font-size: 24px; font-weight: bold; margin-bottom: 6px; }
.stat-label { color: #666; font-size: 13px; }
.stat-card.pending .stat-num { color: #e6a23c; }
.stat-card.danger .stat-num { color: #f56c6c; }
.stat-card.success .stat-num { color: #67c23a; font-size: 20px; }
.stat-card.info .stat-num { color: #409eff; font-size: 20px; }
.stat-card.warning .stat-num { color: #e6a23c; }

.main-tabs { background: #fff; padding: 0 20px; border-radius: 8px; margin-bottom: 20px; }
.tab-content { margin-top: 0; }

.filter-card { margin-bottom: 20px; }
.filter-row { display: flex; align-items: center; gap: 15px; }
.filter-label { color: #666; }

.order-list { display: flex; flex-direction: column; gap: 15px; }
.order-item { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.order-item.overdue { border-left: 4px solid #f56c6c; }
.order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #eee; }
.order-no { color: #666; font-size: 14px; }
.header-tags { display: flex; gap: 8px; }

.order-body { display: flex; gap: 20px; align-items: flex-start; }
.house-info { display: flex; gap: 12px; flex: 1; }
.house-img { width: 100px; height: 75px; border-radius: 6px; flex-shrink: 0; }
.img-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f5f5; color: #ccc; }
.house-detail { flex: 1; }
.house-title { font-size: 15px; font-weight: 500; margin-bottom: 5px; }
.house-address { font-size: 13px; color: #999; }

.order-info { flex: 1; }
.info-row { display: flex; margin-bottom: 8px; font-size: 14px; }
.info-row .label { color: #999; width: 80px; }

.order-amount { text-align: right; min-width: 120px; }
.amount-label { font-size: 13px; color: #999; margin-bottom: 5px; }
.amount-value { font-size: 24px; font-weight: bold; color: #f56c6c; }
.amount-value.success { color: #67c23a; }

.order-footer { margin-top: 15px; padding-top: 15px; border-top: 1px solid #eee; text-align: right; display: flex; gap: 10px; justify-content: flex-end; }

.pagination-wrap { margin-top: 20px; display: flex; justify-content: center; }

/* 报表样式 */
.report-section { display: flex; flex-direction: column; gap: 20px; }
.summary-card .card-header { display: flex; justify-content: space-between; align-items: center; }
.summary-content { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; padding: 10px 0; }
.summary-item { text-align: center; }
.summary-value { font-size: 28px; font-weight: bold; color: #409eff; margin-bottom: 8px; }
.summary-label { color: #666; font-size: 14px; }

.charts-row { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }
.chart-card { min-height: 300px; }
.chart-container { height: 250px; }

.rank-list { padding: 10px 0; }
.rank-item { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.rank-item:last-child { border-bottom: none; }
.rank-num { width: 24px; height: 24px; border-radius: 50%; background: #ddd; color: #666; display: flex; align-items: center; justify-content: center; font-size: 12px; margin-right: 12px; }
.rank-num.rank-1 { background: #ffd700; color: #fff; }
.rank-num.rank-2 { background: #c0c0c0; color: #fff; }
.rank-num.rank-3 { background: #cd7f32; color: #fff; }
.rank-name { flex: 1; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-income { color: #f56c6c; font-weight: 500; }

.export-card .export-form { display: flex; gap: 15px; align-items: center; }

.form-value { font-size: 18px; font-weight: bold; color: #409eff; }
</style>
