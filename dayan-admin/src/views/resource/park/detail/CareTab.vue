<script setup lang="ts">
/**
 * 机构详情页 - 照护 tab。
 *
 * 与 RoomTab 同构（type + price 双层展开行模式），差异：
 * - type 字段：careTypeCode/careTypeName/careLevel(1-5)/careTarget/careItems(JSON)/
 *   careFrequency/nursePatientRatio/assessmentCriteria/description
 * - price 字段：与 ParkRoomPrice 同集，但无 includesItems、无 priceChangeReason
 * - priceType 选项用 BILLING_CYCLE_OPTIONS（统一计费周期）
 *
 * 红线：主键 id；careTypeCode 用户填写非系统生成，update 不可改；
 * price 展开行用 /list（parkCode+careTypeCode）；careItems 用 textarea 原文编辑。
 */
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageCareTypes,
  createCareType,
  updateCareType,
  deleteCareType
} from '@/api/park-care'
import {
  listPricingsByRef,
  createPricing,
  updatePricing,
  deletePricing
} from '@/api/park-pricing'
import { BILLING_CYCLE_OPTIONS, billingCycleLabel, CARE_LEVEL_OPTIONS, careLevelLabel } from '@/types/park'
import type { ParkCareType, ParkCareTypeQuery, ParkPricing } from '@/types/park'
import PricingReviseDialog from './PricingReviseDialog.vue'

const props = defineProps<{
  parkCode: string
}>()

// ---------- 照护 type 列表 ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkCareType,
  ParkCareTypeQuery,
  number
>(
  {
    page: pageCareTypes,
    create: createCareType,
    update: (id, data) => updateCareType(id, data),
    remove: deleteCareType
  },
  {
    initialQuery: { careTypeName: '', status: undefined },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode }
  }
)

loadPage()

// ---------- type 新增/编辑弹窗 ----------
const typeDialogVisible = ref(false)
const typeDialogMode = ref<'create' | 'edit'>('create')
const typeSubmitLoading = ref(false)
const typeFormRef = ref<FormInstance>()

const typeForm = reactive<ParkCareType>({
  id: undefined,
  parkCode: '',
  careTypeCode: '',
  careTypeName: '',
  careLevel: undefined,
  careTarget: '',
  careItems: '',
  careFrequency: '',
  nursePatientRatio: '',
  assessmentCriteria: '',
  description: '',
  sortOrder: 0,
  status: 1
})

const typeRules: FormRules<ParkCareType> = {
  careTypeCode: [
    { required: true, message: '请输入照护编码', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  careTypeName: [
    { required: true, message: '请输入照护名称', trigger: 'blur' },
    { max: 200, message: '不超过 200 个字符', trigger: 'blur' }
  ]
}

function resetTypeForm() {
  Object.assign(typeForm, {
    id: undefined,
    parkCode: '',
    careTypeCode: '',
    careTypeName: '',
    careLevel: undefined,
    careTarget: '',
    careItems: '',
    careFrequency: '',
    nursePatientRatio: '',
    assessmentCriteria: '',
    description: '',
    sortOrder: 0,
    status: 1
  })
}

function openCreateType() {
  typeDialogMode.value = 'create'
  resetTypeForm()
  typeForm.parkCode = props.parkCode
  typeDialogVisible.value = true
}

function openEditType(row: ParkCareType) {
  typeDialogMode.value = 'edit'
  resetTypeForm()
  Object.assign(typeForm, row)
  typeDialogVisible.value = true
}

async function handleTypeSubmit() {
  if (!typeFormRef.value) return
  try {
    await typeFormRef.value.validate()
  } catch {
    return
  }
  typeSubmitLoading.value = true
  try {
    if (typeDialogMode.value === 'create') {
      await createCareType(typeForm)
      ElMessage.success('新增成功')
    } else if (typeForm.id) {
      await updateCareType(typeForm.id, typeForm)
      ElMessage.success('修改成功')
    }
    typeDialogVisible.value = false
    loadPage()
  } finally {
    typeSubmitLoading.value = false
  }
}

async function handleDeleteType(row: ParkCareType) {
  if (!row.id) return
  await ElMessageBox.confirm(
    '确定删除该照护类型吗？删除前请先删除该类型下所有价格记录（不级联删除）。',
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await deleteCareType(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 展开行 price ----------
const priceMap = ref<Map<string, ParkPricing[]>>(new Map())
const priceLoadingMap = ref<Map<string, boolean>>(new Map())

async function loadPrices(row: ParkCareType) {
  if (!row.careTypeCode) return
  priceLoadingMap.value.set(row.careTypeCode, true)
  try {
    const list = await listPricingsByRef(props.parkCode, 'care_type', row.careTypeCode)
    priceMap.value.set(row.careTypeCode, list)
  } catch {
    priceMap.value.set(row.careTypeCode, [])
  } finally {
    priceLoadingMap.value.set(row.careTypeCode, false)
  }
}

function handleExpandChange(row: ParkCareType, expanded: ParkCareType[]) {
  const isExpanded = expanded.some((r) => r.id === row.id)
  if (isExpanded && row.careTypeCode) {
    loadPrices(row)
  }
}

// ---------- 主表"当前价"列：分页加载后并行批量取当前价（避免 N+1 串行）----------
const currentPriceMap = ref<Map<string, string>>(new Map())

function formatPriceText(p: ParkPricing): string {
  const unit = p.billingCycle != null ? '/' + billingCycleLabel(p.billingCycle) : ''
  return `¥${p.salePrice}${unit}`
}

async function loadCurrentPrices(rows: ParkCareType[]) {
  const codes = rows.filter((r) => r.careTypeCode).map((r) => r.careTypeCode)
  if (codes.length === 0) {
    currentPriceMap.value.clear()
    return
  }
  const results = await Promise.all(
    codes.map(async (code) => {
      try {
        const list = await listPricingsByRef(props.parkCode, 'care_type', code)
        return [code, list] as const
      } catch {
        return [code, []] as const
      }
    })
  )
  const m = new Map<string, string>()
  for (const [code, list] of results) {
    const cur = (list as ParkPricing[]).find((p) => p.isCurrent === 1)
    if (cur) m.set(code, formatPriceText(cur))
  }
  currentPriceMap.value = m
}

function refreshCurrentPrice(careTypeCode: string) {
  const list = priceMap.value.get(careTypeCode)
  if (!list) {
    currentPriceMap.value.delete(careTypeCode)
    return
  }
  const cur = list.find((p) => p.isCurrent === 1)
  if (cur) {
    currentPriceMap.value.set(careTypeCode, formatPriceText(cur))
  } else {
    currentPriceMap.value.delete(careTypeCode)
  }
}

watch(
  () => tableData.value,
  (rows) => {
    if (rows && rows.length > 0) loadCurrentPrices(rows)
  }
)

// ---------- price 新增/编辑弹窗 ----------
const priceDialogVisible = ref(false)
const priceDialogMode = ref<'create' | 'edit'>('create')
const priceSubmitLoading = ref(false)
const priceFormRef = ref<FormInstance>()
const priceContext = reactive({ parkCode: '', careTypeCode: '' })

const priceForm = reactive<ParkPricing>({
  id: undefined,
  parkCode: '',
  chargeType: 2,
  refType: 'care_type',
  refCode: '',
  refName: '',
  billingCycle: undefined,
  originalPrice: undefined,
  salePrice: undefined,
  discountRate: undefined,
  priceDescription: '',
  effectiveDate: '',
  expireDate: '',
  isCurrent: 1,
  isPromotion: 0,
  promotionDescription: '',
  sortOrder: 0,
  status: 1
})

const priceRules: FormRules<ParkPricing> = {
  salePrice: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }]
}

/** 折扣率是否被用户手动修改过——手动改过则不再自动算 */
let discountEdited = false
/** 原价/售价变化时自动算折扣率 = 售价/原价*100，保留 2 位 */
watch(
  () => [priceForm.originalPrice, priceForm.salePrice],
  ([orig, sale]) => {
    if (discountEdited) return
    if (orig && sale && orig > 0) {
      priceForm.discountRate = Math.round((sale / orig) * 10000) / 100
    } else {
      priceForm.discountRate = undefined
    }
  }
)

function resetPriceForm() {
  Object.assign(priceForm, {
    id: undefined,
    parkCode: '',
    chargeType: 2,
    refType: 'care_type',
    refCode: '',
    refName: '',
    billingCycle: undefined,
    originalPrice: undefined,
    salePrice: undefined,
    discountRate: undefined,
    priceDescription: '',
    effectiveDate: '',
    expireDate: '',
    isCurrent: 1,
    isPromotion: 0,
    promotionDescription: '',
    sortOrder: 0,
    status: 1
  })
  discountEdited = false
}

function openCreatePrice(careTypeCode: string, careTypeName?: string) {
  priceDialogMode.value = 'create'
  resetPriceForm()
  priceContext.parkCode = props.parkCode
  priceContext.careTypeCode = careTypeCode
  priceForm.parkCode = props.parkCode
  priceForm.refCode = careTypeCode
  priceForm.refName = careTypeName || ''
  priceDialogVisible.value = true
}

function openEditPrice(row: ParkPricing, careTypeCode: string) {
  priceDialogMode.value = 'edit'
  resetPriceForm()
  Object.assign(priceForm, row)
  priceContext.parkCode = props.parkCode
  priceContext.careTypeCode = careTypeCode
  priceDialogVisible.value = true
}

async function handlePriceSubmit() {
  if (!priceFormRef.value) return
  try {
    await priceFormRef.value.validate()
  } catch {
    return
  }
  priceSubmitLoading.value = true
  try {
    priceForm.parkCode = priceContext.parkCode
    priceForm.refCode = priceContext.careTypeCode
    priceForm.refType = 'care_type'
    priceForm.chargeType = 2
    if (priceDialogMode.value === 'create') {
      await createPricing(priceForm)
      ElMessage.success('新增成功')
    } else if (priceForm.id) {
      await updatePricing(priceForm.id, priceForm)
      ElMessage.success('修改成功')
    }
    priceDialogVisible.value = false
    loadPrices({ careTypeCode: priceContext.careTypeCode } as ParkCareType)
    refreshCurrentPrice(priceContext.careTypeCode)
  } finally {
    priceSubmitLoading.value = false
  }
}

async function handleDeletePrice(row: ParkPricing, careTypeCode: string) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该价格记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deletePricing(row.id)
  ElMessage.success('删除成功')
  loadPrices({ careTypeCode } as ParkCareType)
  refreshCurrentPrice(careTypeCode)
}

// ---------- 调价弹窗（版本化 revise：立即/预约生效） ----------
const reviseVisible = ref(false)
const reviseTarget = ref<{ id?: number; salePrice?: number; refName?: string; planName?: string } | undefined>(undefined)
/** 调价后需刷新的展开行上下文 */
const reviseContext = reactive({ careTypeCode: '' })

function openRevise(row: { id?: number; salePrice?: number; refName?: string; planName?: string }, careTypeCode: string) {
  reviseTarget.value = row
  reviseContext.careTypeCode = careTypeCode
  reviseVisible.value = true
}

/** 调价成功后重载该展开行价格 + 同步主表"当前价"列 */
async function handleReviseRevived() {
  if (!reviseContext.careTypeCode) return
  await loadPrices({ careTypeCode: reviseContext.careTypeCode } as ParkCareType)
  refreshCurrentPrice(reviseContext.careTypeCode)
}

// ---------- 辅助渲染 ----------
function statusLabel(v?: number): string {
  return v === 1 ? '启用' : '停售'
}
function statusTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}
function formatDate(s?: string): string {
  if (!s) return '--'
  return s.length >= 10 ? s.slice(0, 10) : s
}

defineExpose({ loadPage })
</script>

<template>
  <div class="care-tab">
    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input
        v-model="query.careTypeName"
        placeholder="照护名称"
        clearable
        style="width: 160px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="启用" :value="1" />
        <el-option label="停售" :value="0" />
      </el-select>
      <div class="toolbar-actions">
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button type="primary" :icon="'Plus'" @click="openCreateType">新增照护</el-button>
      </div>
    </div>

    <!-- 主表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      row-key="id"
      @expand-change="(row: ParkCareType, expanded: ParkCareType[]) => handleExpandChange(row, expanded)"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div v-loading="priceLoadingMap.get(row.careTypeCode)" class="price-block">
            <div class="price-toolbar">
              <span class="price-title">价格记录（{{ row.careTypeName }}）</span>
              <el-button type="primary" size="small" :icon="'Plus'" @click="openCreatePrice(row.careTypeCode, row.careTypeName)">
                新增价格
              </el-button>
            </div>
            <el-table v-if="(priceMap.get(row.careTypeCode) || []).length > 0" :data="priceMap.get(row.careTypeCode) || []" border size="small">
              <el-table-column prop="billingCycle" label="计费周期" width="100" align="center">
                <template #default="{ row: p }">{{ billingCycleLabel(p.billingCycle) }}</template>
              </el-table-column>
              <el-table-column prop="originalPrice" label="原价" width="100" align="right" />
              <el-table-column prop="salePrice" label="售价" width="100" align="right" />
              <el-table-column prop="discountRate" label="折扣率" width="90" align="right" />
              <el-table-column prop="effectiveDate" label="生效日期" width="110" align="center">
                <template #default="{ row: p }">{{ formatDate(p.effectiveDate) }}</template>
              </el-table-column>
              <el-table-column prop="expireDate" label="失效日期" width="110" align="center">
                <template #default="{ row: p }">{{ formatDate(p.expireDate) }}</template>
              </el-table-column>
              <el-table-column prop="isCurrent" label="当前价" width="90" align="center">
                <template #default="{ row: p }">
                  <el-tag v-if="p.isCurrent === 1" type="success" size="small">当前</el-tag>
                  <el-tag v-if="p.pendingFlag === 1" type="warning" size="small">待生效</el-tag>
                  <span v-if="p.isCurrent !== 1 && p.pendingFlag !== 1">—</span>
                </template>
              </el-table-column>
              <el-table-column prop="isPromotion" label="促销" width="80" align="center">
                <template #default="{ row: p }">
                  <el-tag v-if="p.isPromotion === 1" type="warning" size="small">促销</el-tag>
                  <span v-else>—</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row: p }">
                  <el-tag :type="statusTagType(p.status)" size="small">{{ statusLabel(p.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="110" align="center">
                <template #default="{ row: p }">{{ formatDate(p.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row: p }">
                  <el-button link type="primary" size="small" @click="openEditPrice(p, row.careTypeCode)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="handleDeletePrice(p, row.careTypeCode)">删除</el-button>
                  <el-button link type="warning" size="small" @click="openRevise(p, row.careTypeCode)">调价</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty
              v-else-if="!priceLoadingMap.get(row.careTypeCode)"
              description="暂无价格记录，点击右上角新增"
              :image-size="60"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="careTypeCode" label="照护编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="careTypeName" label="照护名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="当前价" width="130" align="center">
        <template #default="{ row }">
          <span v-if="currentPriceMap.get(row.careTypeCode)" class="current-price">
            {{ currentPriceMap.get(row.careTypeCode) }}
          </span>
          <span v-else class="price-empty">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="careLevel" label="等级" width="90" align="center">
        <template #default="{ row }">{{ careLevelLabel(row.careLevel) }}</template>
      </el-table-column>
      <el-table-column prop="careTarget" label="照护对象" min-width="140" show-overflow-tooltip />
      <el-table-column prop="careFrequency" label="频次" min-width="120" show-overflow-tooltip />
      <el-table-column prop="nursePatientRatio" label="护患比" width="100" align="center" />
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="120" align="center">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEditType(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDeleteType(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        :current-page="query.current"
        :page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- type 弹窗 -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="typeDialogMode === 'create' ? '新增照护类型' : '编辑照护类型'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="照护编码" prop="careTypeCode">
              <el-input
                v-model="typeForm.careTypeCode"
                placeholder="业务编码（同机构下唯一）"
                maxlength="50"
                :disabled="typeDialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="照护名称" prop="careTypeName">
              <el-input v-model="typeForm.careTypeName" placeholder="照护名称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="照护等级">
              <el-select v-model="typeForm.careLevel" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in CARE_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="typeForm.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="照护对象">
              <el-input v-model="typeForm.careTarget" placeholder="照护对象" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="护患比">
              <el-input v-model="typeForm.nursePatientRatio" placeholder="如 1:3" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="照护频次">
              <el-input v-model="typeForm.careFrequency" placeholder="照护频次" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="typeForm.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停售</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="评估标准">
              <el-input v-model="typeForm.assessmentCriteria" type="textarea" :rows="2" placeholder="评估标准" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="照护项目(JSON)">
              <el-input v-model="typeForm.careItems" type="textarea" :rows="3" placeholder="照护项目 JSON 原文" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="typeForm.description" type="textarea" :rows="2" placeholder="描述" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitLoading" @click="handleTypeSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- price 弹窗 -->
    <el-dialog
      v-model="priceDialogVisible"
      :title="priceDialogMode === 'create' ? '新增价格' : '编辑价格'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="priceFormRef" :model="priceForm" :rules="priceRules" label-width="100px">
        <el-alert
          v-if="priceDialogMode === 'edit'"
          type="warning"
          :closable="false"
          style="margin-bottom: 8px"
        >
          价格、周期与当前价标记不支持直接编辑，调整价格请使用「调价」。
        </el-alert>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计费周期">
              <el-select v-model="priceForm.billingCycle" placeholder="请选择" clearable :disabled="priceDialogMode === 'edit'" style="width: 100%">
                <el-option v-for="o in BILLING_CYCLE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="priceForm.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价">
              <el-input-number
                v-model="priceForm.originalPrice"
                :min="0"
                :precision="2"
                :disabled="priceDialogMode === 'edit'"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="售价" prop="salePrice">
              <el-input-number
                v-model="priceForm.salePrice"
                :min="0"
                :precision="2"
                :disabled="priceDialogMode === 'edit'"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="折扣率">
              <el-input-number
                v-model="priceForm.discountRate"
                :min="0"
                :max="100"
                :precision="2"
                :disabled="priceDialogMode === 'edit'"
                controls-position="right"
                style="width: 100%"
                @change="discountEdited = true"
              />
              <div class="form-tip">按 售价÷原价 自动计算，可手动覆盖</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生效日期" prop="effectiveDate">
              <el-date-picker
                v-model="priceForm.effectiveDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="失效日期">
              <el-date-picker
                v-model="priceForm.expireDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否当前价">
              <el-switch v-model="priceForm.isCurrent" :active-value="1" :inactive-value="0" :disabled="priceDialogMode === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否促销">
              <el-switch v-model="priceForm.isPromotion" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="促销说明">
              <el-input v-model="priceForm.promotionDescription" placeholder="促销说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="价格说明">
              <el-input v-model="priceForm.priceDescription" type="textarea" :rows="2" placeholder="价格说明" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="priceForm.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停售</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="priceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="priceSubmitLoading" @click="handlePriceSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 调价弹窗（版本化：立即/预约生效） -->
    <PricingReviseDialog v-model="reviseVisible" :pricing="reviseTarget" @revived="handleReviseRevived" />
  </div>
</template>

<style scoped lang="scss">
.care-tab {
  .toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;

    .toolbar-actions {
      display: flex;
      gap: 8px;
      margin-left: auto;
    }
  }
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
  .price-block {
    padding: 12px 16px;
    background: var(--el-fill-color-light);
  }
  .price-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }
  .price-title {
    font-weight: 600;
    color: var(--el-text-color-primary);
  }
  .current-price {
    font-weight: 600;
    color: var(--el-color-success);
  }
  .price-empty {
    color: var(--el-text-color-placeholder);
  }
  .form-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    line-height: 1.4;
    margin-top: 2px;
  }
}
</style>
