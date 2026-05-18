<template>
  <div class="tenant-dashboard">
    <!-- Banner区域：顶部大图 + 搜索框，是首页第一屏的视觉入口 -->
    <section class="banner-section">
      <div class="banner-content">
        <div class="search-box" v-click-outside="() => showHistory = false">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索房源标题、描述、地址"
            size="large"
            class="search-input"
            @keyup.enter="handleSearch"
            @focus="showHistory = true"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button type="primary" @click="handleSearch">
                搜索
              </el-button>
            </template>
          </el-input>
          <!-- 搜索历史下拉面板 -->
          <div class="history-dropdown" v-if="showHistory && searchHistory.length > 0">
            <div class="history-header">
              <span class="history-title">搜索历史</span>
              <span class="history-clear" @click.stop="clearHistory">清空</span>
            </div>
            <div class="history-list">
              <span 
                v-for="(item, index) in searchHistory" 
                :key="index"
                class="history-item"
                @click="selectHistory(item)"
              >
                {{ item }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>



    <!-- 主内容区 -->
    <div class="main-wrapper">
      <div class="content-area">
        <!-- 热门房源 -->
        <section class="house-section">
          <div class="section-header">
            <div class="section-title">
              <h2>热门房源</h2>
              <p>好房源那么多，我们为你精选</p>
            </div>
            <el-button type="primary" link @click="handleMore('hot')">
              更多房源 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div class="house-grid" v-loading="loading.hot">
            <div 
              class="house-card" 
              v-for="house in hotHouses" 
              :key="house.houseId"
              @click="handleHouseClick(house)"
            >
              <div class="house-image">
                <img :src="getFirstImage(house.images)" alt="房源图片" />
              </div>
              <div class="house-info">
                <h3 class="house-title">{{ house.title }}</h3>
                <p class="house-address">{{ house.address }}</p>
                <div class="house-meta">
                  <span class="house-type">{{ house.houseType }}</span>
                  <span class="house-area">{{ house.area }}㎡</span>
                </div>
                <div class="house-price">
                  <span class="price">¥{{ house.rentPrice }}</span>
                  <span class="unit">/月</span>
                </div>
              </div>
            </div>
            <el-empty v-if="!hotHouses.length && !loading.hot" description="暂无热门房源" />
          </div>
        </section>

        <!-- 低价房源 -->
        <section class="house-section">
          <div class="section-header">
            <div class="section-title">
              <h2>低价房源</h2>
              <p>性价比租房，你值得拥有</p>
            </div>
            <el-button type="primary" link @click="handleMore('cheap')">
              更多房源 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div class="house-grid" v-loading="loading.cheap">
            <div 
              class="house-card" 
              v-for="house in cheapHouses" 
              :key="house.houseId"
              @click="handleHouseClick(house)"
            >
              <div class="house-image">
                <img :src="getFirstImage(house.images)" alt="房源图片" />
              </div>
              <div class="house-info">
                <h3 class="house-title">{{ house.title }}</h3>
                <p class="house-address">{{ house.address }}</p>
                <div class="house-meta">
                  <span class="house-type">{{ house.houseType }}</span>
                  <span class="house-area">{{ house.area }}㎡</span>
                </div>
                <div class="house-price">
                  <span class="price">¥{{ house.rentPrice }}</span>
                  <span class="unit">/月</span>
                </div>
              </div>
            </div>
            <el-empty v-if="!cheapHouses.length && !loading.cheap" description="暂无低价房源" />
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, ArrowRight } from '@element-plus/icons-vue'
import { getHotHousesApi, getCheapHousesApi } from '@/api/tenant'

import type { HouseInfo } from '@/types/tenant'

const router = useRouter()
const searchKeyword = ref('')
const hotHouses = ref<HouseInfo[]>([])
const cheapHouses = ref<HouseInfo[]>([])


// 当前选中的城市
const selectedCity = ref(localStorage.getItem('selectedCity') || '')

// 搜索历史
const showHistory = ref(false)
const searchHistory = ref<string[]>([])
const HISTORY_KEY = 'tenant_search_history'
const MAX_HISTORY = 10

// 加载搜索历史
const loadSearchHistory = () => {
  try {
    const history = localStorage.getItem(HISTORY_KEY)
    searchHistory.value = history ? JSON.parse(history) : []
  } catch {
    searchHistory.value = []
  }
}

// 保存搜索历史
const saveSearchHistory = (keyword: string) => {
  if (!keyword.trim()) return
  // 去重并放到最前面
  const history = searchHistory.value.filter(item => item !== keyword)
  history.unshift(keyword)
  // 限制数量
  searchHistory.value = history.slice(0, MAX_HISTORY)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(searchHistory.value))
}

// 清空搜索历史
const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem(HISTORY_KEY)
}

// 选择历史记录
const selectHistory = (keyword: string) => {
  searchKeyword.value = keyword
  showHistory.value = false
  handleSearch()
}

// 自定义指令：点击外部关闭
const vClickOutside = {
  mounted(el: HTMLElement, binding: any) {
    el._clickOutside = (event: MouseEvent) => {
      if (!(el === event.target || el.contains(event.target as Node))) {
        binding.value()
      }
    }
    document.addEventListener('click', el._clickOutside)
  },
  unmounted(el: HTMLElement) {
    document.removeEventListener('click', el._clickOutside)
  }
}

const loading = reactive({
  hot: false,
  cheap: false
})

// 默认房源图片
const defaultHouseImage = 'https://fuss10.elemecdn.com/e/5d/4a731a90594a4af544c0c25941171jpeg.jpeg'

// 获取第一张图片
const getFirstImage = (images: string): string => {
  if (!images) return defaultHouseImage
  try {
    const arr = JSON.parse(images)
    return arr[0] || defaultHouseImage
  } catch {
    return defaultHouseImage
  }
}

// 搜索
const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (keyword) {
    saveSearchHistory(keyword)
  }
  showHistory.value = false
  router.push({ path: '/tenant/search', query: { keyword: searchKeyword.value || undefined } })
}

// 更多房源
const handleMore = (type: string) => {
  router.push({ path: '/tenant/search', query: { sort: type === 'hot' ? 'hot' : 'price_asc' } })
}

// 房源点击 - 跳转到详情页
const handleHouseClick = (house: HouseInfo) => {
  router.push(`/tenant/house/${house.houseId}`)
}

// 加载热门房源
const loadHotHouses = async () => {
  loading.hot = true
  try {
    const data = await getHotHousesApi(selectedCity.value || undefined)
    hotHouses.value = data as unknown as HouseInfo[]
  } catch (e) {
    console.error('加载热门房源失败', e)
  } finally {
    loading.hot = false
  }
}

// 加载低价房源
const loadCheapHouses = async () => {
  loading.cheap = true
  try {
    const data = await getCheapHousesApi(selectedCity.value || undefined)
    cheapHouses.value = data as unknown as HouseInfo[]
  } catch (e) {
    console.error('加载低价房源失败', e)
  } finally {
    loading.cheap = false
  }
}

// 加载所有房源
const loadAllHouses = () => {
  loadHotHouses()
  loadCheapHouses()
}

// 监听城市变化事件
const onCityChanged = (event: CustomEvent) => {
  selectedCity.value = event.detail || ''
  loadAllHouses()
}

onMounted(() => {
  loadSearchHistory()
  loadAllHouses()
  // 监听城市变化事件
  window.addEventListener('cityChanged', onCityChanged as EventListener)
})
</script>

<style scoped>
.tenant-dashboard {
  min-height: calc(100vh - 60px);
}



/* Banner区域：通过背景图和遮罩层实现首页第一屏展示 */
.banner-section {
  /* 默认背景色，当图片加载失败时显示 */
  background-color: #e8e4df;
  /* 使用原登录注册的背景图 */
  background-image: url('https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=2500&q=80');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  /* 按2500*1050比例计算高度，假设宽度100vw */
  height: 42vw;
  max-height: 525px;
  min-height: 300px;
  padding: 0 24px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 遮罩层让搜索框更清晰 */
.banner-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.2);
}

.banner-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
}

.search-box {
  width: 60%;
  max-width: 900px;
  min-width: 500px;
  position: relative;
}

.search-input {
  background: #fff;
  border-radius: 8px 8px 0 0;
  overflow: hidden;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: none;
  padding: 12px 16px;
}

.search-input :deep(.el-input__prefix) {
  color: #909399;
  margin-right: 8px;
}

.search-input :deep(.el-input-group__append) {
  background: #1890ff;
  border: none;
  padding: 0 32px;
}

.search-input :deep(.el-input-group__append .el-button) {
  color: #fff;
  font-size: 15px;
}

/* 搜索历史下拉面板 - 紧贴搜索框下方 */
.history-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 100;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.history-title {
  font-size: 14px;
  color: #606266;
}

.history-clear {
  font-size: 13px;
  color: #909399;
  cursor: pointer;
}

.history-clear:hover {
  color: #1890ff;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 16px;
}

.history-item {
  padding: 6px 14px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
}

.history-item:hover {
  background: #e6f7ff;
  color: #1890ff;
}

/* 主内容区 */
.main-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  gap: 24px;
}

.content-area {
  flex: 1;
}
.house-section {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.section-title h2 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 4px;
}

.section-title p {
  font-size: 14px;
  color: #909399;
}

/* 房源网格 */
.house-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.house-card {
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e4e7ed;
}

.house-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.house-image {
  height: 140px;
  overflow: hidden;
  background: #f5f7fa;
}

.house-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.house-info {
  padding: 12px;
}

.house-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.house-address {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.house-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.house-type, .house-area {
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.house-price {
  display: flex;
  align-items: baseline;
}

.house-price .price {
  font-size: 18px;
  font-weight: bold;
  color: #ff4d4f;
}

.house-price .unit {
  font-size: 12px;
  color: #909399;
  margin-left: 2px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .house-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .house-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 576px) {
  .house-grid {
    grid-template-columns: 1fr;
  }
}
</style>
