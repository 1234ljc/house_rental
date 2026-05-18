<template>
  <div class="my-contract-page">
    <el-card class="header-card">
      <div class="header-content">
        <div class="header-left">
          <h2>我的合同</h2>
          <p class="sub-title">管理您的合同文件，可上传、下载、删除</p>
        </div>
        <div class="header-right">
          <div class="stat-item">
            <span class="stat-num">{{ contractList.length }}</span>
            <span class="stat-label">合同文件</span>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="main-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索合同名称" clearable style="width: 200px" @keyup.enter="filterList">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="filterList">搜索</el-button>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="showUploadDialog">
            <el-icon><Upload /></el-icon> 上传合同
          </el-button>
        </div>
      </div>

      <el-table :data="filteredList" v-loading="loading" stripe>
        <el-table-column label="合同名称" prop="name" min-width="200" />
        <el-table-column label="文件名" prop="fileName" min-width="200">
          <template #default="{ row }">
            <el-icon><Document /></el-icon> {{ row.fileName }}
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="100">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredList.length === 0" description="暂无合同文件" />
    </el-card>

    <!-- 上传弹窗 -->
    <el-dialog v-model="uploadVisible" title="上传合同" width="500px">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="合同名称" required>
          <el-input v-model="uploadForm.name" placeholder="请输入合同名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="选择文件" required>
          <el-upload ref="uploadRef" :auto-upload="false" :limit="1" :on-change="handleFileChange"
            :on-remove="handleFileRemove" accept=".doc,.docx,.pdf">
            <el-button type="primary"><el-icon><Upload /></el-icon> 选择文件</el-button>
            <template #tip>
              <div class="upload-tip">支持 doc, docx, pdf 格式，文件大小不超过 10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, Document } from '@element-plus/icons-vue'
import { getMyContractsApi, uploadMyContractApi, deleteMyContractApi, downloadMyContractApi } from '@/api/landlordContract'

const loading = ref(false)
const contractList = ref<any[]>([])
const keyword = ref('')

const uploadVisible = ref(false)
const uploadForm = reactive({ name: '' })
const uploadFile = ref<File | null>(null)
const uploading = ref(false)
const uploadRef = ref()

const filteredList = computed(() => {
  if (!keyword.value) return contractList.value
  return contractList.value.filter(c => c.name.includes(keyword.value) || c.fileName.includes(keyword.value))
})

const formatFileSize = (size: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

const formatTime = (time: string) => time ? new Date(time).toLocaleString('zh-CN') : ''

const filterList = () => { /* computed 自动过滤 */ }

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getMyContractsApi()
    contractList.value = res || []
  } finally { loading.value = false }
}

const showUploadDialog = () => {
  uploadForm.name = ''
  uploadFile.value = null
  uploadRef.value?.clearFiles()
  uploadVisible.value = true
}

const handleFileChange = (file: any) => { uploadFile.value = file.raw }
const handleFileRemove = () => { uploadFile.value = null }

const submitUpload = async () => {
  if (!uploadForm.name.trim()) { ElMessage.warning('请输入合同名称'); return }
  if (!uploadFile.value) { ElMessage.warning('请选择文件'); return }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    formData.append('name', uploadForm.name)
    
    await uploadMyContractApi(formData)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    loadList()
  } finally { uploading.value = false }
}

const handleDownload = (row: any) => {
  downloadMyContractApi(row.id)
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定删除该合同吗？', '删除确认', { type: 'warning' })
    await deleteMyContractApi(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) { /* 取消 */ }
}

onMounted(() => { loadList() })
</script>

<style scoped>
.my-contract-page { padding: 20px; background: #f5f5f5; min-height: calc(100vh - 60px); }
.header-card { margin-bottom: 20px; }
.header-content { display: flex; justify-content: space-between; align-items: center; }
.header-left h2 { margin: 0 0 8px 0; font-size: 20px; }
.sub-title { margin: 0; color: #666; font-size: 14px; }
.header-right { display: flex; gap: 30px; }
.stat-item { text-align: center; }
.stat-num { display: block; font-size: 28px; font-weight: bold; color: #409eff; }
.stat-label { color: #666; font-size: 13px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 20px; }
.toolbar-left { display: flex; gap: 10px; }
.upload-tip { color: #999; font-size: 12px; margin-top: 5px; }
</style>
