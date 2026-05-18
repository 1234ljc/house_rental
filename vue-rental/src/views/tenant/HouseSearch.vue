<template>
  <div class="house-search-page">
    <!-- 搜索栏 -->
    <div class="search-header">
      <el-input v-model="searchParams.keyword" placeholder="搜索房源标题、描述、地址" class="search-input" size="large" clearable @keyup.enter="doSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
        <template #append>
          <el-button type="primary" @click="doSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <div class="filter-row">
        <span class="filter-label">位置：</span>
        <div class="filter-content">
          <el-select v-model="searchParams.province" placeholder="省份" style="width:120px" clearable @change="onProvinceChange" filterable>
            <el-option v-for="p in provinceList" :key="p" :label="p" :value="p" />
          </el-select>
          <el-select v-model="searchParams.city" placeholder="城市" style="width:120px" clearable @change="onCityChange" :disabled="!searchParams.province" filterable>
            <el-option v-for="c in cityList" :key="c" :label="c" :value="c" />
          </el-select>
          <el-select v-model="searchParams.district" placeholder="区县" style="width:120px" clearable :disabled="!searchParams.city" filterable>
            <el-option v-for="d in districtList" :key="d" :label="d" :value="d" />
          </el-select>
        </div>
      </div>
      <div class="filter-row">
        <span class="filter-label">租金：</span>
        <div class="filter-content">
          <el-tag v-for="item in priceRanges" :key="item.label" :type="isPriceActive(item) ? 'primary' : 'info'" 
            class="filter-tag" @click="selectPrice(item)" effect="plain">{{ item.label }}</el-tag>
        </div>
      </div>
      <div class="filter-row">
        <span class="filter-label">户型：</span>
        <div class="filter-content">
          <el-tag v-for="item in houseTypes" :key="item" :type="searchParams.houseType === item || (item === '不限' && !searchParams.houseType) ? 'primary' : 'info'"
            class="filter-tag" @click="selectHouseType(item)" effect="plain">{{ item }}</el-tag>
        </div>
      </div>
      <div class="filter-row">
        <span class="filter-label">面积：</span>
        <div class="filter-content">
          <el-tag v-for="item in areaRanges" :key="item.label" :type="isAreaActive(item) ? 'primary' : 'info'"
            class="filter-tag" @click="selectArea(item)" effect="plain">{{ item.label }}</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 对比浮动条（在结果栏上方） -->
    <transition name="slide-down">
      <div class="compare-bar-inline" v-if="compareList.length > 0">
        <div class="compare-bar-inner">
          <div class="compare-houses">
            <div class="compare-house-tag" v-for="house in compareList" :key="house.houseId">
              <span class="tag-name">{{ house.title?.substring(0, 8) }}{{ house.title?.length > 8 ? '...' : '' }}</span>
              <span class="tag-price">¥{{ house.rentPrice }}</span>
              <el-icon class="tag-close" @click.stop="toggleCompare(house)"><Close /></el-icon>
            </div>
          </div>
          <div class="compare-actions">
            <span class="compare-count">已选 {{ compareList.length }}/3</span>
            <el-button type="warning" @click="openCompareDialog" :disabled="compareList.length < 2">
              开始对比
            </el-button>
            <el-button @click="compareList = []">清空</el-button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 结果统计和排序 -->
    <div class="result-bar">
      <div class="result-info">
        已为您找到 <span class="count">{{ total }}</span> 套房源
        <el-button link type="primary" @click="resetFilter" v-if="hasFilter">清空条件</el-button>
      </div>
      <div class="sort-tabs">
        <span :class="['sort-item', searchParams.sort === 'latest' ? 'active' : '']" @click="changeSort('latest')">综合排序</span>
        <span :class="['sort-item', searchParams.sort === 'newest' ? 'active' : '']" @click="changeSort('newest')">最新上架</span>
        <span :class="['sort-item', searchParams.sort?.startsWith('price') ? 'active' : '']" @click="togglePriceSort">
          价格
          <el-icon v-if="searchParams.sort === 'price_asc'"><ArrowUp /></el-icon>
          <el-icon v-else-if="searchParams.sort === 'price_desc'"><ArrowDown /></el-icon>
        </span>
        <span :class="['sort-item', searchParams.sort?.startsWith('area') ? 'active' : '']" @click="toggleAreaSort">
          面积
          <el-icon v-if="searchParams.sort === 'area_asc'"><ArrowUp /></el-icon>
          <el-icon v-else-if="searchParams.sort === 'area_desc'"><ArrowDown /></el-icon>
        </span>
        <!-- 列表/地图视图切换 -->
        <div class="view-switch">
          <span :class="['switch-btn', { active: viewMode === 'list' }]" @click="viewMode = 'list'" title="列表模式">
            <el-icon><List /></el-icon>
          </span>
          <span :class="['switch-btn', { active: viewMode === 'map' }]" @click="switchToMapView" title="地图找房">
            <el-icon><MapLocation /></el-icon>
          </span>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <template v-if="viewMode === 'list'">
      <div class="house-list" v-loading="loading">
        <div v-if="houseList.length === 0 && !loading" class="empty-tip">
          <el-empty description="暂无符合条件的房源" />
        </div>
        <div v-else class="house-item" v-for="house in houseList" :key="house.houseId">
          <div class="compare-checkbox" @click.stop="toggleCompare(house)">
            <el-checkbox :model-value="isInCompareList(house.houseId)" />
          </div>
          <div class="house-content" @click="goDetail(house.houseId)">
            <div class="house-image">
              <el-image :src="getFirstImage(house.images)" fit="cover">
                <template #error><div class="image-placeholder"><el-icon><Picture /></el-icon></div></template>
              </el-image>
              <span class="rent-tag" v-if="house.rentOption === 1">整租</span>
              <span class="rent-tag合租" v-else-if="house.rentOption === 2">合租</span>
            </div>
            <div class="house-info">
              <div class="house-title">{{ house.title }}</div>
              <div class="house-location">
                <el-icon><Location /></el-icon>
                {{ house.district || house.city }} · {{ house.address }}
              </div>
              <div class="house-tags">
                <el-tag size="small" type="info">{{ house.houseType }}</el-tag>
                <el-tag size="small" type="info">{{ house.area }}㎡</el-tag>
                <el-tag size="small" type="info" v-if="house.orientation">{{ house.orientation }}</el-tag>
                <el-tag size="small" type="info" v-if="house.floor">{{ house.floor }}</el-tag>
                <el-tag size="small" type="success" v-if="hasFacility(house.facilities, '近地铁')">近地铁</el-tag>
                <el-tag size="small" type="warning" v-if="hasFacility(house.facilities, '精装')">精装</el-tag>
              </div>
              <div class="house-meta">
                <span class="view-count"><el-icon><View /></el-icon> {{ house.viewCount || 0 }}</span>
                <span class="create-time">{{ formatTime(house.createTime) }}</span>
              </div>
            </div>
            <div class="house-price">
              <span class="price-num">{{ house.rentPrice }}</span>
              <span class="price-unit">元/月</span>
              <div class="deposit" v-if="house.depositType">{{ house.depositType }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination background layout="prev, pager, next, jumper" :total="total" :page-size="searchParams.size"
          v-model:current-page="searchParams.page" @current-change="doSearch" />
      </div>
    </template>

    <!-- 地图找房视图 -->
    <template v-if="viewMode === 'map'">
      <div class="map-view-container">
        <div class="map-main">
          <div id="search-map" class="search-map"></div>
        </div>
        <div class="map-side-list">
          <div class="side-list-header">
            <span>当前区域 <b>{{ mapHouseList.length }}</b> 套房源</span>
          </div>
          <div class="side-list-body" v-loading="loading">
            <div v-if="mapHouseList.length === 0 && !loading" class="side-empty">暂无房源</div>
            <div v-for="house in mapHouseList" :key="house.houseId" 
              :class="['side-house-item', { active: activeMapHouseId === house.houseId }]"
              @click="focusMapHouse(house)" @dblclick="goDetail(house.houseId)">
              <el-image :src="getFirstImage(house.images)" fit="cover" class="side-house-img">
                <template #error><div class="image-placeholder small"><el-icon><Picture /></el-icon></div></template>
              </el-image>
              <div class="side-house-info">
                <div class="side-house-title">{{ house.title }}</div>
                <div class="side-house-meta">{{ house.houseType }} · {{ house.area }}㎡</div>
                <div class="side-house-price">¥{{ house.rentPrice }}<small>/月</small></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    
    <!-- AI房源对比对话框 -->
    <HouseCompareDialog ref="compareDialogRef" />
  </div>
</template>


<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, ArrowUp, ArrowDown, Location, View, Picture, List, MapLocation, Close } from '@element-plus/icons-vue'
import { searchHousesApi, type HouseInfo, type HouseSearchParams } from '@/api/tenantHouse'
import { regionData, getProvinceByCityName } from '@/utils/regionData'
import { ElMessage } from 'element-plus'
import HouseCompareDialog from '@/components/HouseCompareDialog.vue'

declare const AMap: any

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const houseList = ref<HouseInfo[]>([])
const total = ref(0)
const compareList = ref<HouseInfo[]>([])
const compareDialogRef = ref()

// 地图找房相关
const viewMode = ref<'list' | 'map'>('list')
let searchMap: any = null
let searchMapMarkers: any[] = []
let searchInfoWindow: any = null
const mapHouseList = ref<HouseInfo[]>([])
const activeMapHouseId = ref<number | null>(null)

const searchParams = reactive<HouseSearchParams>({
  keyword: '',
  province: '',
  city: '',
  district: '',
  minPrice: undefined,
  maxPrice: undefined,
  houseType: '',
  minArea: undefined,
  maxArea: undefined,
  sort: 'latest',
  page: 1,
  size: 12
})

// 省市区数据
const provinceList = computed(() => Object.keys(regionData))
const cityList = computed(() => {
  if (!searchParams.province) return []
  return Object.keys(regionData[searchParams.province] || {})
})
const districtList = computed(() => {
  if (!searchParams.province || !searchParams.city) return []
  return regionData[searchParams.province]?.[searchParams.city] || []
})

// 筛选选项
const priceRanges = [
  { label: '不限', min: undefined, max: undefined },
  { label: '1000以下', min: undefined, max: 1000 },
  { label: '1000-2000', min: 1000, max: 2000 },
  { label: '2000-3000', min: 2000, max: 3000 },
  { label: '3000-5000', min: 3000, max: 5000 },
  { label: '5000以上', min: 5000, max: undefined }
]

const houseTypes = ['不限', '一室', '二室', '三室', '四室', '五室及以上']

const areaRanges = [
  { label: '不限', min: undefined, max: undefined },
  { label: '50㎡以下', min: undefined, max: 50 },
  { label: '50-70㎡', min: 50, max: 70 },
  { label: '70-90㎡', min: 70, max: 90 },
  { label: '90-120㎡', min: 90, max: 120 },
  { label: '120㎡以上', min: 120, max: undefined }
]

// 判断是否有筛选条件（不包括城市，城市通过顶部选择器控制）
const hasFilter = computed(() => {
  return searchParams.keyword || searchParams.province || searchParams.district || searchParams.minPrice !== undefined ||
    searchParams.maxPrice !== undefined || searchParams.houseType || searchParams.minArea !== undefined ||
    searchParams.maxArea !== undefined
})

// 省市区联动
const onProvinceChange = () => {
  searchParams.city = ''
  searchParams.district = ''
  doSearch()
}
const onCityChange = () => {
  // 城市变化时自动填充对应省份
  if (searchParams.city) {
    const province = getProvinceByCityName(searchParams.city)
    if (province) searchParams.province = province
  }
  searchParams.district = ''
  doSearch()
}

// 价格筛选
const isPriceActive = (item: typeof priceRanges[0]) => {
  return searchParams.minPrice === item.min && searchParams.maxPrice === item.max
}
const selectPrice = (item: typeof priceRanges[0]) => {
  searchParams.minPrice = item.min
  searchParams.maxPrice = item.max
  searchParams.page = 1
  doSearch()
}

// 户型筛选
const selectHouseType = (type: string) => {
  searchParams.houseType = type === '不限' ? '' : type
  searchParams.page = 1
  doSearch()
}

// 面积筛选
const isAreaActive = (item: typeof areaRanges[0]) => {
  return searchParams.minArea === item.min && searchParams.maxArea === item.max
}
const selectArea = (item: typeof areaRanges[0]) => {
  searchParams.minArea = item.min
  searchParams.maxArea = item.max
  searchParams.page = 1
  doSearch()
}

// 排序
const changeSort = (sort: string) => {
  searchParams.sort = sort
  searchParams.page = 1
  doSearch()
}
const togglePriceSort = () => {
  if (searchParams.sort === 'price_asc') {
    searchParams.sort = 'price_desc'
  } else {
    searchParams.sort = 'price_asc'
  }
  searchParams.page = 1
  doSearch()
}
const toggleAreaSort = () => {
  if (searchParams.sort === 'area_asc') {
    searchParams.sort = 'area_desc'
  } else {
    searchParams.sort = 'area_asc'
  }
  searchParams.page = 1
  doSearch()
}

// 重置筛选（保留城市选择）
const resetFilter = () => {
  const savedCity = searchParams.city // 保留当前城市
  searchParams.keyword = ''
  searchParams.province = ''
  searchParams.city = savedCity // 恢复城市
  searchParams.district = ''
  searchParams.minPrice = undefined
  searchParams.maxPrice = undefined
  searchParams.houseType = ''
  searchParams.minArea = undefined
  searchParams.maxArea = undefined
  searchParams.sort = 'latest'
  searchParams.page = 1
  doSearch()
}

// 搜索
const doSearch = async () => {
  loading.value = true
  try {
    const res: any = await searchHousesApi(searchParams)
    houseList.value = res.records || []
    total.value = res.total || 0
    // 地图模式下同步更新标注
    if (viewMode.value === 'map' && searchMap) {
      plotHousesOnMap(houseList.value)
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 获取第一张图片
const getFirstImage = (images: string) => {
  if (!images) return ''
  try {
    const arr = JSON.parse(images)
    return arr[0] || ''
  } catch {
    return images.split(',')[0] || ''
  }
}

// 判断是否有某个设施
const hasFacility = (facilities: string, name: string) => {
  if (!facilities) return false
  return facilities.includes(name)
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// 跳转详情
const goDetail = (houseId: number) => {
  router.push(`/tenant/house/${houseId}`)
}

// 对比功能
const toggleCompare = (house: HouseInfo) => {
  const index = compareList.value.findIndex(h => h.houseId === house.houseId)
  if (index > -1) {
    compareList.value.splice(index, 1)
  } else {
    if (compareList.value.length >= 3) {
      ElMessage.warning('最多只能对比3个房源')
      return
    }
    compareList.value.push(house)
  }
}

const isInCompareList = (houseId: number) => {
  return compareList.value.some(h => h.houseId === houseId)
}

const openCompareDialog = () => {
  if (compareList.value.length < 2) {
    ElMessage.warning('请至少选择2个房源进行对比')
    return
  }
  compareDialogRef.value?.open(compareList.value)
}

// ========== 地图找房 ==========
const switchToMapView = async () => {
  viewMode.value = 'map'
  await nextTick()
  initSearchMap()
}

const initSearchMap = () => {
  if (typeof AMap === 'undefined') {
    ElMessage.warning('地图加载中，请稍后重试')
    return
  }
  if (searchMap) { searchMap.destroy(); searchMap = null }

  const cityName = searchParams.city || '北京'
  searchMap = new AMap.Map('search-map', {
    zoom: 12,
    resizeEnable: true,
  })
  searchMap.addControl(new AMap.Scale())
  searchMap.addControl(new AMap.ToolBar({ position: 'RB' }))

  // 设置城市中心
  searchMap.setCity(cityName)

  searchInfoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -30), closeWhenClickMap: true })

  // 标注当前搜索结果
  plotHousesOnMap(houseList.value)
}

const plotHousesOnMap = (houses: HouseInfo[]) => {
  if (!searchMap) return
  // 清除旧标记
  if (searchMapMarkers.length) { searchMap.remove(searchMapMarkers); searchMapMarkers = [] }
  mapHouseList.value = []

  if (houses.length === 0) return

  const geocoder = new AMap.Geocoder({ city: searchParams.city || '全国', batch: false })
  let resolved = 0
  const resolvedHouses: HouseInfo[] = []

  houses.forEach((house) => {
    const fullAddr = `${house.province || ''}${house.city || ''}${house.district || ''}${house.address || ''}`
    geocoder.getLocation(fullAddr, (status: string, result: any) => {
      resolved++
      if (status === 'complete' && result.geocodes.length > 0) {
        const lnglat = result.geocodes[0].location
        resolvedHouses.push(house)

        const marker = new AMap.Marker({
          position: lnglat,
          extData: house,
          label: {
            content: `<div class="map-price-label">¥${house.rentPrice}</div>`,
            offset: new AMap.Pixel(-30, -40),
            direction: 'top'
          }
        })

        marker.on('click', () => {
          activeMapHouseId.value = house.houseId
          const img = getFirstImage(house.images)
          const content = `
            <div style="width:280px;padding:0;cursor:pointer" onclick="window.__goHouseDetail(${house.houseId})">
              <img src="${img}" style="width:100%;height:140px;object-fit:cover;border-radius:6px 6px 0 0" onerror="this.style.display='none'" />
              <div style="padding:10px">
                <div style="font-size:15px;font-weight:600;color:#333;margin-bottom:6px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${house.title}</div>
                <div style="font-size:12px;color:#999;margin-bottom:4px">${house.houseType} · ${house.area}㎡ · ${house.orientation || ''}</div>
                <div style="font-size:18px;font-weight:bold;color:#ff6600">¥${house.rentPrice}<span style="font-size:12px;font-weight:normal">/月</span></div>
              </div>
            </div>`
          searchInfoWindow.setContent(content)
          searchInfoWindow.open(searchMap, marker.getPosition())
        })

        searchMap.add(marker)
        searchMapMarkers.push(marker)
      }

      // 全部解析完后自适应视野
      if (resolved === houses.length && searchMapMarkers.length > 0) {
        mapHouseList.value = resolvedHouses
        if (searchMapMarkers.length > 1) {
          searchMap.setFitView(searchMapMarkers, false, [60, 60, 60, 60])
        }
      }
    })
  })
}

// 全局跳转函数（InfoWindow 内使用）
;(window as any).__goHouseDetail = (houseId: number) => {
  router.push(`/tenant/house/${houseId}`)
}

const focusMapHouse = (house: HouseInfo) => {
  activeMapHouseId.value = house.houseId
  // 找到对应 marker 并触发点击
  const marker = searchMapMarkers.find(m => m.getExtData()?.houseId === house.houseId)
  if (marker) {
    searchMap.setCenter(marker.getPosition())
    searchMap.setZoom(15)
    // 触发 marker 的 click 事件
    const img = getFirstImage(house.images)
    const content = `
      <div style="width:280px;padding:0;cursor:pointer" onclick="window.__goHouseDetail(${house.houseId})">
        <img src="${img}" style="width:100%;height:140px;object-fit:cover;border-radius:6px 6px 0 0" onerror="this.style.display='none'" />
        <div style="padding:10px">
          <div style="font-size:15px;font-weight:600;color:#333;margin-bottom:6px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${house.title}</div>
          <div style="font-size:12px;color:#999;margin-bottom:4px">${house.houseType} · ${house.area}㎡ · ${house.orientation || ''}</div>
          <div style="font-size:18px;font-weight:bold;color:#ff6600">¥${house.rentPrice}<span style="font-size:12px;font-weight:normal">/月</span></div>
        </div>
      </div>`
    searchInfoWindow.setContent(content)
    searchInfoWindow.open(searchMap, marker.getPosition())
  }
}

onMounted(() => {
  // 从URL读取搜索参数（URL参数优先级最高）
  if (route.query.keyword) {
    searchParams.keyword = route.query.keyword as string
  }
  if (route.query.sort) {
    searchParams.sort = route.query.sort as string
  }
  if (route.query.city) {
    searchParams.city = route.query.city as string
    const province = getProvinceByCityName(searchParams.city)
    if (province) searchParams.province = province
  } else {
    // 只有在URL没有指定城市时，才从localStorage读取
    const savedCity = localStorage.getItem('selectedCity')
    if (savedCity && savedCity !== '全部' && savedCity.trim() !== '') {
      searchParams.city = savedCity
      const province = getProvinceByCityName(savedCity)
      if (province) searchParams.province = province
    } else {
      searchParams.city = ''
    }
  }
  
  doSearch()
})

onBeforeUnmount(() => {
  if (searchMap) { searchMap.destroy(); searchMap = null }
  window.removeEventListener('cityChanged', onCityChanged as EventListener)
})

// 监听城市变化事件
const onCityChanged = (event: CustomEvent) => {
  const city = event.detail || ''
  searchParams.city = city
  // 自动填充对应省份
  if (city) {
    const province = getProvinceByCityName(city)
    if (province) searchParams.province = province
  } else {
    searchParams.province = ''
  }
  searchParams.district = ''
  searchParams.page = 1
  doSearch()
}

// 组件挂载后监听城市变化
window.addEventListener('cityChanged', onCityChanged as EventListener)

// 监听路由参数变化
watch(() => route.query, (newQuery) => {
  if (newQuery.keyword !== undefined) {
    searchParams.keyword = (newQuery.keyword as string) || ''
    searchParams.page = 1
    doSearch()
  }
}, { deep: true })
</script>


<style scoped>
.house-search-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}

.search-header {
  max-width: 800px;
  margin: 0 auto 20px;
}

.search-input {
  width: 100%;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  align-items: flex-start;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.filter-row:last-child {
  border-bottom: none;
}

.filter-label {
  width: 60px;
  color: #666;
  flex-shrink: 0;
  line-height: 32px;
}

.filter-content {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.filter-tag:hover {
  border-color: #409eff;
  color: #409eff;
}

.result-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: #fff;
  border-radius: 4px;
  margin-bottom: 20px;
}

.result-info {
  color: #666;
}

.result-info .count {
  color: #ff6600;
  font-weight: bold;
  font-size: 18px;
  margin: 0 5px;
}

.sort-tabs {
  display: flex;
  gap: 20px;
}

.sort-item {
  cursor: pointer;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 4px;
  transition: all 0.3s;
}

.sort-item:hover {
  color: #409eff;
}

.sort-item.active {
  color: #409eff;
  background: #ecf5ff;
}

/* 房源列表 - 横向卡片 */
.house-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.house-item {
  display: flex;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
}

.house-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.compare-checkbox {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  padding: 5px;
  border-radius: 4px;
}

.house-content {
  display: flex;
  width: 100%;
}

.house-image {
  width: 200px;
  height: 150px;
  flex-shrink: 0;
  position: relative;
}

.house-image .el-image {
  width: 100%;
  height: 100%;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #ccc;
  font-size: 40px;
}

.rent-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.house-info {
  flex: 1;
  padding: 15px 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.house-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.house-location {
  color: #999;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
}

.house-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.house-meta {
  display: flex;
  gap: 20px;
  color: #999;
  font-size: 12px;
  margin-top: 10px;
}

.house-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.house-price {
  width: 140px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  padding: 15px 20px;
  border-left: 1px solid #f0f0f0;
}

.price-num {
  font-size: 28px;
  font-weight: bold;
  color: #ff6600;
}

.price-unit {
  color: #ff6600;
  font-size: 14px;
}

.deposit {
  color: #999;
  font-size: 12px;
  margin-top: 5px;
}

.empty-tip {
  padding: 60px 0;
  background: #fff;
  border-radius: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 30px 0;
}

/* 视图切换按钮 */
.view-switch {
  display: flex;
  gap: 2px;
  margin-left: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
.switch-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 30px;
  cursor: pointer;
  color: #606266;
  background: #fff;
  transition: all 0.2s;
}
.switch-btn:hover { color: #409eff; }
.switch-btn.active { background: #409eff; color: #fff; }

/* 地图找房视图 */
.map-view-container {
  display: flex;
  gap: 0;
  height: calc(100vh - 320px);
  min-height: 500px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.map-main {
  flex: 1;
  min-width: 0;
}
.search-map {
  width: 100%;
  height: 100%;
}
.map-side-list {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  display: flex;
  flex-direction: column;
  border-left: 1px solid #e8e8e8;
}
.side-list-header {
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #666;
}
.side-list-header b { color: #ff6600; margin: 0 2px; }
.side-list-body {
  flex: 1;
  overflow-y: auto;
}
.side-empty {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
.side-house-item {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.15s;
}
.side-house-item:hover, .side-house-item.active { background: #f0f7ff; }
.side-house-img {
  width: 80px;
  height: 60px;
  border-radius: 4px;
  flex-shrink: 0;
}
.side-house-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.side-house-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.side-house-meta {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.side-house-price {
  font-size: 15px;
  font-weight: bold;
  color: #ff6600;
  margin-top: 4px;
}
.side-house-price small { font-size: 11px; font-weight: normal; }
.image-placeholder.small { font-size: 20px; }

/* 对比浮动条（内联在结果栏上方） */
.compare-bar-inline {
  background: #fff;
  border-radius: 4px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #e6a23c;
}

.compare-bar-inner {
  padding: 10px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.compare-houses {
  display: flex;
  gap: 10px;
  flex: 1;
  overflow-x: auto;
}

.compare-house-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fdf6ec;
  border: 1px solid #e6a23c;
  border-radius: 6px;
  white-space: nowrap;
}

.tag-name {
  font-size: 13px;
  color: #303133;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-price {
  font-size: 13px;
  font-weight: bold;
  color: #e6a23c;
}

.tag-close {
  cursor: pointer;
  color: #909399;
  font-size: 14px;
}

.tag-close:hover {
  color: #f56c6c;
}

.compare-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 20px;
  flex-shrink: 0;
}

.compare-count {
  font-size: 13px;
  color: #909399;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
