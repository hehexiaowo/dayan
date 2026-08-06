<script setup lang="ts">
/**
 * 机构详情页 - 房型 tab。
 *
 * 架构（type + price 双层，后续照护/餐饮 tab 同模式）：
 * 1. 搜索条（roomTypeName 模糊 + status）+ 新增房型按钮
 * 2. 主表格房型列表，useCrud（idKey:'id', fixedParams:{parkCode}）
 * 3. 展开行：展开时调 listRoomPrices(parkCode, roomTypeCode) 加载价格，
 *    内联小表格 + 新增/编辑/删除（独立 ref + Map 缓存按需加载）
 * 4. 房型新增/编辑 el-dialog，必填 roomTypeCode(≤50)/roomTypeName(≤200)，parkCode 隐藏
 * 5. 价格新增/编辑 el-dialog，业务必填 salePrice/effectiveDate，
 *    roomTypeCode+parkCode 从展开行上下文带入不显示
 *
 * 红线遵守：
 * - 主键 Long id，useCrud 传 idKey:'id'
 * - roomTypeCode 用户填写非系统生成；update 时不可改（编辑弹窗内 disabled）
 * - price 展开行用 /list 端点（parkCode+roomTypeCode 两参）
 * - has*(10 设施) / isCurrent / isPromotion 提交 0/1 非 true/false
 * - JSON 字符串字段（facilities/images/includesItems）用 textarea 原文编辑
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageRoomTypes,
  createRoomType,
  updateRoomType,
  deleteRoomType,
  listRoomPrices,
  createRoomPrice,
  updateRoomPrice,
  deleteRoomPrice
} from '@/api/park-room'
import {
  ROOM_PRICE_TYPE_OPTIONS,
  STAY_TYPE_OPTIONS,
  roomPriceTypeLabel,
  roomCategoryLabel,
  stayTypeLabel
} from '@/types/park'
import type { ParkRoomType, ParkRoomTypeQuery, ParkRoomPrice } from '@/types/park'

const props = defineProps<{
  /** 机构编码（从详情页 prop 带入，create 表单隐藏） */
  parkCode: string
}>()

// ---------- 房型 type 列表（useCrud，主键 id） ----------
// fixedParams:{parkCode} 在 options 创建时绑定 prop 当前值（路由参数在组件生命周期内稳定）
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  ParkRoomType,
  ParkRoomTypeQuery,
  number
>(
  {
    page: pageRoomTypes,
    create: createRoomType,
    update: (id, data) => updateRoomType(id, data),
    remove: deleteRoomType
  },
  {
    initialQuery: { roomTypeName: '', status: undefined },
    idKey: 'id',
    fixedParams: { parkCode: props.parkCode }
  }
)

loadPage()

// ---------- 房型 新增/编辑弹窗 ----------
const typeDialogVisible = ref(false)
const typeDialogMode = ref<'create' | 'edit'>('create')
const typeSubmitLoading = ref(false)
const typeFormRef = ref<FormInstance>()

const typeForm = reactive<ParkRoomType>({
  id: undefined,
  parkCode: '',
  roomTypeCode: '',
  roomTypeName: '',
  stayType: undefined,
  buildingName: '',
  floor: '',
  roomCategory: undefined,
  area: undefined,
  orientation: '',
  bedCount: undefined,
  totalRooms: undefined,
  availableRooms: undefined,
  hasBathroom: 0,
  hasKitchen: 0,
  hasBalcony: 0,
  hasTv: 0,
  hasAircon: 0,
  hasFridge: 0,
  hasWasher: 0,
  hasWifi: 0,
  hasEmergency: 0,
  hasMonitor: 0,
  facilities: '',
  description: '',
  coverImage: '',
  images: '',
  sortOrder: 0,
  status: 1,
  designDescription: '',
  designImage: '',
  additionalImages: ''
})

const typeRules: FormRules<ParkRoomType> = {
  roomTypeCode: [
    { required: true, message: '请输入房型编码', trigger: 'blur' },
    { max: 50, message: '不超过 50 个字符', trigger: 'blur' }
  ],
  roomTypeName: [
    { required: true, message: '请输入房型名称', trigger: 'blur' },
    { max: 200, message: '不超过 200 个字符', trigger: 'blur' }
  ]
}

function resetTypeForm() {
  Object.assign(typeForm, {
    id: undefined,
    parkCode: '',
    roomTypeCode: '',
    roomTypeName: '',
    stayType: undefined,
    buildingName: '',
    floor: '',
    roomCategory: undefined,
    area: undefined,
    orientation: '',
    bedCount: undefined,
    totalRooms: undefined,
    availableRooms: undefined,
    hasBathroom: 0,
    hasKitchen: 0,
    hasBalcony: 0,
    hasTv: 0,
    hasAircon: 0,
    hasFridge: 0,
    hasWasher: 0,
    hasWifi: 0,
    hasEmergency: 0,
    hasMonitor: 0,
    facilities: '',
    description: '',
    coverImage: '',
    images: '',
    sortOrder: 0,
    status: 1,
    designDescription: '',
    designImage: '',
    additionalImages: ''
  })
}

function openCreateType(parkCode: string) {
  typeDialogMode.value = 'create'
  resetTypeForm()
  typeForm.parkCode = parkCode
  typeDialogVisible.value = true
}

function openEditType(row: ParkRoomType) {
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
      await createRoomType(typeForm)
      ElMessage.success('新增成功')
    } else if (typeForm.id) {
      await updateRoomType(typeForm.id, typeForm)
      ElMessage.success('修改成功')
    }
    typeDialogVisible.value = false
    loadPage()
  } finally {
    typeSubmitLoading.value = false
  }
}

async function handleDeleteType(row: ParkRoomType) {
  if (!row.id) return
  await ElMessageBox.confirm(
    '确定删除该房型吗？删除前请先删除该房型下所有价格记录（不级联删除）。',
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await deleteRoomType(row.id)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 展开行 price 管理 ----------
/** 各展开行的 price 列表缓存，key=roomTypeCode */
const priceMap = ref<Map<string, ParkRoomPrice[]>>(new Map())
/** price 加载状态 */
const priceLoadingMap = ref<Map<string, boolean>>(new Map())

async function loadPrices(parkCode: string, row: ParkRoomType) {
  if (!row.roomTypeCode) return
  priceLoadingMap.value.set(row.roomTypeCode, true)
  try {
    const list = await listRoomPrices(parkCode, row.roomTypeCode)
    priceMap.value.set(row.roomTypeCode, list)
  } catch {
    priceMap.value.set(row.roomTypeCode, [])
  } finally {
    priceLoadingMap.value.set(row.roomTypeCode, false)
  }
}

/** 展开行 toggle：展开时按需加载 price */
function handleExpandChange(row: ParkRoomType, expanded: ParkRoomType[], parkCode: string) {
  const isExpanded = expanded.some((r) => r.id === row.id)
  if (isExpanded && row.roomTypeCode) {
    loadPrices(parkCode, row)
  }
}

// ---------- price 新增/编辑弹窗 ----------
const priceDialogVisible = ref(false)
const priceDialogMode = ref<'create' | 'edit'>('create')
const priceSubmitLoading = ref(false)
const priceFormRef = ref<FormInstance>()
/** 当前 price 所属的房型上下文（roomTypeCode + parkCode） */
const priceContext = reactive({ parkCode: '', roomTypeCode: '' })

const priceForm = reactive<ParkRoomPrice>({
  id: undefined,
  parkCode: '',
  roomTypeCode: '',
  priceType: undefined,
  originalPrice: undefined,
  salePrice: undefined,
  discountRate: undefined,
  priceDescription: '',
  includesItems: '',
  effectiveDate: '',
  expireDate: '',
  isCurrent: 0,
  isPromotion: 0,
  promotionDescription: '',
  priceChangeReason: '',
  sortOrder: 0,
  status: 1
})

const priceRules: FormRules<ParkRoomPrice> = {
  salePrice: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }]
}

function resetPriceForm() {
  Object.assign(priceForm, {
    id: undefined,
    parkCode: '',
    roomTypeCode: '',
    priceType: undefined,
    originalPrice: undefined,
    salePrice: undefined,
    discountRate: undefined,
    priceDescription: '',
    includesItems: '',
    effectiveDate: '',
    expireDate: '',
    isCurrent: 0,
    isPromotion: 0,
    promotionDescription: '',
    priceChangeReason: '',
    sortOrder: 0,
    status: 1
  })
}

function openCreatePrice(parkCode: string, roomTypeCode: string) {
  priceDialogMode.value = 'create'
  resetPriceForm()
  priceContext.parkCode = parkCode
  priceContext.roomTypeCode = roomTypeCode
  priceForm.parkCode = parkCode
  priceForm.roomTypeCode = roomTypeCode
  priceDialogVisible.value = true
}

function openEditPrice(row: ParkRoomPrice, parkCode: string, roomTypeCode: string) {
  priceDialogMode.value = 'edit'
  resetPriceForm()
  Object.assign(priceForm, row)
  priceContext.parkCode = parkCode
  priceContext.roomTypeCode = roomTypeCode
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
    // 确保 parkCode + roomTypeCode 从上下文带入（编辑时也不可改外键）
    priceForm.parkCode = priceContext.parkCode
    priceForm.roomTypeCode = priceContext.roomTypeCode
    if (priceDialogMode.value === 'create') {
      await createRoomPrice(priceForm)
      ElMessage.success('新增成功')
    } else if (priceForm.id) {
      await updateRoomPrice(priceForm.id, priceForm)
      ElMessage.success('修改成功')
    }
    priceDialogVisible.value = false
    // 刷新该展开行 price（用上下文 roomTypeCode）
    loadPrices(priceContext.parkCode, { roomTypeCode: priceContext.roomTypeCode } as ParkRoomType)
  } finally {
    priceSubmitLoading.value = false
  }
}

async function handleDeletePrice(row: ParkRoomPrice, parkCode: string, roomTypeCode: string) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该价格记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteRoomPrice(row.id)
  ElMessage.success('删除成功')
  loadPrices(parkCode, { roomTypeCode } as ParkRoomType)
}

// ---------- 辅助渲染 ----------
/** 设施开关 has* 渲染为 ✓ / — */
function checkLabel(v?: number): string {
  return v === 1 ? '✓' : '—'
}

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

// 暴露给父组件（潜在刷新）
defineExpose({ loadPage })
</script>

<template>
  <div class="room-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="房型名称">
        <el-input
          v-model="query.roomTypeName"
          placeholder="房型名称"
          clearable
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停售" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreateType(parkCode)">新增房型</el-button>
      </el-form-item>
    </el-form>

    <!-- 主表格（含展开行 price） -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      row-key="id"
      @expand-change="(row: ParkRoomType, expanded: ParkRoomType[]) => handleExpandChange(row, expanded, parkCode)"
    >
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="price-block" v-loading="priceLoadingMap.get(row.roomTypeCode)">
            <div class="price-toolbar">
              <span class="price-title">价格记录（{{ row.roomTypeName }}）</span>
              <el-button
                type="primary"
                size="small"
                :icon="'Plus'"
                @click="openCreatePrice(parkCode, row.roomTypeCode)"
              >
                新增价格
              </el-button>
            </div>
            <el-table :data="priceMap.get(row.roomTypeCode) || []" border size="small">
              <el-table-column prop="priceType" label="价格类型" width="100" align="center">
                <template #default="{ row: p }">
                  {{ roomPriceTypeLabel(p.priceType) }}
                </template>
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
                  <el-button link type="primary" size="small" @click="openEditPrice(p, parkCode, row.roomTypeCode)">
                    编辑
                  </el-button>
                  <el-button link type="danger" size="small" @click="handleDeletePrice(p, parkCode, row.roomTypeCode)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="roomTypeCode" label="房型编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="roomTypeName" label="房型名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="roomCategory" label="分类" width="90" align="center">
        <template #default="{ row }">{{ roomCategoryLabel(row.roomCategory) }}</template>
      </el-table-column>
      <el-table-column prop="stayType" label="居住类型" width="100" align="center">
        <template #default="{ row }">{{ stayTypeLabel(row.stayType) }}</template>
      </el-table-column>
      <el-table-column prop="area" label="面积(㎡)" width="100" align="right">
        <template #default="{ row }">{{ row.area != null ? row.area : '--' }}</template>
      </el-table-column>
      <el-table-column prop="bedCount" label="床位" width="80" align="center" />
      <el-table-column prop="totalRooms" label="总房间" width="90" align="center" />
      <el-table-column prop="availableRooms" label="可用" width="80" align="center" />
      <el-table-column label="设施配置" width="240" align="center">
        <template #default="{ row }">
          <span class="fac-cell">卫{{ checkLabel(row.hasBathroom) }}</span>
          <span class="fac-cell">厨{{ checkLabel(row.hasKitchen) }}</span>
          <span class="fac-cell">台{{ checkLabel(row.hasBalcony) }}</span>
          <span class="fac-cell">视{{ checkLabel(row.hasTv) }}</span>
          <span class="fac-cell">调{{ checkLabel(row.hasAircon) }}</span>
          <span class="fac-cell">冰{{ checkLabel(row.hasFridge) }}</span>
          <span class="fac-cell">洗{{ checkLabel(row.hasWasher) }}</span>
          <span class="fac-cell">网{{ checkLabel(row.hasWifi) }}</span>
          <span class="fac-cell">呼{{ checkLabel(row.hasEmergency) }}</span>
          <span class="fac-cell">监{{ checkLabel(row.hasMonitor) }}</span>
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

    <!-- 房型 新增/编辑弹窗 -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="typeDialogMode === 'create' ? '新增房型' : '编辑房型'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="房型编码" prop="roomTypeCode">
              <el-input
                v-model="typeForm.roomTypeCode"
                placeholder="业务编码（同机构下唯一）"
                maxlength="50"
                :disabled="typeDialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房型名称" prop="roomTypeName">
              <el-input v-model="typeForm.roomTypeName" placeholder="房型名称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="居住类型">
              <el-input-number
                v-model="typeForm.stayType"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="居住类型">
              <el-select v-model="typeForm.stayType" placeholder="请选择" style="width: 100%">
                <el-option v-for="o in STAY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="楼栋名称">
              <el-input v-model="typeForm.buildingName" placeholder="楼栋名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="楼层">
              <el-input v-model="typeForm.floor" placeholder="楼层" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="面积(㎡)">
              <el-input-number v-model="typeForm.area" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="朝向">
              <el-input v-model="typeForm.orientation" placeholder="朝向" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="床位数">
              <el-input-number v-model="typeForm.bedCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总房间数">
              <el-input-number v-model="typeForm.totalRooms" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="可用房间">
              <el-input-number v-model="typeForm.availableRooms" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="typeForm.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设施配置">
              <div class="fac-switches">
                <el-checkbox v-model="typeForm.hasBathroom" :true-value="1" :false-value="0">独立卫生间</el-checkbox>
                <el-checkbox v-model="typeForm.hasKitchen" :true-value="1" :false-value="0">厨房</el-checkbox>
                <el-checkbox v-model="typeForm.hasBalcony" :true-value="1" :false-value="0">阳台</el-checkbox>
                <el-checkbox v-model="typeForm.hasTv" :true-value="1" :false-value="0">电视</el-checkbox>
                <el-checkbox v-model="typeForm.hasAircon" :true-value="1" :false-value="0">空调</el-checkbox>
                <el-checkbox v-model="typeForm.hasFridge" :true-value="1" :false-value="0">冰箱</el-checkbox>
                <el-checkbox v-model="typeForm.hasWasher" :true-value="1" :false-value="0">洗衣机</el-checkbox>
                <el-checkbox v-model="typeForm.hasWifi" :true-value="1" :false-value="0">WiFi</el-checkbox>
                <el-checkbox v-model="typeForm.hasEmergency" :true-value="1" :false-value="0">紧急呼叫</el-checkbox>
                <el-checkbox v-model="typeForm.hasMonitor" :true-value="1" :false-value="0">监控</el-checkbox>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设施(JSON)">
              <el-input v-model="typeForm.facilities" type="textarea" :rows="2" placeholder="设施配置 JSON 原文" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图URL">
              <el-input v-model="typeForm.coverImage" placeholder="封面图 URL" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设计图URL">
              <el-input v-model="typeForm.designImage" placeholder="设计图 URL" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图片列表(JSON)">
              <el-input v-model="typeForm.images" type="textarea" :rows="2" placeholder="图片列表 JSON 原文" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="附加图片(JSON)">
              <el-input v-model="typeForm.additionalImages" type="textarea" :rows="2" placeholder="附加图片 JSON 原文" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="typeForm.description" type="textarea" :rows="2" placeholder="房型描述" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设计说明">
              <el-input v-model="typeForm.designDescription" type="textarea" :rows="2" placeholder="设计说明" />
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
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitLoading" @click="handleTypeSubmit">确定</el-button>
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
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="价格类型">
              <el-select v-model="priceForm.priceType" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="o in ROOM_PRICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
              />
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
          <el-col :span="24">
            <el-form-item label="包含项目(JSON)">
              <el-input v-model="priceForm.includesItems" type="textarea" :rows="2" placeholder="包含项目 JSON 原文" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="变更原因">
              <el-input v-model="priceForm.priceChangeReason" type="textarea" :rows="2" placeholder="价格变更原因" />
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
.room-tab {
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
  .fac-cell {
    margin-right: 6px;
    font-size: 13px;
  }
  .fac-switches {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 16px;
  }
}
</style>
