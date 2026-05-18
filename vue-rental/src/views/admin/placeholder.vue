<template>
  <div class="placeholder-page">
    <el-empty description="功能开发中...">
      <template #image>
        <el-icon :size="100" color="#909399"><Setting /></el-icon>
      </template>
    </el-empty>
    <p class="page-info">当前页面：{{ route.path }}</p>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Setting } from '@element-plus/icons-vue'

const route = useRoute()
</script>

<style scoped>
.placeholder-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: #fff;
  border-radius: 8px;
}

.page-info {
  margin-top: 16px;
  color: #909399;
  font-size: 14px;
}
</style>
