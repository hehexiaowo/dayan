<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageTemplates,
  getTemplate,
  createTemplate,
  updateTemplate,
  deleteTemplate
} from '@/api/equity'
import type { EquityTemplate, EquityTemplateQuery } from '@/types/equity'
import {
  EquityType,
  EquityLevel,
  EQUITY_TYPE_OPTIONS,
  EQUITY_LEVEL_OPTIONS
} from '@/types/equity'
import { COMMON_STATUS_OPTIONS } from '@/types/common'
import { formatMoney } from '@/utils/format'
import FileUploader from '@/components/FileUploader/index.vue'

/**
 * 权益模板管理页（标准 CRUD）。
 *
 * - 搜索 + 表格 + 分页 + 新增/编辑弹窗；
 * - templateCode 服务端生成，新增表单不含；
 * - 状态：1启用 / 0禁用。
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
} = useCrud<EquityTemplate, EquityTemplateQuery>(
  { page: pageTemplates },
  {
    initialQuery: {
      templateCode: '',
      templateName: '',
      equityType: undefined,
      equityLevel: undefined,
      status: undefined
    }
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<EquityTemplate>({
  templateCode: undefined,
  templateName: '',
  equityType: EquityType.INSTITUTION_STAY,
  equityLevel: EquityLevel.NORMAL,
  equityValue: undefined,
  costPrice: undefined,
  contentDescription: '',
  serviceItems: '',
  applicableParks: '',
  applicableCities: '',
  validDays: undefined,
  shelfLifeDays: undefined,
  isTransferable: 0,
  isStackable: 0,
  maxUseCount: undefined,
  coverImage: '',
  cardDesignUrl: '',
  terms: '',
  sortOrder: 0,
  status: 1,
  remark: ''
})

const rules: FormRules<EquityTemplate> = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  equityType: [{ required: true, message: '请选择权益类型', trigger: 'change' }],
  equityLevel: [{ required: true, message: '请选择权益等级', trigger: 'change' }]
}

function resetForm() {
  Object.assign(form, {
    templateCode: undefined,
    templateName: '',
    equityType: EquityType.INSTITUTION_STAY,
    equityLevel: EquityLevel.NORMAL,
    equityValue: undefined,
    costPrice: undefined,
    contentDescription: '',
    serviceItems: '',
    applicableParks: '',
    applicableCities: '',
    validDays: undefined,
    shelfLifeDays: undefined,
    isTransferable: 0,
    isStackable: 0,
    maxUseCount: undefined,
    coverImage: '',
    cardDesignUrl: '',
    terms: '',
    sortOrder: 0,
    status: 1,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: EquityTemplate) {
  if (!row.templateCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getTemplate(row.templateCode)
    Object.assign(form, {
      templateCode: detail.templateCode,
      templateName: detail.templateName ?? '',
      equityType: detail.equityType ?? EquityType.INSTITUTION_STAY,
      equityLevel: detail.equityLevel ?? EquityLevel.NORMAL,
      equityValue: detail.equityValue,
      costPrice: detail.costPrice,
      contentDescription: detail.contentDescription ?? '',
      serviceItems: detail.serviceItems ?? '',
      applicableParks: detail.applicableParks ?? '',
      applicableCities: detail.applicableCities ?? '',
      validDays: detail.validDays,
      shelfLifeDays: detail.shelfLifeDays,
      isTransferable: detail.isTransferable ?? 0,
      isStackable: detail.isStackable ?? 0,
      maxUseCount: detail.maxUseCount,
      coverImage: detail.coverImage ?? '',
      cardDesignUrl: detail.cardDesignUrl ?? '',
      terms: detail.terms ?? '',
      sortOrder: detail.sortOrder ?? 0,
      status: detail.status ?? 1,
      remark: detail.remark ?? ''
    })
  } catch {
    Object.assign(form, {
      templateCode: row.templateCode,
      templateName: row.templateName ?? '',
      equityType: row.equityType ?? EquityType.INSTITUTION_STAY,
      equityLevel: row.equityLevel ?? EquityLevel.NORMAL,
      equityValue: row.equityValue,
      costPrice: row.costPrice,
      contentDescription: row.contentDescription ?? '',
      serviceItems: row.serviceItems ?? '',
      applicableParks: row.applicableParks ?? '',
      applicableCities: row.applicableCities ?? '',
      validDays: row.validDays,
      shelfLifeDays: row.shelfLifeDays,
      isTransferable: row.isTransferable ?? 0,
      isStackable: row.isStackable ?? 0,
      maxUseCount: row.maxUseCount,
      coverImage: row.coverImage ?? '',
      cardDesignUrl: row.cardDesignUrl ?? '',
      terms: row.terms ?? '',
      sortOrder: row.sortOrder ?? 0,
      status: row.status ?? 1,
      remark: row.remark ?? ''
    })
  }
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
    if (dialogType.value === 'create') {
      await createTemplate(form)
      ElMessage.success('新增成功')
    } else if (form.templateCode) {
      await updateTemplate(form.templateCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  query.templateCode = ''
  query.templateName = ''
  query.equityType = undefined
  query.equityLevel = undefined
  query.status = undefined
  handleSearch()
}

async function handleDeleteRow(row: EquityTemplate) {
  if (!row.templateCode) return
  await ElMessageBox.confirm(`确定删除模板「${row.templateName}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteTemplate(row.templateCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 辅助渲染 ----------
function equityTypeLabel(t?: number): string {
  const found = EQUITY_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function equityLevelLabel(l?: number): string {
  const found = EQUITY_LEVEL_OPTIONS.find((o) => o.value === l)
  return found ? found.label : l != null ? String(l) : '--'
}

/** 状态：1启用 success / 0禁用 info */
function statusTagType(status?: number): 'success' | 'info' {
  return status === 1 ? 'success' : 'info'
}

function statusLabel(status?: number): string {
  return status === 1 ? '启用' : status === 0 ? '禁用' : '--'
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="模板编码">
          <el-input v-model="query.templateCode" placeholder="模板编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="模板名称">
          <el-input v-model="query.templateName" placeholder="模板名称关键字" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="权益类型">
          <el-select v-model="query.equityType" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="o in EQUITY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="权益等级">
          <el-select v-model="query.equityLevel" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in EQUITY_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in COMMON_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>权益模板列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增模板</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="templateCode">
        <el-table-column prop="templateCode" label="模板编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="templateName" label="模板名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="equityType" label="权益类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ equityTypeLabel(row.equityType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="equityLevel" label="权益等级" width="100" align="center">
          <template #default="{ row }">
            {{ equityLevelLabel(row.equityLevel) }}
          </template>
        </el-table-column>
        <el-table-column prop="equityValue" label="权益面值" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.equityValue) }}</template>
        </el-table-column>
        <el-table-column prop="costPrice" label="成本价" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.costPrice) }}</template>
        </el-table-column>
        <el-table-column prop="validDays" label="有效天数" width="100" align="center" />
        <el-table-column prop="isTransferable" label="可转让" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isTransferable === 1 ? 'success' : 'info'" size="small">
              {{ row.isTransferable === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isStackable" label="可叠加" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isStackable === 1 ? 'success' : 'info'" size="small">
              {{ row.isStackable === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="160" fixed="right">
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
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '新增权益模板' : '编辑权益模板'"
      width="880px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="模板名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="权益类型" prop="equityType">
              <el-select v-model="form.equityType" placeholder="权益类型" style="width: 100%">
                <el-option v-for="o in EQUITY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="权益等级" prop="equityLevel">
              <el-select v-model="form.equityLevel" placeholder="权益等级" style="width: 100%">
                <el-option v-for="o in EQUITY_LEVEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="权益面值">
              <el-input-number v-model="form.equityValue" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成本价">
              <el-input-number v-model="form.costPrice" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大使用次数">
              <el-input-number v-model="form.maxUseCount" :min="0" :max="999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="激活后有效天数">
              <el-input-number v-model="form.validDays" :min="0" :max="99999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存有效期天数">
              <el-input-number v-model="form.shelfLifeDays" :min="0" :max="99999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="o in COMMON_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否可转让">
              <el-switch v-model="form.isTransferable" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否可叠加">
              <el-switch v-model="form.isStackable" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="权益内容描述">
              <el-input v-model="form.contentDescription" type="textarea" :rows="2" placeholder="权益内容描述" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="包含服务项目">
              <el-input v-model="form.serviceItems" type="textarea" :rows="2" placeholder="包含服务项目（可多条以逗号分隔）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用机构范围">
              <el-input v-model="form.applicableParks" placeholder="适用机构编码范围" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用城市范围">
              <el-input v-model="form.applicableCities" placeholder="适用城市编码范围" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图">
              <FileUploader v-model="form.coverImage" type="image" module="equity" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="卡面设计图">
              <FileUploader v-model="form.cardDesignUrl" type="image" module="equity" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="使用说明/条款">
              <el-input v-model="form.terms" type="textarea" :rows="3" placeholder="使用说明/条款" />
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

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
