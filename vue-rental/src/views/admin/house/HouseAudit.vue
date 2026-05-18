<template>
  <div class="house-audit-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card pending">
          <div class="stat-content">
            <div class="stat-value">{{ stats.pending }}</div>
            <div class="stat-label">待审核</div>
          </div>
          <el-icon class="stat-icon"><Clock /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card approved">
          <div class="stat-content">
            <div class="stat-value">{{ stats.approved }}</div>
            <div class="stat-label">已通过</div>
          </div>
          <el-icon class="stat-icon"><CircleCheck /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card rejected">
          <div class="stat-content">
            <div class="stat-value">{{ stats.rejected }}</div>
            <div class="stat-label">已驳回</div>
          </div>
          <el-icon class="stat-icon"><CircleClose /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card total">
          <div class="stat-content">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">房源总数</div>
          </div>
          <el-icon class="stat-icon"><House /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选和操作栏 -->
    <el-card class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-select v-model="filterStatus" placeholder="审核状态" style="width: 100%" @change="loadList">
            <el-option label="全部状态" :value="-1" />
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="4" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索房源标题/地址" clearable @keyup.enter="loadList">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadList">搜索</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-col>
        <el-col :span="6" style="text-align: right">
          <el-button type="success" :disabled="!selectedIds.length" @click="handleBatchApprove">
            批量通过 ({{ selectedIds.length }})
          </el-button>
          <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchReject">
            批量驳回 ({{ selectedIds.length }})
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 房源列表 -->
    <el-card class="list-card">
      <el-table :data="houseList" v-loading="loading" @selection-change="onSelectionChange" row-key="houseId">
        <el-table-column type="selection" width="50" :selectable="row => row.status === 0" />
        <el-table-column label="房源信息" min-width="280">
          <template #default="{ row }">
            <div class="house-info">
              <el-image :src="getFirstImage(row.images)" fit="cover" class="house-thumb" />
              <div class="house-detail">
                <div class="house-title">{{ row.title }}</div>
                <div class="house-meta">{{ row.houseType }} | {{ row.area }}㎡ | {{ row.floor }}</div>
                <div class="house-location">{{ row.province }}{{ row.city }}{{ row.district }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="月租金" width="100" align="center">
          <template #default="{ row }">
            <span class="price">¥{{ row.rentPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="房东" width="120">
          <template #default="{ row }">
            <div>{{ row.landlordName }}</div>
            <div class="text-muted">{{ row.landlordPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160" prop="createTime">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看</el-button>
            <template v-if="row.status === 0">
              <el-button link type="success" @click="handleApprove(row)">通过</el-button>
              <el-button link type="danger" @click="handleReject(row)">驳回</el-button>
            </template>
            <el-button v-if="row.status === 4" link type="info" @click="showRejectReason(row)">驳回原因</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="loadList" />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="房源详情" width="800px" destroy-on-close>
      <div v-if="currentHouse" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="房源标题" :span="2">{{ currentHouse.title }}</el-descriptions-item>
          <el-descriptions-item label="房屋户型">{{ currentHouse.houseType }}</el-descriptions-item>
          <el-descriptions-item label="房屋面积">{{ currentHouse.area }}㎡</el-descriptions-item>
          <el-descriptions-item label="所在楼层">{{ currentHouse.floor }}</el-descriptions-item>
          <el-descriptions-item label="房屋朝向">{{ currentHouse.orientation }}</el-descriptions-item>
          <el-descriptions-item label="月租金">¥{{ currentHouse.rentPrice }}</el-descriptions-item>
          <el-descriptions-item label="押付方式">{{ currentHouse.depositType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="位置" :span="2">{{ currentHouse.province }}{{ currentHouse.city }}{{ currentHouse.district }} {{ currentHouse.address }}</el-descriptions-item>
          <el-descriptions-item label="房东姓名">{{ currentHouse.landlordName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentHouse.landlordPhone }}</el-descriptions-item>
          <el-descriptions-item label="房源描述" :span="2">{{ currentHouse.description }}</el-descriptions-item>
          <el-descriptions-item label="配套设施" :span="2">
            <el-tag v-for="f in parseFacilities(currentHouse.facilities)" :key="f" size="small" style="margin-right:4px">{{ f }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div class="image-section">
          <div class="section-title">房源图片</div>
          <div class="image-list">
            <el-image v-for="(img, idx) in parseImages(currentHouse.images)" :key="idx" :src="img" fit="cover" 
              class="detail-image" :preview-src-list="parseImages(currentHouse.images)" :initial-index="idx" />
          </div>
        </div>

        <!-- 房产证件 -->
        <div class="image-section" v-if="currentHouse.propertyLicenseFront">
          <div class="section-title">
            <el-icon><Document /></el-icon> 房产证件材料
          </div>
          <div class="image-list">
            <div class="cert-item" v-if="currentHouse.propertyLicenseFront">
              <el-image :src="currentHouse.propertyLicenseFront" fit="cover" class="detail-image"
                :preview-src-list="[currentHouse.propertyLicenseFront, currentHouse.propertyLicenseBack, currentHouse.propertyLicenseOther].filter(Boolean)" />
              <div class="cert-label">房产证正面</div>
            </div>
            <div class="cert-item" v-if="currentHouse.propertyLicenseBack">
              <el-image :src="currentHouse.propertyLicenseBack" fit="cover" class="detail-image"
                :preview-src-list="[currentHouse.propertyLicenseFront, currentHouse.propertyLicenseBack, currentHouse.propertyLicenseOther].filter(Boolean)" />
              <div class="cert-label">房产证背面</div>
            </div>
            <div class="cert-item" v-if="currentHouse.propertyLicenseOther">
              <el-image :src="currentHouse.propertyLicenseOther" fit="cover" class="detail-image"
                :preview-src-list="[currentHouse.propertyLicenseFront, currentHouse.propertyLicenseBack, currentHouse.propertyLicenseOther].filter(Boolean)" />
              <div class="cert-label">其他证明</div>
            </div>
          </div>
        </div>
        <el-alert v-else title="房东未上传房产证件材料" type="warning" :closable="false" show-icon style="margin-top:12px" />

        <!-- 审核标准 -->
        <el-collapse style="margin-top: 16px">
          <el-collapse-item title="📋 审核标准参考" name="standard">
            <el-table :data="auditStandards" size="small" border>
              <el-table-column prop="item" label="审核项目" width="140" />
              <el-table-column prop="pass" label="通过条件" />
              <el-table-column prop="reject" label="驳回条件" />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>
      <template #footer>
        <template v-if="currentHouse?.status === 0">
          <el-button type="success" @click="handleApprove(currentHouse!)">通过审核</el-button>
          <el-button type="danger" @click="handleReject(currentHouse!)">驳回</el-button>
        </template>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 驳回原因弹窗 -->
    <el-dialog v-model="rejectVisible" title="驳回房源" width="500px">
      <el-form>
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入驳回原因" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="rejectLoading">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, CircleCheck, CircleClose, House, Search, Document } from '@element-plus/icons-vue'
import { getAuditListApi, getHouseDetailApi, approveHouseApi, rejectHouseApi, batchApproveApi, batchRejectApi, getAuditStatsApi, type HouseAuditInfo } from '@/api/adminHouse'

const loading = ref(false)
const houseList = ref<HouseAuditInfo[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filterStatus = ref<number>(-1) // 默认显示全部状态
const keyword = ref('')
const selectedIds = ref<number[]>([])

const stats = ref({ pending: 0, approved: 0, rejected: 0, total: 0 })

const detailVisible = ref(false)
const currentHouse = ref<HouseAuditInfo | null>(null)

const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectLoading = ref(false)
const rejectTarget = ref<{ type: 'single' | 'batch', id?: number }>({ type: 'single' })

// 审核标准
const auditStandards = [
  { item: '房产证件', pass: '提供清晰的房产证正面照片，产权人与房东实名一致', reject: '未上传证件、证件模糊不清、产权人与房东不符' },
  { item: '房源图片', pass: '至少3张清晰真实的室内/外照片', reject: '图片数量不足、图片模糊、与描述严重不符' },
  { item: '房源信息', pass: '标题、地址、面积、租金等信息完整准确', reject: '关键信息缺失、价格明显异常（过高或过低）' },
  { item: '房源描述', pass: '描述真实，无虚假宣传，无违禁词', reject: '含有虚假信息、违禁内容或联系方式' },
  { item: '租金合理性', pass: '租金与同区域同类型房源价格相符', reject: '租金明显偏离市场价格（超出±50%）' },
  { item: '地址真实性', pass: '省市区地址与房产证地址一致', reject: '地址填写错误或与证件不符' },
]

const loadStats = async () => {
  try {
    const res = await getAuditStatsApi() as any
    stats.value = res
  } catch {}
}

const loadList = async () => {
  loading.value = true
  try {
    const params: any = {
      status: filterStatus.value,
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    }
    const res = await getAuditListApi(params) as any
    houseList.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterStatus.value = -1 // 重置为全部状态
  keyword.value = ''
  page.value = 1
  loadList()
}

const onSelectionChange = (rows: HouseAuditInfo[]) => {
  selectedIds.value = rows.map(r => r.houseId)
}

const getFirstImage = (images: string) => {
  try {
    const arr = JSON.parse(images)
    return arr[0] || ''
  } catch { return '' }
}

const parseImages = (images: string): string[] => {
  try { return JSON.parse(images) } catch { return [] }
}

const parseFacilities = (facilities: string): string[] => {
  try { return JSON.parse(facilities) } catch { return [] }
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

const statusText = (status: number) => {
  const map: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已出租', 3: '已下架', 4: '已驳回' }
  return map[status] || '未知'
}

const statusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info', 3: 'info', 4: 'danger' }
  return map[status] || 'info'
}

const showDetail = async (row: HouseAuditInfo) => {
  try {
    const res = await getHouseDetailApi(row.houseId) as any
    currentHouse.value = res
    detailVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

const handleApprove = async (row: HouseAuditInfo) => {
  try {
    await ElMessageBox.confirm(`确定通过房源「${row.title}」的审核吗？`, '确认通过')
    await approveHouseApi(row.houseId)
    ElMessage.success('审核通过')
    detailVisible.value = false
    loadList()
    loadStats()
  } catch {}
}

const handleReject = (row: HouseAuditInfo) => {
  rejectTarget.value = { type: 'single', id: row.houseId }
  rejectReason.value = ''
  rejectVisible.value = true
}

const showRejectReason = (row: HouseAuditInfo) => {
  ElMessageBox.alert(row.auditReason || '无', '驳回原因', { confirmButtonText: '知道了' })
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  rejectLoading.value = true
  try {
    if (rejectTarget.value.type === 'single' && rejectTarget.value.id) {
      await rejectHouseApi(rejectTarget.value.id, rejectReason.value)
    } else {
      await batchRejectApi(selectedIds.value, rejectReason.value)
    }
    ElMessage.success('驳回成功')
    rejectVisible.value = false
    detailVisible.value = false
    loadList()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    rejectLoading.value = false
  }
}

const handleBatchApprove = async () => {
  try {
    await ElMessageBox.confirm(`确定批量通过选中的 ${selectedIds.value.length} 条房源吗？`, '批量通过')
    await batchApproveApi(selectedIds.value)
    ElMessage.success('批量通过成功')
    loadList()
    loadStats()
  } catch {}
}

const handleBatchReject = () => {
  rejectTarget.value = { type: 'batch' }
  rejectReason.value = ''
  rejectVisible.value = true
}

onMounted(() => {
  loadStats()
  loadList()
})
</script>

<style scoped>
.stats-row { margin-bottom: 16px; }
.stat-card { position: relative; overflow: hidden; }
.stat-card .el-card__body { display: flex; align-items: center; justify-content: space-between; padding: 20px; }
.stat-content { z-index: 1; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
.stat-icon { font-size: 48px; opacity: 0.15; }
.stat-card.pending .stat-value { color: #e6a23c; }
.stat-card.approved .stat-value { color: #67c23a; }
.stat-card.rejected .stat-value { color: #f56c6c; }
.stat-card.total .stat-value { color: #409eff; }

.filter-card { margin-bottom: 16px; }
.list-card { margin-bottom: 16px; }

.house-info { display: flex; gap: 12px; }
.house-thumb { width: 80px; height: 60px; border-radius: 4px; flex-shrink: 0; }
.house-detail { flex: 1; min-width: 0; }
.house-title { font-weight: 500; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.house-meta { font-size: 12px; color: #909399; margin-top: 4px; }
.house-location { font-size: 12px; color: #909399; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.price { color: #f56c6c; font-weight: bold; }
.text-muted { font-size: 12px; color: #909399; }

.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }

.detail-content { max-height: 60vh; overflow-y: auto; }
.image-section { margin-top: 20px; }
.section-title { font-weight: 500; margin-bottom: 12px; color: #303133; display: flex; align-items: center; gap: 6px; }
.image-list { display: flex; flex-wrap: wrap; gap: 8px; }
.detail-image { width: 120px; height: 90px; border-radius: 4px; cursor: pointer; }
.cert-item { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.cert-label { font-size: 12px; color: #909399; }
</style>
