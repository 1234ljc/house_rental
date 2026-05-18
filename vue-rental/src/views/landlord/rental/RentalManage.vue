<template>
  <div class="rental-manage-page">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card pending" @click="filterStatus(1)">
        <div class="stat-num">{{ stats.pending }}</div>
        <div class="stat-label">待确认</div>
      </div>
      <div class="stat-card success" @click="filterStatus(2)">
        <div class="stat-num">{{ stats.active }}</div>
        <div class="stat-label">已确认</div>
      </div>
      <div class="stat-card info" @click="filterStatus(3)">
        <div class="stat-num">{{ stats.expired }}</div>
        <div class="stat-label">已到期</div>
      </div>
      <div class="stat-card total" @click="filterStatus(undefined)">
        <div class="stat-num">{{ stats.total }}</div>
        <div class="stat-label">全部合同</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <el-card class="filter-card">
      <div class="filter-row">
        <el-select v-model="filter.status" placeholder="合同状态" clearable style="width:130px" @change="loadList">
          <el-option label="待确认" :value="1" />
          <el-option label="已确认" :value="2" />
          <el-option label="已到期" :value="3" />
          <el-option label="已终止" :value="4" />
        </el-select>
        <el-select v-model="filter.houseId" placeholder="选择房源" clearable style="width:200px" @change="loadList">
          <el-option v-for="h in houseOptions" :key="h.houseId" :label="h.title" :value="h.houseId" />
        </el-select>
        <el-input v-model="filter.keyword" placeholder="搜索租客姓名/手机号" clearable style="width:200px" @keyup.enter="loadList">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="loadList">搜索</el-button>
        <el-button type="success" @click="showCreateDialog">
          <el-icon><Plus /></el-icon> 上传合同
        </el-button>
      </div>
    </el-card>

    <!-- 合同列表 -->
    <div class="contract-list" v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无合同记录">
        <el-button type="primary" @click="showCreateDialog">上传第一份合同</el-button>
      </el-empty>

      <div v-else class="contract-item" v-for="item in list" :key="item.contractId">
        <div class="item-left">
          <el-image :src="getFirstImage(item.house?.images)" fit="cover" class="house-image">
            <template #error><div class="image-placeholder"><el-icon><Picture /></el-icon></div></template>
          </el-image>
        </div>
        <div class="item-center">
          <div class="contract-no">合同编号：{{ item.contractNo }}</div>
          <div class="house-title">{{ item.house?.title }}</div>
          <div class="contract-info">
            <div class="info-item"><el-icon><Calendar /></el-icon><span>{{ item.rentStartDate }} ~ {{ item.rentEndDate }}</span></div>
            <div class="info-item"><el-icon><Money /></el-icon><span>月租 ¥{{ item.monthlyRent }} | 押金 ¥{{ item.depositAmount }}</span></div>
          </div>
          <div class="tenant-info" v-if="item.tenant">
            <el-avatar :size="24" :src="item.tenant.avatar"><el-icon><User /></el-icon></el-avatar>
            <span>租客：{{ item.tenant.realName || item.tenant.username }}</span>
            <span class="phone">{{ item.tenant.phone }}</span>
          </div>
          <div class="sign-info">
            <span :class="{ signed: item.tenantSignTime }">
              <el-icon><Check v-if="item.tenantSignTime" /><Clock v-else /></el-icon>
              租客确认：{{ item.tenantSignTime ? formatTime(item.tenantSignTime) : '未确认' }}
            </span>
          </div>
        </div>
        <div class="item-right">
          <el-tag :type="getStatusType(item.status)" size="large">{{ getStatusText(item.status) }}</el-tag>
          <div class="actions">
            <el-button type="primary" link @click="viewDetail(item)">查看详情</el-button>
            <el-button v-if="item.hasFile" type="success" link @click="downloadContract(item.contractId)">下载合同</el-button>
            <el-button v-if="item.hasFile && isPreviewable(item.fileName)" type="primary" link @click="previewContractFile(item)">预览合同</el-button>
            <el-button v-if="item.status === 1" type="info" link @click="handleReupload(item)">重新上传</el-button>
            <el-button type="default" link @click="handleChat(item)">联系租客</el-button>
            <el-button v-if="item.renewalStatus === 1" type="warning" link @click="showRenewalDialog(item)">处理续租</el-button>
            <el-tag v-if="item.renewalStatus === 2" type="success" size="small">已同意续租</el-tag>
            <el-tag v-if="item.renewalStatus === 3" type="info" size="small">已拒绝续租</el-tag>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="10"
        v-model:current-page="page" @current-change="loadList" />
    </div>

    <!-- 合同详情弹窗 -->
    <el-dialog v-model="detailVisible" title="合同详情" width="800px">
      <div v-if="currentContract">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="合同编号">{{ currentContract.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentContract.status)">{{ getStatusText(currentContract.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="房源">{{ currentContract.house?.title }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ currentContract.house?.address }}</el-descriptions-item>
          <el-descriptions-item label="租客">{{ currentContract.tenant?.realName || currentContract.tenant?.username }}</el-descriptions-item>
          <el-descriptions-item label="租客电话">{{ currentContract.tenant?.phone }}</el-descriptions-item>
          <el-descriptions-item label="租期">{{ currentContract.rentStartDate }} ~ {{ currentContract.rentEndDate }}</el-descriptions-item>
          <el-descriptions-item label="付款日">每月{{ currentContract.paymentDay }}日</el-descriptions-item>
          <el-descriptions-item label="月租金">¥{{ currentContract.monthlyRent }}</el-descriptions-item>
          <el-descriptions-item label="押金">¥{{ currentContract.depositAmount }}</el-descriptions-item>
          <el-descriptions-item label="租客确认">{{ currentContract.tenantSignTime ? formatTime(currentContract.tenantSignTime) : '未确认' }}</el-descriptions-item>
          <el-descriptions-item label="合同文件" v-if="currentContract.hasFile">
            <el-link type="primary" @click="downloadContract(currentContract.contractId)">
              <el-icon><Document /></el-icon> {{ currentContract.fileName || '下载合同文件' }}
            </el-link>
          </el-descriptions-item>
        </el-descriptions>
        <div class="detail-actions" v-if="currentContract.status === 1">
          <el-button type="info" @click="handleReupload(currentContract)">重新上传合同</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 创建合同弹窗 -->
    <el-dialog v-model="createVisible" title="上传合同" width="600px" :close-on-click-modal="false">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="110px">
        <el-form-item label="选择房源" prop="houseId">
          <el-select v-model="createForm.houseId" placeholder="请选择房源" style="width:100%" @change="onHouseChange">
            <el-option v-for="h in houseOptions" :key="h.houseId" :label="h.title" :value="h.houseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择租客" prop="tenantId">
          <el-select v-model="createForm.tenantId" placeholder="请选择租客" filterable style="width:100%">
            <el-option v-for="t in tenantOptions" :key="t.userId"
              :label="`${t.realName || t.username} (${t.phone})`" :value="t.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="起租日期" prop="rentStartDate">
          <el-date-picker v-model="createForm.rentStartDate" type="date" placeholder="选择起租日期"
            value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="租赁月数" prop="rentMonths">
          <el-input-number v-model="createForm.rentMonths" :min="1" :max="36" style="width:100%" />
        </el-form-item>
        <el-form-item label="月租金(元)" prop="monthlyRent">
          <el-input-number v-model="createForm.monthlyRent" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="押金(元)" prop="depositAmount">
          <el-input-number v-model="createForm.depositAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="每月付款日" prop="paymentDay">
          <el-input-number v-model="createForm.paymentDay" :min="1" :max="28" style="width:100%" />
        </el-form-item>
        <el-form-item label="合同文件" prop="file">
          <el-upload ref="createUploadRef" :auto-upload="false" :limit="1"
            :on-change="handleCreateFileChange" :on-remove="() => createFile = null"
            accept=".doc,.docx,.pdf">
            <el-button type="primary"><el-icon><Upload /></el-icon> 选择文件</el-button>
            <template #tip><div class="upload-tip">支持 doc, docx, pdf，不超过 20MB</div></template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="creating">上传并发送给租客</el-button>
      </template>
    </el-dialog>

    <!-- 重新上传弹窗 -->
    <el-dialog v-model="reuploadVisible" title="重新上传合同" width="500px">
      <el-upload ref="reuploadRef" :auto-upload="false" :limit="1"
        :on-change="handleReuploadFileChange" :on-remove="() => reuploadFile = null"
        accept=".doc,.docx,.pdf">
        <el-button type="primary"><el-icon><Upload /></el-icon> 选择新合同文件</el-button>
        <template #tip><div class="upload-tip">支持 doc, docx, pdf，不超过 20MB</div></template>
      </el-upload>
      <template #footer>
        <el-button @click="reuploadVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReupload" :loading="reuploading">确认上传</el-button>
      </template>
    </el-dialog>

    <!-- 续租处理弹窗 -->
    <el-dialog v-model="renewalVisible" title="处理续租申请" width="600px" :close-on-click-modal="false">
      <div class="renewal-info" v-if="renewalContract">
        <el-alert type="info" :closable="false" style="margin-bottom:16px">
          租客 <b>{{ renewalContract.tenant?.realName || renewalContract.tenant?.username }}</b> 申请续租
          【{{ renewalContract.house?.title }}】，当前合同到期日：{{ renewalContract.rentEndDate }}
        </el-alert>
        <el-tabs v-model="renewalAction">
          <el-tab-pane label="同意续租（上传新合同）" name="approve">
            <el-form :model="renewalForm" label-width="110px" style="margin-top:10px">
              <el-form-item label="起租日期" required>
                <el-date-picker v-model="renewalForm.rentStartDate" type="date" placeholder="选择起租日期"
                  value-format="YYYY-MM-DD" style="width:100%" />
              </el-form-item>
              <el-form-item label="租赁月数" required>
                <el-input-number v-model="renewalForm.rentMonths" :min="1" :max="36" style="width:100%" />
              </el-form-item>
              <el-form-item label="月租金(元)">
                <el-input-number v-model="renewalForm.monthlyRent" :min="0" :precision="2" style="width:100%" />
              </el-form-item>
              <el-form-item label="押金(元)">
                <el-input-number v-model="renewalForm.depositAmount" :min="0" :precision="2" style="width:100%" />
              </el-form-item>
              <el-form-item label="每月付款日">
                <el-input-number v-model="renewalForm.paymentDay" :min="1" :max="28" style="width:100%" />
              </el-form-item>
              <el-form-item label="新合同文件" required>
                <el-upload ref="renewalUploadRef" :auto-upload="false" :limit="1"
                  :on-change="handleRenewalFileChange" :on-remove="() => renewalFile = null"
                  accept=".doc,.docx,.pdf">
                  <el-button type="primary"><el-icon><Upload /></el-icon> 选择文件</el-button>
                  <template #tip><div class="upload-tip">支持 doc, docx, pdf，不超过 20MB</div></template>
                </el-upload>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="拒绝续租" name="reject">
            <el-form label-width="80px" style="margin-top:10px">
              <el-form-item label="拒绝原因">
                <el-input v-model="renewalRejectReason" type="textarea" :rows="3" placeholder="可选，填写拒绝原因" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="renewalVisible = false">取消</el-button>
        <el-button v-if="renewalAction === 'approve'" type="primary" @click="submitRenewalApprove" :loading="renewalSubmitting">同意并上传新合同</el-button>
        <el-button v-if="renewalAction === 'reject'" type="danger" @click="submitRenewalReject" :loading="renewalSubmitting">拒绝续租</el-button>
      </template>
    </el-dialog>

    <!-- 合同文件预览弹窗 -->
    <el-dialog v-model="previewVisible" title="合同预览" width="900px" top="5vh">
      <div class="preview-container">
        <iframe v-if="previewType === 'pdf'" :src="previewUrl" class="preview-iframe" />
        <el-image v-else-if="previewType === 'image'" :src="previewUrl" fit="contain" class="preview-image" :preview-src-list="[previewUrl]" />
        <div v-else class="preview-unsupported">该文件格式不支持在线预览，请下载查看</div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadContract(previewContractId)">下载文件</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Upload, Calendar, Money, User, Picture, Check, Clock, Document } from '@element-plus/icons-vue'
import {
  getContractStatsApi, getContractListApi, getContractDetailApi,
  downloadContractApi, reuploadContractApi, createContractDirectApi, getChatTenantsApi,
  approveRenewalApi, rejectRenewalApi
} from '@/api/landlordRental'
import { getHouseListApi } from '@/api/landlordHouse'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const stats = reactive({ pending: 0, active: 0, expired: 0, total: 0 })
const filter = reactive({ status: undefined as number | undefined, houseId: undefined as number | undefined, keyword: '' })
const page = ref(1)
const total = ref(0)
const houseOptions = ref<any[]>([])

// 详情
const detailVisible = ref(false)
const currentContract = ref<any>(null)

// 创建合同
const createVisible = ref(false)
const createFormRef = ref()
const createUploadRef = ref()
const createFile = ref<File | null>(null)
const creating = ref(false)
const tenantOptions = ref<any[]>([])
const createForm = reactive({
  houseId: undefined as number | undefined,
  tenantId: undefined as number | undefined,
  rentStartDate: '',
  rentMonths: 12,
  monthlyRent: 0,
  depositAmount: 0,
  paymentDay: 1
})
const createRules = {
  houseId: [{ required: true, message: '请选择房源' }],
  tenantId: [{ required: true, message: '请选择租客' }],
  rentStartDate: [{ required: true, message: '请选择起租日期' }],
  rentMonths: [{ required: true, message: '请填写租赁月数' }]
}

// 重新上传
const reuploadVisible = ref(false)
const reuploadRef = ref()
const reuploadFile = ref<File | null>(null)
const reuploading = ref(false)
const reuploadContractId = ref<number | null>(null)

const getFirstImage = (images: string) => {
  if (!images) return ''
  try { return JSON.parse(images)[0] || '' } catch { return images.split(',')[0] || '' }
}
const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''
const getStatusText = (s: number) => ({ 0: '草稿', 1: '待确认', 2: '已确认', 3: '已到期', 4: '已终止' }[s] || '未知')
const getStatusType = (s: number) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: '', 4: 'danger' }[s] || 'info')

// 合同预览
const previewVisible = ref(false)
const previewUrl = ref('')
const previewType = ref<'pdf' | 'image' | 'other'>('other')
const previewContractId = ref(0)

const isPreviewable = (fileName: string) => {
  if (!fileName) return false
  const ext = fileName.toLowerCase().split('.').pop()
  return ['pdf', 'jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')
}

const previewContractFile = (item: any) => {
  previewContractId.value = item.contractId
  const ext = (item.fileName || '').toLowerCase().split('.').pop()
  previewType.value = ext === 'pdf' ? 'pdf' : ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '') ? 'image' : 'other'
  const token = localStorage.getItem('token_landlord')
  previewUrl.value = `/api/landlord/rental/contract/download/${item.contractId}?token=${token}`
  previewVisible.value = true
}

const filterStatus = (s: number | undefined) => { filter.status = s; page.value = 1; loadList() }

const loadStats = async () => {
  try {
    const res: any = await getContractStatsApi()
    stats.pending = res.pending || 0
    stats.active = res.active || 0
    stats.expired = res.expired || 0
    stats.total = (res.pending || 0) + (res.active || 0) + (res.expired || 0) + (res.terminated || 0) + (res.draft || 0)
  } catch (e) { console.error(e) }
}

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getContractListApi({ status: filter.status, houseId: filter.houseId, keyword: filter.keyword, page: page.value, size: 10 })
    list.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const loadHouses = async () => {
  try {
    const res: any = await getHouseListApi({ page: 1, size: 100, status: 1 })
    houseOptions.value = res.records || res || []
  } catch (e) { console.error(e) }
}

const loadChatTenants = async () => {
  try {
    const res: any = await getChatTenantsApi()
    tenantOptions.value = res || []
  } catch (e) { console.error(e) }
}

const viewDetail = async (item: any) => {
  try {
    const res: any = await getContractDetailApi(item.contractId)
    currentContract.value = res
    detailVisible.value = true
  } catch (e) { console.error(e) }
}

const downloadContract = (contractId: number) => { downloadContractApi(contractId) }

const showCreateDialog = () => {
  Object.assign(createForm, { houseId: undefined, tenantId: undefined, rentStartDate: '', rentMonths: 12, monthlyRent: 0, depositAmount: 0, paymentDay: 1 })
  createFile.value = null
  createUploadRef.value?.clearFiles()
  createVisible.value = true
}

const onHouseChange = (houseId: number) => {
  const house = houseOptions.value.find(h => h.houseId === houseId)
  if (house) {
    createForm.monthlyRent = house.rentPrice || 0
    // 计算押金
    const dt = house.depositType || ''
    if (dt.includes('押一')) createForm.depositAmount = house.rentPrice
    else if (dt.includes('押二')) createForm.depositAmount = house.rentPrice * 2
    else if (dt.includes('押三')) createForm.depositAmount = house.rentPrice * 3
    else createForm.depositAmount = house.rentPrice
  }
}

const searchTenant = async (keyword: string) => {
  // 已改为下拉框，此方法保留兼容但不再使用
}

const handleCreateFileChange = (file: any) => { createFile.value = file.raw }

const submitCreate = async () => {
  await createFormRef.value?.validate()
  if (!createFile.value) { ElMessage.warning('请选择合同文件'); return }
  creating.value = true
  try {
    const fd = new FormData()
    fd.append('file', createFile.value)
    fd.append('houseId', String(createForm.houseId))
    fd.append('tenantId', String(createForm.tenantId))
    fd.append('rentStartDate', createForm.rentStartDate)
    fd.append('rentMonths', String(createForm.rentMonths))
    fd.append('monthlyRent', String(createForm.monthlyRent))
    fd.append('depositAmount', String(createForm.depositAmount))
    fd.append('paymentDay', String(createForm.paymentDay))
    await createContractDirectApi(fd)
    ElMessage.success('合同已上传并发送给租客')
    createVisible.value = false
    loadStats(); loadList()
  } catch (e: any) { ElMessage.error(e.message || '上传失败') } finally { creating.value = false }
}

const handleReupload = (item: any) => {
  reuploadContractId.value = item.contractId
  reuploadFile.value = null
  reuploadRef.value?.clearFiles()
  reuploadVisible.value = true
  detailVisible.value = false
}
const handleReuploadFileChange = (file: any) => { reuploadFile.value = file.raw }
const submitReupload = async () => {
  if (!reuploadFile.value) { ElMessage.warning('请选择文件'); return }
  reuploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', reuploadFile.value)
    await reuploadContractApi(reuploadContractId.value!, fd)
    ElMessage.success('合同已重新上传')
    reuploadVisible.value = false
    loadList()
  } catch (e: any) { ElMessage.error(e.message || '上传失败') } finally { reuploading.value = false }
}

const handleChat = (item: any) => {
  router.push({ path: '/landlord/chat', query: { tenantId: item.tenant?.userId, houseId: item.houseId } })
}

// 续租处理
const renewalVisible = ref(false)
const renewalContract = ref<any>(null)
const renewalAction = ref('approve')
const renewalSubmitting = ref(false)
const renewalFile = ref<File | null>(null)
const renewalUploadRef = ref()
const renewalRejectReason = ref('')
const renewalForm = reactive({
  rentStartDate: '',
  rentMonths: 12,
  monthlyRent: 0,
  depositAmount: 0,
  paymentDay: 1
})

const showRenewalDialog = (item: any) => {
  renewalContract.value = item
  renewalAction.value = 'approve'
  renewalRejectReason.value = ''
  renewalFile.value = null
  renewalUploadRef.value?.clearFiles()
  // 预填：起租日期为原合同到期日次日，租金/押金/付款日沿用
  const endDate = item.rentEndDate ? new Date(item.rentEndDate) : new Date()
  endDate.setDate(endDate.getDate() + 1)
  renewalForm.rentStartDate = endDate.toISOString().split('T')[0]
  renewalForm.rentMonths = 12
  renewalForm.monthlyRent = item.monthlyRent || 0
  renewalForm.depositAmount = item.depositAmount || 0
  renewalForm.paymentDay = item.paymentDay || 1
  renewalVisible.value = true
}

const handleRenewalFileChange = (file: any) => { renewalFile.value = file.raw }

const submitRenewalApprove = async () => {
  if (!renewalForm.rentStartDate) { ElMessage.warning('请选择起租日期'); return }
  if (!renewalFile.value) { ElMessage.warning('请上传新合同文件'); return }
  renewalSubmitting.value = true
  try {
    const fd = new FormData()
    fd.append('file', renewalFile.value)
    fd.append('rentStartDate', renewalForm.rentStartDate)
    fd.append('rentMonths', String(renewalForm.rentMonths))
    fd.append('monthlyRent', String(renewalForm.monthlyRent))
    fd.append('depositAmount', String(renewalForm.depositAmount))
    fd.append('paymentDay', String(renewalForm.paymentDay))
    await approveRenewalApi(renewalContract.value.contractId, fd)
    ElMessage.success('已同意续租，新合同已发送给租客')
    renewalVisible.value = false
    loadStats(); loadList()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') } finally { renewalSubmitting.value = false }
}

const submitRenewalReject = async () => {
  renewalSubmitting.value = true
  try {
    await rejectRenewalApi(renewalContract.value.contractId, { reason: renewalRejectReason.value })
    ElMessage.success('已拒绝续租申请')
    renewalVisible.value = false
    loadStats(); loadList()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') } finally { renewalSubmitting.value = false }
}

onMounted(() => { loadStats(); loadList(); loadHouses(); loadChatTenants() })
</script>

<style scoped>
.rental-manage-page { padding: 20px; background: #f5f5f5; min-height: calc(100vh - 60px); }
.stats-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 20px; text-align: center; cursor: pointer; transition: all 0.3s; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-num { font-size: 32px; font-weight: bold; margin-bottom: 8px; }
.stat-label { color: #666; font-size: 14px; }
.stat-card.pending .stat-num { color: #e6a23c; }
.stat-card.success .stat-num { color: #67c23a; }
.stat-card.info .stat-num { color: #909399; }
.stat-card.total .stat-num { color: #409eff; }
.filter-card { margin-bottom: 20px; }
.filter-row { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.contract-list { display: flex; flex-direction: column; gap: 15px; }
.contract-item { display: flex; background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.item-left { width: 160px; height: 120px; flex-shrink: 0; margin-right: 20px; }
.house-image { width: 100%; height: 100%; border-radius: 6px; }
.image-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f5f5; color: #ccc; font-size: 30px; }
.item-center { flex: 1; min-width: 0; }
.contract-no { font-size: 13px; color: #999; margin-bottom: 5px; }
.house-title { font-size: 16px; font-weight: 500; color: #333; margin-bottom: 10px; }
.contract-info { display: flex; gap: 20px; margin-bottom: 10px; }
.info-item { display: flex; align-items: center; gap: 5px; color: #409eff; font-size: 14px; }
.tenant-info { display: flex; align-items: center; gap: 8px; color: #666; font-size: 13px; margin-bottom: 8px; }
.tenant-info .phone { color: #409eff; }
.sign-info { display: flex; gap: 20px; font-size: 13px; color: #999; }
.sign-info span { display: flex; align-items: center; gap: 4px; }
.sign-info .signed { color: #67c23a; }
.item-right { width: 150px; flex-shrink: 0; display: flex; flex-direction: column; align-items: flex-end; gap: 10px; }
.actions { display: flex; flex-direction: column; gap: 5px; align-items: flex-end; }
.pagination-wrap { display: flex; justify-content: center; padding: 30px 0; }
.detail-actions { margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; display: flex; gap: 10px; justify-content: center; }
.upload-tip { color: #999; font-size: 12px; margin-top: 5px; }
.preview-container { min-height: 500px; display: flex; align-items: center; justify-content: center; }
.preview-iframe { width: 100%; height: 70vh; border: none; border-radius: 4px; }
.preview-image { max-width: 100%; max-height: 70vh; }
.preview-unsupported { color: #999; font-size: 16px; text-align: center; padding: 60px; }
</style>
