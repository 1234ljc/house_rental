<template>
  <div class="credit-score-page">
    <div class="page-header">
      <h3>信用评分</h3>
      <p class="desc">您的平台信用评分，基于您的行为自动计算</p>
    </div>

    <div class="credit-content" v-loading="loading">
      <!-- 信用分展示 -->
      <div class="credit-score-display">
        <div class="score-circle">
          <div class="score-number" :class="getCreditScoreClass(creditReport?.score)">
            {{ creditReport?.score || 600 }}
          </div>
          <div class="score-label">信用分</div>
        </div>
        <div class="score-info">
          <div class="score-level">
            <el-tag :type="getCreditScoreType(creditReport?.score || 600)" size="large">
              {{ creditReport?.level || '一般' }}
            </el-tag>
          </div>
          <el-button type="primary" @click="refreshCreditScore" :loading="loading">
            <el-icon><Refresh /></el-icon>
            刷新信用分
          </el-button>
        </div>
      </div>

      <!-- 分数构成 -->
      <div class="credit-details" v-if="creditReport">
        <h4>分数构成</h4>
        
        <!-- 基础分 -->
        <div class="detail-item completed">
          <div class="item-left">
            <el-icon class="check-icon"><CircleCheckFilled /></el-icon>
            <span class="item-label">基础分</span>
          </div>
          <span class="item-value">+{{ creditReport.details?.baseScore || 600 }}分</span>
        </div>

        <!-- 实名认证 -->
        <div class="detail-item" :class="creditReport.details?.realnameScore ? 'completed' : 'pending'">
          <div class="item-left">
            <el-icon v-if="creditReport.details?.realnameScore" class="check-icon"><CircleCheckFilled /></el-icon>
            <el-icon v-else class="pending-icon"><WarningFilled /></el-icon>
            <span class="item-label">实名认证</span>
          </div>
          <span class="item-value" v-if="creditReport.details?.realnameScore">
            已完成 +{{ creditReport.details.realnameScore }}分
          </span>
          <span class="item-value pending-text" v-else>
            待完成 (可获得+100分)
          </span>
        </div>

        <!-- 账号时长 -->
        <div class="detail-item" :class="creditReport.details?.timeScore >= 50 ? 'completed' : 'in-progress'">
          <div class="item-left">
            <el-icon v-if="creditReport.details?.timeScore >= 50" class="check-icon"><CircleCheckFilled /></el-icon>
            <el-icon v-else class="progress-icon"><Clock /></el-icon>
            <span class="item-label">账号使用时长</span>
          </div>
          <span class="item-value" v-if="creditReport.details?.timeScore">
            已获得 +{{ creditReport.details.timeScore }}分 / 最高50分
          </span>
          <span class="item-value pending-text" v-else>
            待积累 (最高+50分)
          </span>
        </div>

        <!-- 完成合同 -->
        <div class="detail-item" :class="creditReport.details?.contractScore >= 200 ? 'completed' : 'in-progress'">
          <div class="item-left">
            <el-icon v-if="creditReport.details?.contractScore >= 200" class="check-icon"><CircleCheckFilled /></el-icon>
            <el-icon v-else class="progress-icon"><Clock /></el-icon>
            <span class="item-label">完成租赁合同</span>
          </div>
          <span class="item-value" v-if="creditReport.details?.contractScore">
            已完成{{ creditReport.details.completedContracts }}个 +{{ creditReport.details.contractScore }}分 / 最高200分
          </span>
          <span class="item-value pending-text" v-else>
            待完成 (每个+50分，最高200分)
          </span>
        </div>
      </div>

      <!-- 提升建议 -->
      <div class="credit-suggestions" v-if="creditReport?.suggestions?.length">
        <h4>提升建议</h4>
        <div class="suggestion-list">
          <div v-for="(item, index) in creditReport.suggestions" :key="index" class="suggestion-item">
            <el-icon color="#409eff"><InfoFilled /></el-icon>
            <span>{{ item }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, InfoFilled, CircleCheckFilled, WarningFilled, Clock } from '@element-plus/icons-vue'
import { getCreditReportApi, updateCreditScoreApi } from '@/api/credit'

const loading = ref(false)
const creditReport = ref<any>(null)

// 获取信用报告
const fetchCreditReport = async () => {
  loading.value = true
  try {
    const res = await getCreditReportApi()
    creditReport.value = res
  } catch (error) {
    console.error('获取信用报告失败', error)
    ElMessage.error('获取信用报告失败')
  } finally {
    loading.value = false
  }
}

// 刷新信用分
const refreshCreditScore = async () => {
  loading.value = true
  try {
    await updateCreditScoreApi()
    await fetchCreditReport()
    ElMessage.success('信用分已更新')
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 信用分样式
const getCreditScoreClass = (score: number) => {
  if (score >= 900) return 'excellent'
  if (score >= 800) return 'good'
  if (score >= 700) return 'normal'
  if (score >= 600) return 'fair'
  return 'poor'
}

const getCreditScoreType = (score: number) => {
  if (score >= 900) return 'success'
  if (score >= 800) return 'success'
  if (score >= 700) return ''
  if (score >= 600) return 'warning'
  return 'danger'
}

onMounted(() => {
  fetchCreditReport()
})
</script>

<style scoped>
.credit-score-page {
  padding: 8px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h3 {
  font-size: 18px;
  color: #303133;
  margin: 0 0 8px 0;
}

.page-header .desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.credit-content {
  min-height: 400px;
}

/* 信用分展示 */
.credit-score-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  margin-bottom: 32px;
}

.score-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 140px;
  height: 140px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.score-number {
  font-size: 42px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 6px;
}

.score-number.excellent {
  color: #52c41a;
}

.score-number.good {
  color: #1890ff;
}

.score-number.normal {
  color: #13c2c2;
}

.score-number.fair {
  color: #faad14;
}

.score-number.poor {
  color: #f5222d;
}

.score-label {
  font-size: 14px;
  color: #666;
}

.score-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 20px;
}

.score-level .el-tag {
  font-size: 20px;
  padding: 10px 24px;
}

/* 分数构成 */
.credit-details {
  margin-bottom: 32px;
}

.credit-details h4 {
  font-size: 16px;
  color: #303133;
  margin: 0 0 20px 0;
  font-weight: 600;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  margin-bottom: 12px;
  border-radius: 8px;
  border: 2px solid #f0f0f0;
  transition: all 0.3s;
}

.detail-item.completed {
  background: #f6ffed;
  border-color: #b7eb8f;
}

.detail-item.pending {
  background: #fff7e6;
  border-color: #ffd591;
}

.detail-item.in-progress {
  background: #e6f7ff;
  border-color: #91d5ff;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.check-icon {
  font-size: 20px;
  color: #52c41a;
}

.pending-icon {
  font-size: 20px;
  color: #faad14;
}

.progress-icon {
  font-size: 20px;
  color: #1890ff;
}

.item-label {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
}

.item-value {
  font-size: 14px;
  color: #52c41a;
  font-weight: 600;
}

.pending-text {
  color: #faad14;
}

/* 提升建议 */
.credit-suggestions h4 {
  font-size: 16px;
  color: #303133;
  margin: 0 0 16px 0;
  font-weight: 600;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  background: #f0f9ff;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  border-left: 3px solid #409eff;
}

.suggestion-item .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}
</style>
