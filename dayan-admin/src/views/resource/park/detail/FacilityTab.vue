<script setup lang="ts">
/**
 * 机构详情页 - 设施 tab。
 *
 * 架构（facility 主表 + price 子表双层，仿 RoomTab 模式）：
 * 1. 搜索条（facilityTypeName + facilityTypeCategory + status）+ 新增设施按钮
 * 2. 主表格设施列表，useCrud（idKey:'id', fixedParams:{parkCode}）
 * 3. 展开行：展开时调 listFacilityPrices(parkCode, facilityTypeCode) 加载价格，
 *    内联小表格 + 新增/编辑/删除（独立 ref + Map 缓存按需加载）
 * 4. 设施新增/编辑 el-dialog，必填 facilityTypeCode(≤50)/facilityTypeName(≤200)，parkCode 隐藏
 * 5. 价格新增/编辑 el-dialog，业务必填 salePrice/effectiveDate，
 *    facilityTypeCode+parkCode 从展开行上下文带入不显示；priceUnit 自由文本（次/月/场/小时）
 *
 * 红线遵守：
 * - 主键 Long id，useCrud 传 idKey:'id'
 * - facilityTypeCode 用户填写非系统生成；update 时不可改（编辑弹窗内 disabled）
 * - price 展开行用 /list 端点（parkCode+facilityTypeCode 两参）
 * - isCurrent / isPromotion 提交 0/1 非 true/false
 */
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageFacilityTypes,
  createFacilityType,
  updateFacilityType,
  deleteFacilityType
} from '@/api/park-facility'
import {
  listPricingsByRef,
  createPricing,
  updatePricing,
  deletePricing
} from '@/api/park-pricing'
import { FACILITY_TYPE_CATEGORY_OPTIONS, facilityTypeCategoryLabel } from '@/types/park'
import type { ParkFacilityType, ParkFacilityTypeQuery, ParkPricing } from '@/types/park'
import FileUploader from '@/components/FileUploader/index.vue'
import PricingReviseDialog from './PricingReviseDialog.vue'

const props = defineProps<{ parkCode: string }>()

// ---------- 设施 列表（useCrud，主键 id） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkFacilityType,
  ParkFacilityTypeQuery,
  number
>(
  {
    page: pageFacilityTypes,
    create: createFacilityType,
    update: (id, data) => updateFacilityType(id, data),
    remove: deleteFacilityType
  },
  {
    initialQuery: { facilityTypeCode: '', facilityTypeName: '', facilityTypeCategory: undefined, status: undefined },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode }
  }
)

loadPage()

// ---------- 设施 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<ParkFacilityType>({
  id: undefined,
  parkCode: '',
  facilityTypeCode: '',
  facilityTypeName: '',
  facilityTypeCategory: undefined,
  buildingName: '',
  floor: '',
  area: undefined,
  capacity: undefined,
  openTime: '',
  facilityTypeDescription: '',
  coverImage: '',
  images: '',
  sortOrder: 0,
  status: 1
})

const rules: FormRules<ParkFacilityType> = {
  facilityTypeCode: [
    { required: true, message: '请输入设施编码', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  facilityTypeName: [
    { required: true, message: '请输入设施名称', trigger: 'blur' },
    { max: 200, message: '不超过 200 个字符', trigger: 'blur' }
  ]
}

/** images：后端是 string（逗号分隔或 JSON 数组），FileUploader 多图用 string[] */
const imagesModel = computed<string[]>({
  get() {
    const raw = form.images
    if (!raw) return []
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) return parsed.filter((x) => typeof x === 'string')
    } catch {
      // 非 JSON，按逗号分隔
    }
    return raw.split(',').map((s) => s.trim()).filter(Boolean)
  },
  set(val: string[]) {
    form.images = val.length > 0 ? JSON.stringify(val) : ''
  }
})

function resetForm() {
  Object.assign(form, {
    id: undefined,
    parkCode: '',
    facilityTypeCode: '',
    facilityTypeName: '',
    facilityTypeCategory: undefined,
    buildingName: '',
    floor: '',
    area: undefined,
    capacity: undefined,
    openTime: '',
    facilityTypeDescription: '',
    coverImage: '',
    images: '',
    sortOrder: 0,
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.parkCode = props.parkCode
  dialogVisible.value = true
}

function openEdit(row: ParkFacilityType) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createFacilityType(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateFacilityType(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParkFacilityType) {
  if (!row.id) return
  await ElMessageBox.confirm(
    '确定删除该设施吗？删除前请先删除该设施下所有价格记录（不级联删除）。',
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await deleteFacilityType(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 展开行 price 管理 ----------
/** 各展开行的 price 列表缓存，key=facilityTypeCode */
const priceMap = ref<Map<string, ParkPricing[]>>(new Map())
/** price 加载状态 */
const priceLoadingMap = ref<Map<string, boolean>>(new Map())

async function loadPrices(parkCode: string, row: ParkFacilityType) {
  if (!row.facilityTypeCode) return
  priceLoadingMap.value.set(row.facilityTypeCode, true)
  try {
    const list = await listPricingsByRef(parkCode, 'facility_type', row.facilityTypeCode)
    priceMap.value.set(row.facilityTypeCode, list)
  } catch {
    priceMap.value.set(row.facilityTypeCode, [])
  } finally {
    priceLoadingMap.value.set(row.facilityTypeCode, false)
  }
}

/** 展开行 toggle：展开时按需加载 price */
function handleExpandChange(row: ParkFacilityType, expanded: ParkFacilityType[], parkCode: string) {
  const isExpanded = expanded.some((r) => r.id === row.id)
  if (isExpanded && row.facilityTypeCode) {
    loadPrices(parkCode, row)
  }
}

// ---------- 主表"当前价"列：分页加载后并行批量取当前价（避免 N+1 串行）----------
/** key=facilityTypeCode → 当前价展示文本（如 "¥150/次"），无当前价则无 key */
const currentPriceMap = ref<Map<string, string>>(new Map())

/** 取一条 price 的展示文本 */
function formatPriceText(p: ParkPricing): string {
  const unit = p.priceUnit ? `/${p.priceUnit}` : ''
  return `¥${p.salePrice}${unit}`
}

/** 分页数据变化后，批量拉取每行当前价（Promise.all 并行，单页最多 size 条） */
async function loadCurrentPrices(rows: ParkFacilityType[]) {
  const codes = rows.filter((r) => r.facilityTypeCode).map((r) => r.facilityTypeCode)
  if (codes.length === 0) {
    currentPriceMap.value.clear()
    return
  }
  const results = await Promise.all(
    codes.map(async (code) => {
      try {
        const list = await listPricingsByRef(props.parkCode, 'facility_type', code)
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
function refreshCurrentPrice(facilityTypeCode: string) {
  const list = priceMap.value.get(facilityTypeCode)
  if (!list) {
    currentPriceMap.value.delete(facilityTypeCode)
    return
  }
  const cur = list.find((p) => p.isCurrent === 1)
  if (cur) {
    currentPriceMap.value.set(facilityTypeCode, formatPriceText(cur))
  } else {
    currentPriceMap.value.delete(facilityTypeCode)
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
/** 当前 price 所属的设施上下文（facilityTypeCode + parkCode） */
const priceContext = reactive({ parkCode: '', facilityTypeCode: '' })

const priceForm = reactive<ParkPricing>({
  id: undefined,
  parkCode: '',
  chargeType: 5,
  refType: 'facility_type',
  refCode: '',
  refName: '',
  priceUnit: '',
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
    chargeType: 5,
    refType: 'facility_type',
    refCode: '',
    refName: '',
    priceUnit: '',
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

function openCreatePrice(parkCode: string, facilityTypeCode: string, facilityTypeName?: string) {
  priceDialogMode.value = 'create'
  resetPriceForm()
  priceContext.parkCode = parkCode
  priceContext.facilityTypeCode = facilityTypeCode
  priceForm.parkCode = parkCode
  priceForm.refCode = facilityTypeCode
  priceForm.refName = facilityTypeName || ''
  priceDialogVisible.value = true
}

function openEditPrice(row: ParkPricing, parkCode: string, facilityTypeCode: string) {
  priceDialogMode.value = 'edit'
  resetPriceForm()
  Object.assign(priceForm, row)
  priceContext.parkCode = parkCode
  priceContext.facilityTypeCode = facilityTypeCode
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
    // 确保 parkCode + refCode 从上下文带入（编辑时也不可改外键）
    priceForm.parkCode = priceContext.parkCode
    priceForm.refCode = priceContext.facilityTypeCode
    priceForm.refType = 'facility_type'
    priceForm.chargeType = 5
    if (priceDialogMode.value === 'create') {
      await createPricing(priceForm)
      ElMessage.success('新增成功')
    } else if (priceForm.id) {
      await updatePricing(priceForm.id, priceForm)
      ElMessage.success('修改成功')
    }
    priceDialogVisible.value = false
    // 刷新该展开行 price（用上下文 facilityTypeCode）
    loadPrices(priceContext.parkCode, { facilityTypeCode: priceContext.facilityTypeCode } as ParkFacilityType)
    // 同步主表"当前价"列
    refreshCurrentPrice(priceContext.facilityTypeCode)
  } finally {
    priceSubmitLoading.value = false
  }
}

async function handleDeletePrice(row: ParkPricing, parkCode: string, facilityTypeCode: string) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该价格记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deletePricing(row.id)
  ElMessage.success('删除成功')
  loadPrices(parkCode, { facilityTypeCode } as ParkFacilityType)
  refreshCurrentPrice(facilityTypeCode)
}

// ---------- 调价弹窗（版本化 revise：立即/预约生效） ----------
const reviseVisible = ref(false)
const reviseTarget = ref<{ id?: number; salePrice?: number; refName?: string; planName?: string } | undefined>(undefined)
/** 调价后需刷新的展开行上下文 */
const reviseContext = reactive({ parkCode: '', facilityTypeCode: '' })

function openRevise(
  row: { id?: number; salePrice?: number; refName?: string; planName?: string },
  parkCode: string,
  facilityTypeCode: string
) {
  reviseTarget.value = row
  reviseContext.parkCode = parkCode
  reviseContext.facilityTypeCode = facilityTypeCode
  reviseVisible.value = true
}

/** 调价成功后重载该展开行价格 + 同步主表"当前价"列 */
async function handleReviseRevived() {
  if (!reviseContext.facilityTypeCode) return
  await loadPrices(reviseContext.parkCode, { facilityTypeCode: reviseContext.facilityTypeCode } as ParkFacilityType)
  refreshCurrentPrice(reviseContext.facilityTypeCode)
}

// ---------- 辅助渲染 ----------
function statusLabel(v?: number): string {
  return v === 1 ? '启用' : '停用'
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
  <div class="facility-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="设施名称">
        <el-input v-model="query.facilityTypeName" placeholder="设施名称" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="设施类别">
        <el-select v-model="query.facilityTypeCategory" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="o in FACILITY_TYPE_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增设施</el-button>
      </el-form-item>
    </el-form>

    <!-- 主表格（含展开行 price） -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      row-key="id"
      @expand-change="(row: ParkFacilityType, expanded: ParkFacilityType[]) => handleExpandChange(row, expanded, parkCode)"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div v-loading="priceLoadingMap.get(row.facilityTypeCode)" class="price-block">
            <div class="price-toolbar">
              <span class="price-title">价格记录（{{ row.facilityTypeName }}）</span>
              <el-button
                type="primary"
                size="small"
                :icon="'Plus'"
                @click="openCreatePrice(parkCode, row.facilityTypeCode, row.facilityTypeName)"
              >
                新增价格
              </el-button>
            </div>
            <el-table v-if="(priceMap.get(row.facilityTypeCode) || []).length > 0" :data="priceMap.get(row.facilityTypeCode) || []" border size="small">
              <el-table-column prop="priceUnit" label="计费单位" width="100" align="center">
                <template #default="{ row: p }">{{ p.priceUnit || '--' }}</template>
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
                  <el-button link type="primary" size="small" @click="openEditPrice(p, parkCode, row.facilityTypeCode)">
                    编辑
                  </el-button>
                  <el-button link type="danger" size="small" @click="handleDeletePrice(p, parkCode, row.facilityTypeCode)">
                    删除
                  </el-button>
                  <el-button link type="warning" size="small" @click="openRevise(p, parkCode, row.facilityTypeCode)">
                    调价
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty
              v-else-if="!priceLoadingMap.get(row.facilityTypeCode)"
              description="暂无价格记录，点击右上角新增"
              :image-size="60"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="facilityTypeCode" label="设施编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="facilityTypeName" label="设施名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="当前价" width="130" align="center">
        <template #default="{ row }">
          <span v-if="currentPriceMap.get(row.facilityTypeCode)" class="current-price">
            {{ currentPriceMap.get(row.facilityTypeCode) }}
          </span>
          <span v-else class="price-empty">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="facilityTypeCategory" label="类别" width="90" align="center">
        <template #default="{ row }">{{ facilityTypeCategoryLabel(row.facilityTypeCategory) }}</template>
      </el-table-column>
      <el-table-column prop="buildingName" label="楼栋" width="120" show-overflow-tooltip />
      <el-table-column prop="floor" label="楼层" width="90" align="center" />
      <el-table-column prop="area" label="面积(㎡)" width="100" align="right" />
      <el-table-column prop="capacity" label="容纳人数" width="100" align="right" />
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
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 设施 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增设施' : '编辑设施'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设施编码" prop="facilityTypeCode">
              <el-input
                v-model="form.facilityTypeCode"
                placeholder="业务编码（同机构下唯一）"
                maxlength="50"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设施名称" prop="facilityTypeName">
              <el-input v-model="form.facilityTypeName" placeholder="设施名称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设施类别">
              <el-select v-model="form.facilityTypeCategory" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in FACILITY_TYPE_CATEGORY_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="楼栋名称">
              <el-input v-model="form.buildingName" placeholder="楼栋名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="楼层">
              <el-input v-model="form.floor" placeholder="楼层" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="面积(㎡)">
              <el-input-number v-model="form.area" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="容纳人数">
              <el-input-number v-model="form.capacity" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开放时间">
              <el-input v-model="form.openTime" placeholder="如 08:00-22:00" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="park" register-asset asset-ref-type1="park" :asset-ref-code="props.parkCode" asset-ref-type2="facility_type" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图片(JSON)">
              <FileUploader v-model="imagesModel" type="image" multiple module="park" register-asset asset-ref-type1="park" :asset-ref-code="props.parkCode" asset-ref-type2="facility_type" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设施描述">
              <el-input v-model="form.facilityTypeDescription" type="textarea" :rows="2" placeholder="设施描述" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 价格 新增/编辑弹窗 -->
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
            <el-form-item label="计费单位">
              <el-input v-model="priceForm.priceUnit" placeholder="如 次/月/场/小时" maxlength="50" />
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
                <el-radio :value="0">停用</el-radio>
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
.facility-tab {
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
