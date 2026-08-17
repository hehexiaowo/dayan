<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageServiceItems,
  getServiceItem,
  createServiceItem,
  updateServiceItem,
  deleteServiceItem
} from '@/api/service-item'
import type { ServiceItem, ServiceItemQuery } from '@/types/service-item'
import {
  ItemCategory,
  ITEM_CATEGORY_OPTIONS,
  ITEM_SUBTYPE_OPTIONS,
  networkScopeSummary
} from '@/types/service-item'
import { COMMON_STATUS_OPTIONS } from '@/types/common'
import { formatMoney } from '@/utils/format'
import NetworkScopeSelector from '@/components/NetworkScopeSelector.vue'

/**
 * 服务项目管理页（标准 CRUD）。
 *
 * - 搜索 + 表格 + 分页 + 新增/编辑弹窗；
 * - itemCode 服务端生成，新增表单不含；
 * - 项目大类切换时动态显示字段（安排权益显示 subtype，费用权益显示 covered_items）；
 * - 服务网络：通用选择器（全部/自选机构，可精确到房型——随心住类需要）。
 */

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<ServiceItem, ServiceItemQuery>(
  { page: pageServiceItems },
  {
    initialQuery: {
      itemCode: '',
      itemName: '',
      itemCategory: undefined,
      status: undefined
    },
    idKey: 'itemCode'
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): ServiceItem => ({
  itemName: '',
  itemCategory: ItemCategory.ARRANGEMENT,
  itemSubtype: undefined,
  itemValue: undefined,
  costBearing: 0,
  networkScope: null,
  coveredItems: '',
  validDays: 365,
  maxUseCount: 1,
  description: '',
  sortOrder: 0,
  status: 1
})

const form = reactive<ServiceItem>(defaultForm())

const rules: FormRules<ServiceItem> = {
  itemName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  itemCategory: [{ required: true, message: '请选择项目大类', trigger: 'change' }]
}

function onCategoryChange(val: number) {
  // 安排权益自动设客户自负，费用权益自动设系统承担
  form.costBearing = val === ItemCategory.ARRANGEMENT ? 0 : 1
  if (val === ItemCategory.COST) {
    form.itemSubtype = undefined
  }
}

function categoryLabel(cat?: number) {
  return ITEM_CATEGORY_OPTIONS.find((o) => o.value === cat)?.label ?? '—'
}

function categoryTagType(cat?: number) {
  return ITEM_CATEGORY_OPTIONS.find((o) => o.value === cat)?.tagType ?? 'info'
}

function subtypeLabel(sub?: number) {
  if (!sub) return '—'
  return ITEM_SUBTYPE_OPTIONS.find((o) => o.value === sub)?.label ?? '—'
}

function resetForm() {
  Object.assign(form, defaultForm())
  formRef.value?.resetFields()
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: ServiceItem) {
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getServiceItem(row.itemCode!)
    Object.assign(form, detail)
  } catch {
    Object.assign(form, row)
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  // coveredItems（费用权益补贴明细）以 JSON 数组提交，提交前校验格式；为空放行
  if (form.coveredItems && form.coveredItems.trim()) {
    try {
      JSON.parse(form.coveredItems)
    } catch {
      ElMessage.error('JSON 格式错误，请检查补贴明细')
      return
    }
  }
  submitLoading.value = true
  try {
    if (dialogType.value === 'create') {
      await createServiceItem(form)
      ElMessage.success('创建成功')
    } else {
      await updateServiceItem(form.itemCode!, form)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  Object.assign(query, { itemCode: '', itemName: '', itemCategory: undefined, status: undefined })
  handleSearch()
}

async function handleDeleteRow(row: ServiceItem) {
  await ElMessageBox.confirm(`确认删除「${row.itemName}」？`, '提示', { type: 'warning' })
  await deleteServiceItem(row.itemCode!)
  ElMessage.success('删除成功')
  loadPage()
}

loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.itemCode"
          placeholder="项目编码"
          clearable
          style="width: 150px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.itemName"
          placeholder="项目名称"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.itemCategory" placeholder="项目大类" clearable style="width: 140px">
          <el-option
            v-for="opt in ITEM_CATEGORY_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option
            v-for="opt in COMMON_STATUS_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <div class="toolbar-actions">
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">服务项目列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新建项目</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="tableData" row-key="itemCode" border>
        <el-table-column prop="itemCode" label="项目编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="itemName" label="项目名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="大类" width="100">
          <template #default="{ row }">
            <el-tag :type="categoryTagType(row.itemCategory)">{{ categoryLabel(row.itemCategory) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="子类" width="100">
          <template #default="{ row }">{{ subtypeLabel(row.itemSubtype) }}</template>
        </el-table-column>
        <el-table-column label="服务网络" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="networkScopeSummary(row.networkScope) === '全部机构'" size="small" type="info">全部机构</el-tag>
            <el-tag v-else size="small" type="success">{{ networkScopeSummary(row.networkScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="面值/单价" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.itemValue) }}</template>
        </el-table-column>
        <el-table-column label="承担方" width="90">
          <template #default="{ row }">{{ row.costBearing === 1 ? '系统' : '客户' }}</template>
        </el-table-column>
        <el-table-column prop="validDays" label="有效天数" width="90" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ COMMON_STATUS_OPTIONS.find((o) => o.value === row.status)?.label ?? row.status }}</el-tag>
          </template>
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
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新建服务项目' : '编辑服务项目'"
      width="800px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目名称" prop="itemName">
              <el-input v-model="form.itemName" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目大类" prop="itemCategory">
              <el-select v-model="form.itemCategory" style="width: 100%" @change="onCategoryChange">
                <el-option
                  v-for="opt in ITEM_CATEGORY_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="面值/单价">
              <el-input-number v-model="form.itemValue" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 安排权益独有字段 -->
        <el-row v-if="form.itemCategory === ItemCategory.ARRANGEMENT" :gutter="16">
          <el-col :span="12">
            <el-form-item label="子类">
              <el-select v-model="form.itemSubtype" style="width: 100%">
                <el-option
                  v-for="opt in ITEM_SUBTYPE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <div class="field-hint">服务网络按子类业态圈定（旅游短居/活力长居/照护长居）</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效天数">
              <el-input-number v-model="form.validDays" :min="0" :max="99999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 服务网络（两大类通用）：全部=业态全部在营机构；自选可精确到机构房型 -->
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="服务网络">
              <NetworkScopeSelector v-model="form.networkScope" title="配置服务网络范围" />
              <div class="field-hint">随心住类可精确到机构的房型；其他安排权益可自选机构范围</div>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 费用权益独有字段 -->
        <el-row v-if="form.itemCategory === ItemCategory.COST" :gutter="16">
          <el-col :span="24">
            <el-form-item label="补贴明细">
              <el-input
                v-model="form.coveredItems"
                type="textarea"
                :rows="3"
                placeholder='JSON数组，如 [{"room_type":"双人间","service_content":"基础护理","quantity":5}]'
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.itemCategory === ItemCategory.COST" :gutter="16">
          <el-col :span="12">
            <el-form-item label="有效天数">
              <el-input-number v-model="form.validDays" :min="0" :max="99999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最大使用次数">
              <el-input-number v-model="form.maxUseCount" :min="0" :max="999999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option
                  v-for="opt in COMMON_STATUS_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="项目说明">
              <el-input v-model="form.description" type="textarea" :rows="2" />
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
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
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
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.field-hint {
  font-size: 12px;
  color: #999;
  line-height: 1.4;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}
</style>
