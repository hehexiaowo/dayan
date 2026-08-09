<script setup lang="ts">
/**
 * 商品详情页 - 旅居配置 tab（goodsType=4 时显示）。
 *
 * 分页模式：useCrud（主键 id 自增 number，传 idKey:'id'，fixedParams:{goodsCode}）。
 *
 * 关键约束：
 * - 主键是自增 id（number），update/delete 都用 id。
 * - skuCode 服务端生成 GJ 前缀，前端不传。
 * - create 必填：goodsCode（fixedParams 带入）、parkCode、roomTypeCode。
 * - 后端校验：minDays ≤ maxDays（maxDays 可空=不限）、effectiveDate ≤ expireDate（expireDate 可空=不限）。
 *   update 时合并已有值再校验——前端表单做轻量预校验提示（非必须，后端兜底）。
 * - effectiveDate/expireDate 是 LocalDate（传 YYYY-MM-DD），用 el-date-picker。
 * - priceUnit 旅居默认"元/月"（与主表的"元"不同）。
 * - salesCount create 硬编码 0，UpdateDTO 无此字段，表单不展示。
 * - roomTypeCode/careTypeCode/foodTypeCode 无跨模块选择器文档，暂用 el-input 兜底。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageSojourns,
  createSojourn,
  updateSojourn,
  deleteSojourn
} from '@/api/goods-sku'
import {
  SKU_STATUS_OPTIONS,
  skuStatusLabel,
  skuStatusTagType
} from '@/types/goods'
import type { GoodsSojourn, GoodsSojournQuery } from '@/types/goods'
import { formatDateTime, formatDate } from '@/utils/format'

const props = defineProps<{
  /** 商品编码（从详情页 prop 带入，create 表单隐藏） */
  goodsCode: string
}>()

// ---------- 旅居配置列表（useCrud，主键 id 自增 number） ----------
const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<GoodsSojourn, GoodsSojournQuery, number>(
    {
      page: pageSojourns,
      create: createSojourn,
      update: (id, data) => updateSojourn(id, data),
      remove: deleteSojourn
    },
    {
      initialQuery: { skuName: '', parkCode: '', roomTypeCode: '', status: undefined },
      idKey: 'id',
      fixedParams: { goodsCode: props.goodsCode }
    }
  )

loadPage()

// ---------- 新增/编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 旅居价格单位默认值（与主表的"元"不同） */
const DEFAULT_PRICE_UNIT_SOJOURN = '元/月'

const form = reactive<GoodsSojourn>({
  id: undefined,
  goodsCode: '',
  skuCode: undefined,
  skuName: '',
  parkCode: '',
  roomTypeCode: '',
  roomTypeName: '',
  careTypeCode: '',
  foodTypeCode: '',
  skuPrice: undefined,
  priceUnit: DEFAULT_PRICE_UNIT_SOJOURN,
  minDays: undefined,
  maxDays: undefined,
  stock: undefined,
  effectiveDate: '',
  expireDate: '',
  sortOrder: 0,
  status: 1
})

const rules: FormRules<GoodsSojourn> = {
  parkCode: [{ required: true, message: '请输入园区编码', trigger: 'blur' }],
  roomTypeCode: [{ required: true, message: '请输入房型编码', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    goodsCode: '',
    skuCode: undefined,
    skuName: '',
    parkCode: '',
    roomTypeCode: '',
    roomTypeName: '',
    careTypeCode: '',
    foodTypeCode: '',
    skuPrice: undefined,
    priceUnit: DEFAULT_PRICE_UNIT_SOJOURN,
    minDays: undefined,
    maxDays: undefined,
    stock: undefined,
    effectiveDate: '',
    expireDate: '',
    sortOrder: 0,
    status: 1
  })
}

function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  form.goodsCode = props.goodsCode
  dialogVisible.value = true
}

function openEdit(row: GoodsSojourn) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

/** 轻量预校验：minDays ≤ maxDays、effectiveDate ≤ expireDate（后端兜底，仅提示） */
function preValidate(): string | null {
  if (
    form.minDays != null &&
    form.maxDays != null &&
    form.minDays > form.maxDays
  ) {
    return '最少入住天数不能大于最多入住天数'
  }
  if (
    form.effectiveDate &&
    form.expireDate &&
    form.effectiveDate > form.expireDate
  ) {
    return '生效日期不能晚于失效日期'
  }
  return null
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const err = preValidate()
  if (err) {
    ElMessage.warning(err)
    return
  }
  submitLoading.value = true
  try {
    form.goodsCode = props.goodsCode
    if (dialogMode.value === 'create') {
      await createSojourn(form)
      ElMessage.success('新增成功')
    } else if (form.id) {
      await updateSojourn(form.id, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

async function handleDeleteRow(row: GoodsSojourn) {
  if (!row.id) return
  await ElMessageBox.confirm('确定删除该旅居配置记录？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteSojourn(row.id)
  ElMessage.success('删除成功')
  loadPage()
}
</script>

<template>
  <div class="sku-tab">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="规格名称">
        <el-input
          v-model="query.skuName"
          placeholder="规格名称"
          clearable
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="园区编码">
        <el-input
          v-model="query.parkCode"
          placeholder="园区编码"
          clearable
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="房型编码">
        <el-input
          v-model="query.roomTypeCode"
          placeholder="房型编码"
          clearable
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="o in SKU_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Plus'" @click="openCreate">新增旅居配置</el-button>
      </el-form-item>
    </el-form>

    <!-- 主表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
      <el-table-column prop="skuCode" label="规格编码" min-width="140" show-overflow-tooltip />
      <el-table-column prop="skuName" label="规格名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="parkCode" label="园区编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="roomTypeCode" label="房型编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="roomTypeName" label="房型名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="skuPrice" label="SKU 价格" width="110" align="right" />
      <el-table-column prop="priceUnit" label="价格单位" width="100" align="center" />
      <el-table-column prop="minDays" label="最少天" width="90" align="center" />
      <el-table-column prop="maxDays" label="最多天" width="90" align="center">
        <template #default="{ row }">{{ row.maxDays != null ? row.maxDays : '不限' }}</template>
      </el-table-column>
      <el-table-column prop="effectiveDate" label="生效日期" width="110" align="center">
        <template #default="{ row }">{{ row.effectiveDate ? formatDate(row.effectiveDate) : '--' }}</template>
      </el-table-column>
      <el-table-column prop="expireDate" label="失效日期" width="110" align="center">
        <template #default="{ row }">{{ row.expireDate ? formatDate(row.expireDate) : '不限' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="skuStatusTagType(row.status)" size="small">{{ skuStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增旅居配置' : '编辑旅居配置'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格名称">
              <el-input v-model="form.skuName" placeholder="规格名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="园区编码" prop="parkCode">
              <el-input v-model="form.parkCode" placeholder="园区编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房型编码" prop="roomTypeCode">
              <!-- TODO: roomTypeCode 暂无跨模块选择器文档，先用 input 兜底 -->
              <el-input v-model="form.roomTypeCode" placeholder="房型编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房型名称">
              <el-input v-model="form.roomTypeName" placeholder="房型名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <!-- TODO: careTypeCode 暂无跨模块选择器文档，先用 input 兜底 -->
            <el-form-item label="照护类型编码">
              <el-input v-model="form.careTypeCode" placeholder="照护类型编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <!-- TODO: foodTypeCode 暂无跨模块选择器文档，先用 input 兜底 -->
            <el-form-item label="餐饮类型编码">
              <el-input v-model="form.foodTypeCode" placeholder="餐饮类型编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="SKU 价格">
              <el-input-number v-model="form.skuPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="价格单位">
              <el-input v-model="form.priceUnit" placeholder="元/月 等" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存">
              <el-input-number v-model="form.stock" :min="0" :max="9999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最少天数">
              <el-input-number v-model="form.minDays" :min="0" :max="9999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最多天数">
              <!-- maxDays 可空=不限 -->
              <el-input-number v-model="form.maxDays" :min="0" :max="9999999" controls-position="right" style="width: 100%" placeholder="空=不限" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <!-- effectiveDate 是 LocalDate，传 YYYY-MM-DD -->
            <el-form-item label="生效日期">
              <el-date-picker
                v-model="form.effectiveDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <!-- expireDate 是 LocalDate，可空=不限 -->
            <el-form-item label="失效日期">
              <el-date-picker
                v-model="form.expireDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="空=不限"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">在售</el-radio>
                <el-radio :value="0">停售</el-radio>
              </el-radio-group>
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

<style scoped lang="scss">
.sku-tab {
  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
