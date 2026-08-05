<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageBatches,
  getBatch,
  getBatchStats,
  createBatch,
  updateBatch,
  deleteBatch
} from '@/api/equity'
import type { EquityBatch, EquityBatchQuery, EquityBatchStats } from '@/types/equity'
import { BatchStatus, BATCH_STATUS_OPTIONS } from '@/types/equity'

/**
 * 权益批次管理页（CRUD + 统计）。
 *
 * - 搜索 + 表格 + 分页 + 新增/编辑弹窗；
 * - batchCode 服务端生成，新增表单不含；
 * - 表格重点展示多种 count 字段（已生成/已分配/已出库/已激活/已使用/已过期/已作废/剩余）；
 * - 点击「统计」可拉取 /stats/{batchCode} 弹窗展示汇总；
 * - batchStatus：0草稿/1生产中/2已完成/3已作废。
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
} = useCrud<EquityBatch, EquityBatchQuery>(
  { page: pageBatches },
  {
    initialQuery: {
      batchCode: '',
      batchName: '',
      templateCode: '',
      channelCode: '',
      batchStatus: undefined
    }
  }
)

// ---------- 新增 / 编辑弹窗 ----------
const dialogVisible = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<EquityBatch>({
  batchCode: undefined,
  batchName: '',
  templateCode: '',
  channelCode: '',
  totalQuantity: undefined,
  unitCost: undefined,
  totalCost: undefined,
  produceDate: '',
  expireDate: '',
  batchStatus: BatchStatus.DRAFT,
  remark: ''
})

const rules: FormRules<EquityBatch> = {
  batchName: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入关联模板编码', trigger: 'blur' }],
  totalQuantity: [{ required: true, message: '请输入总数量', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    batchCode: undefined,
    batchName: '',
    templateCode: '',
    channelCode: '',
    totalQuantity: undefined,
    unitCost: undefined,
    totalCost: undefined,
    produceDate: '',
    expireDate: '',
    batchStatus: BatchStatus.DRAFT,
    remark: ''
  })
}

function openCreate() {
  dialogType.value = 'create'
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: EquityBatch) {
  if (!row.batchCode) return
  dialogType.value = 'edit'
  resetForm()
  try {
    const detail = await getBatch(row.batchCode)
    Object.assign(form, {
      batchCode: detail.batchCode,
      batchName: detail.batchName ?? '',
      templateCode: detail.templateCode ?? '',
      channelCode: detail.channelCode ?? '',
      totalQuantity: detail.totalQuantity,
      unitCost: detail.unitCost,
      totalCost: detail.totalCost,
      produceDate: detail.produceDate ?? '',
      expireDate: detail.expireDate ?? '',
      batchStatus: detail.batchStatus ?? BatchStatus.DRAFT,
      remark: detail.remark ?? ''
    })
  } catch {
    Object.assign(form, {
      batchCode: row.batchCode,
      batchName: row.batchName ?? '',
      templateCode: row.templateCode ?? '',
      channelCode: row.channelCode ?? '',
      totalQuantity: row.totalQuantity,
      unitCost: row.unitCost,
      totalCost: row.totalCost,
      produceDate: row.produceDate ?? '',
      expireDate: row.expireDate ?? '',
      batchStatus: row.batchStatus ?? BatchStatus.DRAFT,
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
      await createBatch(form)
      ElMessage.success('新增成功')
    } else if (form.batchCode) {
      await updateBatch(form.batchCode, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  query.batchCode = ''
  query.batchName = ''
  query.templateCode = ''
  query.channelCode = ''
  query.batchStatus = undefined
  handleSearch()
}

async function handleDeleteRow(row: EquityBatch) {
  if (!row.batchCode) return
  await ElMessageBox.confirm(`确定删除批次「${row.batchName}」吗？（仅未生产批次可删除）`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteBatch(row.batchCode)
  ElMessage.success('删除成功')
  loadPage()
}

// ---------- 统计弹窗 ----------
const statsDialogVisible = ref(false)
const statsLoading = ref(false)
const statsData = ref<EquityBatchStats>({})
const statsBatchName = ref('')

async function openStats(row: EquityBatch) {
  if (!row.batchCode) return
  statsBatchName.value = row.batchName ?? ''
  statsDialogVisible.value = true
  statsLoading.value = true
  try {
    statsData.value = await getBatchStats(row.batchCode)
  } catch {
    statsData.value = {}
  } finally {
    statsLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function batchStatusLabel(s?: number): string {
  const found = BATCH_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/** 批次状态 tag：0草稿info / 1生产中warning / 2已完成success / 3已作废danger */
function batchStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case BatchStatus.PRODUCING:
      return 'warning'
    case BatchStatus.COMPLETED:
      return 'success'
    case BatchStatus.VOIDED:
      return 'danger'
    case BatchStatus.DRAFT:
    default:
      return 'info'
  }
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="批次编码">
          <el-input v-model="query.batchCode" placeholder="批次编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="批次名称">
          <el-input v-model="query.batchName" placeholder="批次名称关键字" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="模板编码">
          <el-input v-model="query.templateCode" placeholder="模板编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="渠道编码">
          <el-input v-model="query.channelCode" placeholder="渠道编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="批次状态">
          <el-select v-model="query.batchStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in BATCH_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>权益批次列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增批次</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="batchCode">
        <el-table-column prop="batchCode" label="批次编码" min-width="150" show-overflow-tooltip fixed="left" />
        <el-table-column prop="batchName" label="批次名称" min-width="160" show-overflow-tooltip fixed="left" />
        <el-table-column prop="templateCode" label="模板编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="channelCode" label="渠道编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="totalQuantity" label="总数量" width="90" align="right" />
        <el-table-column label="生产/分配/出库" width="170" align="center">
          <template #default="{ row }">
            <span class="count-cell">
              <el-tag size="small" type="warning">{{ row.producedCount ?? 0 }}</el-tag>
              /
              <el-tag size="small" type="primary">{{ row.allocatedCount ?? 0 }}</el-tag>
              /
              <el-tag size="small" type="success">{{ row.outboundCount ?? 0 }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="activatedCount" label="已激活" width="90" align="right" />
        <el-table-column prop="usedCount" label="已使用" width="90" align="right" />
        <el-table-column prop="expiredCount" label="已过期" width="90" align="right" />
        <el-table-column prop="voidedCount" label="已作废" width="90" align="right" />
        <el-table-column prop="remainCount" label="剩余可用" width="100" align="right">
          <template #default="{ row }">
            <el-tag size="small" type="success">{{ row.remainCount ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unitCost" label="单位成本" width="100" align="right" />
        <el-table-column prop="totalCost" label="批次总成本" width="110" align="right" />
        <el-table-column prop="produceDate" label="生产日期" width="120" align="center" />
        <el-table-column prop="expireDate" label="批次有效期" width="120" align="center" />
        <el-table-column prop="batchStatus" label="批次状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="batchStatusTagType(row.batchStatus)">
              {{ batchStatusLabel(row.batchStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="info" size="small" @click="openStats(row)">统计</el-button>
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
      :title="dialogType === 'create' ? '新增权益批次' : '编辑权益批次'"
      width="780px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="批次名称" prop="batchName">
              <el-input v-model="form.batchName" placeholder="批次名称" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="form.templateCode" placeholder="权益模板编码（templateCode）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道编码">
              <el-input v-model="form.channelCode" placeholder="分配渠道编码（channelCode）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总数量" prop="totalQuantity">
              <el-input-number v-model="form.totalQuantity" :min="0" :max="9999999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位成本">
              <el-input-number v-model="form.unitCost" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次总成本">
              <el-input-number v-model="form.totalCost" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生产日期">
              <el-date-picker
                v-model="form.produceDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="生产日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次有效期">
              <el-date-picker
                v-model="form.expireDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="批次有效期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次状态">
              <el-select v-model="form.batchStatus" placeholder="批次状态" style="width: 100%">
                <el-option v-for="o in BATCH_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
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

    <!-- 统计弹窗 -->
    <el-dialog v-model="statsDialogVisible" :title="`批次统计 - ${statsBatchName}`" width="600px">
      <el-descriptions v-loading="statsLoading" :column="3" border>
        <el-descriptions-item label="总数量">{{ statsData.totalQuantity ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已生成">{{ statsData.producedCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已分配">{{ statsData.allocatedCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已出库">{{ statsData.outboundCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已激活">{{ statsData.activatedCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已使用">{{ statsData.usedCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已过期">{{ statsData.expiredCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已作废">{{ statsData.voidedCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="剩余可用">
          <el-tag type="success" size="small">{{ statsData.remainCount ?? 0 }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="statsDialogVisible = false">关闭</el-button>
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

.count-cell {
  white-space: nowrap;
}
</style>
