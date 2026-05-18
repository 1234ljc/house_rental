<template>
  <div class="report-manage">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon pending"><el-icon><Clock /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待审核举报</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon approved"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.approved }}</div>
          <div class="stat-label">已通过（删帖）</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon rejected"><el-icon><CircleClose /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.rejected }}</div>
          <div class="stat-label">已驳回</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon total"><el-icon><ChatDotRound /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalComments }}</div>
          <div class="stat-label">总帖子数</div>
        </div>
      </div>
    </div>

    <!-- 举报列表 -->
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span>举报审核</span>
          <el-radio-group v-model="filter.status" size="small" @change="filter.page = 1; loadList()">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已驳回</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column label="举报人" width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.reporterAvatar">{{ row.reporterName?.[0] }}</el-avatar>
              <span>{{ row.reporterName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="被举报帖子" min-width="250">
          <template #default="{ row }">
            <div class="comment-cell">
              <div class="comment-text">{{ row.commentContent }}</div>
              <div class="comment-meta">
                <span>发帖人：{{ row.authorName }}</span>
                <span v-if="row.houseTitle">房源：{{ row.houseTitle }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="举报原因" min-width="180" prop="reason" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="举报时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="danger" link @click="handleApprove(row)">通过（删帖）</el-button>
              <el-button type="info" link @click="handleReject(row)">驳回</el-button>
            </template>
            <span v-else class="audit-info">
              {{ row.auditRemark || (row.status === 1 ? '已删帖' : '已驳回') }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="filter.size"
          v-model:current-page="filter.page" @current-change="loadList" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, CircleCheck, CircleClose, ChatDotRound } from '@element-plus/icons-vue'
import { getReportsApi, getReportStatsApi, approveReportApi, rejectReportApi } from '@/api/adminComment'

const stats = reactive({ pending: 0, approved: 0, rejected: 0, totalComments: 0 })
const filter = reactive({ status: undefined as number | undefined, page: 1, size: 10 })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''
const statusType = (s: number) => ({ 0: 'warning', 1: 'danger', 2: 'info' }[s] || 'info')
const statusText = (s: number) => ({ 0: '待审核', 1: '已通过', 2: '已驳回' }[s] || '未知')

const loadStats = async () => {
  try { Object.assign(stats, await getReportStatsApi()) } catch (e) { console.error(e) }
}

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getReportsApi(filter)
    list.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const handleApprove = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('确认删除该帖子？可填写备注', '审核通过', {
      inputPlaceholder: '审核备注（选填）', confirmButtonText: '确认删帖', cancelButtonText: '取消', type: 'warning'
    })
    await approveReportApi(row.reportId, value)
    ElMessage.success('已删除帖子')
    loadStats(); loadList()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

const handleReject = async (row: any) => {
  try {
    const { value } = await ElMessageBox.prompt('驳回该举报？可填写原因', '驳回举报', {
      inputPlaceholder: '驳回原因（选填）', confirmButtonText: '确认驳回', cancelButtonText: '取消'
    })
    await rejectReportApi(row.reportId, value)
    ElMessage.success('已驳回举报')
    loadStats(); loadList()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.message || '操作失败') }
}

onMounted(() => { loadStats(); loadList() })
</script>

<style scoped>
.report-manage { padding: 0; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 20px; display: flex; align-items: center; gap: 15px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.stat-icon { width: 50px; height: 50px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; }
.stat-icon.pending { background: linear-gradient(135deg, #e6a23c, #f7ba2a); }
.stat-icon.approved { background: linear-gradient(135deg, #f56c6c, #ff7875); }
.stat-icon.rejected { background: linear-gradient(135deg, #909399, #b1b3b8); }
.stat-icon.total { background: linear-gradient(135deg, #409eff, #79bbff); }
.stat-value { font-size: 28px; font-weight: bold; color: #333; }
.stat-label { font-size: 14px; color: #999; margin-top: 4px; }
.list-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.user-cell { display: flex; align-items: center; gap: 8px; }
.comment-cell {}
.comment-text { font-size: 14px; color: #333; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.comment-meta { font-size: 12px; color: #999; margin-top: 4px; display: flex; gap: 12px; }
.audit-info { font-size: 12px; color: #999; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
