<template>
  <el-dialog
    v-model="visible"
    title="AI房源对比分析"
    width="900px"
    :close-on-click-modal="false"
  >
    <div class="compare-dialog">
      <!-- 选择的房源列表 -->
      <div class="selected-houses" v-if="selectedHouses.length > 0">
        <div class="house-card" v-for="(house, index) in selectedHouses" :key="house.houseId">
          <div class="house-header">
            <span class="house-number">房源{{ index + 1 }}</span>
            <el-button link type="danger" @click="removeHouse(index)" size="small">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <div class="house-info">
            <div class="house-title">{{ house.title }}</div>
            <div class="house-details">
              <span>{{ house.houseType }}</span>
              <span>{{ house.area }}㎡</span>
              <span class="price">{{ house.rentPrice }}元/月</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 提示信息 -->
      <el-alert
        v-if="selectedHouses.length < 2"
        title="请至少选择2个房源进行对比（最多3个）"
        type="info"
        :closable="false"
        style="margin: 20px 0"
      />

      <!-- AI分析结果 -->
      <div class="analysis-result" v-if="analysisResult">
        <div class="result-header">
          <el-icon><MagicStick /></el-icon>
          <span>AI分析结果</span>
        </div>
        <div class="result-content" v-html="formatAnalysis(analysisResult)"></div>
      </div>

      <!-- 加载状态 -->
      <div class="loading-state" v-if="analyzing">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>AI正在分析中，请稍候...</span>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button
        type="primary"
        @click="startAnalysis"
        :loading="analyzing"
        :disabled="selectedHouses.length < 2"
      >
        开始对比分析
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Close, MagicStick, Loading } from '@element-plus/icons-vue'
import { compareHousesApi } from '@/api/ai'
import { ElMessage } from 'element-plus'

const visible = ref(false)
const selectedHouses = ref<any[]>([])
const analyzing = ref(false)
const analysisResult = ref('')

const open = (houses: any[]) => {
  selectedHouses.value = houses.slice(0, 3) // 最多3个
  analysisResult.value = ''
  visible.value = true
}

const removeHouse = (index: number) => {
  selectedHouses.value.splice(index, 1)
  analysisResult.value = '' // 清空之前的分析结果
}

const startAnalysis = async () => {
  if (selectedHouses.value.length < 2) {
    ElMessage.warning('请至少选择2个房源')
    return
  }

  analyzing.value = true
  try {
    const res: any = await compareHousesApi({
      houses: selectedHouses.value
    })

    if (res) {
      analysisResult.value = res
      ElMessage.success('分析完成')
    } else {
      ElMessage.error('分析失败，请稍后重试')
    }
  } catch (error) {
    console.error('AI对比分析失败:', error)
    ElMessage.error('分析失败，请稍后重试')
  } finally {
    analyzing.value = false
  }
}

const formatAnalysis = (text: string) => {
  // 将换行符转换为<br>
  return text.replace(/\n/g, '<br>')
}

defineExpose({
  open
})
</script>

<style scoped>
.compare-dialog {
  min-height: 300px;
}

.selected-houses {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.house-card {
  flex: 1;
  min-width: 250px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 15px;
  background: #f9f9f9;
}

.house-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.house-number {
  font-weight: bold;
  color: #409eff;
}

.house-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.house-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.house-details {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #666;
}

.house-details .price {
  color: #ff6600;
  font-weight: bold;
}

.analysis-result {
  margin-top: 20px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 15px;
}

.result-content {
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  color: #409eff;
  font-size: 14px;
}

.loading-state .el-icon {
  font-size: 24px;
}
</style>
