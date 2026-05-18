<template>
  <div class="contract-monitor">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">全部合同</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card draft">
          <div class="stat-value">{{ stats.draft || 0 }}</div>
          <div class="stat-label">草稿</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card pending">
          <div class="stat-value">{{ stats.pending || 0 }}</div>
          <div class="stat-label">待确认</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card active">
          <div class="stat-value">{{ stats.active || 0 }}</div>
          <div class="stat-label">已确认</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card expired">
          <div class="stat-value">{{ stats.expired || 0 }}</div>
          <div class="stat-label">已到期</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card terminated">
          <div class="stat-value">{{ stats.terminated || 0 }}</div>
          <div class="stat-label">已终止</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选区域 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="合同状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="全部" :value="-1" />
            <el-option label="草稿" :value="0" />
            <el-option label="待确认" :value="1" />
            <el-option label="已确认" :value="2" />
            <el-option label="已到期" :value="3" />
            <el-option label="已终止" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同编号">
          <el-input v-model="queryParams.keyword" placeholder="搜索合同编号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 合同列表 -->
    <el-card class="table-card">
      <el-table :data="contractList" v-loading="loading" stripe>
        <el-table-column prop="contractNo" label="合同编号" width="180" />
        <el-table-column prop="houseTitle" label="房源" min-width="200" show-overflow-tooltip />
        <el-table-column label="房东" width="120">
          <template #default="{ row }">
            <div>{{ row.landlordName }}</div>
            <div class="sub-text">{{ row.landlordPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="租客" width="120">
          <template #default="{ row }">
            <div>{{ row.tenantName }}</div>
            <div class="sub-text">{{ row.tenantPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="租期" width="200">
          <template #default="{ row }">
            {{ row.rentStartDate }} ~ {{ row.rentEndDate }}
          </template>
        </el-table-column>
        <el-table-column prop="monthlyRent" label="月租金" width="100">
          <template #default="{ row }">
            ¥{{ row.monthlyRent }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="danger" link @click="handleTerminate(row)" v-if="row.status !== 4">终止</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadContractList"
        @current-change="loadContractList"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <!-- 合同详情弹窗 -->
    <el-dialog v-model="detailVisible" title="合同详情" width="700px">
      <el-descriptions :column="2" border v-if="currentContract">
        <el-descriptions-item label="合同编号">{{ currentContract.contractNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentContract.status)">{{ getStatusText(currentContract.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="房源">{{ currentContract.houseTitle }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ currentContract.houseAddress }}</el-descriptions-item>
        <el-descriptions-item label="房东">{{ currentContract.landlordName }} ({{ currentContract.landlordPhone }})</el-descriptions-item>
        <el-descriptions-item label="租客">{{ currentContract.tenantName }} ({{ currentContract.tenantPhone }})</el-descriptions-item>
        <el-descriptions-item label="租期开始">{{ currentContract.rentStartDate }}</el-descriptions-item>
        <el-descriptions-item label="租期结束">{{ currentContract.rentEndDate }}</el-descriptions-item>
        <el-descriptions-item label="月租金">¥{{ currentContract.monthlyRent }}</el-descriptions-item>
        <el-descriptions-item label="押金">¥{{ currentContract.depositAmount }}</el-descriptions-item>
        <el-descriptions-item label="付款日">每月{{ currentContract.paymentDay }}日</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentContract.createTime }}</el-descriptions-item>
        <el-descriptions-item label="租客确认时间" :span="2">{{ currentContract.tenantSignTime || '未确认' }}</el-descriptions-item>
        <el-descriptions-item label="合同上传时间" :span="2">{{ currentContract.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 终止合同弹窗 -->
    <el-dialog v-model="terminateVisible" title="终止合同" width="500px">
      <el-form :model="terminateForm" label-width="80px">
        <el-alert type="warning" :closable="false" style="margin-bottom: 16px">
          终止合同将通知双方，请谨慎操作
        </el-alert>
        <el-form-item label="终止原因" required>
          <el-input v-model="terminateForm.reason" type="textarea" :rows="4" placeholder="请输入终止原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="terminateVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmTerminate" :loading="terminateLoading">确认终止</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getContractList, getContractStats, getContractDetail, terminateContract } from '@/api/adminRental'

const loading = ref(false)
const contractList = ref<any[]>([])
const total = ref(0)
const stats = ref<any>({})

const queryParams = reactive({
  status: -1,
  keyword: '',
  page: 1,
  size: 10
})

const detailVisible = ref(false)
const currentContract = ref<any>(null)

const terminateVisible = ref(false)
const terminateLoading = ref(false)
const terminateForm = reactive({
  contractId: 0,
  reason: ''
})

const getStatusType = (status: number) => {
  const types: Record<number, string> = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: '',
    4: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '待确认',
    2: '已确认',
    3: '已到期',
    4: '已终止'
  }
  return texts[status] || '未知'
}

const loadStats = async () => {
  try {
    const res: any = await getContractStats()
    stats.value = res
  } catch (e) {
    console.error(e)
  }
}

const loadContractList = async () => {
  loading.value = true
  try {
    const res: any = await getContractList(queryParams)
    contractList.value = res.records
    total.value = res.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  loadContractList()
}

const handleReset = () => {
  queryParams.status = -1
  queryParams.keyword = ''
  queryParams.page = 1
  loadContractList()
}

const handleView = async (row: any) => {
  try {
    const res: any = await getContractDetail(row.contractId)
    currentContract.value = res
    detailVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

const handleTerminate = (row: any) => {
  terminateForm.contractId = row.contractId
  terminateForm.reason = ''
  terminateVisible.value = true
}

const confirmTerminate = async () => {
  if (!terminateForm.reason.trim()) {
    ElMessage.warning('请输入终止原因')
    return
  }
  terminateLoading.value = true
  try {
    await terminateContract(terminateForm.contractId, terminateForm.reason)
    ElMessage.success('合同已终止')
    terminateVisible.value = false
    loadContractList()
    loadStats()
  } catch (e) {
    console.error(e)
  } finally {
    terminateLoading.value = false
  }
}

onMounted(() => {
  loadStats()
  loadContractList()
})
</script>

<style scoped>
.contract-monitor {
  padding: 20px;
}
.stats-row {
  margin-bottom: 16px;
}
.stat-card {
  text-align: center;
  padding: 16px 0;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}
.stat-card.draft .stat-value { color: #909399; }
.stat-card.pending .stat-value { color: #e6a23c; }
.stat-card.active .stat-value { color: #67c23a; }
.stat-card.expired .stat-value { color: #606266; }
.stat-card.terminated .stat-value { color: #f56c6c; }
.filter-card {
  margin-bottom: 16px;
}
.table-card {
  margin-bottom: 16px;
}
.sub-text {
  font-size: 12px;
  color: #909399;
}
</style>
