<template>
  <div class="order-page">
    <h2 class="page-title">我的订单</h2>

    <!-- 筛选 -->
    <el-card class="filter-card">
      <div class="filter-row">
        <span class="filter-label">订单状态：</span>
        <el-radio-group v-model="filter.status" @change="loadOrders">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="0">待支付</el-radio-button>
          <el-radio-button :value="1">已支付</el-radio-button>
        </el-radio-group>
        <span class="filter-label" style="margin-left: 30px;">订单类型：</span>
        <el-radio-group v-model="filter.orderType" @change="loadOrders">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="0">首期支付</el-radio-button>
          <el-radio-button :value="1">租金支付</el-radio-button>
          <el-radio-button :value="2">押金退还</el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <!-- 订单列表 -->
    <div class="order-list" v-loading="loading">
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单记录" />
      <div v-else class="order-item" v-for="order in orders" :key="order.orderId">
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <span class="order-time">{{ formatTime(order.createTime) }}</span>
          <el-tag :type="order.paymentStatus === 1 ? 'success' : 'warning'">
            {{ order.paymentStatus === 1 ? '已支付' : '待支付' }}
          </el-tag>
          <!-- 待支付订单显示超时提醒 -->
          <el-tag v-if="order.paymentStatus === 0 && getOverdueMinutes(order.createTime) >= 30" type="danger" effect="dark">
            <el-icon><Warning /></el-icon>
            {{ getOverdueText(order.createTime) }}
          </el-tag>
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
            <div class="info-row"><span class="label">订单类型：</span>{{ getOrderTypeName(order.orderType) }}</div>
            <div class="info-row"><span class="label">收款方：</span>{{ order.landlord?.realName || '房东' }}</div>
            <div class="info-row" v-if="order.paymentStatus === 1">
              <span class="label">支付方式：</span>{{ getPaymentMethodName(order.paymentMethod) }}
            </div>
            <div class="info-row" v-if="order.paymentStatus === 1">
              <span class="label">支付时间：</span>{{ formatTime(order.paymentTime) }}
            </div>
          </div>
          <div class="order-amount">
            <div class="amount-label">{{ order.paymentStatus === 1 ? '实付金额' : '应付金额' }}</div>
            <div class="amount-value" :class="{ success: order.paymentStatus === 1 }">
              ¥{{ formatMoney(order.paymentStatus === 1 ? order.payAmount : order.totalAmount) }}
            </div>
          </div>
        </div>
        <div class="order-footer" v-if="order.paymentStatus === 0 && order.orderType !== 2">
          <el-button type="primary" @click="goPayment">去支付</el-button>
        </div>
      </div>
    </div>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="10"
        v-model:current-page="page" @current-change="loadOrders" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Picture, Warning } from '@element-plus/icons-vue'
import { getOrderListApi, initOrdersApi } from '@/api/tenantOrder'

const router = useRouter()
const loading = ref(false)
const orders = ref<any[]>([])
const filter = reactive({ status: undefined as number | undefined, orderType: undefined as number | undefined })
const page = ref(1)
const total = ref(0)

const formatMoney = (val: any) => val === null || val === undefined ? '0.00' : Number(val).toFixed(2)
const formatTime = (time: string) => time ? new Date(time).toLocaleString('zh-CN') : ''
const getFirstImage = (images: string) => {
  if (!images) return ''
  try { return JSON.parse(images)[0] || '' } catch { return images.split(',')[0] || '' }
}
const getOrderTypeName = (type: number) => ({ 0: '首期支付（租金+押金）', 1: '租金支付', 2: '押金退还' }[type] || '未知')
const getPaymentMethodName = (method: number) => ({ 1: '支付宝', 2: '微信支付', 3: '银行卡' }[method] || '未知')

// 计算订单超时分钟数
const getOverdueMinutes = (createTime: string) => {
  if (!createTime) return 0
  const now = new Date().getTime()
  const create = new Date(createTime).getTime()
  return Math.floor((now - create) / 1000 / 60)
}

// 获取超时提醒文本
const getOverdueText = (createTime: string) => {
  const minutes = getOverdueMinutes(createTime)
  if (minutes < 60) return `已超时${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `已超时${hours}小时`
  const days = Math.floor(hours / 24)
  return `已超时${days}天`
}

const loadOrders = async () => {
  loading.value = true
  try {
    const res: any = await getOrderListApi({ 
      paymentStatus: filter.status, 
      orderType: filter.orderType, 
      page: page.value, 
      size: 10 
    })
    orders.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const goPayment = () => { router.push('/tenant/payment') }

onMounted(async () => {
  try { await initOrdersApi() } catch (e) { console.error(e) }
  loadOrders()
})
</script>

<style scoped>
.order-page { padding: 20px; background: #f5f5f5; min-height: calc(100vh - 60px); }
.page-title { margin: 0 0 20px; font-size: 20px; font-weight: 600; }
.filter-card { margin-bottom: 20px; }
.filter-row { display: flex; align-items: center; gap: 15px; flex-wrap: wrap; }
.filter-label { color: #666; }
.order-list { display: flex; flex-direction: column; gap: 15px; }
.order-item { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.order-header { display: flex; align-items: center; gap: 15px; margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #eee; }
.order-no { color: #333; font-size: 14px; font-weight: 500; }
.order-time { color: #999; font-size: 13px; flex: 1; }
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
.order-footer { margin-top: 15px; padding-top: 15px; border-top: 1px solid #eee; text-align: right; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: center; }
</style>
