<template>
  <div class="after-page">
    <el-tabs v-model="activeTab" class="main-tabs">
      <el-tab-pane label="我的租约" name="contract" />
      <el-tab-pane label="租金缴纳" name="rent" />
      <el-tab-pane label="问题反馈" name="issue" />
      <el-tab-pane label="退租管理" name="checkout" />
    </el-tabs>

    <!-- 我的租约 -->
    <div v-show="activeTab === 'contract'" class="tab-content">
      <div class="contract-list" v-loading="contractLoading">
        <el-empty v-if="!contractLoading && contracts.length === 0" description="暂无租约" />
        <div v-else class="contract-card" v-for="item in contracts" :key="item.contractId">
          <div class="card-header">
            <span class="contract-no">{{ item.contractNo }}</span>
            <el-tag :type="item.status === 2 ? 'success' : 'info'">
              {{ item.status === 2 ? '租赁中' : '已结束' }}
            </el-tag>
          </div>
          <div class="card-body">
            <div class="house-info">
              <el-image :src="getFirstImage(item.houseImages)" fit="cover" class="house-img">
                <template #error><div class="img-placeholder"><el-icon><Picture /></el-icon></div></template>
              </el-image>
              <div class="house-detail">
                <div class="house-title">{{ item.houseTitle }}</div>
                <div class="house-address">{{ item.houseAddress }}</div>
              </div>
            </div>
            <div class="contract-info">
              <div class="info-row"><span class="label">租期：</span>{{ item.rentStartDate }} ~ {{ item.rentEndDate }}</div>
              <div class="info-row"><span class="label">月租：</span>¥{{ item.monthlyRent }}</div>
              <div class="info-row"><span class="label">付款日：</span>每月{{ item.paymentDay }}日</div>
              <div class="info-row"><span class="label">房东：</span>{{ item.landlordName }} {{ item.landlordPhone }}</div>
            </div>
          </div>
          <div class="card-footer" v-if="item.status === 2">
            <el-button type="primary" size="small" @click="openIssueDialog(item)">问题反馈</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 租金缴纳 -->
    <div v-show="activeTab === 'rent'" class="tab-content">
      <div class="rent-list" v-loading="rentLoading">
        <el-empty v-if="!rentLoading && rentList.length === 0" description="暂无租金记录" />
        <el-table v-else :data="rentList" stripe>
          <el-table-column prop="orderNo" label="订单号" width="180" />
          <el-table-column prop="houseTitle" label="房源" min-width="150" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ row.orderType === 0 ? '首期支付' : '租金' }}</template>
          </el-table-column>
          <el-table-column label="金额" width="100">
            <template #default="{ row }">¥{{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.paymentStatus === 1 ? 'success' : (row.isOverdue ? 'danger' : 'warning')" size="small">
                {{ row.paymentStatus === 1 ? '已支付' : (row.isOverdue ? '逾期' : '待支付') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="支付时间" width="170">
            <template #default="{ row }">{{ row.paymentTime ? formatTime(row.paymentTime) : '-' }}</template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap" v-if="rentTotal > 10">
          <el-pagination background layout="prev, pager, next" :total="rentTotal" :page-size="10"
            v-model:current-page="rentPage" @current-change="loadRentList" />
        </div>
      </div>
    </div>

    <!-- 问题反馈 -->
    <div v-show="activeTab === 'issue'" class="tab-content">
      <el-card class="filter-card">
        <el-radio-group v-model="issueFilter" @change="loadIssueList">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="0">待处理</el-radio-button>
          <el-radio-button :value="1">处理中</el-radio-button>
          <el-radio-button :value="2">已解决</el-radio-button>
        </el-radio-group>
      </el-card>
      <div class="issue-list" v-loading="issueLoading">
        <el-empty v-if="!issueLoading && issueList.length === 0" description="暂无问题反馈" />
        <div v-else class="issue-card" v-for="item in issueList" :key="item.manageId">
          <div class="issue-header">
            <span class="issue-type">{{ item.manageType === 0 ? '维修申请' : '其他问题' }}</span>
            <el-tag :type="getIssueStatusType(item.status)" size="small">{{ getIssueStatusText(item.status) }}</el-tag>
          </div>
          <div class="issue-body">
            <div class="issue-house">{{ item.houseTitle }} - {{ item.contractNo }}</div>
            <div class="issue-content">{{ item.content }}</div>
            <div class="issue-images" v-if="item.images">
              <el-image v-for="(img, idx) in parseImages(item.images)" :key="idx" :src="img" 
                :preview-src-list="parseImages(item.images)" fit="cover" class="issue-img" />
            </div>
            <div class="issue-response" v-if="item.responseContent">
              <div class="response-label">房东回复：</div>
              <div class="response-content">{{ item.responseContent }}</div>
            </div>
          </div>
          <div class="issue-footer">
            <span class="issue-time">{{ formatTime(item.createTime) }}</span>
            <el-button v-if="item.status !== 2" type="primary" link size="small" @click="openAppendDialog(item)">补充说明</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 退租管理 -->
    <div v-show="activeTab === 'checkout'" class="tab-content">
      <el-card class="filter-card">
        <el-button type="primary" @click="openCheckoutDialog">申请退租</el-button>
      </el-card>
      <div class="checkout-list" v-loading="checkoutLoading">
        <el-empty v-if="!checkoutLoading && checkoutList.length === 0" description="暂无退租申请" />
        <div v-else class="checkout-card" v-for="item in checkoutList" :key="item.manageId">
          <div class="checkout-header">
            <span class="checkout-house">{{ item.houseTitle }}</span>
            <el-tag :type="getCheckoutStatusType(item.status)" size="small">{{ getCheckoutStatusText(item.status) }}</el-tag>
          </div>
          <div class="checkout-body">
            <div class="info-row"><span class="label">合同编号：</span>{{ item.contractNo }}</div>
            <div class="info-row"><span class="label">退租原因：</span></div>
            <div class="checkout-content">{{ item.content }}</div>
            <div class="info-row" v-if="item.responseContent"><span class="label">房东回复：</span>{{ item.responseContent }}</div>
          </div>
          <div class="checkout-footer">
            <span class="checkout-time">申请时间：{{ formatTime(item.createTime) }}</span>
            <div class="checkout-actions">
              <el-button v-if="item.status === 0" type="danger" size="small" @click="cancelCheckout(item)">取消申请</el-button>
              <el-button v-if="item.status === 3" type="primary" size="small" @click="confirmCheckout(item)">确认交接完成</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 问题反馈弹窗 -->
    <el-dialog v-model="issueDialogVisible" title="问题反馈" width="550px">
      <el-form :model="issueForm" label-width="80px">
        <el-form-item label="房源">
          <span>{{ currentContract?.houseTitle }}</span>
        </el-form-item>
        <el-form-item label="问题类型">
          <el-radio-group v-model="issueForm.manageType">
            <el-radio :value="0">维修申请</el-radio>
            <el-radio :value="1">其他问题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="问题描述" required>
          <el-input v-model="issueForm.content" type="textarea" :rows="4" placeholder="请详细描述您遇到的问题" />
        </el-form-item>
        <el-form-item label="上传图片">
          <el-upload action="#" list-type="picture-card" :auto-upload="false" 
            :on-change="handleImageChange" :file-list="issueImages" :limit="5">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitIssue" :loading="issueSubmitting">提交</el-button>
      </template>
    </el-dialog>

    <!-- 补充说明弹窗 -->
    <el-dialog v-model="appendDialogVisible" title="补充说明" width="450px">
      <el-input v-model="appendContent" type="textarea" :rows="4" placeholder="请输入补充内容" />
      <template #footer>
        <el-button @click="appendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAppend" :loading="appendSubmitting">提交</el-button>
      </template>
    </el-dialog>

    <!-- 退租申请弹窗 -->
    <el-dialog v-model="checkoutDialogVisible" title="申请退租" width="550px">
      <el-form :model="checkoutForm" label-width="100px">
        <el-form-item label="选择租约" required>
          <el-select v-model="checkoutForm.contractId" placeholder="请选择要退租的租约" style="width: 100%">
            <el-option v-for="c in activeContracts" :key="c.contractId" :label="c.houseTitle" :value="c.contractId">
              <span>{{ c.houseTitle }}</span>
              <span style="color: #999; margin-left: 10px;">{{ c.contractNo }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="期望退租日期" required>
          <el-date-picker v-model="checkoutForm.expectDate" type="date" placeholder="选择日期" 
            value-format="YYYY-MM-DD" style="width: 100%" :disabled-date="(d: Date) => d < new Date()" />
        </el-form-item>
        <el-form-item label="退租原因" required>
          <el-input v-model="checkoutForm.reason" type="textarea" :rows="4" placeholder="请说明退租原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkoutDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCheckout" :loading="checkoutSubmitting">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Plus, User } from '@element-plus/icons-vue'
import {
  getActiveContractsApi, getRentListApi, getIssueListApi, submitIssueApi, appendIssueApi,
  getCheckoutListApi, applyCheckoutApi, cancelCheckoutApi, confirmCheckoutApi
} from '@/api/tenantAfter'

const activeTab = ref('contract')

// 租约
const contractLoading = ref(false)
const contracts = ref<any[]>([])

// 租金
const rentLoading = ref(false)
const rentList = ref<any[]>([])
const rentPage = ref(1)
const rentTotal = ref(0)

// 问题
const issueLoading = ref(false)
const issueList = ref<any[]>([])
const issueFilter = ref<number | undefined>(undefined)
const issueDialogVisible = ref(false)
const issueForm = reactive({ contractId: 0, manageType: 0, content: '' })
const issueImages = ref<any[]>([])
const issueSubmitting = ref(false)
const currentContract = ref<any>(null)
const appendDialogVisible = ref(false)
const appendContent = ref('')
const appendSubmitting = ref(false)
const currentIssue = ref<any>(null)

// 退租
const checkoutLoading = ref(false)
const checkoutList = ref<any[]>([])
const checkoutDialogVisible = ref(false)
const checkoutForm = reactive({ contractId: 0, expectDate: '', reason: '' })
const checkoutSubmitting = ref(false)
const activeContracts = ref<any[]>([])

const formatTime = (time: string) => time ? new Date(time).toLocaleString('zh-CN') : ''
const getFirstImage = (images: string) => {
  if (!images) return ''
  try { return JSON.parse(images)[0] || '' } catch { return images.split(',')[0] || '' }
}
const parseImages = (images: string) => {
  if (!images) return []
  try { return JSON.parse(images) } catch { return images.split(',') }
}
const getIssueStatusText = (status: number) => ({ 0: '待处理', 1: '处理中', 2: '已解决' }[status] || '未知')
const getIssueStatusType = (status: number) => ({ 0: 'warning', 1: 'primary', 2: 'success' }[status] || 'info')
const getCheckoutStatusText = (status: number) => ({ 0: '待审核', 1: '已同意', 2: '已拒绝', 3: '待交接', 4: '已完成' }[status] || '未知')
const getCheckoutStatusType = (status: number) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'primary', 4: 'info' }[status] || 'info')

const loadContracts = async () => {
  contractLoading.value = true
  try {
    const res: any = await getActiveContractsApi()
    contracts.value = res || []
    activeContracts.value = (res || []).filter((c: any) => c.status === 2)
  } finally { contractLoading.value = false }
}

const loadRentList = async () => {
  rentLoading.value = true
  try {
    const res: any = await getRentListApi({ page: rentPage.value, size: 10 })
    rentList.value = res.records || []
    rentTotal.value = res.total || 0
  } finally { rentLoading.value = false }
}

const loadIssueList = async () => {
  issueLoading.value = true
  try {
    const res: any = await getIssueListApi({ status: issueFilter.value, page: 1, size: 50 })
    issueList.value = res.records || []
  } finally { issueLoading.value = false }
}

const openIssueDialog = (contract: any) => {
  currentContract.value = contract
  issueForm.contractId = contract.contractId
  issueForm.manageType = 0
  issueForm.content = ''
  issueImages.value = []
  issueDialogVisible.value = true
}

const handleImageChange = (file: any) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    issueImages.value.push({ url: e.target?.result, raw: file.raw })
  }
  reader.readAsDataURL(file.raw)
}

const submitIssue = async () => {
  if (!issueForm.content.trim()) { ElMessage.warning('请填写问题描述'); return }
  issueSubmitting.value = true
  try {
    const images = issueImages.value.map(i => i.url)
    await submitIssueApi({
      contractId: issueForm.contractId,
      content: issueForm.content,
      manageType: issueForm.manageType,
      images: images.length > 0 ? JSON.stringify(images) : undefined
    })
    ElMessage.success('问题已提交')
    issueDialogVisible.value = false
    loadIssueList()
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally { issueSubmitting.value = false }
}

const openAppendDialog = (issue: any) => {
  currentIssue.value = issue
  appendContent.value = ''
  appendDialogVisible.value = true
}

const submitAppend = async () => {
  if (!appendContent.value.trim()) { ElMessage.warning('请输入补充内容'); return }
  appendSubmitting.value = true
  try {
    await appendIssueApi(currentIssue.value.manageId, { content: appendContent.value })
    ElMessage.success('补充成功')
    appendDialogVisible.value = false
    loadIssueList()
  } finally { appendSubmitting.value = false }
}

// 退租相关
const loadCheckoutList = async () => {
  checkoutLoading.value = true
  try {
    const res: any = await getCheckoutListApi()
    checkoutList.value = res || []
  } finally { checkoutLoading.value = false }
}

const openCheckoutDialog = () => {
  if (activeContracts.value.length === 0) {
    ElMessage.warning('暂无可退租的租约')
    return
  }
  checkoutForm.contractId = activeContracts.value[0]?.contractId || 0
  checkoutForm.expectDate = ''
  checkoutForm.reason = ''
  checkoutDialogVisible.value = true
}

const submitCheckout = async () => {
  if (!checkoutForm.contractId) { ElMessage.warning('请选择租约'); return }
  if (!checkoutForm.expectDate) { ElMessage.warning('请选择期望退租日期'); return }
  if (!checkoutForm.reason.trim()) { ElMessage.warning('请填写退租原因'); return }
  checkoutSubmitting.value = true
  try {
    await applyCheckoutApi(checkoutForm)
    ElMessage.success('退租申请已提交')
    checkoutDialogVisible.value = false
    loadCheckoutList()
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally { checkoutSubmitting.value = false }
}

const cancelCheckout = async (item: any) => {
  try {
    await ElMessageBox.confirm('确定取消该退租申请吗？', '取消确认', { type: 'warning' })
    await cancelCheckoutApi(item.manageId)
    ElMessage.success('已取消')
    loadCheckoutList()
  } catch (e) { /* cancel */ }
}

const confirmCheckout = async (item: any) => {
  try {
    await ElMessageBox.confirm('确认房屋交接已完成？', '确认交接', { type: 'info' })
    await confirmCheckoutApi(item.manageId)
    ElMessage.success('交接完成')
    loadCheckoutList()
    loadContracts()
  } catch (e) { /* cancel */ }
}

onMounted(() => {
  loadContracts()
  loadRentList()
  loadIssueList()
  loadCheckoutList()
})
</script>


<style scoped>
.after-page { padding: 20px; background: #f5f5f5; min-height: calc(100vh - 60px); }
.main-tabs { background: #fff; padding: 0 20px; border-radius: 8px; margin-bottom: 20px; }
.tab-content { margin-top: 0; }

/* 租约卡片 */
.contract-list { display: flex; flex-direction: column; gap: 15px; }
.contract-card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #eee; }
.contract-no { font-weight: 500; color: #666; }
.card-body { display: flex; gap: 20px; }
.house-info { display: flex; gap: 12px; flex: 1; }
.house-img { width: 120px; height: 90px; border-radius: 6px; flex-shrink: 0; }
.img-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f5f5; color: #ccc; }
.house-detail { flex: 1; }
.house-title { font-size: 16px; font-weight: 500; margin-bottom: 5px; }
.house-address { font-size: 13px; color: #999; }
.contract-info { flex: 1; }
.info-row { margin-bottom: 8px; font-size: 14px; }
.info-row .label { color: #999; }
.card-footer { margin-top: 15px; padding-top: 15px; border-top: 1px solid #eee; text-align: right; }

/* 问题反馈 */
.filter-card { margin-bottom: 20px; }
.issue-list { display: flex; flex-direction: column; gap: 15px; }
.issue-card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.issue-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.issue-type { font-weight: 500; }
.issue-body { }
.issue-house { font-size: 13px; color: #999; margin-bottom: 8px; }
.issue-content { font-size: 14px; line-height: 1.6; margin-bottom: 10px; white-space: pre-wrap; }
.issue-images { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.issue-img { width: 80px; height: 80px; border-radius: 4px; }
.issue-response { background: #f5f7fa; padding: 12px; border-radius: 6px; margin-top: 10px; }
.response-label { font-size: 13px; color: #666; margin-bottom: 5px; }
.response-content { font-size: 14px; }
.issue-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; padding-top: 12px; border-top: 1px solid #eee; }
.issue-time { font-size: 13px; color: #999; }

.pagination-wrap { margin-top: 20px; display: flex; justify-content: center; }

/* 退租卡片 */
.checkout-list { display: flex; flex-direction: column; gap: 15px; }
.checkout-card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.checkout-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #eee; }
.checkout-house { font-size: 16px; font-weight: 500; }
.checkout-body .info-row { margin-bottom: 8px; font-size: 14px; }
.checkout-body .info-row .label { color: #999; }
.checkout-content { background: #f5f7fa; padding: 12px; border-radius: 6px; margin: 10px 0; font-size: 14px; line-height: 1.8; white-space: pre-wrap; }
.checkout-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 15px; padding-top: 15px; border-top: 1px solid #eee; }
.checkout-time { font-size: 13px; color: #999; }
.checkout-actions { display: flex; gap: 10px; }
</style>
