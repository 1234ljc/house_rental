<template>
  <div class="user-list-page">
    <!-- 搜索筛选 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="用户名/手机号/姓名"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card class="table-card">
      <el-table :data="userList" v-loading="loading" stripe>
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :size="40" :src="row.avatar || defaultAvatar">
              <el-icon><User /></el-icon>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" width="100">
          <template #default="{ row }">
            {{ row.realName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160">
          <template #default="{ row }">
            {{ row.email || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="实名状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getRealnameTagType(row.realnameStatus)" size="small">
              {{ getRealnameText(row.realnameStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">
              <el-icon><View /></el-icon> 详情
            </el-button>
            <el-button 
              :type="row.status === 1 ? 'danger' : 'success'" 
              link 
              size="small" 
              @click="handleToggleStatus(row)"
            >
              <el-icon><component :is="row.status === 1 ? 'Lock' : 'Unlock'" /></el-icon>
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="warning" link size="small" @click="handleResetPassword(row)">
              <el-icon><Key /></el-icon> 重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUserList"
          @current-change="loadUserList"
        />
      </div>
    </el-card>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="600px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="用户ID">{{ currentUser.userId }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ currentUser.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ maskIdCard(currentUser.idCard) }}</el-descriptions-item>
        <el-descriptions-item label="用户类型">
          <el-tag>{{ getUserTypeText(currentUser.userType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="currentUser.status === 1 ? 'success' : 'danger'">
            {{ currentUser.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="实名状态">
          <el-tag :type="getRealnameTagType(currentUser.realnameStatus)">
            {{ getRealnameText(currentUser.realnameStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="认证时间">{{ formatTime(currentUser.realnameTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">{{ formatTime(currentUser.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核意见" :span="2" v-if="currentUser.realnameAuditReason">
          {{ currentUser.realnameAuditReason }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, View, Lock, Unlock, Key, User } from '@element-plus/icons-vue'
import { getUserListApi, updateUserStatusApi, resetPasswordApi } from '@/api/adminUser'
import type { UserInfo } from '@/types/user'
import dayjs from 'dayjs'

const props = defineProps<{
  userType: number
}>()

const loading = ref(false)
const userList = ref<UserInfo[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])
const detailVisible = ref(false)
const currentUser = ref<UserInfo | null>(null)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const queryParams = reactive({
  userType: props.userType,
  status: undefined as number | undefined,
  startDate: '',
  endDate: '',
  keyword: '',
  page: 1,
  size: 10
})

// 监听日期范围变化
watch(dateRange, (val) => {
  if (val && val.length === 2) {
    queryParams.startDate = val[0]
    queryParams.endDate = val[1]
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
})

// 加载用户列表
const loadUserList = async () => {
  loading.value = true
  try {
    const data = await getUserListApi(queryParams) as any
    userList.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error('加载用户列表失败', e)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  queryParams.page = 1
  loadUserList()
}

// 重置
const handleReset = () => {
  queryParams.status = undefined
  queryParams.startDate = ''
  queryParams.endDate = ''
  queryParams.keyword = ''
  dateRange.value = []
  queryParams.page = 1
  loadUserList()
}

// 查看详情
const handleDetail = (row: UserInfo) => {
  currentUser.value = row
  detailVisible.value = true
}

// 禁用/启用
const handleToggleStatus = async (row: UserInfo) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 0 ? '禁用' : '启用'

  try {
    await ElMessageBox.confirm(`确定要${action}用户 "${row.username}" 吗？`, '提示', {
      type: 'warning'
    })

    await updateUserStatusApi(row.userId, newStatus)
    ElMessage.success(`${action}成功`)
    loadUserList()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

// 重置密码
const handleResetPassword = async (row: UserInfo) => {
  try {
    await ElMessageBox.confirm(`确定要重置用户 "${row.username}" 的密码吗？重置后密码为：123456`, '提示', {
      type: 'warning'
    })

    await resetPasswordApi(row.userId)
    ElMessage.success('密码重置成功，新密码为：123456')
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('重置密码失败')
    }
  }
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 获取实名状态文本
const getRealnameText = (status: number) => {
  const map: Record<number, string> = {
    0: '未认证',
    1: '已认证',
    2: '认证失败',
    3: '审核中'
  }
  return map[status] || '未知'
}

// 获取实名状态标签类型
const getRealnameTagType = (status: number) => {
  const map: Record<number, string> = {
    0: 'info',
    1: 'success',
    2: 'danger',
    3: 'warning'
  }
  return map[status] || 'info'
}

// 获取用户类型文本
const getUserTypeText = (type: number) => {
  const map: Record<number, string> = {
    1: '租客',
    2: '房东',
    3: '管理员'
  }
  return map[type] || '未知'
}

// 脱敏身份证号
const maskIdCard = (idCard: string) => {
  if (!idCard) return '-'
  if (idCard.length > 10) {
    return idCard.slice(0, 6) + '********' + idCard.slice(-4)
  }
  return idCard
}

onMounted(() => {
  loadUserList()
})
</script>

<style scoped>
.user-list-page {
  padding: 0;
}

.search-card {
  margin-bottom: 16px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.table-card {
  min-height: 400px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
