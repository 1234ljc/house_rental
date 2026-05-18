<template>
  <div class="order-manage">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">全部订单</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card pending">
          <div class="stat-value">{{ stats.pending || 0 }}</div>
          <div class="stat-label">待支付</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card paid">
          <div class="stat-value">{{ stats.paid || 0 }}</div>
          <div class="stat-label">已支付</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card failed">
          <div class="stat-value">{{ stats.failed || 0 }}</div>
          <div class="stat-label">支付失败</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card success-rate">
          <div class="stat-value">{{ stats.successRate || 0 }}%</div>
          <div class="stat-label">成功率</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card amount">
          <div class="stat-value">¥{{ formatAmount(stats.totalAmount) }}</div>
          <div class="stat-label">总交易额</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选区域 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="支付状态">
          <el-select v-model="queryParams.paymentStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" :value="-1" />
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="支付失败" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单类型">
          <el-select v-model="queryParams.orderType" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" :value="-1" />
            <el-option label="首期支付" :value="0" />
            <el-option label="租金支付" :value="1" />
            <el-option label="押金退还" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单编号">
          <el-input v-model="queryParams.keyword" placeholder="搜索订单编号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" 
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 订单列表 -->
    <el-card class="table-card">
      <el-table :data="orderList" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="houseTitle" label="房源" min-width="180" show-overflow-tooltip />
        <el-table-column label="租客" width="120">
          <template #default="{ row }">
            <div>{{ row.tenantName }}</div>
            <div class="sub-text">{{ row.tenantPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="orderType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getOrderTypeTag(row.orderType)" size="small">{{ getOrderTypeText(row.orderType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="金额" width="120">
          <template #default="{ row }">
            <span class="amount-text">¥{{ row.payAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="100">
          <template #default="{ row }">{{ getPaymentMethodText(row.paymentMethod) }}</template>
        </el-table-column>
        <el-table-column prop="paymentStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.paymentStatus)">{{ getStatusText(row.paymentStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadOrderList"
        @current-change="loadOrderList"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="650px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentOrder.paymentStatus)">{{ getStatusText(currentOrder.paymentStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单类型">{{ getOrderTypeText(currentOrder.orderType) }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ getPaymentMethodText(currentOrder.paymentMethod) }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">¥{{ currentOrder.payAmount }}</el-descriptions-item>
        <el-descriptions-item label="房源">{{ currentOrder.houseTitle }}</el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ currentOrder.contractNo }}</el-descriptions-item>
        <el-descriptions-item label="房东">{{ currentOrder.landlordName }} ({{ currentOrder.landlordPhone }})</el-descriptions-item>
        <el-descriptions-item label="租客">{{ currentOrder.tenantName }} ({{ currentOrder.tenantPhone }})</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ currentOrder.paymentTime || '未支付' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { getOrderList, getOrderStats, getOrderDetail } from '@/api/adminFinance'

const loading = ref(false)
const orderList = ref<any[]>([])
const total = ref(0)
const stats = ref<any>({})
const dateRange = ref<string[]>([])

const queryParams = reactive({
  paymentStatus: -1,
  orderType: -1,
  keyword: '',
  startDate: '',
  endDate: '',
  page: 1,
  size: 10
})

const detailVisible = ref(false)
const currentOrder = ref<any>(null)

watch(dateRange, (val) => {
  queryParams.startDate = val?.[0] || ''
  queryParams.endDate = val?.[1] || ''
})

const formatAmount = (amount: any) => {
  if (!amount) return '0'
  return Number(amount).toLocaleString()
}

const getStatusType = (status: number) => {
  const types: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '待支付', 1: '已支付', 2: '支付失败' }
  return texts[status] || '未知'
}

const getOrderTypeTag = (type: number) => {
  const tags: Record<number, string> = { 0: '', 1: 'success', 2: 'warning' }
  return tags[type] || 'info'
}

const getOrderTypeText = (type: number) => {
  const texts: Record<number, string> = { 0: '首期支付', 1: '租金支付', 2: '押金退还' }
  return texts[type] || '未知'
}

const getPaymentMethodText = (method: number) => {
  const texts: Record<number, string> = { 1: '支付宝', 2: '微信', 3: '银行卡' }
  return texts[method] || '-'
}

const loadStats = async () => {
  try {
    const res: any = await getOrderStats()
    stats.value = res
  } catch (e) {
    console.error(e)
  }
}

const loadOrderList = async () => {
  loading.value = true
  try {
    const res: any = await getOrderList(queryParams)
    orderList.value = res.records
    total.value = res.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  loadOrderList()
}

const handleReset = () => {
  queryParams.paymentStatus = -1
  queryParams.orderType = -1
  queryParams.keyword = ''
  queryParams.startDate = ''
  queryParams.endDate = ''
  dateRange.value = []
  queryParams.page = 1
  loadOrderList()
}

const handleView = async (row: any) => {
  try {
    const res: any = await getOrderDetail(row.orderId)
    currentOrder.value = res
    detailVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadStats()
  loadOrderList()
})
</script>

<style scoped>
.order-manage { padding: 20px; }
.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 16px 0; }
.stat-value { font-size: 26px; font-weight: bold; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 8px; }
.stat-card.pending .stat-value { color: #e6a23c; }
.stat-card.paid .stat-value { color: #67c23a; }
.stat-card.failed .stat-value { color: #f56c6c; }
.stat-card.success-rate .stat-value { color: #409eff; }
.stat-card.amount .stat-value { color: #e6a23c; font-size: 22px; }
.filter-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }
.sub-text { font-size: 12px; color: #909399; }
.amount-text { color: #e6a23c; font-weight: 500; }
</style>
