<template>
  <div class="realname-audit">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <el-card class="stat-card pending" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon"><el-icon :size="32"><Clock /></el-icon></div>
          <div class="stat-info">
            <p class="stat-value">{{ stats.pending || 0 }}</p>
            <p class="stat-label">待审核</p>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card passed" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon"><el-icon :size="32"><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <p class="stat-value">{{ stats.passed || 0 }}</p>
            <p class="stat-label">已通过</p>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card rejected" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon"><el-icon :size="32"><CircleClose /></el-icon></div>
          <div class="stat-info">
            <p class="stat-value">{{ stats.rejected || 0 }}</p>
            <p class="stat-label">已驳回</p>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card total" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon"><el-icon :size="32"><Document /></el-icon></div>
          <div class="stat-info">
            <p class="stat-value">{{ stats.total || 0 }}</p>
            <p class="stat-label">总申请</p>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 搜索筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="审核状态">
          <el-select v-model="filterForm.authStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="filterForm.userType" placeholder="全部" clearable style="width: 120px">
            <el-option label="租客" :value="1" />
            <el-option label="房东" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="姓名/身份证号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="申请时间">
          <el-date-picker v-model="filterForm.dateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="userType" label="用户类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.userType === 1 ? 'success' : 'warning'" size="small">
              {{ row.userType === 1 ? '租客' : '房东' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="idCard" label="身份证号" width="180">
          <template #default="{ row }">{{ maskIdCard(row.idCard) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="authStatus" label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.authStatus)" size="small">{{ getStatusText(row.authStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="auditTime" label="审核时间" width="170">
          <template #default="{ row }">{{ row.auditTime ? formatTime(row.auditTime) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button v-if="row.authStatus === 0" type="success" link size="small" @click="handleAudit(row)">审核</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]" :total="pagination.total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="viewDialogVisible" title="实名认证详情" width="700px">
      <div class="detail-content" v-if="currentRecord">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ currentRecord.username }}</el-descriptions-item>
          <el-descriptions-item label="用户类型">
            <el-tag :type="currentRecord.userType === 1 ? 'success' : 'warning'" size="small">
              {{ currentRecord.userType === 1 ? '租客' : '房东' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ currentRecord.realName }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ currentRecord.idCard }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentRecord.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentRecord.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(currentRecord.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag :type="getStatusType(currentRecord.authStatus)" size="small">{{ getStatusText(currentRecord.authStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentRecord.auditTime" label="审核时间">{{ formatTime(currentRecord.auditTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentRecord.auditReason" label="审核意见" :span="2">{{ currentRecord.auditReason }}</el-descriptions-item>
        </el-descriptions>

        <div class="id-card-images">
          <div class="image-item">
            <p class="image-label">身份证正面（人像面）</p>
            <el-image :src="currentRecord.idCardFront" fit="contain" :preview-src-list="[currentRecord.idCardFront]" />
          </div>
          <div class="image-item">
            <p class="image-label">身份证反面（国徽面）</p>
            <el-image :src="currentRecord.idCardBack" fit="contain" :preview-src-list="[currentRecord.idCardBack]" />
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog v-model="auditDialogVisible" title="审核实名认证" width="500px">
      <div class="audit-info" v-if="currentRecord">
        <p><strong>申请人：</strong>{{ currentRecord.username }}（{{ currentRecord.userType === 1 ? '租客' : '房东' }}）</p>
        <p><strong>真实姓名：</strong>{{ currentRecord.realName }}</p>
        <p><strong>身份证号：</strong>{{ currentRecord.idCard }}</p>
      </div>
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="100px" class="audit-form">
        <el-form-item label="审核结果" prop="authStatus">
          <el-radio-group v-model="auditForm.authStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见" prop="auditReason">
          <el-input v-model="auditForm.auditReason" type="textarea" :rows="3" 
            :placeholder="auditForm.authStatus === 2 ? '请填写驳回原因（必填）' : '审核意见（选填）'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAuditSubmit" :loading="auditSubmitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Clock, CircleCheck, CircleClose, Document, Search, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref<any[]>([])
const stats = ref<any>({})

const filterForm = reactive({
  authStatus: null as number | null,
  userType: null as number | null,
  keyword: '',
  dateRange: null as string[] | null
})

const pagination = reactive({ page: 1, size: 10, total: 0 })

// 查看详情
const viewDialogVisible = ref(false)
const currentRecord = ref<any>(null)

// 审核
const auditDialogVisible = ref(false)
const auditFormRef = ref<FormInstance>()
const auditSubmitting = ref(false)
const auditForm = reactive({ authStatus: 1, auditReason: '' })

const auditRules: FormRules = {
  authStatus: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  auditReason: [{
    validator: (rule, value, callback) => {
      if (auditForm.authStatus === 2 && !value) {
        callback(new Error('驳回时必须填写原因'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }]
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const res: any = await request.get('/admin/realname/stats')
    stats.value = res
  } catch (error) { console.error(error) }
}

// 获取列表数据
const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      size: pagination.size
    }
    if (filterForm.authStatus !== null) params.authStatus = filterForm.authStatus
    if (filterForm.userType !== null) params.userType = filterForm.userType
    if (filterForm.keyword) params.keyword = filterForm.keyword
    if (filterForm.dateRange?.length === 2) {
      params.startDate = filterForm.dateRange[0]
      params.endDate = filterForm.dateRange[1]
    }

    const res: any = await request.get('/admin/realname/list', { params })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; fetchData() }
const handleReset = () => {
  filterForm.authStatus = null
  filterForm.userType = null
  filterForm.keyword = ''
  filterForm.dateRange = null
  handleSearch()
}

const handleView = async (row: any) => {
  try {
    const res: any = await request.get(`/admin/realname/${row.authId}`)
    currentRecord.value = res
    viewDialogVisible.value = true
  } catch (error) { ElMessage.error('获取详情失败') }
}

const handleAudit = (row: any) => {
  currentRecord.value = row
  auditForm.authStatus = 1
  auditForm.auditReason = ''
  auditDialogVisible.value = true
}

const handleAuditSubmit = async () => {
  if (!auditFormRef.value) return
  await auditFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    await ElMessageBox.confirm(
      `确定要${auditForm.authStatus === 1 ? '通过' : '驳回'}该实名认证申请吗？`,
      '确认审核',
      { type: 'warning' }
    )

    auditSubmitting.value = true
    try {
      await request.put(`/admin/realname/${currentRecord.value.authId}/audit`, auditForm)
      ElMessage.success('审核成功')
      auditDialogVisible.value = false
      fetchData()
      fetchStats()
    } catch (error: any) {
      ElMessage.error(error.message || '审核失败')
    } finally {
      auditSubmitting.value = false
    }
  })
}

const getStatusType = (status: number) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  return 'danger'
}

const getStatusText = (status: number) => {
  if (status === 0) return '待审核'
  if (status === 1) return '已通过'
  return '已驳回'
}

const maskIdCard = (idCard: string) => {
  if (!idCard || idCard.length < 8) return idCard
  return idCard.substring(0, 6) + '********' + idCard.substring(idCard.length - 4)
}

const formatTime = (time: string) => time ? new Date(time).toLocaleString('zh-CN') : '-'

onMounted(() => { fetchStats(); fetchData() })
</script>

<style scoped>
.stats-row { display: flex; gap: 16px; margin-bottom: 16px; }
.stat-card { flex: 1; }
.stat-content { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 60px; height: 60px; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
.stat-card.pending .stat-icon { background: #fdf6ec; color: #e6a23c; }
.stat-card.passed .stat-icon { background: #f0f9eb; color: #67c23a; }
.stat-card.rejected .stat-icon { background: #fef0f0; color: #f56c6c; }
.stat-card.total .stat-icon { background: #ecf5ff; color: #409eff; }
.stat-info { flex: 1; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; margin: 0; }
.stat-label { font-size: 14px; color: #909399; margin: 4px 0 0; }

.filter-card { margin-bottom: 16px; }
.filter-form { display: flex; flex-wrap: wrap; gap: 8px; }

.table-card { margin-bottom: 16px; }
.pagination-container { margin-top: 16px; display: flex; justify-content: flex-end; }

.detail-content { padding: 10px 0; }
.id-card-images { display: flex; gap: 24px; margin-top: 20px; }
.image-item { flex: 1; text-align: center; }
.image-label { font-size: 14px; color: #606266; margin-bottom: 8px; }
.image-item :deep(.el-image) { width: 100%; height: 200px; border: 1px solid #e4e7ed; border-radius: 4px; }

.audit-info { background: #f5f7fa; padding: 16px; border-radius: 4px; margin-bottom: 20px; }
.audit-info p { margin: 8px 0; font-size: 14px; color: #606266; }
.audit-form { margin-top: 16px; }
</style>
