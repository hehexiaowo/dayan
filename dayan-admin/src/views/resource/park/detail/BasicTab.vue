<script setup lang="ts">
/**
 * 机构详情页 - 基本信息 tab。
 *
 * 只读展示 ParkInfo 全量关键字段（el-descriptions，分组），提供"编辑"按钮打开 el-dialog + el-form
 * 修改主表（提交 updatePark）。
 *
 * 注：operateStatus 由状态机驱动（transition 端点），评分/统计字段只读展示不在编辑表单中。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getPark, updatePark } from '@/api/park'
import { getScore, updateScore } from '@/api/park-score'
import {
  DAYAN_LEVEL_OPTIONS,
  ABILITY_TYPE_OPTIONS,
  NETWORK_TAG_OPTIONS,
  NATURE_TYPE_OPTIONS,
  PARK_OPERATE_STATUS_OPTIONS,
  CONTRACT_PERIOD_OPTIONS,
  IS_HOT_OPTIONS,
  SUB_SCRIPT_OPTIONS
} from '@/types/park'
import type { ParkInfo, ParkScore } from '@/types/park'
import RegionSelect from '@/components/RegionSelect.vue'
import FileUploader from '@/components/FileUploader/index.vue'
import { formatFileUrl } from '@/utils/file'

const props = defineProps<{
  /** 机构编码（从详情页路由 prop 带入） */
  parkCode: string
}>()

const loading = ref(false)
const parkInfo = ref<ParkInfo | null>(null)
/** 评分独立加载（从 park_score 表） */
const parkScore = ref<ParkScore | null>(null)

async function loadDetail() {
  if (!props.parkCode) return
  loading.value = true
  try {
    parkInfo.value = await getPark(props.parkCode)
  } catch {
    parkInfo.value = null
  } finally {
    loading.value = false
  }
}

/** 独立加载评分（park_score 表，与 park_info 拆分） */
async function loadScore() {
  if (!props.parkCode) return
  try {
    parkScore.value = await getScore(props.parkCode)
  } catch {
    parkScore.value = null
  }
}

loadDetail()
loadScore()

// ---------- 辅助渲染 ----------
function operateStatusLabel(s?: number): string {
  const found = PARK_OPERATE_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : '--'
}

function abilityTypeLabel(v?: number): string {
  const found = ABILITY_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function natureTypeLabel(v?: number): string {
  const found = NATURE_TYPE_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function dayanLevelLabel(v?: number): string {
  const found = DAYAN_LEVEL_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function contractPeriodLabel(v?: number): string {
  const found = CONTRACT_PERIOD_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function isHotLabel(v?: number): string {
  const found = IS_HOT_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function subScriptLabel(v?: string): string {
  const found = SUB_SCRIPT_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}

// ---------- 编辑弹窗 ----------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ParkInfo>({
  parkCode: undefined,
  fullName: '',
  shortName: '',
  supplierCode: '',
  brand: '',
  brandIntroduction: '',
  brandLogo: '',
  operationSubject: '',
  operationSubjectDescription: '',
  importantShareholders: '',
  partnerCompany: '',
  businessLicenseNo: '',
  businessBd: '',
  abilityType: undefined,
  networkTags: [],
  natureType: undefined,
  specialtyTag: '',
  dayanLevel: undefined,
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  longitude: '',
  latitude: '',
  serviceHotline: '',
  baseDescription: '',
  totalArea: '',
  buildingArea: '',
  greenAreaRate: '',
  totalBeds: undefined,
  availableBeds: undefined,
  occupancyRate: '',
  staffCount: undefined,
  nurseCount: undefined,
  nursePatientRatio: '',
  minPriceDisplay: undefined,
  maxPriceDisplay: undefined,
  priceUnit: '',
  checkInAgeMin: undefined,
  checkInAgeMax: undefined,
  checkInDescription: '',
  depositAmount: undefined,
  depositDescription: '',
  contractPeriod: undefined,
  isHot: undefined,
  subScript: '',
  sortOrder: 0,
  remark: ''
})

const rules: FormRules<ParkInfo> = {
  fullName: [{ required: true, message: '请输入机构全称', trigger: 'blur' }]
}

function openEdit() {
  if (!parkInfo.value) return
  const d = parkInfo.value
  Object.assign(form, {
    parkCode: d.parkCode,
    fullName: d.fullName ?? '',
    shortName: d.shortName ?? '',
    supplierCode: d.supplierCode ?? '',
    brand: d.brand ?? '',
    brandIntroduction: d.brandIntroduction ?? '',
    brandLogo: d.brandLogo ?? '',
    operationSubject: d.operationSubject ?? '',
    operationSubjectDescription: d.operationSubjectDescription ?? '',
    importantShareholders: d.importantShareholders ?? '',
    partnerCompany: d.partnerCompany ?? '',
    businessLicenseNo: d.businessLicenseNo ?? '',
    businessBd: d.businessBd ?? '',
    abilityType: d.abilityType,
    networkTags: d.networkTags ?? [],
    natureType: d.natureType,
    specialtyTag: d.specialtyTag ?? '',
    dayanLevel: d.dayanLevel,
    provinceCode: d.provinceCode ?? '',
    cityCode: d.cityCode ?? '',
    districtCode: d.districtCode ?? '',
    address: d.address ?? '',
    longitude: d.longitude ?? '',
    latitude: d.latitude ?? '',
    serviceHotline: d.serviceHotline ?? '',
    baseDescription: d.baseDescription ?? '',
    totalArea: d.totalArea ?? '',
    buildingArea: d.buildingArea ?? '',
    greenAreaRate: d.greenAreaRate ?? '',
    totalBeds: d.totalBeds,
    availableBeds: d.availableBeds,
    occupancyRate: d.occupancyRate ?? '',
    staffCount: d.staffCount,
    nurseCount: d.nurseCount,
    nursePatientRatio: d.nursePatientRatio ?? '',
    minPriceDisplay: d.minPriceDisplay,
    maxPriceDisplay: d.maxPriceDisplay,
    priceUnit: d.priceUnit ?? '',
    checkInAgeMin: d.checkInAgeMin,
    checkInAgeMax: d.checkInAgeMax,
    checkInDescription: d.checkInDescription ?? '',
    depositAmount: d.depositAmount,
    depositDescription: d.depositDescription ?? '',
    contractPeriod: d.contractPeriod,
    isHot: d.isHot,
    subScript: d.subScript ?? '',
    sortOrder: d.sortOrder ?? 0,
    remark: d.remark ?? ''
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!form.parkCode) return
  submitLoading.value = true
  try {
    await updatePark(form.parkCode, form)
    ElMessage.success('修改成功')
    dialogVisible.value = false
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

// ---------- 评分编辑弹窗 ----------
const scoreDialogVisible = ref(false)
const scoreSubmitLoading = ref(false)
const scoreFormRef = ref<FormInstance>()

const scoreForm = reactive<ParkScore>({
  scoreTotal: undefined,
  scoreEnvironment: undefined,
  scoreRecreation: undefined,
  scoreNursing: undefined,
  scoreFood: undefined,
  scoreService: undefined,
  scorePrice: undefined,
  scoreDescription: ''
})

function openEditScore() {
  const s = parkScore.value
  Object.assign(scoreForm, {
    scoreTotal: s?.scoreTotal,
    scoreEnvironment: s?.scoreEnvironment,
    scoreRecreation: s?.scoreRecreation,
    scoreNursing: s?.scoreNursing,
    scoreFood: s?.scoreFood,
    scoreService: s?.scoreService,
    scorePrice: s?.scorePrice,
    scoreDescription: s?.scoreDescription ?? ''
  })
  scoreDialogVisible.value = true
}

async function handleScoreSubmit() {
  if (!scoreFormRef.value) return
  try {
    await scoreFormRef.value.validate()
  } catch {
    return
  }
  scoreSubmitLoading.value = true
  try {
    await updateScore(props.parkCode, scoreForm)
    ElMessage.success('评分已保存')
    scoreDialogVisible.value = false
    await loadScore()
  } finally {
    scoreSubmitLoading.value = false
  }
}

/** 暴露刷新方法，供详情页外部刷新 */
defineExpose({ loadDetail, loadScore })
</script>

<template>
  <div v-loading="loading">
    <template v-if="parkInfo">
      <div class="basic-toolbar">
        <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
      </div>

      <el-descriptions :column="3" border title="基础信息">
        <el-descriptions-item label="机构编码">{{ parkInfo.parkCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="机构全称">{{ parkInfo.fullName }}</el-descriptions-item>
        <el-descriptions-item label="机构简称">{{ parkInfo.shortName ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="供应商编码">{{ parkInfo.supplierCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ parkInfo.brand ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="运营状态">
          {{ operateStatusLabel(parkInfo.operateStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="能力类型">{{ abilityTypeLabel(parkInfo.abilityType) }}</el-descriptions-item>
        <el-descriptions-item label="性质类型">{{ natureTypeLabel(parkInfo.natureType) }}</el-descriptions-item>
        <el-descriptions-item label="大雁等级">{{ dayanLevelLabel(parkInfo.dayanLevel) }}</el-descriptions-item>
        <el-descriptions-item label="特色标签" :span="3">{{ parkInfo.specialtyTag ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="基地简介" :span="3">{{ parkInfo.baseDescription ?? '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="3" border title="地理位置" class="mt16">
        <el-descriptions-item label="所在地区" :span="3">
          {{ parkInfo.province }}{{ parkInfo.city }}{{ parkInfo.district }}
        </el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="3">{{ parkInfo.address ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="经度">{{ parkInfo.longitude ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="纬度">{{ parkInfo.latitude ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="服务热线">{{ parkInfo.serviceHotline ?? '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="3" border title="品牌与运营主体" class="mt16">
        <el-descriptions-item label="品牌Logo" :span="3">
          <el-image
            v-if="parkInfo.brandLogo"
            :src="formatFileUrl(parkInfo.brandLogo)"
            fit="cover"
            style="width: 80px; height: 80px; border-radius: 4px"
            :preview-src-list="[formatFileUrl(parkInfo.brandLogo)]"
            preview-teleported
          />
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="品牌简介" :span="3">{{ parkInfo.brandIntroduction ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="运营主体">{{ parkInfo.operationSubject ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="营业执照号">{{ parkInfo.businessLicenseNo ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="商务BD">{{ parkInfo.businessBd ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="运营主体介绍" :span="3">{{ parkInfo.operationSubjectDescription ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="重要股东" :span="3">{{ parkInfo.importantShareholders ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="合作公司" :span="3">{{ parkInfo.partnerCompany ?? '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="3" border title="规模与人员" class="mt16">
        <el-descriptions-item label="总床位">{{ parkInfo.totalBeds ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="可用床位">{{ parkInfo.availableBeds ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="入住率">{{ parkInfo.occupancyRate ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="占地面积">{{ parkInfo.totalArea ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="建筑面积">{{ parkInfo.buildingArea ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="绿化率">{{ parkInfo.greenAreaRate ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="员工总数">{{ parkInfo.staffCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="护理人员数">{{ parkInfo.nurseCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="护患比">{{ parkInfo.nursePatientRatio ?? '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="3" border title="价格与入住条件" class="mt16">
        <el-descriptions-item label="最低月费">{{ parkInfo.minPriceDisplay ?? '--' }}{{ parkInfo.priceUnit ? ' ' + parkInfo.priceUnit : '' }}</el-descriptions-item>
        <el-descriptions-item label="最高月费">{{ parkInfo.maxPriceDisplay ?? '--' }}{{ parkInfo.priceUnit ? ' ' + parkInfo.priceUnit : '' }}</el-descriptions-item>
        <el-descriptions-item label="合同期限">{{ contractPeriodLabel(parkInfo.contractPeriod) }}</el-descriptions-item>
        <el-descriptions-item label="入住最低年龄">{{ parkInfo.checkInAgeMin ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="入住最高年龄">{{ parkInfo.checkInAgeMax ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="押金金额">{{ parkInfo.depositAmount != null ? '¥' + parkInfo.depositAmount : '--' }}</el-descriptions-item>
        <el-descriptions-item label="入住说明" :span="3">{{ parkInfo.checkInDescription ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="押金说明" :span="3">{{ parkInfo.depositDescription ?? '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="3" border title="评分" class="mt16">
        <template #extra>
          <el-button type="primary" size="small" :icon="'Edit'" @click="openEditScore">编辑评分</el-button>
        </template>
        <el-descriptions-item label="总评分">
          <el-tag v-if="parkScore?.scoreTotal != null" type="warning" size="small">{{ parkScore.scoreTotal }}</el-tag>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="环境">{{ parkScore?.scoreEnvironment ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="文娱">{{ parkScore?.scoreRecreation ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="护理">{{ parkScore?.scoreNursing ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="餐食">{{ parkScore?.scoreFood ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="服务">{{ parkScore?.scoreService ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="价格">{{ parkScore?.scorePrice ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="评分描述" :span="2">{{ parkScore?.scoreDescription ?? '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="3" border title="运营统计" class="mt16">
        <el-descriptions-item label="平台评级">{{ isHotLabel(parkInfo.isHot) }}</el-descriptions-item>
        <el-descriptions-item label="首页角标">{{ subScriptLabel(parkInfo.subScript) }}</el-descriptions-item>
        <el-descriptions-item label="是否已发布">{{ parkInfo.isPublished === 1 ? '已发布' : '未发布' }}</el-descriptions-item>
        <el-descriptions-item label="开业时间">{{ formatDate(parkInfo.openingTime) }}</el-descriptions-item>
        <el-descriptions-item label="浏览次数">{{ parkInfo.viewCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="收藏次数">{{ parkInfo.collectCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(parkInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ parkInfo.remark ?? '--' }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="未加载到机构信息" />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑机构基本信息"
      width="860px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="机构编码">
              <el-input v-model="form.parkCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构全称" prop="fullName">
              <el-input v-model="form.fullName" placeholder="机构全称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构简称">
              <el-input v-model="form.shortName" placeholder="机构简称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商编码">
              <el-input v-model="form.supplierCode" placeholder="供应商编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="form.brand" placeholder="品牌" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="特色标签">
              <el-input v-model="form.specialtyTag" placeholder="多个标签逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="能力类型">
              <el-select v-model="form.abilityType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in ABILITY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="网络归属">
              <el-select v-model="form.networkTags" multiple placeholder="选择网络" style="width: 100%">
                <el-option v-for="o in NETWORK_TAG_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="性质类型">
              <el-select v-model="form.natureType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in NATURE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="大雁等级">
              <el-select v-model="form.dayanLevel" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in DAYAN_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">品牌与运营主体</el-divider>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="品牌Logo">
              <FileUploader v-model="form.brandLogo" type="image" module="park" :asset-park-code="props.parkCode" asset-source-type="park_info" :asset-source-ref="props.parkCode" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="品牌简介">
              <el-input v-model="form.brandIntroduction" type="textarea" :rows="2" placeholder="品牌简介" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运营主体">
              <el-input v-model="form.operationSubject" placeholder="运营主体名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="营业执照号">
              <el-input v-model="form.businessLicenseNo" placeholder="营业执照号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商务BD">
              <el-input v-model="form.businessBd" placeholder="商务BD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合作公司">
              <el-input v-model="form.partnerCompany" placeholder="合作公司" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="运营主体介绍">
              <el-input v-model="form.operationSubjectDescription" type="textarea" :rows="2" placeholder="运营主体介绍" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="重要股东">
              <el-input v-model="form.importantShareholders" type="textarea" :rows="2" placeholder="重要股东信息" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">地理位置</el-divider>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="所在地区">
              <RegionSelect
                v-model:province-code="form.provinceCode"
                v-model:city-code="form.cityCode"
                v-model:district-code="form.districtCode"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="详细地址" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="经度">
              <el-input v-model="form.longitude" placeholder="如 116.310003" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="纬度">
              <el-input v-model="form.latitude" placeholder="如 39.991234" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="服务热线">
              <el-input v-model="form.serviceHotline" placeholder="服务热线" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">规模与价格</el-divider>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="总床位">
              <el-input-number v-model="form.totalBeds" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="可用床位">
              <el-input-number v-model="form.availableBeds" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="入住率">
              <el-input v-model="form.occupancyRate" placeholder="如 80%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="绿化率">
              <el-input v-model="form.greenAreaRate" placeholder="如 35%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="占地面积">
              <el-input v-model="form.totalArea" placeholder="如 5000㎡" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="建筑面积">
              <el-input v-model="form.buildingArea" placeholder="如 3000㎡" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="员工总数">
              <el-input-number v-model="form.staffCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="护理人员">
              <el-input-number v-model="form.nurseCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="护患比">
              <el-input v-model="form.nursePatientRatio" placeholder="如 1:5" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最低月费">
              <el-input-number v-model="form.minPriceDisplay" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最高月费">
              <el-input-number v-model="form.maxPriceDisplay" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="价格单位">
              <el-input v-model="form.priceUnit" placeholder="如 元/月" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">入住条件</el-divider>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="入住最低年龄">
              <el-input-number v-model="form.checkInAgeMin" :min="0" :max="150" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="入住最高年龄">
              <el-input-number v-model="form.checkInAgeMax" :min="0" :max="150" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="合同期限">
              <el-select v-model="form.contractPeriod" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in CONTRACT_PERIOD_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="押金金额">
              <el-input-number v-model="form.depositAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="入住说明">
              <el-input v-model="form.checkInDescription" type="textarea" :rows="2" placeholder="入住说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="押金说明">
              <el-input v-model="form.depositDescription" type="textarea" :rows="2" placeholder="押金说明" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">展示与运营</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="平台评级">
              <el-select v-model="form.isHot" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in IS_HOT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="首页角标">
              <el-select v-model="form.subScript" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in SUB_SCRIPT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="基地简介">
              <el-input v-model="form.baseDescription" type="textarea" :rows="3" placeholder="基地简介" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="内部备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 评分编辑弹窗 -->
    <el-dialog
      v-model="scoreDialogVisible"
      title="编辑机构评分"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="scoreFormRef" :model="scoreForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="总评分">
              <el-input-number v-model="scoreForm.scoreTotal" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="环境评分">
              <el-input-number v-model="scoreForm.scoreEnvironment" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文娱评分">
              <el-input-number v-model="scoreForm.scoreRecreation" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="护理评分">
              <el-input-number v-model="scoreForm.scoreNursing" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="餐食评分">
              <el-input-number v-model="scoreForm.scoreFood" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务评分">
              <el-input-number v-model="scoreForm.scoreService" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格评分">
              <el-input-number v-model="scoreForm.scorePrice" :min="0" :max="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评分描述">
              <el-input v-model="scoreForm.scoreDescription" type="textarea" :rows="2" placeholder="评分说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="scoreDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="scoreSubmitLoading" @click="handleScoreSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.basic-toolbar {
  margin-bottom: 16px;
}
.mt16 {
  margin-top: 16px;
}
</style>
