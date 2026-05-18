<template>
  <div class="house-monitor-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" :class="{ active: filterStatus === -1 }" @click="setFilter(-1)">
          <div class="stat-value total">{{ stats.total }}</div>
          <div class="stat-label">全部房源</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" :class="{ active: filterStatus === 0 }" @click="setFilter(0)">
          <div class="stat-value pending">{{ stats.pending }}</div>
          <div class="stat-label">待审核</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" :class="{ active: filterStatus === 1 }" @click="setFilter(1)">
          <div class="stat-value available">{{ stats.available }}</div>
          <div class="stat-label">可出租</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" :class="{ active: filterStatus === 2 }" @click="setFilter(2)">
          <div class="stat-value rented">{{ stats.rented }}</div>
          <div class="stat-label">已出租</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" :class="{ active: filterStatus === 3 }" @click="setFilter(3)">
          <div class="stat-value offline">{{ stats.offline }}</div>
          <div class="stat-label">已下架</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card" :class="{ active: filterStatus === 4 }" @click="setFilter(4)">
          <div class="stat-value rejected">{{ stats.rejected }}</div>
          <div class="stat-label">已驳回</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="城市">
          <el-input v-model="filterForm.city" placeholder="城市" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="区域">
          <el-input v-model="filterForm.district" placeholder="区域" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="filterForm.minPrice" :min="0" placeholder="最低" controls-position="right" style="width: 100px" />
          <span style="margin: 0 8px">-</span>
          <el-input-number v-model="filterForm.maxPrice" :min="0" placeholder="最高" controls-position="right" style="width: 100px" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="标题/地址" clearable style="width: 150px" @keyup.enter="loadList" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">搜索</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="batch-actions" v-if="selectedIds.length > 0">
        <el-button type="danger" @click="handleBatchOffline">批量下架 ({{ selectedIds.length }})</el-button>
      </div>
    </el-card>

    <!-- 房源列表 -->
    <el-card class="list-card">
      <el-table :data="houseList" v-loading="loading" @selection-change="onSelectionChange" row-key="houseId">
        <el-table-column type="selection" width="50" :selectable="row => row.status === 1" />
        <el-table-column label="房源信息" min-width="300">
          <template #default="{ row }">
            <div class="house-info">
              <el-image :src="getFirstImage(row.images)" fit="cover" class="house-thumb">
                <template #error><div class="img-error"><el-icon><Picture /></el-icon></div></template>
              </el-image>
              <div class="house-detail">
                <div class="house-title">{{ row.title }}</div>
                <div class="house-meta">{{ row.houseType }} | {{ row.area }}㎡ | {{ row.floor }}</div>
                <div class="house-location">{{ row.city }} {{ row.district }} {{ row.address }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="月租金" width="100" align="center">
          <template #default="{ row }"><span class="price">¥{{ row.rentPrice }}</span></template>
        </el-table-column>
        <el-table-column label="房东" width="130">
          <template #default="{ row }">
            <div>{{ row.landlordName }}</div>
            <div class="text-muted">{{ row.landlordPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="浏览/收藏" width="100" align="center">
          <template #default="{ row }">
            <div>{{ row.viewCount || 0 }} / {{ row.collectCount || 0 }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看</el-button>
            <el-button link type="warning" @click="showEditDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 1 || row.status === 2" link type="danger" @click="handleOffline(row)">下架</el-button>
            <el-button v-if="row.status === 3" link type="success" @click="handleOnline(row)">恢复</el-button>
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
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusType(currentHouse.status)">{{ statusText(currentHouse.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="位置" :span="2">{{ currentHouse.city }} {{ currentHouse.district }} {{ currentHouse.address }}</el-descriptions-item>
          <el-descriptions-item label="房东">{{ currentHouse.landlordName }} ({{ currentHouse.landlordPhone }})</el-descriptions-item>
          <el-descriptions-item label="浏览/收藏">{{ currentHouse.viewCount || 0 }} / {{ currentHouse.collectCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="房源描述" :span="2">{{ currentHouse.description }}</el-descriptions-item>
          <el-descriptions-item v-if="currentHouse.auditReason" label="备注" :span="2">
            <span class="text-danger">{{ currentHouse.auditReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="image-section">
          <div class="section-title">房源图片</div>
          <div class="image-list">
            <el-image v-for="(img, idx) in parseImages(currentHouse.images)" :key="idx" :src="img" fit="cover"
              class="detail-image" :preview-src-list="parseImages(currentHouse.images)" :initial-index="idx" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button v-if="currentHouse?.status === 1 || currentHouse?.status === 2" type="danger" @click="handleOffline(currentHouse!)">下架房源</el-button>
        <el-button v-if="currentHouse?.status === 3" type="success" @click="handleOnline(currentHouse!)">恢复上架</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑房源信息" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" maxlength="50" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="editForm.address" />
        </el-form-item>
        <el-form-item label="月租金">
          <el-input-number v-model="editForm.rentPrice" :min="0" :max="100000" />
        </el-form-item>
        <el-form-item label="面积">
          <el-input-number v-model="editForm.area" :min="0" :max="1000" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="editLoading">保存</el-button>
      </template>
    </el-dialog>

    <!-- 下架原因弹窗 -->
    <el-dialog v-model="offlineVisible" title="下架房源" width="500px">
      <el-form>
        <el-form-item label="下架原因" required>
          <el-input v-model="offlineReason" type="textarea" :rows="4" placeholder="请输入下架原因（将通知房东）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offlineVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmOffline" :loading="offlineLoading">确认下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import {
  getMonitorListApi, getMonitorStatsApi, getHouseDetailApi,
  offlineHouseApi, onlineHouseApi, editHouseApi, batchOfflineApi
} from '@/api/adminHouse'

const loading = ref(false)
const houseList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filterStatus = ref(0) // 默认显示待审核
const selectedIds = ref<number[]>([])

const filterForm = reactive({
  city: '',
  district: '',
  minPrice: undefined as number | undefined,
  maxPrice: undefined as number | undefined,
  keyword: ''
})

const stats = ref({ total: 0, pending: 0, available: 0, rented: 0, offline: 0, rejected: 0 })

const detailVisible = ref(false)
const currentHouse = ref<any>(null)

const editVisible = ref(false)
const editForm = reactive({ title: '', address: '', rentPrice: 0, area: 0, description: '' })
const editLoading = ref(false)
const editHouseId = ref(0)

const offlineVisible = ref(false)
const offlineReason = ref('')
const offlineLoading = ref(false)
const offlineTarget = ref<{ type: 'single' | 'batch', id?: number }>({ type: 'single' })

const loadStats = async () => {
  try {
    const res: any = await getMonitorStatsApi()
    stats.value = res
  } catch {}
}

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getMonitorListApi({
      status: filterStatus.value,
      city: filterForm.city || undefined,
      district: filterForm.district || undefined,
      minPrice: filterForm.minPrice,
      maxPrice: filterForm.maxPrice,
      keyword: filterForm.keyword || undefined,
      page: page.value,
      size: size.value
    })
    houseList.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const setFilter = (status: number) => {
  filterStatus.value = status
  page.value = 1
  loadList()
}

const resetFilter = () => {
  filterStatus.value = -1
  filterForm.city = ''
  filterForm.district = ''
  filterForm.minPrice = undefined
  filterForm.maxPrice = undefined
  filterForm.keyword = ''
  page.value = 1
  loadList()
}

const onSelectionChange = (rows: any[]) => {
  selectedIds.value = rows.map(r => r.houseId)
}

const getFirstImage = (images: string) => {
  try { return JSON.parse(images)[0] || '' } catch { return '' }
}

const parseImages = (images: string): string[] => {
  try { return JSON.parse(images) } catch { return [] }
}

const formatTime = (time: string) => time ? time.replace('T', ' ').substring(0, 16) : '-'

const statusText = (status: number) => {
  const map: Record<number, string> = { 0: '待审核', 1: '可出租', 2: '已出租', 3: '已下架', 4: '已驳回' }
  return map[status] || '未知'
}

const statusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'primary', 3: 'info', 4: 'danger' }
  return map[status] || 'info'
}

const showDetail = async (row: any) => {
  try {
    const res: any = await getHouseDetailApi(row.houseId)
    currentHouse.value = res
    detailVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

const showEditDialog = (row: any) => {
  editHouseId.value = row.houseId
  editForm.title = row.title
  editForm.address = row.address
  editForm.rentPrice = row.rentPrice
  editForm.area = row.area
  editForm.description = row.description || ''
  editVisible.value = true
}

const submitEdit = async () => {
  editLoading.value = true
  try {
    await editHouseApi(editHouseId.value, editForm)
    ElMessage.success('保存成功')
    editVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    editLoading.value = false
  }
}

const handleOffline = (row: any) => {
  offlineTarget.value = { type: 'single', id: row.houseId }
  offlineReason.value = ''
  offlineVisible.value = true
}

const handleBatchOffline = () => {
  offlineTarget.value = { type: 'batch' }
  offlineReason.value = ''
  offlineVisible.value = true
}

const confirmOffline = async () => {
  if (!offlineReason.value.trim()) {
    ElMessage.warning('请输入下架原因')
    return
  }
  offlineLoading.value = true
  try {
    if (offlineTarget.value.type === 'single' && offlineTarget.value.id) {
      await offlineHouseApi(offlineTarget.value.id, offlineReason.value)
    } else {
      await batchOfflineApi(selectedIds.value, offlineReason.value)
    }
    ElMessage.success('下架成功')
    offlineVisible.value = false
    detailVisible.value = false
    loadList()
    loadStats()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    offlineLoading.value = false
  }
}

const handleOnline = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定恢复上架房源「${row.title}」吗？`, '恢复上架')
    await onlineHouseApi(row.houseId)
    ElMessage.success('已恢复上架')
    detailVisible.value = false
    loadList()
    loadStats()
  } catch {}
}

onMounted(() => {
  loadStats()
  loadList()
})
</script>

<style scoped>
.stats-row { margin-bottom: 16px; }
.stat-card { cursor: pointer; transition: all 0.3s; text-align: center; padding: 16px 0; }
.stat-card:hover { transform: translateY(-2px); }
.stat-card.active { border-color: #409eff; background: #ecf5ff; }
.stat-value { font-size: 28px; font-weight: bold; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
.stat-value.total { color: #409eff; }
.stat-value.pending { color: #e6a23c; }
.stat-value.available { color: #67c23a; }
.stat-value.rented { color: #409eff; }
.stat-value.offline { color: #909399; }
.stat-value.rejected { color: #f56c6c; }

.filter-card { margin-bottom: 16px; }
.filter-card :deep(.el-card__body) { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; }
.batch-actions { margin-top: 10px; }

.list-card { margin-bottom: 16px; }
.house-info { display: flex; gap: 12px; }
.house-thumb { width: 80px; height: 60px; border-radius: 4px; flex-shrink: 0; }
.img-error { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f5f5; color: #ccc; }
.house-detail { flex: 1; min-width: 0; }
.house-title { font-weight: 500; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.house-meta { font-size: 12px; color: #909399; margin-top: 4px; }
.house-location { font-size: 12px; color: #909399; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.price { color: #f56c6c; font-weight: bold; }
.text-muted { font-size: 12px; color: #909399; }
.text-danger { color: #f56c6c; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }

.detail-content { max-height: 60vh; overflow-y: auto; }
.image-section { margin-top: 20px; }
.section-title { font-weight: 500; margin-bottom: 12px; color: #303133; }
.image-list { display: flex; flex-wrap: wrap; gap: 8px; }
.detail-image { width: 120px; height: 90px; border-radius: 4px; cursor: pointer; }
</style>
