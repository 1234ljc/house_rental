<template>
  <div class="admin-manage">
    <!-- 操作栏 -->
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="用户名/姓名/手机号" clearable style="width: 200px" @keyup.enter="loadAdminList" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="status" style="width: 120px">
            <el-option label="全部" :value="-1" />
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAdminList">搜索</el-button>
          <el-button type="success" @click="handleAdd">添加管理员</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 管理员列表 -->
    <el-card>
      <el-table :data="adminList" v-loading="loading" stripe>
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="name in row.roleNames" :key="name" size="small" style="margin-right: 4px">{{ name }}</el-tag>
            <span v-if="!row.roleNames?.length" class="no-role">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button :type="row.status === 1 ? 'warning' : 'success'" link @click="handleToggleStatus(row)" :disabled="row.userId === 1">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)" :disabled="row.userId === 1">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next" @size-change="loadAdminList" @current-change="loadAdminList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑管理员' : '添加管理员'" width="550px" destroy-on-close>
      <el-form :model="adminForm" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="adminForm.username" :disabled="isEdit" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码" :required="!isEdit">
          <el-input v-model="adminForm.password" type="password" show-password :placeholder="isEdit ? '不修改请留空' : '登录密码'" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="adminForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="adminForm.phone" placeholder="手机号码" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="adminForm.email" placeholder="电子邮箱" />
        </el-form-item>
        <el-form-item label="分配角色">
          <el-checkbox-group v-model="adminForm.roleIds">
            <el-checkbox v-for="role in roleList" :key="role.roleId" :value="role.roleId">{{ role.roleName }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminList, addAdmin, updateAdmin, toggleAdminStatus, deleteAdmin, getAllRoles } from '@/api/adminPermission'

const loading = ref(false)
const adminList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const status = ref(-1)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const roleList = ref<any[]>([])

const adminForm = reactive({
  userId: 0,
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  roleIds: [] as number[]
})

const loadAdminList = async () => {
  loading.value = true
  try {
    const res: any = await getAdminList({ keyword: keyword.value, status: status.value, page: page.value, size: size.value })
    adminList.value = res.records
    total.value = res.total
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const loadRoles = async () => {
  try {
    const res: any = await getAllRoles()
    roleList.value = res
  } catch (e) { console.error(e) }
}

const handleAdd = () => {
  isEdit.value = false
  adminForm.userId = 0
  adminForm.username = ''
  adminForm.password = ''
  adminForm.realName = ''
  adminForm.phone = ''
  adminForm.email = ''
  adminForm.roleIds = []
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  adminForm.userId = row.userId
  adminForm.username = row.username
  adminForm.password = ''
  adminForm.realName = row.realName || ''
  adminForm.phone = row.phone || ''
  adminForm.email = row.email || ''
  adminForm.roleIds = row.roleIds || []
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!adminForm.username) {
    ElMessage.warning('请填写用户名')
    return
  }
  if (!isEdit.value && !adminForm.password) {
    ElMessage.warning('请填写密码')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      const data: any = { realName: adminForm.realName, phone: adminForm.phone, email: adminForm.email, roleIds: adminForm.roleIds }
      if (adminForm.password) data.password = adminForm.password
      await updateAdmin(adminForm.userId, data)
      ElMessage.success('更新成功')
    } else {
      await addAdmin({
        username: adminForm.username,
        password: adminForm.password,
        realName: adminForm.realName,
        phone: adminForm.phone,
        email: adminForm.email,
        roleIds: adminForm.roleIds
      })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadAdminList()
  } catch (e) { console.error(e) }
  finally { submitting.value = false }
}

const handleToggleStatus = (row: any) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  ElMessageBox.confirm(`确定${action}管理员"${row.username}"吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await toggleAdminStatus(row.userId, newStatus)
      ElMessage.success(`已${action}`)
      loadAdminList()
    }).catch(() => {})
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除管理员"${row.username}"吗？此操作不可恢复！`, '警告', { type: 'error' })
    .then(async () => {
      await deleteAdmin(row.userId)
      ElMessage.success('删除成功')
      loadAdminList()
    }).catch(() => {})
}

onMounted(() => {
  loadAdminList()
  loadRoles()
})
</script>

<style scoped>
.admin-manage { padding: 20px; }
.filter-card { margin-bottom: 16px; }
.no-role { color: #909399; font-size: 12px; }
</style>
