<template>
  <div class="house-publish-page">
    <RealnameAlert :require-realname="true" feature-name="发布房源功能" />
    
    <template v-if="isRealnameVerified">
      <el-card class="main-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon :size="22" color="#409eff"><House /></el-icon>
            <span class="header-title">{{ isEdit ? '编辑房源' : '发布新房源' }}</span>
          </div>
        </template>

        <!-- 步骤条 -->
        <el-steps :active="currentStep" finish-status="success" align-center class="step-bar">
          <el-step title="基本信息" description="房源基础属性" />
          <el-step title="位置与配套" description="地址和设施" />
          <el-step title="图片与确认" description="上传图片并提交" />
        </el-steps>

        <!-- AI智能解析 -->
        <el-card class="ai-parse-card" shadow="never" v-if="currentStep === 0">
          <div class="ai-parse-header">
            <el-icon :size="20" color="#67c23a"><MagicStick /></el-icon>
            <span class="ai-parse-title">AI智能发布</span>
            <el-tag type="success" size="small">快捷便利</el-tag>
          </div>
          <div class="ai-parse-content">
            <el-input
              v-model="aiDescription"
              type="textarea"
              :rows="4"
              placeholder="输入房源描述，AI将自动帮您填充表单。例如：朝阳区三里屯附近，精装两室一厅，85平米，南北通透，6楼共18层，月租5000元，押一付三，配备空调冰箱洗衣机..."
              maxlength="500"
              show-word-limit
            />
            <div class="ai-parse-actions">
              <el-button type="success" :icon="MagicStick" @click="parseWithAI" :loading="aiParsing">
                AI智能解析
              </el-button>
              <span class="ai-parse-tip">
                <el-icon><InfoFilled /></el-icon>
                AI会自动提取房源信息并填充表单，您可以随时调整
              </span>
            </div>
          </div>
        </el-card>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="publish-form">
          
          <!-- 第一步：基本信息 -->
          <el-card v-show="currentStep === 0" class="step-card" shadow="never">
            <template #header>
              <span class="section-title">房源基本信息</span>
            </template>
            
            <el-form-item label="房源标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入房源标题，如：精装修两室一厅近地铁" maxlength="50" show-word-limit />
            </el-form-item>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="房屋户型" prop="houseType">
                  <el-select v-model="form.houseType" placeholder="请选择户型" style="width: 100%">
                    <el-option label="一室" value="一室" />
                    <el-option label="一室一厅" value="一室一厅" />
                    <el-option label="两室一厅" value="两室一厅" />
                    <el-option label="三室一厅" value="三室一厅" />
                    <el-option label="三室两厅" value="三室两厅" />
                    <el-option label="四室及以上" value="四室及以上" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="房屋面积" prop="area">
                  <el-input-number v-model="form.area" :min="1" :max="9999" :precision="1" placeholder="面积" style="width: calc(100% - 50px)" />
                  <span class="unit-text">㎡</span>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="所在楼层" prop="floor">
                  <el-input-number v-model="form.floor" :min="1" :max="99" placeholder="楼层" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="总楼层" prop="totalFloor">
                  <el-input-number v-model="form.totalFloor" :min="1" :max="99" placeholder="总层" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="朝向" prop="orientation">
                  <el-select v-model="form.orientation" placeholder="请选择" style="width: 100%">
                    <el-option label="东" value="东" />
                    <el-option label="南" value="南" />
                    <el-option label="西" value="西" />
                    <el-option label="北" value="北" />
                    <el-option label="南北通透" value="南北通透" />
                    <el-option label="东南" value="东南" />
                    <el-option label="东北" value="东北" />
                    <el-option label="西南" value="西南" />
                    <el-option label="西北" value="西北" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="月租金" prop="rentPrice">
                  <el-input-number v-model="form.rentPrice" :min="0" :max="999999" placeholder="租金" style="width: calc(100% - 50px)" />
                  <span class="unit-text">元/月</span>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="押付方式" prop="depositType">
                  <el-select v-model="form.depositType" placeholder="请选择押付方式" style="width: 100%">
                    <el-option label="押一付一" value="押一付一" />
                    <el-option label="押一付二" value="押一付二" />
                    <el-option label="押一付三" value="押一付三" />
                    <el-option label="押二付一" value="押二付一" />
                    <el-option label="押二付三" value="押二付三" />
                    <el-option label="押三付一" value="押三付一" />
                    <el-option label="押三付三" value="押三付三" />
                    <el-option label="免押金" value="免押金" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-card>

          <!-- 第二步：位置与配套 -->
          <el-card v-show="currentStep === 1" class="step-card" shadow="never">
            <template #header>
              <span class="section-title">位置与配套设施</span>
            </template>

            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="省份" prop="province">
                  <el-select v-model="form.province" placeholder="请选择省份" style="width: 100%" @change="onProvinceChange" filterable>
                    <el-option v-for="p in provinceList" :key="p" :label="p" :value="p" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="城市" prop="city">
                  <el-select v-model="form.city" placeholder="请选择城市" style="width: 100%" @change="onCityChange" :disabled="!form.province" filterable>
                    <el-option v-for="c in cityList" :key="c" :label="c" :value="c" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="区县" prop="district">
                  <el-select v-model="form.district" placeholder="请选择区县" style="width: 100%" :disabled="!form.city" filterable>
                    <el-option v-for="d in districtList" :key="d" :label="d" :value="d" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="详细地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入详细地址，如：XX小区X栋X单元" maxlength="100" />
            </el-form-item>

            <!-- 地图定位预览 -->
            <el-form-item label="地图定位">
              <div class="map-locate-wrap">
                <div class="map-locate-bar">
                  <el-button type="primary" size="small" @click="locateOnMap" :loading="mapLocating">
                    <el-icon><Location /></el-icon> 地址定位
                  </el-button>
                  <span class="map-locate-tip" v-if="form.longitude && form.latitude">
                    <el-icon color="#67c23a"><CircleCheck /></el-icon>
                    已定位 ({{ form.longitude.toFixed(4) }}, {{ form.latitude.toFixed(4) }})
                  </span>
                  <span class="map-locate-tip" v-else>
                    <el-icon color="#e6a23c"><InfoFilled /></el-icon>
                    填写详细地址后自动定位，或在地图上点击/拖拽标记选择位置
                  </span>
                </div>
                <div id="publish-map" class="publish-map"></div>
              </div>
            </el-form-item>

            <el-form-item label="房源描述" prop="description">
              <el-input 
                v-model="form.description" 
                type="textarea" 
                :rows="6" 
                placeholder="请详细描述房源特点、周边配套、交通情况等" 
                maxlength="1000" 
                show-word-limit 
              />
            </el-form-item>

            <el-form-item label="配套设施">
              <div class="facility-grid">
                <el-checkbox 
                  v-for="item in facilityOptions" 
                  :key="item" 
                  v-model="facilitiesMap[item]"
                  class="facility-item"
                >
                  {{ item }}
                </el-checkbox>
              </div>
            </el-form-item>
          </el-card>

          <!-- 第三步：图片与确认 -->
          <el-card v-show="currentStep === 2" class="step-card" shadow="never">
            <template #header>
              <span class="section-title">上传房源图片</span>
            </template>

            <el-form-item label="房源图片" class="image-upload-item">
              <div class="upload-wrapper">
                <el-upload
                  ref="uploadRef"
                  v-model:file-list="imageFileList"
                  list-type="picture-card"
                  :limit="9"
                  :multiple="true"
                  :before-upload="beforeUpload"
                  :on-remove="onRemove"
                  :http-request="onUpload"
                  :on-exceed="onExceed"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                  class="image-uploader"
                >
                  <div class="upload-trigger">
                    <el-icon :size="28"><Plus /></el-icon>
                    <span class="upload-text">上传图片</span>
                  </div>
                  <template #tip>
                    <div class="upload-tip">
                      <el-icon><InfoFilled /></el-icon>
                      支持多选上传，最多9张，单张不超过5MB，支持jpg/png/gif/webp格式
                    </div>
                  </template>
                </el-upload>
              </div>
            </el-form-item>

            <!-- 房产证件上传 -->
            <el-divider content-position="left">
              <el-icon><Document /></el-icon> 房源证件材料（审核必需）
            </el-divider>
            <el-alert
              title="请上传房产证或其他产权证明材料，管理员将依据证件进行审核。证件信息仅用于平台审核，不对租客公开。"
              type="warning"
              :closable="false"
              show-icon
              style="margin-bottom: 16px"
            />
            <el-form-item>
              <template #label>
                <span>房产证正面 <span style="color:#f56c6c">*</span></span>
              </template>
              <div class="cert-upload-area">
                <el-upload
                  :auto-upload="false"
                  list-type="picture-card"
                  :limit="1"
                  :on-change="(f: any) => onCertUpload(f, 'front')"
                  :on-remove="() => certImages.front = ''"
                  :file-list="certFileLists.front"
                  accept="image/jpeg,image/png,image/webp"
                  class="cert-uploader"
                >
                  <div class="upload-trigger">
                    <el-icon :size="24"><Plus /></el-icon>
                    <span class="upload-text">上传正面</span>
                  </div>
                </el-upload>
                <div class="cert-tip">
                  <el-icon color="#e6a23c"><InfoFilled /></el-icon>
                  <span>房产证首页（含产权人姓名、房屋地址）</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="房产证背面">
              <div class="cert-upload-area">
                <el-upload
                  :auto-upload="false"
                  list-type="picture-card"
                  :limit="1"
                  :on-change="(f: any) => onCertUpload(f, 'back')"
                  :on-remove="() => certImages.back = ''"
                  :file-list="certFileLists.back"
                  accept="image/jpeg,image/png,image/webp"
                  class="cert-uploader"
                >
                  <div class="upload-trigger">
                    <el-icon :size="24"><Plus /></el-icon>
                    <span class="upload-text">上传背面</span>
                  </div>
                </el-upload>
                <div class="cert-tip">
                  <el-icon color="#909399"><InfoFilled /></el-icon>
                  <span>选填，如有多页可上传</span>
                </div>
              </div>
            </el-form-item>
            <el-form-item label="其他证明">
              <div class="cert-upload-area">
                <el-upload
                  :auto-upload="false"
                  list-type="picture-card"
                  :limit="1"
                  :on-change="(f: any) => onCertUpload(f, 'other')"
                  :on-remove="() => certImages.other = ''"
                  :file-list="certFileLists.other"
                  accept="image/jpeg,image/png,image/webp"
                  class="cert-uploader"
                >
                  <div class="upload-trigger">
                    <el-icon :size="24"><Plus /></el-icon>
                    <span class="upload-text">上传证明</span>
                  </div>
                </el-upload>
                <div class="cert-tip">
                  <el-icon color="#909399"><InfoFilled /></el-icon>
                  <span>选填，如购房合同、委托书等</span>
                </div>
              </div>
            </el-form-item>

            <!-- 信息预览 -->
            <div class="preview-section">
              <div class="preview-title">
                <el-icon><Document /></el-icon>
                <span>信息预览</span>
              </div>
              <el-descriptions :column="2" border class="preview-desc">
                <el-descriptions-item label="房源标题" :span="2">
                  <span class="preview-value">{{ form.title || '-' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="户型">{{ form.houseType || '-' }}</el-descriptions-item>
                <el-descriptions-item label="面积">{{ form.area ? form.area + ' ㎡' : '-' }}</el-descriptions-item>
                <el-descriptions-item label="楼层">{{ form.floor && form.totalFloor ? form.floor + '/' + form.totalFloor + '层' : '-' }}</el-descriptions-item>
                <el-descriptions-item label="朝向">{{ form.orientation || '-' }}</el-descriptions-item>
                <el-descriptions-item label="月租金">
                  <span class="price-text">{{ form.rentPrice ? '¥' + form.rentPrice : '-' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="押付方式">{{ form.depositType || '-' }}</el-descriptions-item>
                <el-descriptions-item label="位置" :span="2">
                  {{ [form.province, form.city, form.district, form.address].filter(Boolean).join(' ') || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="配套设施" :span="2">
                  <el-tag v-for="f in selectedFacilities" :key="f" size="small" class="facility-tag">{{ f }}</el-tag>
                  <span v-if="!selectedFacilities.length">-</span>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-card>
        </el-form>

        <!-- 底部按钮 -->
        <div class="action-buttons">
          <el-button v-if="currentStep > 0" @click="currentStep--" size="large">
            <el-icon><ArrowLeft /></el-icon> 上一步
          </el-button>
          <el-button v-if="currentStep < 2" type="primary" @click="nextStep" size="large">
            下一步 <el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-button v-if="currentStep === 2" type="primary" @click="handleSubmit" :loading="submitting" size="large">
            <el-icon><Check /></el-icon> 提交审核
          </el-button>
          <el-button @click="handleReset" size="large">重置</el-button>
        </div>
      </el-card>
    </template>

    <template v-else>
      <el-card shadow="hover">
        <el-empty description="完成实名认证后即可发布房源">
          <el-button type="primary" @click="$router.push('/landlord/profile/realname')">去认证</el-button>
        </el-empty>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadFile, type UploadRequestOptions, type UploadInstance } from 'element-plus'
import { Plus, House, InfoFilled, Document, ArrowLeft, ArrowRight, Check, MagicStick, Location, CircleCheck } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import RealnameAlert from '@/components/RealnameAlert.vue'
import { publishHouseApi, updateHouseApi, getHouseDetailApi, type HousePublishDTO, type HouseInfo } from '@/api/landlordHouse'
import { provinceList, getCityList, getDistrictList } from '@/utils/regionData'
import { parseHouseDescriptionApi } from '@/api/ai'

declare const AMap: any

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isRealnameVerified = computed(() => userStore.isRealnameVerified)
const formRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()
const submitting = ref(false)
const currentStep = ref(0)
const isEdit = computed(() => !!route.query.id)
const editHouse = ref<HouseInfo | null>(null)
const aiParsing = ref(false)
const aiDescription = ref('')

const form = ref({
  title: '',
  description: '',
  province: '',
  city: '',
  district: '',
  address: '',
  area: null as number | null,
  houseType: '',
  floor: null as number | null,
  totalFloor: null as number | null,
  orientation: '',
  rentPrice: null as number | null,
  depositType: '',
  longitude: null as number | null,
  latitude: null as number | null
})

// 地图相关
let publishMap: any = null
let publishMarker: any = null
const mapLocating = ref(false)
let addressDebounceTimer: ReturnType<typeof setTimeout> | null = null

// 配套设施使用对象映射，方便操作
const facilityOptions = ['空调', '冰箱', '洗衣机', '热水器', '电视', '宽带', '衣柜', '床', '独立卫生间', '独立阳台', '电梯', '停车位', '燃气', '暖气', '门禁']
const facilitiesMap = reactive<Record<string, boolean>>({})
facilityOptions.forEach(f => facilitiesMap[f] = false)

const selectedFacilities = computed(() => facilityOptions.filter(f => facilitiesMap[f]))

const imageFileList = ref<UploadFile[]>([])
const imageUrls = ref<string[]>([])

// 房产证件图片
const certImages = reactive({ front: '', back: '', other: '' })
const certFileLists = reactive<Record<string, UploadFile[]>>({ front: [], back: [], other: [] })

const onCertUpload = async (file: any, type: 'front' | 'back' | 'other') => {
  try {
    const base64 = await compressImage(file.raw)
    certImages[type] = base64
    certFileLists[type] = [file]
  } catch {
    ElMessage.error('图片处理失败')
  }
}

const cityList = computed(() => getCityList(form.value.province))
const districtList = computed(() => getDistrictList(form.value.province, form.value.city))
const onProvinceChange = () => { form.value.city = ''; form.value.district = '' }
const onCityChange = () => { form.value.district = '' }

const rules: FormRules = {
  title: [{ required: true, message: '请输入房源标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入房源描述', trigger: 'blur' }],
  province: [{ required: true, message: '请选择省份', trigger: 'change' }],
  city: [{ required: true, message: '请选择城市', trigger: 'change' }],
  district: [{ required: true, message: '请选择区县', trigger: 'change' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  area: [{ required: true, message: '请输入房屋面积', trigger: 'blur' }],
  houseType: [{ required: true, message: '请选择房屋户型', trigger: 'change' }],
  floor: [{ required: true, message: '请输入所在楼层', trigger: 'blur' }],
  totalFloor: [{ required: true, message: '请输入总楼层', trigger: 'blur' }],
  orientation: [{ required: true, message: '请选择朝向', trigger: 'change' }],
  rentPrice: [{ required: true, message: '请输入月租金', trigger: 'blur' }],
  depositType: [{ required: true, message: '请选择押付方式', trigger: 'change' }]
}

const step1Fields = ['title', 'houseType', 'area', 'floor', 'totalFloor', 'orientation', 'rentPrice', 'depositType']
const step2Fields = ['province', 'city', 'district', 'address', 'description']

const nextStep = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validateField(currentStep.value === 0 ? step1Fields : step2Fields)
    currentStep.value++
    // 进入第二步时初始化地图
    if (currentStep.value === 1) {
      await nextTick()
      initPublishMap()
    }
  } catch {
    ElMessage.warning('请完善必填信息')
  }
}

// ========== 发布页地图定位 ==========
const initPublishMap = () => {
  if (typeof AMap === 'undefined' || publishMap) return
  publishMap = new AMap.Map('publish-map', {
    zoom: 13,
    resizeEnable: true,
  })
  publishMap.addControl(new AMap.Scale())

  // 点击地图放置/移动标记
  publishMap.on('click', (e: any) => {
    const lnglat = e.lnglat
    placePublishMarker(lnglat)
    form.value.longitude = lnglat.getLng()
    form.value.latitude = lnglat.getLat()
    // 点击地图也触发逆地理编码
    reverseGeocode(lnglat)
  })

  // 如果已有经纬度（编辑模式），直接定位
  if (form.value.longitude && form.value.latitude) {
    const lnglat = new AMap.LngLat(form.value.longitude, form.value.latitude)
    publishMap.setCenter(lnglat)
    publishMap.setZoom(15)
    placePublishMarker(lnglat)
  }
}

const placePublishMarker = (lnglat: any) => {
  if (publishMarker) {
    publishMarker.setPosition(lnglat)
  } else {
    publishMarker = new AMap.Marker({
      position: lnglat,
      draggable: true,
      title: '拖拽调整位置'
    })
    publishMarker.on('dragend', (e: any) => {
      const pos = publishMarker.getPosition()
      form.value.longitude = pos.getLng()
      form.value.latitude = pos.getLat()
      // 逆地理编码：坐标 → 地址
      reverseGeocode(pos)
    })
    publishMap.add(publishMarker)
  }
}

// 逆地理编码：根据坐标更新详细地址
const reverseGeocode = (lnglat: any) => {
  if (typeof AMap === 'undefined') return
  const geocoder = new AMap.Geocoder({ radius: 100 })
  geocoder.getAddress(lnglat, (status: string, result: any) => {
    if (status === 'complete' && result.regeocode) {
      const addr = result.regeocode.addressComponent
      const formattedAddr = result.regeocode.formattedAddress

      // 更新省市区（如果能解析到）
      if (addr.province) form.value.province = addr.province
      if (addr.city) form.value.city = addr.city || addr.province // 直辖市 city 可能为空
      if (addr.district) form.value.district = addr.district

      // 更新详细地址（去掉省市区前缀，只保留街道以下）
      const prefix = `${addr.province || ''}${addr.city || ''}${addr.district || ''}`
      const detail = formattedAddr.startsWith(prefix)
        ? formattedAddr.slice(prefix.length)
        : formattedAddr
      form.value.address = detail || formattedAddr
    }
  })
}

const locateOnMap = () => {
  if (typeof AMap === 'undefined') {
    ElMessage.warning('地图加载中，请稍后重试')
    return
  }
  if (!publishMap) initPublishMap()

  const fullAddr = `${form.value.province || ''}${form.value.city || ''}${form.value.district || ''}${form.value.address || ''}`
  if (!fullAddr.trim()) {
    ElMessage.warning('请先填写地址信息')
    return
  }

  mapLocating.value = true
  const geocoder = new AMap.Geocoder({ city: form.value.city || '全国' })
  geocoder.getLocation(fullAddr, (status: string, result: any) => {
    mapLocating.value = false
    if (status === 'complete' && result.geocodes.length > 0) {
      const lnglat = result.geocodes[0].location
      publishMap.setCenter(lnglat)
      publishMap.setZoom(16)
      placePublishMarker(lnglat)
      form.value.longitude = lnglat.getLng()
      form.value.latitude = lnglat.getLat()
      ElMessage.success('定位成功，可拖拽标记微调位置')
    } else {
      ElMessage.warning('地址解析失败，请检查地址或在地图上手动点击定位')
    }
  })
}

// 监听详细地址变化，防抖 800ms 后自动重新定位（仅在地图已初始化时）
watch(() => form.value.address, (val) => {
  if (!val || !publishMap) return
  if (addressDebounceTimer) clearTimeout(addressDebounceTimer)
  addressDebounceTimer = setTimeout(() => {
    const fullAddr = `${form.value.province || ''}${form.value.city || ''}${form.value.district || ''}${val}`
    if (!fullAddr.trim()) return
    const geocoder = new AMap.Geocoder({ city: form.value.city || '全国' })
    geocoder.getLocation(fullAddr, (status: string, result: any) => {
      if (status === 'complete' && result.geocodes.length > 0) {
        const lnglat = result.geocodes[0].location
        publishMap.setCenter(lnglat)
        publishMap.setZoom(16)
        placePublishMarker(lnglat)
        form.value.longitude = lnglat.getLng()
        form.value.latitude = lnglat.getLat()
      }
    })
  }, 800)
})

const beforeUpload = (file: File) => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG/GIF/WEBP 格式的图片')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const onExceed = () => {
  ElMessage.warning('最多只能上传9张图片')
}

// 压缩图片
const compressImage = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height
        // 限制最大宽度为1200
        if (width > 1200) {
          height = (1200 / width) * height
          width = 1200
        }
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx?.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', 0.85))
      }
      img.onerror = reject
      img.src = e.target?.result as string
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const onUpload = async (opt: UploadRequestOptions) => {
  try {
    const base64 = await compressImage(opt.file)
    imageUrls.value.push(base64)
  } catch {
    ElMessage.error('图片处理失败')
  }
}

const onRemove = (file: UploadFile) => {
  const index = imageFileList.value.findIndex(f => f.uid === file.uid)
  if (index > -1) {
    imageUrls.value.splice(index, 1)
  }
}

const loadEditData = async () => {
  const id = route.query.id as string
  if (!id) return
  try {
    const house = await getHouseDetailApi(Number(id)) as unknown as HouseInfo
    editHouse.value = house
    
    // 解析楼层
    let floor = null, totalFloor = null
    if (house.floor) {
      const match = house.floor.match(/(\d+)\/(\d+)/)
      if (match) {
        floor = parseInt(match[1])
        totalFloor = parseInt(match[2])
      }
    }
    
    form.value = {
      title: house.title,
      description: house.description,
      province: house.province,
      city: house.city,
      district: house.district,
      address: house.address,
      area: house.area,
      houseType: house.houseType,
      floor,
      totalFloor,
      orientation: house.orientation,
      rentPrice: house.rentPrice,
      depositType: house.depositType || '',
      longitude: house.longitude || null,
      latitude: house.latitude || null
    }
    
    // 解析配套设施
    if (house.facilities) {
      try {
        const facilities = JSON.parse(house.facilities) as string[]
        facilities.forEach(f => {
          if (facilitiesMap.hasOwnProperty(f)) {
            facilitiesMap[f] = true
          }
        })
      } catch {}
    }
    
    // 解析图片
    if (house.images) {
      try {
        const imgs = JSON.parse(house.images) as string[]
        imageUrls.value = imgs
        imageFileList.value = imgs.map((url, i) => ({
          uid: -i - 1,
          name: `image-${i + 1}`,
          status: 'success',
          url
        } as UploadFile))
      } catch {}
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载房源信息失败')
    router.push('/landlord/house/list')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请完善必填信息')
      return
    }
    
    if (!imageUrls.value.length) {
      ElMessage.error('请至少上传一张房源图片')
      return
    }
    
    if (!certImages.front) {
      ElMessage.error('请上传房产证正面照片')
      return
    }
    
    submitting.value = true
    try {
      const floorStr = form.value.floor && form.value.totalFloor 
        ? `${form.value.floor}/${form.value.totalFloor}层` 
        : ''
      
      const data: HousePublishDTO = {
        title: form.value.title,
        description: form.value.description,
        province: form.value.province,
        city: form.value.city,
        district: form.value.district,
        address: form.value.address,
        area: form.value.area!,
        houseType: form.value.houseType,
        floor: floorStr,
        orientation: form.value.orientation,
        rentPrice: form.value.rentPrice!,
        depositType: form.value.depositType,
        rentOption: 3, // 默认都支持
        facilities: JSON.stringify(selectedFacilities.value),
        images: JSON.stringify(imageUrls.value),
        longitude: form.value.longitude || undefined,
        latitude: form.value.latitude || undefined,
        propertyLicenseFront: certImages.front,
        propertyLicenseBack: certImages.back || undefined,
        propertyLicenseOther: certImages.other || undefined
      }
      
      if (isEdit.value) {
        await updateHouseApi(Number(route.query.id), data)
        ElMessage.success('房源更新成功，等待审核')
      } else {
        await publishHouseApi(data)
        ElMessage.success('房源发布成功，等待审核')
      }
      router.push('/landlord/house/list')
    } catch (e: any) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleReset = () => {
  form.value = {
    title: '',
    description: '',
    province: '',
    city: '',
    district: '',
    address: '',
    area: null,
    houseType: '',
    floor: null,
    totalFloor: null,
    orientation: '',
    rentPrice: null,
    depositType: '',
    longitude: null,
    latitude: null
  }
  facilityOptions.forEach(f => facilitiesMap[f] = false)
  imageFileList.value = []
  imageUrls.value = []
  certImages.front = ''
  certImages.back = ''
  certImages.other = ''
  certFileLists.front = []
  certFileLists.back = []
  certFileLists.other = []
  currentStep.value = 0
  formRef.value?.resetFields()
  if (publishMarker && publishMap) {
    publishMap.remove(publishMarker)
    publishMarker = null
  }
}

// AI智能解析房源描述
const parseWithAI = async () => {
  const description = aiDescription.value.trim()
  if (!description) {
    ElMessage.warning('请先输入房源描述')
    return
  }
  
  aiParsing.value = true
  try {
    const res: any = await parseHouseDescriptionApi({ description })
    
    if (res) {
      // 解析JSON
      let parsedData
      try {
        parsedData = JSON.parse(res)
      } catch (e) {
        ElMessage.error('AI返回数据格式错误，请重试')
        return
      }
      
      // 填充表单
      if (parsedData.title) form.value.title = parsedData.title
      if (parsedData.houseType) form.value.houseType = parsedData.houseType
      if (parsedData.area) form.value.area = parsedData.area
      if (parsedData.floor) form.value.floor = parsedData.floor
      if (parsedData.totalFloor) form.value.totalFloor = parsedData.totalFloor
      if (parsedData.orientation) form.value.orientation = parsedData.orientation
      if (parsedData.rentPrice) form.value.rentPrice = parsedData.rentPrice
      if (parsedData.depositType) form.value.depositType = parsedData.depositType
      if (parsedData.description) form.value.description = parsedData.description
      
      // 填充地址
      if (parsedData.province) {
        form.value.province = parsedData.province
      }
      if (parsedData.city) {
        await nextTick()
        form.value.city = parsedData.city
      }
      if (parsedData.district) {
        await nextTick()
        form.value.district = parsedData.district
      }
      if (parsedData.detailAddress) form.value.address = parsedData.detailAddress
      
      // 填充设施
      if (parsedData.facilities && Array.isArray(parsedData.facilities)) {
        parsedData.facilities.forEach((facility: string) => {
          if (facilityOptions.includes(facility)) {
            facilitiesMap[facility] = true
          }
        })
      }
      
      ElMessage.success('AI解析成功！请检查并调整信息')
      // 清空AI输入框
      aiDescription.value = ''
    } else {
      ElMessage.warning('AI解析失败，请重试')
    }
  } catch (e: any) {
    console.error('AI解析失败', e)
    ElMessage.error('解析失败，请稍后重试')
  } finally {
    aiParsing.value = false
  }
}

onMounted(() => {
  if (isEdit.value) {
    loadEditData()
  }
})

onBeforeUnmount(() => {
  if (publishMap) { publishMap.destroy(); publishMap = null }
  if (addressDebounceTimer) clearTimeout(addressDebounceTimer)
})
</script>

<style scoped>
.house-publish-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 120px);
}

.main-card {
  max-width: 960px;
  margin: 0 auto;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.step-bar {
  margin: 24px 0 32px;
  padding: 0 20px;
}

/* AI智能解析卡片 */
.ai-parse-card {
  margin: 20px 0;
  border: 2px solid #67c23a;
  border-radius: 8px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
}

.ai-parse-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.ai-parse-title {
  font-size: 16px;
  font-weight: 600;
  color: #67c23a;
}

.ai-parse-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-parse-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-parse-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #67c23a;
  flex: 1;
}

.publish-form {
  padding: 0 20px;
}

.step-card {
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.step-card :deep(.el-card__header) {
  padding: 14px 20px;
  background: #fafafa;
  border-bottom: 1px solid #e4e7ed;
}

.section-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.unit-text {
  display: inline-block;
  width: 45px;
  text-align: right;
  color: #606266;
  font-size: 14px;
}

/* 配套设施网格 */
.facility-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px 16px;
}

.facility-item {
  margin-right: 0 !important;
}

/* AI生成按钮区域 */
.ai-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.ai-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  font-size: 13px;
  color: #0050b3;
  flex: 1;
}

.ai-tip .el-icon {
  font-size: 16px;
  flex-shrink: 0;
}

/* 图片上传 */
.upload-wrapper {
  width: 100%;
}

.image-uploader :deep(.el-upload-list--picture-card) {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-uploader :deep(.el-upload--picture-card) {
  width: 120px;
  height: 120px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  transition: all 0.3s;
}

.image-uploader :deep(.el-upload--picture-card:hover) {
  border-color: #409eff;
}

.image-uploader :deep(.el-upload-list__item) {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  margin: 0;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #8c939d;
}

.upload-text {
  font-size: 12px;
  margin-top: 6px;
}

.upload-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}

/* 预览区域 */
.preview-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px dashed #e4e7ed;
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.preview-desc {
  background: #fafafa;
  border-radius: 6px;
}

.preview-value {
  font-weight: 500;
}

.price-text {
  color: #f56c6c;
  font-weight: 600;
}

.facility-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}

/* 证件上传 */
.cert-upload-area {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.cert-uploader :deep(.el-upload--picture-card),
.cert-uploader :deep(.el-upload-list__item) {
  width: 140px;
  height: 100px;
  border-radius: 6px;
}

.cert-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  padding-top: 8px;
  max-width: 200px;
  line-height: 1.5;
}

/* 底部按钮 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px 0 8px;
  border-top: 1px solid #ebeef5;
  margin-top: 24px;
}

.action-buttons .el-button {
  min-width: 120px;
}

/* 响应式 */
@media (max-width: 768px) {
  .facility-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .image-uploader :deep(.el-upload--picture-card),
  .image-uploader :deep(.el-upload-list__item) {
    width: 100px;
    height: 100px;
  }
}

/* 地图定位 */
.map-locate-wrap {
  width: 100%;
}
.map-locate-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.map-locate-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}
.publish-map {
  width: 100%;
  height: 300px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}
</style>
