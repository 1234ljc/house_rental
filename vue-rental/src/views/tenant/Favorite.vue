<template>
  <div class="favorite-page">
    <div class="page-header">
      <h2>我的收藏</h2>
      <span class="count">共 {{ total }} 套房源</span>
      <el-button v-if="compareList.length >= 2" type="warning" size="small" 
        @click="openCompareDialog" style="margin-left: 10px">
        对比房源 ({{ compareList.length }})
      </el-button>
      <el-button v-if="total > 0" :type="showMap ? 'primary' : 'default'" size="small" 
        @click="toggleMap" style="margin-left:auto">
        <el-icon><MapLocation /></el-icon> {{ showMap ? '收起地图' : '地图总览' }}
      </el-button>
    </div>

    <!-- 地图总览 -->
    <div v-if="showMap" class="fav-map-wrap">
      <div id="fav-map" class="fav-map"></div>
    </div>

    <div class="favorite-list" v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无收藏的房源">
        <el-button type="primary" @click="$router.push('/tenant/search')">去看看房源</el-button>
      </el-empty>

      <div v-else class="house-item" v-for="house in list" :key="house.houseId">
        <div class="compare-checkbox" @click.stop="toggleCompare(house)">
          <el-checkbox :model-value="isInCompareList(house.houseId)" />
        </div>
        <div class="house-content" @click="goDetail(house.houseId)">
          <div class="house-image">
            <el-image :src="getFirstImage(house.images)" fit="cover">
              <template #error><div class="image-placeholder"><el-icon><Picture /></el-icon></div></template>
            </el-image>
            <el-tag class="status-tag" :type="getStatusType(house.status)" size="small">{{ getStatusText(house.status) }}</el-tag>
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
            </div>
          </div>
          <div class="house-right">
            <div class="house-price">
              <span class="price-num">{{ house.rentPrice }}</span>
              <span class="price-unit">元/月</span>
            </div>
            <el-button type="danger" link @click.stop="handleRemove(house.houseId)">
              <el-icon><Delete /></el-icon> 取消收藏
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize"
        v-model:current-page="currentPage" @current-change="loadList" />
    </div>

    <!-- AI房源对比对话框 -->
    <HouseCompareDialog ref="compareDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Picture, Delete, MapLocation } from '@element-plus/icons-vue'
import { getFavoriteListApi, removeFavoriteApi } from '@/api/tenantHouse'
import HouseCompareDialog from '@/components/HouseCompareDialog.vue'

declare const AMap: any

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

// 地图相关
const showMap = ref(false)
let favMap: any = null
let favMarkers: any[] = []
let favInfoWindow: any = null

// 对比功能
const compareList = ref<any[]>([])
const compareDialogRef = ref()

const toggleCompare = (house: any) => {
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

const getFirstImage = (images: string) => {
  if (!images) return ''
  try {
    const arr = JSON.parse(images)
    return arr[0] || ''
  } catch {
    return images.split(',')[0] || ''
  }
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待审核', 1: '可出租', 2: '已出租', 3: '已下架', 4: '审核驳回' }
  return map[status] || '未知'
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'danger' }
  return map[status] || 'info'
}

const loadList = async () => {
  loading.value = true
  try {
    const res: any = await getFavoriteListApi({ page: currentPage.value, size: pageSize.value })
    list.value = res.records || []
    total.value = res.total || 0
    // 如果地图已打开，更新标注
    if (showMap.value && favMap) {
      plotFavOnMap()
    }
  } finally {
    loading.value = false
  }
}

const goDetail = (houseId: number) => {
  router.push(`/tenant/house/${houseId}`)
}

const handleRemove = async (houseId: number) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏该房源吗？', '提示', { type: 'warning' })
    await removeFavoriteApi(houseId)
    ElMessage.success('已取消收藏')
    loadList()
  } catch (e) {
    // 取消操作
  }
}

// ========== 地图总览 ==========
const toggleMap = async () => {
  showMap.value = !showMap.value
  if (showMap.value) {
    await nextTick()
    initFavMap()
  } else {
    if (favMap) { favMap.destroy(); favMap = null }
  }
}

const initFavMap = () => {
  if (typeof AMap === 'undefined') {
    ElMessage.warning('地图加载中，请稍后重试')
    return
  }
  if (favMap) { favMap.destroy(); favMap = null }

  favMap = new AMap.Map('fav-map', {
    zoom: 11,
    resizeEnable: true,
  })
  favMap.addControl(new AMap.Scale())
  favMap.addControl(new AMap.ToolBar({ position: 'RB' }))
  favInfoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -30), closeWhenClickMap: true })

  plotFavOnMap()
}

const plotFavOnMap = () => {
  if (!favMap) return
  if (favMarkers.length) { favMap.remove(favMarkers); favMarkers = [] }
  if (list.value.length === 0) return

  const geocoder = new AMap.Geocoder({ city: '全国' })

  list.value.forEach((house: any) => {
    const fullAddr = `${house.province || ''}${house.city || ''}${house.district || ''}${house.address || ''}`
    geocoder.getLocation(fullAddr, (status: string, result: any) => {
      if (status === 'complete' && result.geocodes.length > 0) {
        const lnglat = result.geocodes[0].location
        const marker = new AMap.Marker({
          position: lnglat,
          label: {
            content: `<div class="map-price-label">¥${house.rentPrice}</div>`,
            offset: new AMap.Pixel(-30, -40),
            direction: 'top'
          }
        })

        marker.on('click', () => {
          const img = getFirstImage(house.images)
          const content = `
            <div style="width:260px;padding:0;cursor:pointer" onclick="window.__goFavDetail(${house.houseId})">
              <img src="${img}" style="width:100%;height:130px;object-fit:cover;border-radius:6px 6px 0 0" onerror="this.style.display='none'" />
              <div style="padding:10px">
                <div style="font-size:14px;font-weight:600;color:#333;margin-bottom:4px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${house.title}</div>
                <div style="font-size:12px;color:#999;margin-bottom:4px">${house.houseType} · ${house.area}㎡</div>
                <div style="font-size:16px;font-weight:bold;color:#ff6600">¥${house.rentPrice}<span style="font-size:12px;font-weight:normal">/月</span></div>
              </div>
            </div>`
          favInfoWindow.setContent(content)
          favInfoWindow.open(favMap, marker.getPosition())
        })

        favMap.add(marker)
        favMarkers.push(marker)

        // 自适应视野
        if (favMarkers.length > 1) {
          favMap.setFitView(favMarkers, false, [60, 60, 60, 60])
        } else {
          favMap.setCenter(lnglat)
          favMap.setZoom(14)
        }
      }
    })
  })
}

;(window as any).__goFavDetail = (houseId: number) => {
  router.push(`/tenant/house/${houseId}`)
}

onMounted(() => loadList())

onBeforeUnmount(() => {
  if (favMap) { favMap.destroy(); favMap = null }
})
</script>

<style scoped>
.favorite-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.page-header .count {
  color: #999;
  font-size: 14px;
}

/* 地图总览 */
.fav-map-wrap {
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.fav-map {
  width: 100%;
  height: 380px;
}

.favorite-list {
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

.house-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
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

.status-tag {
  position: absolute;
  top: 10px;
  left: 10px;
}

.house-info {
  flex: 1;
  padding: 15px 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.house-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin-bottom: 10px;
}

.house-location {
  color: #999;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;
}

.house-tags {
  display: flex;
  gap: 8px;
}

.house-right {
  width: 150px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 15px;
  border-left: 1px solid #f0f0f0;
}

.house-price {
  margin-bottom: 15px;
}

.price-num {
  font-size: 24px;
  font-weight: bold;
  color: #ff6600;
}

.price-unit {
  color: #ff6600;
  font-size: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 30px 0;
}
</style>
