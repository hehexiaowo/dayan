<script setup lang="ts">
/**
 * 机构详情页 - 餐饮 tab。
 *
 * 与 CareTab 同构（type + price 双层展开行模式），差异：
 * - type 字段：foodTypeCode/foodTypeName/mealPlan(1/2/3/4)/dietFeatures/sampleMenu(JSON)/
 *   specialDiet(0/1)/specialDietDescription/description/coverImage
 * - price 字段：与 ParkCarePrice 完全一致（外键换 foodTypeCode）
 * - priceType 选项用 BILLING_CYCLE_OPTIONS（统一计费周期）
 *
 * 红线：主键 id；foodTypeCode 用户填写非系统生成，update 不可改；
 * price 展开行用 /list（parkCode+foodTypeCode）；sampleMenu 用 textarea 原文编辑；
 * specialDiet 提交 0/1 非 true/false。
 */
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageFoodTypes,
  createFoodType,
  updateFoodType,
  deleteFoodType
} from '@/api/park-food'
import {
  listPricingsByRef,
  createPricing,
  updatePricing,
  deletePricing
} from '@/api/park-pricing'
import { BILLING_CYCLE_OPTIONS, billingCycleLabel } from '@/types/park'
import type { ParkFoodType, ParkFoodTypeQuery, ParkPricing } from '@/types/park'
import FileUploader from '@/components/FileUploader/index.vue'

const props = defineProps<{
  parkCode: string
}>()

// ---------- 餐饮 type 列表 ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkFoodType,
  ParkFoodTypeQuery,
  number
>(
  {
    page: pageFoodTypes,
    create: createFoodType,
    update: (id, data) => updateFoodType(id, data),
    remove: deleteFoodType
  },
  {
    initialQuery: { foodTypeName: '', status: undefined },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode }
  }
)

loadPage()

/** 餐食方案选项（mealPlan） */
const MEAL_PLAN_OPTIONS = [
  { label: '方案一', value: 1 },
  { label: '方案二', value: 2 },
  { label: '方案三', value: 3 },
  { label: '方案四', value: 4 }
] as const

function mealPlanLabel(v?: number): string {
  const found = MEAL_PLAN_OPTIONS.find((o) => o.value === v)
  return found ? found.label : '--'
}

// ---------- type 新增/编辑弹窗 ----------
const typeDialogVisible = ref(false)
const typeDialogMode = ref<'create' | 'edit'>('create')
const typeSubmitLoading = ref(false)
const typeFormRef = ref<FormInstance>()

const typeForm = reactive<ParkFoodType>({
  id: undefined,
  parkCode: '',
  foodTypeCode: '',
  foodTypeName: '',
  mealPlan: undefined,
  dietFeatures: '',
  sampleMenu: '',
  specialDiet: 0,
  specialDietDescription: '',
  description: '',
  coverImage: '',
  sortOrder: 0,
  status: 1
})

const typeRules: FormRules<ParkFoodType> = {
  foodTypeCode: [
    { required: true, message: '请输入餐饮编码', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  foodTypeName: [
    { required: true, message: '请输入餐饮名称', trigger: 'blur' },
    { max: 200, message: '不超过 200 个字符', trigger: 'blur' }
  ]
}

function resetTypeForm() {
  Object.assign(typeForm, {
    id: undefined,
    parkCode: '',
    foodTypeCode: '',
    foodTypeName: '',
    mealPlan: undefined,
    dietFeatures: '',
    sampleMenu: '',
    specialDiet: 0,
    specialDietDescription: '',
    description: '',
    coverImage: '',
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

function openEditType(row: ParkFoodType) {
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
      await createFoodType(typeForm)
      ElMessage.success('新增成功')
    } else if (typeForm.id) {
      await updateFoodType(typeForm.id, typeForm)
      ElMessage.success('修改成功')
    }
    typeDialogVisible.value = false
    loadPage()
  } finally {
    typeSubmitLoading.value = false
  }
}

async function handleDeleteType(row: ParkFoodType) {
  if (!row.id) return
  await ElMessageBox.confirm(
    '确定删除该餐饮类型吗？删除前请先删除该类型下所有价格记录（不级联删除）。',
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await deleteFoodType(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 展开行 price ----------
const priceMap = ref<Map<string, ParkPricing[]>>(new Map())
const priceLoadingMap = ref<Map<string, boolean>>(new Map())

async function loadPrices(row: ParkFoodType) {
  if (!row.foodTypeCode) return
  priceLoadingMap.value.set(row.foodTypeCode, true)
  try {
    const list = await listPricingsByRef(props.parkCode, 'food_type', row.foodTypeCode)
    priceMap.value.set(row.foodTypeCode, list)
  } catch {
    priceMap.value.set(row.foodTypeCode, [])
  } finally {
    priceLoadingMap.value.set(row.foodTypeCode, false)
  }
}

function handleExpandChange(row: ParkFoodType, expanded: ParkFoodType[]) {
  const isExpanded = expanded.some((r) => r.id === row.id)
  if (isExpanded && row.foodTypeCode) {
    loadPrices(row)
  }
}

// ---------- 主表"当前价"列：分页加载后并行批量取当前价（避免 N+1 串行）----------
/** key=foodTypeCode → 当前价展示文本（如 "¥150/月"），无当前价则无 key */
const currentPriceMap = ref<Map<string, string>>(new Map())

/** 取一条 price 的展示文本 */
function formatPriceText(p: ParkPricing): string {
  const unit = p.billingCycle != null ? '/' + billingCycleLabel(p.billingCycle) : ''
  return `¥${p.salePrice}${unit}`
}

/** 分页数据变化后，批量拉取每行当前价（Promise.all 并行，单页最多 size 条） */
async function loadCurrentPrices(rows: ParkFoodType[]) {
  const codes = rows.filter((r) => r.foodTypeCode).map((r) => r.foodTypeCode)
  if (codes.length === 0) {
    currentPriceMap.value.clear()
    return
  }
  const results = await Promise.all(
    codes.map(async (code) => {
      try {
        const list = await listPricingsByRef(props.parkCode, 'food_type', code)
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

/** price 增删改后同步刷新当前价（复用已加载的 priceMap，避免重复请求） */
function refreshCurrentPrice(foodTypeCode: string) {
  const list = priceMap.value.get(foodTypeCode)
  if (!list) {
    currentPriceMap.value.delete(foodTypeCode)
    return
  }
  const cur = list.find((p) => p.isCurrent === 1)
  if (cur) {
    currentPriceMap.value.set(foodTypeCode, formatPriceText(cur))
  } else {
    currentPriceMap.value.delete(foodTypeCode)
  }
}

/** 监听分页数据变化，自动加载当前价 */
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
const priceContext = reactive({ parkCode: '', foodTypeCode: '' })

const priceForm = reactive<ParkPricing>({
  id: undefined,
  parkCode: '',
  chargeType: 3,
  refType: 'food_type',
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
    chargeType: 3,
    refType: 'food_type',
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

function openCreatePrice(foodTypeCode: string, foodTypeName?: string) {
  priceDialogMode.value = 'create'
  resetPriceForm()
  priceContext.parkCode = props.parkCode
  priceContext.foodTypeCode = foodTypeCode
  priceForm.parkCode = props.parkCode
  priceForm.refCode = foodTypeCode
  priceForm.refName = foodTypeName || ''
  priceDialogVisible.value = true
}

function openEditPrice(row: ParkPricing, foodTypeCode: string) {
  priceDialogMode.value = 'edit'
  resetPriceForm()
  Object.assign(priceForm, row)
  priceContext.parkCode = props.parkCode
  priceContext.foodTypeCode = foodTypeCode
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
    priceForm.refCode = priceContext.foodTypeCode
    priceForm.refType = 'food_type'
    priceForm.chargeType = 3
    if (priceDialogMode.value === 'create') {
      await createPricing(priceForm)
      ElMessage.success('新增成功')
    } else if (priceForm.id) {
      await updatePricing(priceForm.id, priceForm)
      ElMessage.success('修改成功')
    }
    priceDialogVisible.value = false
    loadPrices({ foodTypeCode: priceContext.foodTypeCode } as ParkFoodType)
    refreshCurrentPrice(priceContext.foodTypeCode)
  } finally {
    priceSubmitLoading.value = false
  }
}

async function handleDeletePrice(row: ParkPricing, foodTypeCode: string) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该价格记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deletePricing(row.id)
  ElMessage.success('删除成功')
  loadPrices({ foodTypeCode } as ParkFoodType)
  refreshCurrentPrice(foodTypeCode)
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
  <div class="food-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="餐饮名称">
        <el-input v-model="query.foodTypeName" placeholder="餐饮名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停售" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreateType">新增餐饮</el-button>
      </el-form-item>
    </el-form>

    <!-- 主表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      row-key="id"
      @expand-change="(row: ParkFoodType, expanded: ParkFoodType[]) => handleExpandChange(row, expanded)"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="price-block" v-loading="priceLoadingMap.get(row.foodTypeCode)">
            <div class="price-toolbar">
              <span class="price-title">价格记录（{{ row.foodTypeName }}）</span>
              <el-button type="primary" size="small" :icon="'Plus'" @click="openCreatePrice(row.foodTypeCode, row.foodTypeName)">
                新增价格
              </el-button>
            </div>
            <el-table v-if="(priceMap.get(row.foodTypeCode) || []).length > 0" :data="priceMap.get(row.foodTypeCode) || []" border size="small">
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
              <el-table-column prop="isCurrent" label="当前价" width="80" align="center">
                <template #default="{ row: p }">
                  <el-tag v-if="p.isCurrent === 1" type="success" size="small">当前</el-tag>
                  <span v-else>—</span>
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
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row: p }">
                  <el-button link type="primary" size="small" @click="openEditPrice(p, row.foodTypeCode)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="handleDeletePrice(p, row.foodTypeCode)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty
              v-else-if="!priceLoadingMap.get(row.foodTypeCode)"
              description="暂无价格记录，点击右上角新增"
              :image-size="60"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="foodTypeCode" label="餐饮编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="foodTypeName" label="餐饮名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="当前价" width="130" align="center">
        <template #default="{ row }">
          <span v-if="currentPriceMap.get(row.foodTypeCode)" class="current-price">
            {{ currentPriceMap.get(row.foodTypeCode) }}
          </span>
          <span v-else class="price-empty">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="mealPlan" label="餐食方案" width="100" align="center">
        <template #default="{ row }">{{ mealPlanLabel(row.mealPlan) }}</template>
      </el-table-column>
      <el-table-column prop="dietFeatures" label="饮食特色" min-width="160" show-overflow-tooltip />
      <el-table-column prop="specialDiet" label="特殊饮食" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.specialDiet === 1" type="warning" size="small">有</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
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
      :title="typeDialogMode === 'create' ? '新增餐饮类型' : '编辑餐饮类型'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="餐饮编码" prop="foodTypeCode">
              <el-input
                v-model="typeForm.foodTypeCode"
                placeholder="业务编码（同机构下唯一）"
                maxlength="50"
                :disabled="typeDialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="餐饮名称" prop="foodTypeName">
              <el-input v-model="typeForm.foodTypeName" placeholder="餐饮名称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="餐食方案">
              <el-select v-model="typeForm.mealPlan" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in MEAL_PLAN_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="typeForm.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="饮食特色">
              <el-input v-model="typeForm.dietFeatures" placeholder="饮食特色" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="样板菜单(JSON)">
              <el-input v-model="typeForm.sampleMenu" type="textarea" :rows="3" placeholder="样板菜单 JSON 原文" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="特殊饮食">
              <el-switch v-model="typeForm.specialDiet" :active-value="1" :inactive-value="0" />
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
            <el-form-item label="特殊饮食说明">
              <el-input v-model="typeForm.specialDietDescription" type="textarea" :rows="2" placeholder="特殊饮食说明" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图">
              <FileUploader v-model="typeForm.coverImage" type="image" module="park" :asset-park-code="props.parkCode" asset-source-type="food_type" :asset-source-ref="typeForm.foodTypeCode" />
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
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计费周期">
              <el-select v-model="priceForm.billingCycle" placeholder="请选择" clearable style="width: 100%">
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
              <el-switch v-model="priceForm.isCurrent" :active-value="1" :inactive-value="0" />
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
  </div>
</template>

<style scoped lang="scss">
.food-tab {
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
