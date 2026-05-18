<template>
  <div class="payment-page">
    <!-- 支付豆卡片 -->
    <div class="beans-card">
      <div class="beans-left">
        <div class="beans-icon">🫘</div>
        <div class="beans-info">
          <div class="beans-label">我的支付豆</div>
          <div class="beans-value">{{ beansInfo.beans }}</div>
          <div class="beans-tip">≈ ¥{{ formatMoney(beansInfo.beansValue) }} 可抵扣</div>
        </div>
      </div>
      <div class="beans-right">
        <div class="beans-stat">
          <span class="stat-label">累计获得</span>
          <span class="stat-value">{{ beansInfo.totalEarned }}</span>
        </div>
        <div class="beans-stat">
          <span class="stat-label">累计使用</span>
          <span class="stat-value">{{ beansInfo.totalUsed }}</span>
        </div>
      </div>
      <div class="beans-rules" @click="showRules = true">
        <el-icon><QuestionFilled /></el-icon>
        <span>规则</span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card pending" @click="activeTab = 'pending'">
        <div class="stat-num">{{ stats.pending }}</div>
        <div class="stat-label">待支付</div>
      </div>
      <div class="stat-card success" @click="activeTab = 'history'">
        <div class="stat-num">{{ stats.paid }}</div>
        <div class="stat-label">已支付</div>
      </div>
      <div class="stat-card info">
        <div class="stat-num">¥{{ formatMoney(stats.totalPaid) }}</div>
        <div class="stat-label">累计支付</div>
      </div>
      <div class="stat-card warning" @click="activeTab = 'deposit'">
        <div class="stat-num">{{ stats.depositRefunding }}</div>
        <div class="stat-label">押金退还中</div>
      </div>
    </div>

    <!-- Tab切换 -->
    <el-tabs v-model="activeTab" class="main-tabs">
      <el-tab-pane label="待支付" name="pending" />
      <el-tab-pane label="支付记录" name="history" />
      <el-tab-pane label="押金退还" name="deposit" />
    </el-tabs>

    <!-- 待支付订单 -->
    <div v-show="activeTab === 'pending'" class="tab-content">
      <div class="order-list" v-loading="loading">
        <el-empty v-if="!loading && pendingOrders.length === 0" description="暂无待支付订单" />
        <div v-else class="order-item" v-for="order in pendingOrders" :key="order.orderId">
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <el-tag type="warning">待支付</el-tag>
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
              <div class="info-row"><span class="label">订单类型：</span><span>{{ getOrderTypeName(order.orderType) }}</span></div>
              <div class="info-row"><span class="label">收款方：</span><span>{{ order.landlord?.realName || '房东' }}</span></div>
              <div class="info-row"><span class="label">创建时间：</span><span>{{ formatTime(order.createTime) }}</span></div>
            </div>
            <div class="order-amount">
              <div class="amount-label">应付金额</div>
              <div class="amount-value">¥{{ formatMoney(order.totalAmount) }}</div>
            </div>
          </div>
          <div class="order-footer">
            <el-button type="primary" @click="handlePay(order)">立即支付</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 支付记录 -->
    <div v-show="activeTab === 'history'" class="tab-content">
      <el-card class="filter-card">
        <div class="filter-row">
          <span class="filter-label">订单类型：</span>
          <el-radio-group v-model="historyFilter.orderType" @change="loadHistoryOrders">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">首期支付</el-radio-button>
            <el-radio-button :value="1">租金支付</el-radio-button>
          </el-radio-group>
        </div>
      </el-card>
      <div class="order-list" v-loading="historyLoading">
        <el-empty v-if="!historyLoading && historyOrders.length === 0" description="暂无支付记录" />
        <div v-else class="order-item paid" v-for="order in historyOrders" :key="order.orderId">
          <div class="order-header">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <el-tag type="success">已支付</el-tag>
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
              <div class="info-row"><span class="label">订单类型：</span><span>{{ getOrderTypeName(order.orderType) }}</span></div>
              <div class="info-row"><span class="label">支付方式：</span><span>{{ getPaymentMethodName(order.paymentMethod) }}</span></div>
              <div class="info-row"><span class="label">支付时间：</span><span>{{ formatTime(order.paymentTime) }}</span></div>
            </div>
            <div class="order-amount">
              <div class="amount-label">实付金额</div>
              <div class="amount-value success">¥{{ formatMoney(order.payAmount) }}</div>
              <div class="beans-earned" v-if="order.payAmount > 0">+{{ Math.floor(order.payAmount * 10) }} 支付豆</div>
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-wrap" v-if="historyTotal > 0">
        <el-pagination background layout="prev, pager, next" :total="historyTotal" :page-size="10"
          v-model:current-page="historyPage" @current-change="loadHistoryOrders" />
      </div>
    </div>

    <!-- 押金退还 -->
    <div v-show="activeTab === 'deposit'" class="tab-content">
      <div class="order-list" v-loading="depositLoading">
        <el-empty v-if="!depositLoading && depositOrders.length === 0" description="暂无押金退还记录">
          <p class="empty-tip">合同到期或终止后可申请押金退还</p>
        </el-empty>
        <div v-else class="order-item deposit" v-for="order in depositOrders" :key="order.orderId">
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
              <div class="info-row"><span class="label">申请押金：</span><span>¥{{ formatMoney(order.totalAmount) }}</span></div>
              <div class="info-row" v-if="order.paymentStatus !== 0"><span class="label">实退金额：</span><span :class="{ 'text-success': order.paymentStatus === 1 }">¥{{ formatMoney(order.payAmount) }}</span></div>
              <div class="info-row"><span class="label">申请时间：</span><span>{{ formatTime(order.createTime) }}</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 支付弹窗 -->
    <el-dialog v-model="payVisible" title="订单支付" width="500px" :close-on-click-modal="false">
      <div class="pay-dialog" v-if="currentOrder">
        <div class="pay-amount-section">
          <div class="pay-amount-label">支付金额</div>
          <div class="pay-amount-value">¥{{ formatMoney(actualPayAmount) }}</div>
          <div class="pay-amount-original" v-if="useBeans && beansCalc.usableBeans > 0">
            原价 ¥{{ formatMoney(currentOrder.totalAmount) }}
          </div>
        </div>
        
        <div class="pay-info-section">
          <div class="pay-info-item"><span class="label">订单类型</span><span>{{ getOrderTypeName(currentOrder.orderType) }}</span></div>
          <div class="pay-info-item"><span class="label">房源</span><span>{{ currentOrder.house?.title }}</span></div>
          <div class="pay-info-item"><span class="label">收款方</span><span>{{ currentOrder.landlord?.realName || '房东' }}</span></div>
        </div>
        
        <!-- 支付豆抵扣 -->
        <div class="beans-section" v-if="beansCalc.currentBeans > 0">
          <div class="beans-header">
            <div class="beans-left-info">
              <span class="beans-icon-small">🫘</span>
              <span class="beans-text">支付豆抵扣</span>
              <span class="beans-available">可用 {{ beansCalc.usableBeans }} 豆，抵扣 ¥{{ formatMoney(beansCalc.discountAmount) }}</span>
            </div>
            <el-switch v-model="useBeans" :disabled="beansCalc.usableBeans === 0" />
          </div>
        </div>
        
        <!-- 支付方式 -->
        <div class="pay-method-section">
          <div class="method-title">选择支付方式</div>
          <div class="method-list">
            <div class="method-item" :class="{ active: paymentMethod === 1 }" @click="paymentMethod = 1">
              <div class="method-icon alipay"><img src="https://gw.alipayobjects.com/mdn/rms_ce4c6f/afts/img/A*XMCgSYx3f50AAAAAAAAAAABkARQnAQ" /></div>
              <span class="method-name">支付宝</span>
              <el-icon v-if="paymentMethod === 1" class="method-check"><CircleCheckFilled /></el-icon>
            </div>
            <div class="method-item" :class="{ active: paymentMethod === 2 }" @click="paymentMethod = 2">
              <div class="method-icon wechat"><img src="https://res.wx.qq.com/a/wx_fed/assets/res/NTI4MWU5.ico" /></div>
              <span class="method-name">微信支付</span>
              <el-icon v-if="paymentMethod === 2" class="method-check"><CircleCheckFilled /></el-icon>
            </div>
            <div class="method-item" :class="{ active: paymentMethod === 3 }" @click="paymentMethod = 3">
              <div class="method-icon bank"><el-icon :size="24"><CreditCard /></el-icon></div>
              <span class="method-name">银行卡</span>
              <el-icon v-if="paymentMethod === 3" class="method-check"><CircleCheckFilled /></el-icon>
            </div>
          </div>
        </div>
        
        <div class="earn-tip" v-if="actualPayAmount > 0">
          支付后可获得 <span class="earn-beans">{{ Math.floor(actualPayAmount * 10) }}</span> 支付豆
        </div>
      </div>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" size="large" @click="confirmPay" :loading="paying">
          确认支付 ¥{{ formatMoney(actualPayAmount) }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 支付成功弹窗 -->
    <el-dialog v-model="paySuccessVisible" title="" width="400px" :close-on-click-modal="false" :show-close="false">
      <div class="pay-success">
        <div class="success-icon">✅</div>
        <div class="success-title">支付成功</div>
        <div class="success-amount">实付 ¥{{ formatMoney(payResult.actualPay) }}</div>
        <div class="success-beans" v-if="payResult.beansEarned > 0">
          <div class="beans-earn-icon">🫘</div>
          <div class="beans-earn-text">
            <span>恭喜获得</span>
            <span class="beans-num">{{ payResult.beansEarned }}</span>
            <span>支付豆</span>
          </div>
        </div>
        <div class="success-info" v-if="payResult.beansUsed > 0">
          本次使用 {{ payResult.beansUsed }} 支付豆，抵扣 ¥{{ formatMoney(payResult.beansDiscount) }}
        </div>
        <div class="current-beans">当前支付豆余额：<span>{{ payResult.currentBeans }}</span></div>
      </div>
      <template #footer>
        <el-button type="primary" @click="paySuccessVisible = false">完成</el-button>
      </template>
    </el-dialog>

    <!-- 支付豆规则弹窗 -->
    <el-dialog v-model="showRules" title="支付豆规则" width="500px">
      <div class="rules-content">
        <h4>什么是支付豆？</h4>
        <p>支付豆是平台推出的会员权益，可在支付租金时抵扣现金。</p>
        <h4>支付豆价值</h4>
        <p>1000支付豆 = 1元，可在下次支付时抵扣。</p>
        <h4>如何获得支付豆？</h4>
        <p>每次成功支付租金后，按实付金额获得支付豆奖励。例如：支付100元租金，可获得1000支付豆。</p>
        <h4>使用规则</h4>
        <ul>
          <li>每笔订单最多可使用支付豆抵扣订单金额的10%</li>
          <li>支付豆不可提现、不可转让</li>
          <li>支付豆长期有效，不会过期</li>
        </ul>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, QuestionFilled, CircleCheckFilled, CreditCard } from '@element-plus/icons-vue'
import { getOrderStatsApi, getOrderListApi, payOrderApi, initOrdersApi, getBeansInfoApi, calcBeansForOrderApi } from '@/api/tenantOrder'

const activeTab = ref('pending')
const loading = ref(false)
const historyLoading = ref(false)
const depositLoading = ref(false)

const stats = reactive({ pending: 0, paid: 0, totalPaid: 0, depositRefunding: 0 })
const beansInfo = reactive({ beans: 0, beansValue: 0, totalEarned: 0, totalUsed: 0 })
const pendingOrders = ref<any[]>([])
const historyOrders = ref<any[]>([])
const depositOrders = ref<any[]>([])
const historyFilter = reactive({ orderType: undefined as number | undefined })
const historyPage = ref(1)
const historyTotal = ref(0)

const payVisible = ref(false)
const currentOrder = ref<any>(null)
const paymentMethod = ref(1)
const paying = ref(false)
const useBeans = ref(true)
const beansCalc = reactive({ currentBeans: 0, maxUsableBeans: 0, usableBeans: 0, discountAmount: 0 })

const paySuccessVisible = ref(false)
const payResult = reactive({ actualPay: 0, beansUsed: 0, beansDiscount: 0, beansEarned: 0, currentBeans: 0 })
const showRules = ref(false)

const actualPayAmount = computed(() => {
  if (!currentOrder.value) return 0
  const total = Number(currentOrder.value.totalAmount)
  if (useBeans.value && beansCalc.usableBeans > 0) {
    return Math.max(0, total - Number(beansCalc.discountAmount))
  }
  return total
})

const formatMoney = (val: any) => val === null || val === undefined ? '0.00' : Number(val).toFixed(2)
const formatTime = (time: string) => time ? new Date(time).toLocaleString('zh-CN') : ''
const getFirstImage = (images: string) => {
  if (!images) return ''
  try { return JSON.parse(images)[0] || '' } catch { return images.split(',')[0] || '' }
}
const getOrderTypeName = (type: number) => ({ 0: '首期支付（租金+押金）', 1: '租金支付', 2: '押金退还' }[type] || '未知')
const getPaymentMethodName = (method: number) => ({ 1: '支付宝', 2: '微信支付', 3: '银行卡' }[method] || '未知')
const getDepositStatusName = (status: number) => ({ 0: '待处理', 1: '已退还', 2: '已扣除' }[status] || '未知')
const getDepositStatusType = (status: number) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info')

const loadStats = async () => {
  try { const res: any = await getOrderStatsApi(); Object.assign(stats, res) } catch (e) { console.error(e) }
}
const loadBeansInfo = async () => {
  try { const res: any = await getBeansInfoApi(); Object.assign(beansInfo, res) } catch (e) { console.error(e) }
}
const loadPendingOrders = async () => {
  loading.value = true
  try {
    const res: any = await getOrderListApi({ paymentStatus: 0, page: 1, size: 50 })
    pendingOrders.value = (res.records || []).filter((o: any) => o.orderType !== 2)
  } finally { loading.value = false }
}
const loadHistoryOrders = async () => {
  historyLoading.value = true
  try {
    const res: any = await getOrderListApi({ orderType: historyFilter.orderType, paymentStatus: 1, page: historyPage.value, size: 10 })
    historyOrders.value = (res.records || []).filter((o: any) => o.orderType !== 2)
    historyTotal.value = res.total || 0
  } finally { historyLoading.value = false }
}
const loadDepositOrders = async () => {
  depositLoading.value = true
  try {
    const res: any = await getOrderListApi({ orderType: 2, page: 1, size: 50 })
    depositOrders.value = res.records || []
  } finally { depositLoading.value = false }
}
const handlePay = async (order: any) => {
  currentOrder.value = order
  paymentMethod.value = 1
  useBeans.value = true
  try {
    const res: any = await calcBeansForOrderApi(order.orderId)
    Object.assign(beansCalc, res)
  } catch (e) { beansCalc.currentBeans = 0; beansCalc.usableBeans = 0; beansCalc.discountAmount = 0 }
  payVisible.value = true
}
const confirmPay = async () => {
  try {
    await ElMessageBox.confirm('确认支付该订单吗？', '确认支付', { type: 'info' })
    paying.value = true
    const useBeansAmount = useBeans.value ? beansCalc.usableBeans : 0
    const res: any = await payOrderApi(currentOrder.value.orderId, { paymentMethod: paymentMethod.value, useBeans: useBeansAmount })
    Object.assign(payResult, res)
    payVisible.value = false
    paySuccessVisible.value = true
    loadStats(); loadBeansInfo(); loadPendingOrders(); loadHistoryOrders()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '支付失败') } finally { paying.value = false }
}
onMounted(async () => {
  try { await initOrdersApi() } catch (e) { console.error(e) }
  loadStats(); loadBeansInfo(); loadPendingOrders(); loadHistoryOrders(); loadDepositOrders()
})
</script>

<style scoped>
.payment-page { padding: 20px; background: #f5f5f5; min-height: calc(100vh - 60px); }

.beans-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px; padding: 24px; color: #fff; display: flex; align-items: center; margin-bottom: 20px; position: relative; }
.beans-left { display: flex; align-items: center; flex: 1; }
.beans-icon { font-size: 48px; margin-right: 20px; }
.beans-label { font-size: 14px; opacity: 0.9; margin-bottom: 5px; }
.beans-value { font-size: 36px; font-weight: bold; }
.beans-tip { font-size: 13px; opacity: 0.8; margin-top: 5px; }
.beans-right { display: flex; gap: 40px; }
.beans-stat { text-align: center; }
.stat-label { display: block; font-size: 13px; opacity: 0.8; margin-bottom: 5px; }
.stat-value { font-size: 20px; font-weight: 600; }
.beans-rules { position: absolute; top: 15px; right: 15px; display: flex; align-items: center; gap: 4px; font-size: 13px; opacity: 0.8; cursor: pointer; }
.beans-rules:hover { opacity: 1; }

.stats-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 20px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.06); cursor: pointer; transition: all 0.3s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-num { font-size: 28px; font-weight: bold; margin-bottom: 8px; }
.stat-card.pending .stat-num { color: #e6a23c; }
.stat-card.success .stat-num { color: #67c23a; }
.stat-card.info .stat-num { color: #409eff; font-size: 22px; }
.stat-card.warning .stat-num { color: #f56c6c; }

.main-tabs { background: #fff; padding: 0 20px; border-radius: 8px; margin-bottom: 20px; }
.filter-card { margin-bottom: 20px; }
.filter-row { display: flex; align-items: center; gap: 15px; }
.filter-label { color: #666; }

.order-list { display: flex; flex-direction: column; gap: 15px; }
.order-item { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #eee; }
.order-no { color: #666; font-size: 14px; }
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
.beans-earned { font-size: 12px; color: #667eea; margin-top: 5px; }
.order-footer { margin-top: 15px; padding-top: 15px; border-top: 1px solid #eee; text-align: right; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: center; }
.empty-tip { color: #999; font-size: 13px; margin-top: 10px; }
.text-success { color: #67c23a; }

/* 支付弹窗优化 */
.pay-dialog { padding: 0; }
.pay-amount-section { text-align: center; padding: 30px 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px; margin-bottom: 20px; color: #fff; }
.pay-amount-label { font-size: 14px; opacity: 0.9; margin-bottom: 8px; }
.pay-amount-value { font-size: 42px; font-weight: bold; }
.pay-amount-original { font-size: 14px; opacity: 0.7; text-decoration: line-through; margin-top: 5px; }
.pay-info-section { padding: 15px; background: #f9f9f9; border-radius: 8px; margin-bottom: 15px; }
.pay-info-item { display: flex; justify-content: space-between; padding: 8px 0; font-size: 14px; }
.pay-info-item .label { color: #999; }
.beans-section { padding: 15px; background: #f9f0ff; border-radius: 8px; margin-bottom: 15px; }
.beans-header { display: flex; justify-content: space-between; align-items: center; }
.beans-left-info { display: flex; align-items: center; gap: 8px; }
.beans-icon-small { font-size: 20px; }
.beans-text { font-weight: 500; color: #667eea; }
.beans-available { font-size: 12px; color: #999; }
.pay-method-section { margin-bottom: 15px; }
.method-title { font-size: 14px; font-weight: 500; margin-bottom: 12px; color: #333; }
.method-list { display: flex; flex-direction: column; gap: 10px; }
.method-item { display: flex; align-items: center; padding: 15px; border: 2px solid #eee; border-radius: 10px; cursor: pointer; transition: all 0.3s; }
.method-item:hover { border-color: #d0d0d0; }
.method-item.active { border-color: #667eea; background: #f9f0ff; }
.method-icon { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-right: 12px; overflow: hidden; background: #f5f5f5; }
.method-icon img { width: 28px; height: 28px; object-fit: contain; }
.method-icon.bank { background: linear-gradient(135deg, #ff6b35, #f7931e); color: #fff; }
.method-name { flex: 1; font-size: 15px; font-weight: 500; }
.method-check { color: #667eea; font-size: 20px; }
.earn-tip { text-align: center; padding: 12px; background: #f0f9eb; border-radius: 8px; font-size: 13px; color: #67c23a; }
.earn-beans { font-weight: 600; color: #667eea; }

/* 支付成功弹窗 */
.pay-success { text-align: center; padding: 20px 0; }
.success-icon { font-size: 60px; margin-bottom: 15px; }
.success-title { font-size: 20px; font-weight: 600; margin-bottom: 10px; }
.success-amount { font-size: 16px; color: #666; margin-bottom: 20px; }
.success-beans { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px; color: #fff; margin-bottom: 15px; }
.beans-earn-icon { font-size: 36px; }
.beans-earn-text { font-size: 16px; }
.beans-num { font-size: 28px; font-weight: bold; margin: 0 5px; }
.success-info { font-size: 13px; color: #999; margin-bottom: 10px; }
.current-beans { font-size: 14px; color: #666; }
.current-beans span { color: #667eea; font-weight: 600; }
.rules-content { line-height: 1.8; }
.rules-content h4 { margin: 15px 0 10px; color: #333; }
.rules-content p { color: #666; margin: 5px 0; }
.rules-content ul { padding-left: 20px; color: #666; }
.rules-content li { margin: 5px 0; }
</style>
