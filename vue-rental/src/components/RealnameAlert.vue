<template>
  <el-alert
    v-if="show"
    :title="title"
    :type="alertType"
    show-icon
    :closable="false"
    class="realname-alert"
  >
    <template #default>
      <div class="alert-content">
        <span>{{ description }}</span>
        <el-button v-if="showButton" type="primary" size="small" @click="goToRealname">
          {{ buttonText }}
        </el-button>
      </div>
    </template>
  </el-alert>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const props = withDefaults(defineProps<{
  requireRealname?: boolean // 是否需要实名认证才能使用该功能
  featureName?: string // 功能名称，用于提示
}>(), {
  requireRealname: true,
  featureName: '该功能'
})

const router = useRouter()
const userStore = useUserStore()

const realnameStatus = computed(() => userStore.realnameStatus)
const userType = computed(() => userStore.userType)

// 是否显示提示
// realnameStatus: -1=未认证, 0=审核中, 1=已通过, 2=已驳回
const show = computed(() => {
  if (!props.requireRealname) return false
  // 已通过认证不显示
  if (realnameStatus.value === 1) return false
  return true
})

// 提示类型
const alertType = computed(() => {
  if (realnameStatus.value === 0) return 'warning'
  if (realnameStatus.value === 2) return 'error'
  return 'info' // -1 或其他值表示未认证
})

// 标题
const title = computed(() => {
  if (realnameStatus.value === 0) return '实名认证审核中'
  if (realnameStatus.value === 2) return '实名认证被驳回'
  return '请先完成实名认证'
})

// 描述
const description = computed(() => {
  if (realnameStatus.value === 0) {
    return `您的实名认证正在审核中，审核通过后即可使用${props.featureName}`
  }
  if (realnameStatus.value === 2) {
    return `您的实名认证被驳回，请重新提交认证后使用${props.featureName}`
  }
  return `${props.featureName}需要完成实名认证后才能使用`
})

// 是否显示按钮
const showButton = computed(() => {
  // 审核中不显示按钮
  return realnameStatus.value !== 0
})

// 按钮文字
const buttonText = computed(() => {
  if (realnameStatus.value === 2) return '重新认证'
  return '去认证'
})

// 跳转到实名认证页面
const goToRealname = () => {
  if (userType.value === 1) {
    // 租客
    router.push('/tenant/profile/realname')
  } else if (userType.value === 2) {
    // 房东
    router.push({ path: '/landlord/profile', query: { tab: 'realname' } })
  }
}
</script>

<style scoped>
.realname-alert {
  margin-bottom: 16px;
}

.alert-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 16px;
}
</style>
