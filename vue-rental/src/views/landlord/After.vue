<template>
  <div class="after-page">
    <el-tabs v-model="activeTab" class="main-tabs">
      <el-tab-pane label="租客管理" name="tenant" />
      <el-tab-pane label="问题处理" name="issue">
        <template #label>
          <span>问题处理</span>
          <el-badge v-if="issueStats.pending > 0" :value="issueStats.pending" class="tab-badge" />
        </template>
      </el-tab-pane>
      <el-tab-pane label="退租管理" name="checkout">
        <template #label>
          <span>退租管理</span>
          <el-badge v-if="checkoutStats.pending > 0" :value="checkoutStats.pending" class="tab-badge" />
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- 租客管理 -->
    <div v-show="activeTab === 'tenant'" class="tab-content">
      <el-tabs v-model="tenantTab" type="card" @tab-change="handleTenantTabChange">
        <el-tab-pane label="当前租客" name="current" />
        <el-tab-pane label="历史租客" name="history" />
      </el-tabs>

      <div v-show="tenantTab === 'current'" class="tenant-list" v-loading="currentTenantLoading">
        <el-empty v-if="!currentTenantLoading && currentTenants.length === 0" description="暂无当前租客" />
        <div v-else class="tenant-card" v-for="item in currentTenants" :key="item.contractId">
          <div class="tenant-info">
            <el-avatar :size="50" :src="item.tenantAvatar"><el-icon><User /></el-icon></el-avatar>
            <div class="tenant-detail">
              <div class="tenant-name">{{ item.tenantName }}</div>
              <div class="tenant-phone">{{ item.tenantPhone }}</div>
            </div>
          </div>
          <div class="contract-info">
            <div class="info-item"><span class="label">房源：</span>{{ item.houseTitle }}</div>
            <div class="info-item"><span class="label">租期：</span>{{ item.rentStartDate }} ~ {{ item.rentEndDate }}</div>
            <div class="info-item"><span class="label">月租：</span>¥{{ item.monthlyRent }}</div>
          </div>
          <el-tag type="success">租赁中</el-tag>
        </div>
        <div class="pagination-wrap" v-if="currentTenantTotal > 5">
          <el-pagination background layout="prev, pager, next" :total="currentTenantTotal" :page-size="5"
            v-model:current-page="currentTenantPage" @current-change="loadCurrentTenants" />
        </div>
      </div>

      <div v-show="tenantTab === 'history'" class="tenant-list" v-loading="historyTenantLoading">
        <el-empty v-if="!historyTenantLoading && historyTenants.length === 0" description="暂无历史租客" />
        <div v-else class="tenant-card" v-for="item in historyTenants" :key="item.contractId">
          <div class="tenant-info">
            <el-avatar :size="50" :src="item.tenantAvatar"><el-icon><User /></el-icon></el-avatar>
            <div class="tenant-detail">
              <div class="tenant-name">{{ item.tenantName }}</div>
              <div class="tenant-phone">{{ item.tenantPhone }}</div>
            </div>
          </div>
          <div class="contract-info">
            <div class="info-item"><span class="label">房源：</span>{{ item.houseTitle }}</div>
            <div class="info-item"><span class="label">租期：</span>{{ item.rentStartDate }} ~ {{ item.rentEndDate }}</div>
          </div>
          <div class="card-actions">
            <el-tag :type="item.status === 3 ? 'info' : 'danger'">{{ item.status === 3 ? '已到期' : '已终止' }}</el-tag>
          </div>
        </div>
        <div class="pagination-wrap" v-if="historyTenantTotal > 5">
          <el-pagination background layout="prev, pager, next" :total="historyTenantTotal" :page-size="5"
            v-model:current-page="historyTenantPage" @current-change="loadHistoryTenants" />
        </div>
      </div>
    </div>

    <!-- 问题处理 -->
    <div v-show="activeTab === 'issue'" class="tab-content">
      <div class="stats-cards">
        <div class="stat-card pending" @click="issueFilter = 0; issuePage = 1; loadIssueList()">
          <div class="stat-num">{{ issueStats.pending }}</div>
          <div class="stat-label">待处理</div>
        </div>
        <div class="stat-card processing" @click="issueFilter = 1; issuePage = 1; loadIssueList()">
          <div class="stat-num">{{ issueStats.processing }}</div>
          <div class="stat-label">处理中</div>
        </div>
        <div class="stat-card completed" @click="issueFilter = 2; issuePage = 1; loadIssueList()">
          <div class="stat-num">{{ issueStats.completed }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <el-card class="filter-card">
        <el-radio-group v-model="issueFilter" @change="issuePage = 1; loadIssueList()">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="0">待处理</el-radio-button>
          <el-radio-button :value="1">处理中</el-radio-button>
          <el-radio-button :value="2">已完成</el-radio-button>
        </el-radio-group>
      </el-card>
      <div class="issue-list" v-loading="issueLoading">
        <el-empty v-if="!issueLoading && issueList.length === 0" description="暂无问题反馈" />
        <div v-else class="issue-card" v-for="item in issueList" :key="item.manageId">
          <div class="issue-header">
            <div class="issue-from">
              <span class="issue-type">{{ item.manageType === 0 ? '维修申请' : '其他问题' }}</span>
              <span class="issue-tenant">{{ item.tenantName }} - {{ item.tenantPhone }}</span>
            </div>
            <el-tag :type="getIssueStatusType(item.status)" size="small">{{ getIssueStatusText(item.status) }}</el-tag>
          </div>
          <div class="issue-body">
            <div class="issue-house">{{ item.houseTitle }} - {{ item.contractNo }}</div>
            <div class="issue-content">{{ item.content }}</div>
            <div class="issue-images" v-if="item.images">
              <el-image v-for="(img, idx) in parseImages(item.images)" :key="idx" :src="img" :preview-src-list="parseImages(item.images)" fit="cover" class="issue-img" />
            </div>
            <div class="issue-response" v-if="item.responseContent">
              <div class="response-label">我的回复：</div>
              <div class="response-content">{{ item.responseContent }}</div>
            </div>
          </div>
          <div class="issue-footer">
            <span class="issue-time">{{ formatTime(item.createTime) }}</span>
            <div class="issue-actions" v-if="item.status !== 2">
              <el-button type="primary" size="small" @click="openProcessDialog(item)">处理</el-button>
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-wrap" v-if="issueTotal > 5">
        <el-pagination background layout="prev, pager, next" :total="issueTotal" :page-size="5"
          v-model:current-page="issuePage" @current-change="loadIssueList" />
      </div>
    </div>

    <!-- 退租管理 -->
    <div v-show="activeTab === 'checkout'" class="tab-content">
      <div class="stats-cards">
        <div class="stat-card pending" @click="checkoutFilter = 0; checkoutPage = 1; loadCheckoutList()">
          <div class="stat-num">{{ checkoutStats.pending }}</div>
          <div class="stat-label">待审核</div>
        </div>
        <div class="stat-card processing" @click="checkoutFilter = 3; checkoutPage = 1; loadCheckoutList()">
          <div class="stat-num">{{ checkoutStats.handover }}</div>
          <div class="stat-label">待交接</div>
        </div>
        <div class="stat-card completed" @click="checkoutFilter = 4; checkoutPage = 1; loadCheckoutList()">
          <div class="stat-num">{{ checkoutStats.completed }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <el-card class="filter-card">
        <el-radio-group v-model="checkoutFilter" @change="checkoutPage = 1; loadCheckoutList()">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="0">待审核</el-radio-button>
          <el-radio-button :value="1">已同意</el-radio-button>
          <el-radio-button :value="3">待交接</el-radio-button>
          <el-radio-button :value="4">已完成</el-radio-button>
        </el-radio-group>
      </el-card>
      <div class="checkout-list" v-loading="checkoutLoading">
        <el-empty v-if="!checkoutLoading && checkoutList.length === 0" description="暂无退租申请" />
        <div v-else class="checkout-card" v-for="item in checkoutList" :key="item.manageId">
          <div class="checkout-header">
            <div class="checkout-info">
              <span class="checkout-house">{{ item.houseTitle }}</span>
              <span class="checkout-tenant">{{ item.tenantName }} - {{ item.tenantPhone }}</span>
            </div>
            <el-tag :type="getCheckoutStatusType(item.status)" size="small">{{ getCheckoutStatusText(item.status) }}</el-tag>
          </div>
          <div class="checkout-body">
            <div class="info-row"><span class="label">合同编号：</span>{{ item.contractNo }}</div>
            <div class="info-row"><span class="label">退租信息：</span></div>
            <div class="checkout-content">{{ item.content }}</div>
            <div class="info-row" v-if="item.responseContent"><span class="label">我的回复：</span>{{ item.responseContent }}</div>
          </div>
          <div class="checkout-footer">
            <span class="checkout-time">申请时间：{{ formatTime(item.createTime) }}</span>
            <div class="checkout-actions">
              <el-button v-if="item.status === 0" type="primary" size="small" @click="openAuditDialog(item)">审核</el-button>
              <el-button v-if="item.status === 1" type="warning" size="small" @click="openHandoverDialog(item)">安排交接</el-button>
              <el-button v-if="item.status === 3" type="success" size="small" @click="openCompleteDialog(item)">完成退租</el-button>
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-wrap" v-if="checkoutTotal > 5">
        <el-pagination background layout="prev, pager, next" :total="checkoutTotal" :page-size="5"
          v-model:current-page="checkoutPage" @current-change="loadCheckoutList" />
      </div>
    </div>

    <!-- 问题处理弹窗 -->
    <el-dialog v-model="processDialogVisible" title="处理问题" width="500px">
      <el-form :model="processForm" label-width="80px">
        <el-form-item label="问题"><div class="process-content">{{ currentIssue?.content }}</div></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="processForm.status">
            <el-radio :value="1">处理中</el-radio>
            <el-radio :value="2">已解决</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复"><el-input v-model="processForm.response" type="textarea" :rows="4" placeholder="请输入回复内容" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProcess" :loading="processSubmitting">提交</el-button>
      </template>
    </el-dialog>

    <!-- 退租审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="审核退租申请" width="500px">
      <el-form :model="auditForm" label-width="80px">
        <el-form-item label="租客">{{ currentCheckout?.tenantName }}</el-form-item>
        <el-form-item label="房源">{{ currentCheckout?.houseTitle }}</el-form-item>
        <el-form-item label="期望日期">{{ currentCheckout?.expectDate }}</el-form-item>
        <el-form-item label="退租原因"><div class="audit-reason">{{ currentCheckout?.content }}</div></el-form-item>
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="auditForm.action">
            <el-radio :value="1">同意退租</el-radio>
            <el-radio :value="2">拒绝退租</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复说明"><el-input v-model="auditForm.response" type="textarea" :rows="3" placeholder="请输入回复说明（选填）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAudit" :loading="auditSubmitting">提交</el-button>
      </template>
    </el-dialog>

    <!-- 安排交接弹窗 -->
    <el-dialog v-model="handoverDialogVisible" title="安排房屋交接" width="500px">
      <el-form :model="handoverForm" label-width="80px">
        <el-form-item label="租客">{{ currentCheckout?.tenantName }}</el-form-item>
        <el-form-item label="房源">{{ currentCheckout?.houseTitle }}</el-form-item>
        <el-form-item label="交接时间"><el-date-picker v-model="handoverForm.handoverTime" type="datetime" placeholder="选择交接时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="交接备注"><el-input v-model="handoverForm.handoverNote" type="textarea" :rows="3" placeholder="请输入交接注意事项（选填）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handoverDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandover" :loading="handoverSubmitting">确认安排</el-button>
      </template>
    </el-dialog>

    <!-- 完成退租弹窗 -->
    <el-dialog v-model="completeDialogVisible" title="完成退租" width="500px">
      <el-form :model="completeForm" label-width="100px">
        <el-form-item label="租客">{{ currentCheckout?.tenantName }}</el-form-item>
        <el-form-item label="房源">{{ currentCheckout?.houseTitle }}</el-form-item>
        <el-form-item label="房屋损坏情况"><el-input v-model="completeForm.damageDesc" type="textarea" :rows="3" placeholder="如有损坏请描述（选填）" /></el-form-item>
        <el-form-item label="押金扣除说明"><el-input v-model="completeForm.deductReason" type="textarea" :rows="3" placeholder="如需扣除押金请说明原因（选填）" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplete" :loading="completeSubmitting">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import {
  getCurrentTenantsApi, getHistoryTenantsApi, getIssueListApi, getIssueStatsApi, processIssueApi,
  getCheckoutListApi, getCheckoutStatsApi, auditCheckoutApi, arrangeHandoverApi, completeCheckoutApi
} from '@/api/landlordAfter'

const activeTab = ref('tenant')
const tenantTab = ref('current')

// 租客 - 当前
const currentTenantLoading = ref(false)
const currentTenants = ref<any[]>([])
const currentTenantPage = ref(1)
const currentTenantTotal = ref(0)
// 租客 - 历史
const historyTenantLoading = ref(false)
const historyTenants = ref<any[]>([])
const historyTenantPage = ref(1)
const historyTenantTotal = ref(0)

// 问题
const issueLoading = ref(false)
const issueList = ref<any[]>([])
const issueFilter = ref<number | undefined>(undefined)
const issuePage = ref(1)
const issueTotal = ref(0)
const issueStats = reactive({ pending: 0, processing: 0, completed: 0 })
const processDialogVisible = ref(false)
const processForm = reactive({ status: 1, response: '' })
const processSubmitting = ref(false)
const currentIssue = ref<any>(null)

// 退租
const checkoutLoading = ref(false)
const checkoutList = ref<any[]>([])
const checkoutFilter = ref<number | undefined>(undefined)
const checkoutPage = ref(1)
const checkoutTotal = ref(0)
const checkoutStats = reactive({ pending: 0, handover: 0, completed: 0 })
const currentCheckout = ref<any>(null)
const auditDialogVisible = ref(false)
const auditForm = reactive({ action: 1, response: '' })
const auditSubmitting = ref(false)
const handoverDialogVisible = ref(false)
const handoverForm = reactive({ handoverTime: '', handoverNote: '' })
const handoverSubmitting = ref(false)
const completeDialogVisible = ref(false)
const completeForm = reactive({ damageDesc: '', deductReason: '' })
const completeSubmitting = ref(false)

const formatTime = (time: string) => time ? new Date(time).toLocaleString('zh-CN') : ''
const parseImages = (images: string) => { if (!images) return []; try { return JSON.parse(images) } catch { return images.split(',') } }
const getIssueStatusText = (status: number) => ({ 0: '待处理', 1: '处理中', 2: '已解决' }[status] || '未知')
const getIssueStatusType = (status: number) => ({ 0: 'warning', 1: 'primary', 2: 'success' }[status] || 'info')
const getCheckoutStatusText = (status: number) => ({ 0: '待审核', 1: '已同意', 2: '已拒绝', 3: '待交接', 4: '已完成' }[status] || '未知')
const getCheckoutStatusType = (status: number) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'primary', 4: 'info' }[status] || 'info')

const handleTenantTabChange = () => { if (tenantTab.value === 'current') loadCurrentTenants(); else loadHistoryTenants() }

const loadCurrentTenants = async () => {
  currentTenantLoading.value = true
  try {
    const res: any = await getCurrentTenantsApi()
    const all = res || []
    currentTenantTotal.value = all.length
    const start = (currentTenantPage.value - 1) * 5
    currentTenants.value = all.slice(start, start + 5)
  } finally { currentTenantLoading.value = false }
}
const loadHistoryTenants = async () => {
  historyTenantLoading.value = true
  try {
    const res: any = await getHistoryTenantsApi({ page: historyTenantPage.value, size: 5 })
    historyTenants.value = res.records || []
    historyTenantTotal.value = res.total || 0
  } finally { historyTenantLoading.value = false }
}
const loadIssueStats = async () => { try { const res: any = await getIssueStatsApi(); Object.assign(issueStats, res) } catch (e) { console.error(e) } }
const loadIssueList = async () => {
  issueLoading.value = true
  try {
    const res: any = await getIssueListApi({ status: issueFilter.value, page: issuePage.value, size: 5 })
    issueList.value = res.records || []
    issueTotal.value = res.total || 0
  } finally { issueLoading.value = false }
}
const openProcessDialog = (issue: any) => { currentIssue.value = issue; processForm.status = issue.status === 0 ? 1 : 2; processForm.response = issue.responseContent || ''; processDialogVisible.value = true }
const submitProcess = async () => {
  processSubmitting.value = true
  try { await processIssueApi(currentIssue.value.manageId, processForm); ElMessage.success('处理成功'); processDialogVisible.value = false; loadIssueStats(); loadIssueList() }
  catch (e: any) { ElMessage.error(e.message || '处理失败') } finally { processSubmitting.value = false }
}
const loadCheckoutStats = async () => { try { const res: any = await getCheckoutStatsApi(); Object.assign(checkoutStats, res) } catch (e) { console.error(e) } }
const loadCheckoutList = async () => {
  checkoutLoading.value = true
  try {
    const res: any = await getCheckoutListApi({ status: checkoutFilter.value, page: checkoutPage.value, size: 5 })
    checkoutList.value = res.records || []
    checkoutTotal.value = res.total || 0
  } finally { checkoutLoading.value = false }
}
const openAuditDialog = (item: any) => { currentCheckout.value = item; auditForm.action = 1; auditForm.response = ''; auditDialogVisible.value = true }
const submitAudit = async () => {
  auditSubmitting.value = true
  try { await auditCheckoutApi(currentCheckout.value.manageId, auditForm); ElMessage.success('审核成功'); auditDialogVisible.value = false; loadCheckoutStats(); loadCheckoutList() }
  catch (e: any) { ElMessage.error(e.message || '审核失败') } finally { auditSubmitting.value = false }
}
const openHandoverDialog = (item: any) => { currentCheckout.value = item; handoverForm.handoverTime = ''; handoverForm.handoverNote = ''; handoverDialogVisible.value = true }
const submitHandover = async () => {
  if (!handoverForm.handoverTime) { ElMessage.warning('请选择交接时间'); return }
  handoverSubmitting.value = true
  try { await arrangeHandoverApi(currentCheckout.value.manageId, handoverForm); ElMessage.success('已安排交接'); handoverDialogVisible.value = false; loadCheckoutStats(); loadCheckoutList() }
  catch (e: any) { ElMessage.error(e.message || '操作失败') } finally { handoverSubmitting.value = false }
}
const openCompleteDialog = (item: any) => { currentCheckout.value = item; completeForm.damageDesc = ''; completeForm.deductReason = ''; completeDialogVisible.value = true }
const submitComplete = async () => {
  completeSubmitting.value = true
  try { await completeCheckoutApi(currentCheckout.value.manageId, completeForm); ElMessage.success('退租已完成'); completeDialogVisible.value = false; loadCheckoutStats(); loadCheckoutList() }
  catch (e: any) { ElMessage.error(e.message || '操作失败') } finally { completeSubmitting.value = false }
}

onMounted(() => {
  loadCurrentTenants()
  loadIssueStats()
  loadIssueList()
  loadCheckoutStats()
  loadCheckoutList()
})
</script>

<style scoped>
.after-page { padding: 20px; background: #f5f5f5; min-height: calc(100vh - 60px); }
.main-tabs { margin-bottom: 20px; }
.tab-badge { margin-left: 5px; }
.tab-content { background: #fff; border-radius: 8px; padding: 20px; }
.stats-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; margin-bottom: 20px; }
.stat-card { background: #f8f9fa; border-radius: 8px; padding: 15px; text-align: center; cursor: pointer; transition: all 0.3s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-num { font-size: 28px; font-weight: bold; margin-bottom: 5px; }
.stat-label { color: #666; font-size: 13px; }
.stat-card.pending .stat-num { color: #e6a23c; }
.stat-card.processing .stat-num { color: #409eff; }
.stat-card.completed .stat-num { color: #67c23a; }
.filter-card { margin-bottom: 20px; }
.tenant-list, .issue-list, .checkout-list { display: flex; flex-direction: column; gap: 15px; }
.tenant-card { display: flex; align-items: center; gap: 20px; padding: 15px; background: #fafafa; border-radius: 8px; }
.tenant-info { display: flex; align-items: center; gap: 12px; min-width: 180px; }
.tenant-detail { display: flex; flex-direction: column; gap: 4px; }
.tenant-name { font-weight: 500; }
.tenant-phone { font-size: 13px; color: #999; }
.contract-info { flex: 1; display: flex; flex-wrap: wrap; gap: 15px; }
.info-item { font-size: 13px; color: #666; }
.info-item .label { color: #999; }
.card-actions { display: flex; align-items: center; gap: 10px; }
.issue-card, .checkout-card { background: #fafafa; border-radius: 8px; padding: 15px; }
.issue-header, .checkout-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.issue-from { display: flex; align-items: center; gap: 10px; }
.issue-type { font-weight: 500; color: #409eff; }
.issue-tenant { font-size: 13px; color: #666; }
.issue-body, .checkout-body { margin-bottom: 12px; }
.issue-house, .checkout-info { font-size: 13px; color: #999; margin-bottom: 8px; }
.checkout-info { display: flex; gap: 15px; }
.checkout-house { font-weight: 500; }
.checkout-tenant { font-size: 13px; color: #666; }
.issue-content, .checkout-content { color: #333; line-height: 1.6; margin: 8px 0; }
.issue-images { display: flex; gap: 10px; margin-top: 10px; }
.issue-img { width: 80px; height: 80px; border-radius: 4px; cursor: pointer; }
.issue-response { margin-top: 10px; padding: 10px; background: #e8f4ff; border-radius: 4px; }
.response-label { font-size: 12px; color: #409eff; margin-bottom: 5px; }
.response-content { color: #333; font-size: 13px; }
.issue-footer, .checkout-footer { display: flex; justify-content: space-between; align-items: center; }
.issue-time, .checkout-time { font-size: 12px; color: #999; }
.info-row { font-size: 13px; color: #666; margin-bottom: 5px; }
.info-row .label { color: #999; }
.pagination-wrap { display: flex; justify-content: center; padding: 20px 0; }
.process-content, .audit-reason { color: #333; line-height: 1.6; padding: 10px; background: #f5f5f5; border-radius: 4px; }
</style>
