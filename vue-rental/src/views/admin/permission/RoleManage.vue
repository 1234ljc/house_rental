<template>
  <div class="role-manage">
    <!-- 操作栏 -->
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="关键词">
          <el-input v-model="keyword" placeholder="角色名称/编码" clearable style="width: 200px" @keyup.enter="loadRoleList" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadRoleList">搜索</el-button>
          <el-button type="success" @click="handleAdd">添加角色</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 角色列表 -->
    <el-card>
      <el-table :data="roleList" v-loading="loading" stripe>
        <el-table-column prop="roleId" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleDesc" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="permCount" label="权限数" width="100" />
        <el-table-column prop="userCount" label="用户数" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleCopy(row)">复制</el-button>
            <el-button type="danger" link @click="handleDelete(row)" :disabled="row.roleId === 1">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next" @size-change="loadRoleList" @current-change="loadRoleList"
        style="margin-top: 16px; justify-content: flex-end" />
    </el-card>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '添加角色'" width="700px" destroy-on-close>
      <el-form :model="roleForm" label-width="100px">
        <el-form-item label="角色编码" required>
          <el-input v-model="roleForm.roleCode" :disabled="isEdit" placeholder="如：ADMIN" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.roleName" placeholder="如：普通管理员" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="roleForm.roleDesc" type="textarea" :rows="2" placeholder="角色描述" />
        </el-form-item>
        <el-form-item label="权限分配">
          <el-tree ref="permTreeRef" :data="permTree" show-checkbox node-key="permId" :props="{ label: 'permName', children: 'children' }"
            :default-checked-keys="roleForm.permIds" default-expand-all />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 复制弹窗 -->
    <el-dialog v-model="copyVisible" title="复制角色" width="450px">
      <el-form :model="copyForm" label-width="100px">
        <el-form-item label="源角色">{{ copyForm.sourceName }}</el-form-item>
        <el-form-item label="新角色编码" required>
          <el-input v-model="copyForm.roleCode" placeholder="新角色编码" />
        </el-form-item>
        <el-form-item label="新角色名称" required>
          <el-input v-model="copyForm.roleName" placeholder="新角色名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCopySubmit" :loading="copying">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTree } from 'element-plus'
import { getRoleList, getRoleDetail, addRole, updateRole, deleteRole, copyRole, getPermissionTree } from '@/api/adminPermission'

const loading = ref(false)
const roleList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const permTreeRef = ref<InstanceType<typeof ElTree>>()
const permTree = ref<any[]>([])

const roleForm = reactive({
  roleId: 0,
  roleCode: '',
  roleName: '',
  roleDesc: '',
  permIds: [] as number[]
})

const copyVisible = ref(false)
const copying = ref(false)
const copyForm = reactive({
  sourceId: 0,
  sourceName: '',
  roleCode: '',
  roleName: ''
})

const loadRoleList = async () => {
  loading.value = true
  try {
    const res: any = await getRoleList({ keyword: keyword.value, page: page.value, size: size.value })
    roleList.value = res.records
    total.value = res.total
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const loadPermTree = async () => {
  try {
    const res: any = await getPermissionTree()
    permTree.value = res
  } catch (e) { console.error(e) }
}

const handleAdd = () => {
  isEdit.value = false
  roleForm.roleId = 0
  roleForm.roleCode = ''
  roleForm.roleName = ''
  roleForm.roleDesc = ''
  roleForm.permIds = []
  dialogVisible.value = true
}

const handleEdit = async (row: any) => {
  isEdit.value = true
  try {
    const res: any = await getRoleDetail(row.roleId)
    roleForm.roleId = res.roleId
    roleForm.roleCode = res.roleCode
    roleForm.roleName = res.roleName
    roleForm.roleDesc = res.roleDesc || ''
    roleForm.permIds = res.permIds || []
    dialogVisible.value = true
  } catch (e) { console.error(e) }
}

const handleSubmit = async () => {
  if (!roleForm.roleCode || !roleForm.roleName) {
    ElMessage.warning('请填写角色编码和名称')
    return
  }
  submitting.value = true
  try {
    const checkedKeys = permTreeRef.value?.getCheckedKeys(false) as number[] || []
    const halfCheckedKeys = permTreeRef.value?.getHalfCheckedKeys() as number[] || []
    const permIds = [...checkedKeys, ...halfCheckedKeys]
    
    if (isEdit.value) {
      await updateRole(roleForm.roleId, { roleName: roleForm.roleName, roleDesc: roleForm.roleDesc, permIds })
      ElMessage.success('更新成功')
    } else {
      await addRole({ roleCode: roleForm.roleCode, roleName: roleForm.roleName, roleDesc: roleForm.roleDesc, permIds })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadRoleList()
  } catch (e) { console.error(e) }
  finally { submitting.value = false }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除角色"${row.roleName}"吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteRole(row.roleId)
      ElMessage.success('删除成功')
      loadRoleList()
    }).catch(() => {})
}

const handleCopy = (row: any) => {
  copyForm.sourceId = row.roleId
  copyForm.sourceName = row.roleName
  copyForm.roleCode = ''
  copyForm.roleName = ''
  copyVisible.value = true
}

const handleCopySubmit = async () => {
  if (!copyForm.roleCode || !copyForm.roleName) {
    ElMessage.warning('请填写新角色编码和名称')
    return
  }
  copying.value = true
  try {
    await copyRole(copyForm.sourceId, { roleCode: copyForm.roleCode, roleName: copyForm.roleName })
    ElMessage.success('复制成功')
    copyVisible.value = false
    loadRoleList()
  } catch (e) { console.error(e) }
  finally { copying.value = false }
}

onMounted(() => {
  loadRoleList()
  loadPermTree()
})
</script>

<style scoped>
.role-manage { padding: 20px; }
.filter-card { margin-bottom: 16px; }
</style>
