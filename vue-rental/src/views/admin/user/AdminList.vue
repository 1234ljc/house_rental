<template>
  <div class="admin-list-page">
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
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon> 添加管理员
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
        <el-table-column prop="email" label="邮箱" width="160">
          <template #default="{ row }">
            {{ row.email || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="实名状态" width="110">
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
    <el-dialog v-model="detailVisible" title="管理员详情" width="600px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="用户ID">{{ currentUser.userId }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ currentUser.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实名状态">
          <el-tag :type="getRealnameTagType(currentUser.realnameStatus)">
            {{ getRealnameText(currentUser.realnameStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="currentUser.status === 1 ? 'success' : 'danger'">
            {{ currentUser.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">{{ formatTime(currentUser.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 添加管理员弹窗 -->
    <el-dialog v-model="addVisible" title="添加管理员" width="500px" @close="resetAddForm">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" placeholder="请输入用户名" maxlength="20" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" placeholder="请输入密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="addForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="addForm.realName" placeholder="请输入真实姓名（选填）" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号（选填）" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="addForm.email" placeholder="请输入邮箱（选填）" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleSubmitAdd">确定添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, View, Lock, Unlock, Key, User, Plus } from '@element-plus/icons-vue'
import { getUserListApi, updateUserStatusApi, resetPasswordApi, addAdminApi } from '@/api/adminUser'
import type { UserInfo } from '@/types/user'
import dayjs from 'dayjs'

const loading = ref(false)
const userList = ref<UserInfo[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])
const detailVisible = ref(false)
const currentUser = ref<UserInfo | null>(null)

// 添加管理员相关
const addVisible = ref(false)
const addLoading = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: ''
})

// 表单验证规则
const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== addForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validatePhone = (rule: any, value: string, callback: any) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const validateEmail = (rule: any, value: string, callback: any) => {
  if (value && !/^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/.test(value)) {
    callback(new Error('请输入正确的邮箱格式'))
  } else {
    callback()
  }
}

const addRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ]
}

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const queryParams = reactive({
  userType: 3, // 管理员
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

// 添加管理员
const handleAdd = () => {
  addVisible.value = true
}

// 重置添加表单
const resetAddForm = () => {
  addForm.username = ''
  addForm.password = ''
  addForm.confirmPassword = ''
  addForm.realName = ''
  addForm.phone = ''
  addForm.email = ''
  addFormRef.value?.resetFields()
}

// 提交添加
const handleSubmitAdd = async () => {
  if (!addFormRef.value) return

  try {
    await addFormRef.value.validate()
    addLoading.value = true

    await addAdminApi({
      username: addForm.username,
      password: addForm.password,
      realName: addForm.realName || undefined,
      phone: addForm.phone || undefined,
      email: addForm.email || undefined
    })

    ElMessage.success('添加管理员成功')
    addVisible.value = false
    loadUserList()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message) {
      // 表单验证失败不提示
    }
  } finally {
    addLoading.value = false
  }
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
    await ElMessageBox.confirm(`确定要${action}管理员 "${row.username}" 吗？`, '提示', {
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
    await ElMessageBox.confirm(`确定要重置管理员 "${row.username}" 的密码吗？重置后密码为：123456`, '提示', {
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

onMounted(() => {
  loadUserList()
})
</script>

<style scoped>
.admin-list-page {
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
