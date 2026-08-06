<script setup lang="ts">
/**
 * 机构详情页 - 基本信息 tab。
 *
 * 只读展示 ParkInfo 关键字段（el-descriptions），提供"编辑"按钮打开 el-dialog + el-form
 * 修改主表（提交 updatePark）。复用主列表页编辑表单字段集。
 *
 * 注：operateStatus 由状态机驱动（transition 端点），本表单不含该字段。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getPark, updatePark } from '@/api/park'
import {
  DAYAN_LEVEL_OPTIONS,
  ABILITY_TYPE_OPTIONS,
  NATURE_TYPE_OPTIONS,
  PARK_OPERATE_STATUS_OPTIONS
} from '@/types/park'
import type { ParkInfo } from '@/types/park'
import RegionSelect from '@/components/RegionSelect.vue'

const props = defineProps<{
  /** 机构编码（从详情页路由 prop 带入） */
  parkCode: string
}>()

const loading = ref(false)
const parkInfo = ref<ParkInfo | null>(null)

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

loadDetail()

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

function formatDate(s?: string): string {
  if (!s) return '--'
  // 截取 YYYY-MM-DD（后端返回完整时间戳时只取日期部分）
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
  abilityType: undefined,
  natureType: undefined,
  dayanLevel: undefined,
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  serviceHotline: '',
  totalBeds: undefined,
  availableBeds: undefined,
  baseDescription: '',
  specialtyTag: '',
  remark: ''
})

const rules: FormRules<ParkInfo> = {
  fullName: [{ required: true, message: '请输入机构全称', trigger: 'blur' }]
}

function openEdit() {
  if (!parkInfo.value) return
  Object.assign(form, {
    parkCode: parkInfo.value.parkCode,
    fullName: parkInfo.value.fullName ?? '',
    shortName: parkInfo.value.shortName ?? '',
    supplierCode: parkInfo.value.supplierCode ?? '',
    brand: parkInfo.value.brand ?? '',
    abilityType: parkInfo.value.abilityType,
    natureType: parkInfo.value.natureType,
    dayanLevel: parkInfo.value.dayanLevel,
    provinceCode: parkInfo.value.provinceCode ?? '',
    cityCode: parkInfo.value.cityCode ?? '',
    districtCode: parkInfo.value.districtCode ?? '',
    address: parkInfo.value.address ?? '',
    serviceHotline: parkInfo.value.serviceHotline ?? '',
    totalBeds: parkInfo.value.totalBeds,
    availableBeds: parkInfo.value.availableBeds,
    baseDescription: parkInfo.value.baseDescription ?? '',
    specialtyTag: parkInfo.value.specialtyTag ?? '',
    remark: parkInfo.value.remark ?? ''
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

/** 暴露刷新方法，供详情页外部刷新 */
defineExpose({ loadDetail })
</script>

<template>
  <div v-loading="loading">
    <template v-if="parkInfo">
      <div class="basic-toolbar">
        <el-button type="primary" :icon="'Edit'" @click="openEdit">编辑基本信息</el-button>
      </div>

      <el-descriptions :column="3" border>
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
        <el-descriptions-item label="所在地区" :span="3">
          {{ parkInfo.province }}{{ parkInfo.city }}{{ parkInfo.district }}
        </el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="3">{{ parkInfo.address ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="服务热线">{{ parkInfo.serviceHotline ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="总床位">{{ parkInfo.totalBeds ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="可用床位">{{ parkInfo.availableBeds ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="特色标签" :span="3">{{ parkInfo.specialtyTag ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="基地简介" :span="3">{{ parkInfo.baseDescription ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(parkInfo.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ parkInfo.remark ?? '--' }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <el-empty v-else-if="!loading" description="未加载到机构信息" />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="编辑机构基本信息"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
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
              <el-input v-model="form.specialtyTag" placeholder="特色标签" />
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
          <el-col :span="12">
            <el-form-item label="服务热线">
              <el-input v-model="form.serviceHotline" placeholder="服务热线" />
            </el-form-item>
          </el-col>
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
  </div>
</template>

<style scoped>
.basic-toolbar {
  margin-bottom: 16px;
}
</style>
