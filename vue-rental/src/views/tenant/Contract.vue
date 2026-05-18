<template>
  <div class="contract-page">
    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card pending" @click="filterStatus(1)">
        <div class="stat-num">{{ stats.pendingSign }}</div>
        <div class="stat-label">待确认</div>
      </div>
      <div class="stat-card accepted" @click="filterStatus(2)">
        <div class="stat-num">{{ stats.active }}</div>
        <div class="stat-label">已确认</div>
      </div>
      <div class="stat-card info" @click="filterStatus(3)">
        <div class="stat-num">{{ stats.expired }}</div>
        <div class="stat-label">已到期</div>
      </div>
    </div>

    <!-- 到期提醒横幅 -->
    <div v-if="expiringContracts.length > 0" class="expiry-banners">
      <div
        v-for="item in expiringContracts"
        :key="item.contractId"
        class="expiry-banner"
        :class="getDaysLeft(item.rentEndDate) !== null && getDaysLeft(item.rentEndDate)! <= 7 ? 'urgent' : 'warning'"
      >
        <el-icon class="banner-icon"><Bell /></el-icon>
        <span class="banner-text">
          <b>{{ item.house?.title }}</b> 合同将于
          <b>{{ item.rentEndDate }}</b> 到期，还有
          <b>{{ getDaysLeft(item.rentEndDate) }} 天</b>，请及时续租
        </span>
        <el-button
          v-if="!item.renewalStatus || item.renewalStatus === 0 || item.renewalStatus === 3"
          size="small"
          :type="getDaysLeft(item.rentEndDate) !== null && getDaysLeft(item.rentEndDate)! <= 7 ? 'danger' : 'warning'"
          @click="handleRenewal(item)"
        >立即续租</el-button>
        <el-tag v-else-if="item.renewalStatus === 1" type="warning" size="small">续租申请中</el-tag>
        <el-tag v-else-if="item.renewalStatus === 2" type="success" size="small">续租已同意</el-tag>
      </div>
    </div>

    <!-- 筛选 -->
    <el-card class="filter-card">
      <div class="filter-row">
        <span class="filter-label">状态筛选：</span>
        <el-radio-group v-model="filter.status" @change="loadList">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button :value="1">待确认</el-radio-button>
          <el-radio-button :value="2">已确认</el-radio-button>
          <el-radio-button :value="3">已到期</el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <!-- 合同列表 -->
    <div class="contract-list" v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无合同记录" />
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
            <div class="info-item"><el-icon><Calendar /></el-icon><span>租期：{{ item.rentStartDate }} ~ {{ item.rentEndDate }}</span></div>
            <div class="info-item"><el-icon><Money /></el-icon><span>月租：¥{{ item.monthlyRent }} | 押金：¥{{ item.depositAmount }}</span></div>
          </div>
          <div class="landlord-info" v-if="item.landlord">
            <el-avatar :size="24" :src="item.landlord.avatar"><el-icon><User /></el-icon></el-avatar>
            <span>房东：{{ item.landlord.realName || item.landlord.username }}</span>
            <span class="phone">{{ item.landlord.phone }}</span>
          </div>
          <div class="sign-info">
            <span :class="{ signed: item.tenantSignTime }">
              <el-icon><Check v-if="item.tenantSignTime" /><Clock v-else /></el-icon>
              租客确认：{{ item.tenantSignTime ? formatTime(item.tenantSignTime) : '未确认' }}
            </span>
            <span v-if="item.status === 2 && getDaysLeft(item.rentEndDate) !== null" class="days-left" :class="getDaysLeftClass(getDaysLeft(item.rentEndDate))">
              ⏳ 距到期还有 {{ getDaysLeft(item.rentEndDate) }} 天
            </span>
          </div>
        </div>
        <div class="item-right">
          <el-tag :type="getStatusType(item.status)" size="large">{{ getStatusText(item.status) }}</el-tag>
          <div class="actions">
            <el-button type="primary" link @click="viewDetail(item)">查看详情</el-button>
            <el-button v-if="item.hasFile" type="success" link @click="downloadContract(item.contractId)">下载合同</el-button>
            <el-button v-if="item.hasFile && isPreviewable(item.fileName)" type="primary" link @click="previewContract(item)">预览合同</el-button>
            <el-button v-if="item.status === 1 && !item.tenantSignTime" type="warning" link @click="handleSign(item)">确认合同</el-button>
            <el-button v-if="item.status === 1 && !item.tenantSignTime" type="info" link @click="contactLandlord(item)">联系房东</el-button>
            <el-button v-if="(item.status === 3 || item.status === 4)" type="danger" link @click="applyDepositRefund(item)">申请退押金</el-button>
            <el-button v-if="(item.status === 2 || item.status === 3) && (!item.renewalStatus || item.renewalStatus === 0 || item.renewalStatus === 3)" type="primary" link @click="handleRenewal(item)">申请续租</el-button>
            <el-tag v-if="item.renewalStatus === 1" type="warning" size="small">续租申请中</el-tag>
            <el-tag v-if="item.renewalStatus === 2" type="success" size="small">续租已同意</el-tag>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="10"
        v-model:current-page="page" @current-change="loadList" />
    </div>

    <!-- 合同详情弹窗 -->
    <el-dialog v-model="detailVisible" title="合同详情" width="900px">
      <div class="contract-detail" v-if="currentContract">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="合同编号">{{ currentContract.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentContract.status)">{{ getStatusText(currentContract.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="房源">{{ currentContract.house?.title }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ currentContract.house?.address }}</el-descriptions-item>
          <el-descriptions-item label="房东">{{ currentContract.landlord?.realName || currentContract.landlord?.username }}</el-descriptions-item>
          <el-descriptions-item label="房东电话">{{ currentContract.landlord?.phone }}</el-descriptions-item>
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
        <div class="detail-actions" v-if="currentContract.status === 1 && !currentContract.tenantSignTime">
          <el-button type="primary" @click="handleSign(currentContract)">确认合同</el-button>
          <el-button type="info" @click="contactLandlord(currentContract)">联系房东</el-button>
        </div>
        <div class="detail-actions" v-if="currentContract.status === 3 || currentContract.status === 4">
          <el-button type="danger" @click="applyDepositRefund(currentContract)">申请退押金</el-button>
          <el-button v-if="(currentContract.status === 2 || currentContract.status === 3) && (!currentContract.renewalStatus || currentContract.renewalStatus === 0 || currentContract.renewalStatus === 3)" type="primary" @click="handleRenewal(currentContract)">申请续租</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 身份验证弹窗 -->
    <el-dialog v-model="verifyVisible" title="身份验证" width="450px" :close-on-click-modal="false">
      <div class="verify-tips">
        <el-icon color="#e6a23c"><Warning /></el-icon>
        <span>为确保是本人操作，请验证您的身份信息</span>
      </div>
      <el-form :model="verifyForm" label-width="120px" class="verify-form">
        <el-form-item label="身份证后6位" required>
          <el-input v-model="verifyForm.idCardLast6" maxlength="6" placeholder="请输入身份证后6位" />
        </el-form-item>
        <el-form-item label="手机号后4位" required>
          <el-input v-model="verifyForm.phoneLast4" maxlength="4" placeholder="请输入手机号后4位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button type="primary" @click="handleVerify" :loading="verifying">验证</el-button>
      </template>
    </el-dialog>

    <!-- 电子签名弹窗 -->
    <el-dialog v-model="signatureVisible" title="确认签名" width="650px" :close-on-click-modal="false">
      <div class="signature-tips"><p>请在下方区域签署您的姓名，确认该合同与线下签署的一致。</p></div>
      <div class="signature-container">
        <canvas ref="signatureCanvas" class="signature-canvas" 
          @mousedown="startDrawing" @mousemove="draw" @mouseup="stopDrawing" @mouseleave="stopDrawing"
          @touchstart="startDrawingTouch" @touchmove="drawTouch" @touchend="stopDrawing" />
      </div>
      <div class="signature-actions"><el-button @click="clearSignature">清除重签</el-button></div>
      <template #footer>
        <el-button @click="signatureVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSign" :loading="signing">确认</el-button>
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
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Clock, User, Warning, Picture, Money, Check, Document, Bell } from '@element-plus/icons-vue'
import { getContractListApi, getContractDetailApi, getContractStatsApi, signContractWithSignatureApi, downloadContractFileApi, verifyIdentityApi, applyRenewalApi } from '@/api/tenantRental'
import { applyDepositRefundApi } from '@/api/tenantOrder'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const stats = reactive({ pendingSign: 0, active: 0, expired: 0 })
const filter = reactive({ status: undefined as number | undefined })
const page = ref(1)
const total = ref(0)

// 即将到期合同（≤30天，已确认状态）
const expiringContracts = computed(() =>
  list.value.filter(item => {
    if (item.status !== 2) return false
    const days = getDaysLeft(item.rentEndDate)
    return days !== null && days <= 30
  }).sort((a, b) => (getDaysLeft(a.rentEndDate) ?? 999) - (getDaysLeft(b.rentEndDate) ?? 999))
)

const detailVisible = ref(false)
const currentContract = ref<any>(null)
const verifyVisible = ref(false)
const verifyForm = reactive({ idCardLast6: '', phoneLast4: '' })
const verifying = ref(false)
const signingContract = ref<any>(null)
const signatureVisible = ref(false)
const signatureCanvas = ref<HTMLCanvasElement | null>(null)
const signing = ref(false)
let isDrawing = false
let ctx: CanvasRenderingContext2D | null = null

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

const getPreviewType = (fileName: string): 'pdf' | 'image' | 'other' => {
  if (!fileName) return 'other'
  const ext = fileName.toLowerCase().split('.').pop()
  if (ext === 'pdf') return 'pdf'
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')) return 'image'
  return 'other'
}

const previewContract = (item: any) => {
  previewContractId.value = item.contractId
  previewType.value = getPreviewType(item.fileName)
  // 构建预览URL - 使用后端的文件下载接口
  const tokenKey = 'token_tenant'
  const token = localStorage.getItem(tokenKey)
  previewUrl.value = `/api/tenant/rental/contract/download/${item.contractId}?token=${token}`
  previewVisible.value = true
}

const getFirstImage = (images: string) => {
  if (!images) return ''
  try { return JSON.parse(images)[0] || '' } catch { return images.split(',')[0] || '' }
}
const formatTime = (time: string) => time ? new Date(time).toLocaleString('zh-CN') : ''
const getStatusText = (status: number) => ({ 0: '草稿', 1: '待确认', 2: '已确认', 3: '已到期', 4: '已终止' }[status] || '未知')
const getStatusType = (status: number) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: '', 4: 'danger' }[status] || 'info')

const getDaysLeft = (endDate: string) => {
  if (!endDate) return null
  const end = new Date(endDate)
  const now = new Date()
  const diff = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  return diff >= 0 ? diff : null
}
const getDaysLeftClass = (days: number | null) => {
  if (days === null) return ''
  if (days <= 7) return 'urgent'
  if (days <= 30) return 'warning'
  return 'normal'
}

const loadStats = async () => {
  try { const res: any = await getContractStatsApi(); Object.assign(stats, res) } catch (e) { console.error(e) }
}
const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getContractListApi({ status: filter.status, page: page.value, size: 10 })
    list.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}
const filterStatus = (status: number | undefined) => { filter.status = status; page.value = 1; loadList() }
const viewDetail = async (item: any) => {
  try { const res: any = await getContractDetailApi(item.contractId); currentContract.value = res; detailVisible.value = true } catch (e) { console.error(e) }
}
const downloadContract = (contractId: number) => { downloadContractFileApi(contractId) }
const handleSign = async (item: any) => {
  // 检查实名认证状态
  await userStore.refreshUserInfo()
  if (!userStore.isRealnameVerified) {
    try {
      await ElMessageBox.confirm(
        '确认合同需要先完成实名认证，是否前往认证？',
        '需要实名认证',
        { confirmButtonText: '去认证', cancelButtonText: '取消', type: 'warning' }
      )
      router.push('/tenant/profile/realname')
    } catch { /* cancel */ }
    return
  }
  signingContract.value = item
  verifyForm.idCardLast6 = ''
  verifyForm.phoneLast4 = ''
  verifyVisible.value = true
}
const handleVerify = async () => {
  if (!verifyForm.idCardLast6 || verifyForm.idCardLast6.length !== 6) { ElMessage.warning('请输入正确的身份证后6位'); return }
  if (!verifyForm.phoneLast4 || verifyForm.phoneLast4.length !== 4) { ElMessage.warning('请输入正确的手机号后4位'); return }
  verifying.value = true
  try {
    await verifyIdentityApi({ idCardLast6: verifyForm.idCardLast6, phoneLast4: verifyForm.phoneLast4 })
    ElMessage.success('身份验证通过')
    verifyVisible.value = false
    signatureVisible.value = true
    await nextTick()
    initCanvas()
  } catch (e: any) { ElMessage.error(e.message || '身份验证失败') } finally { verifying.value = false }
}

const initCanvas = () => {
  const canvas = signatureCanvas.value
  if (!canvas) return
  canvas.width = 600; canvas.height = 200
  ctx = canvas.getContext('2d')
  if (ctx) { ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, canvas.width, canvas.height); ctx.strokeStyle = '#000'; ctx.lineWidth = 2; ctx.lineCap = 'round'; ctx.lineJoin = 'round' }
}
const startDrawing = (e: MouseEvent) => { if (!ctx) return; isDrawing = true; ctx.beginPath(); const rect = signatureCanvas.value!.getBoundingClientRect(); ctx.moveTo(e.clientX - rect.left, e.clientY - rect.top) }
const draw = (e: MouseEvent) => { if (!isDrawing || !ctx) return; const rect = signatureCanvas.value!.getBoundingClientRect(); ctx.lineTo(e.clientX - rect.left, e.clientY - rect.top); ctx.stroke() }
const startDrawingTouch = (e: TouchEvent) => { e.preventDefault(); if (!ctx) return; isDrawing = true; ctx.beginPath(); const rect = signatureCanvas.value!.getBoundingClientRect(); const touch = e.touches[0]; ctx.moveTo(touch.clientX - rect.left, touch.clientY - rect.top) }
const drawTouch = (e: TouchEvent) => { e.preventDefault(); if (!isDrawing || !ctx) return; const rect = signatureCanvas.value!.getBoundingClientRect(); const touch = e.touches[0]; ctx.lineTo(touch.clientX - rect.left, touch.clientY - rect.top); ctx.stroke() }
const stopDrawing = () => { isDrawing = false }
const clearSignature = () => { initCanvas() }
const isCanvasEmpty = () => {
  const canvas = signatureCanvas.value; if (!canvas) return true
  const ctx = canvas.getContext('2d'); if (!ctx) return true
  const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height); const data = imageData.data
  for (let i = 0; i < data.length; i += 4) { if (data[i] !== 255 || data[i + 1] !== 255 || data[i + 2] !== 255) return false }
  return true
}
const confirmSign = async () => {
  if (isCanvasEmpty()) { ElMessage.warning('请先签署您的姓名'); return }
  try {
    await ElMessageBox.confirm('确认该合同与线下签署的一致吗？确认后将生成支付订单。', '确认合同', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '再看看' })
    signing.value = true
    const signature = signatureCanvas.value!.toDataURL('image/png')
    await signContractWithSignatureApi(signingContract.value.contractId, { signature })
    ElMessage.success('合同确认成功')
    signatureVisible.value = false
    detailVisible.value = false
    loadStats(); loadList()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '确认失败') } finally { signing.value = false }
}
const contactLandlord = (item: any) => { router.push({ path: '/tenant/chat', query: { houseId: item.houseId } }) }

const applyDepositRefund = async (item: any) => {
  try {
    await ElMessageBox.confirm(
      `确定申请退还【${item.house?.title}】的押金 ¥${item.depositAmount} 吗？`,
      '申请退押金',
      { confirmButtonText: '确定申请', cancelButtonText: '取消', type: 'info' }
    )
    await applyDepositRefundApi(item.contractId)
    ElMessage.success('押金退还申请已提交，请等待房东处理')
  } catch (e: any) {
    if (e !== 'cancel' && e !== undefined) ElMessage.error(e.message || '申请失败')
  }
}

const handleRenewal = async (item: any) => {
  try {
    await ElMessageBox.confirm(
      `确定申请续租【${item.house?.title}】吗？房东同意后会上传新合同。`,
      '申请续租',
      { confirmButtonText: '确定申请', cancelButtonText: '取消', type: 'info' }
    )
    await applyRenewalApi(item.contractId)
    ElMessage.success('续租申请已提交，请等待房东处理')
    detailVisible.value = false
    loadStats(); loadList()
  } catch (e: any) {
    if (e !== 'cancel' && e !== undefined) ElMessage.error(e.message || '申请失败')
  }
}

onMounted(() => { loadStats(); loadList() })
</script>

<style scoped>
/* 到期提醒横幅 */
.expiry-banners { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
.expiry-banner { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-radius: 8px; font-size: 14px; }
.expiry-banner.warning { background: #fdf6ec; border: 1px solid #f5dab1; color: #e6a23c; }
.expiry-banner.urgent { background: #fef0f0; border: 1px solid #fbc4c4; color: #f56c6c; }
.banner-icon { font-size: 18px; flex-shrink: 0; }
.banner-text { flex: 1; }
.contract-page { padding: 20px; background: #f5f5f5; min-height: calc(100vh - 60px); }
.stats-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 20px; text-align: center; cursor: pointer; transition: all 0.3s; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-num { font-size: 32px; font-weight: bold; margin-bottom: 8px; }
.stat-label { color: #666; font-size: 14px; }
.stat-card.pending .stat-num { color: #e6a23c; }
.stat-card.accepted .stat-num { color: #67c23a; }
.stat-card.info .stat-num { color: #909399; }
.filter-card { margin-bottom: 20px; }
.filter-row { display: flex; align-items: center; gap: 15px; }
.filter-label { color: #666; }
.contract-list { display: flex; flex-direction: column; gap: 15px; }
.contract-item { display: flex; background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.item-left { width: 180px; height: 135px; flex-shrink: 0; margin-right: 20px; }
.house-image { width: 100%; height: 100%; border-radius: 6px; }
.image-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f5f5; color: #ccc; font-size: 30px; }
.item-center { flex: 1; min-width: 0; }
.contract-no { font-size: 13px; color: #999; margin-bottom: 5px; }
.house-title { font-size: 16px; font-weight: 500; color: #333; margin-bottom: 10px; }
.contract-info { display: flex; gap: 25px; margin-bottom: 10px; }
.info-item { display: flex; align-items: center; gap: 5px; color: #409eff; font-size: 14px; }
.landlord-info { display: flex; align-items: center; gap: 8px; color: #666; font-size: 13px; margin-bottom: 8px; }
.landlord-info .phone { color: #409eff; }
.sign-info { display: flex; gap: 20px; font-size: 13px; color: #999; }
.sign-info span { display: flex; align-items: center; gap: 4px; }
.sign-info .signed { color: #67c23a; }
.days-left { font-weight: 500; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.days-left.urgent { color: #f56c6c; background: #fef0f0; }
.days-left.warning { color: #e6a23c; background: #fdf6ec; }
.days-left.normal { color: #67c23a; background: #f0f9eb; }
.item-right { width: 150px; flex-shrink: 0; display: flex; flex-direction: column; align-items: flex-end; gap: 10px; }
.actions { display: flex; flex-direction: column; gap: 5px; align-items: flex-end; }
.pagination-wrap { display: flex; justify-content: center; padding: 30px 0; }
.detail-actions { margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; display: flex; gap: 10px; justify-content: center; }
.verify-tips { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: #fdf6ec; border-radius: 4px; margin-bottom: 20px; color: #e6a23c; }
.verify-form { padding: 0 20px; }
.signature-tips { padding: 10px 0; color: #606266; font-size: 14px; }
.signature-container { border: 2px dashed #dcdfe6; border-radius: 4px; margin: 10px 0; background: #fafafa; }
.signature-canvas { display: block; width: 100%; height: 200px; cursor: crosshair; touch-action: none; }
.signature-actions { display: flex; justify-content: flex-end; margin-top: 10px; }

/* 合同预览 */
.preview-container { min-height: 500px; display: flex; align-items: center; justify-content: center; }
.preview-iframe { width: 100%; height: 70vh; border: none; border-radius: 4px; }
.preview-image { max-width: 100%; max-height: 70vh; }
.preview-unsupported { color: #999; font-size: 16px; text-align: center; padding: 60px; }
</style>
