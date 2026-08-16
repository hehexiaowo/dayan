<script setup lang="ts">
/**
 * 财务对账管理页（状态机驱动）。
 *
 * 后端 FinanceReconciliationAdminController（/admin-api/finance/reconciliation）：
 * - page / list / getDetail 查询；
 * - create 创建（→0 对账中）；
 * - complete（0→1 已完成，无差异，path）；
 * - submit-diff（0→2 待确认，body）；
 * - confirm（2→3 已确认，body，终态）。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageReconciliations,
  getReconciliation,
  createReconciliation,
  completeReconciliation,
  submitDiffReconciliation,
  confirmReconciliation
} from '@/api/finance-reconciliation'
import type { FinanceReconciliation, FinanceReconciliationQuery } from '@/types/finance-reconciliation'
import {
  ReconStatus,
  RECON_TYPE_OPTIONS,
  RECON_RESULT_OPTIONS,
  RECON_STATUS_OPTIONS
} from '@/types/finance-reconciliation'
import { formatMoney, formatDateTime, formatDate } from '@/utils/format'

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<FinanceReconciliation, FinanceReconciliationQuery>(
  { page: pageReconciliations },
  {
    initialQuery: {
      reconCode: '',
      reconType: undefined,
      targetCode: '',
      reconResult: undefined,
      status: undefined
    }
  }
)

function handleReset() {
  query.reconCode = ''
  query.reconType = undefined
  query.targetCode = ''
  query.reconResult = undefined
  query.status = undefined
  handleSearch()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const detail = ref<FinanceReconciliation>({})

async function openDetail(row: FinanceReconciliation) {
  if (!row.reconCode) return
  try {
    detail.value = await getReconciliation(row.reconCode)
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

// ---------- 新增对账弹窗 ----------
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  reconType: 1,
  targetCode: '',
  targetName: '',
  periodRange: [] as string[],
  ourOrderCount: undefined as number | undefined,
  ourTotalAmount: undefined as number | undefined,
  theirOrderCount: undefined as number | undefined,
  theirTotalAmount: undefined as number | undefined,
  diffCount: 0,
  diffAmount: 0,
  operatorCode: '',
  remark: ''
})
const createRules: FormRules<typeof createForm> = {
  reconType: [{ required: true, message: '请选择对账类型', trigger: 'change' }],
  targetCode: [{ required: true, message: '请输入对账对象编码', trigger: 'blur' }],
  targetName: [{ required: true, message: '请输入对账对象名称', trigger: 'blur' }],
  ourOrderCount: [{ required: true, message: '请输入我方订单数', trigger: 'blur' }],
  ourTotalAmount: [{ required: true, message: '请输入我方总金额', trigger: 'blur' }],
  operatorCode: [{ required: true, message: '请输入操作人编码', trigger: 'blur' }]
}

function openCreate() {
  Object.assign(createForm, {
    reconType: 1,
    targetCode: '',
    targetName: '',
    periodRange: [],
    ourOrderCount: undefined,
    ourTotalAmount: undefined,
    theirOrderCount: undefined,
    theirTotalAmount: undefined,
    diffCount: 0,
    diffAmount: 0,
    operatorCode: '',
    remark: ''
  })
  createVisible.value = true
}

async function handleCreateSubmit() {
  if (!createFormRef.value) return
  if (!createForm.periodRange || createForm.periodRange.length !== 2) {
    ElMessage.warning('请选择对账周期')
    return
  }
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  createLoading.value = true
  try {
    await createReconciliation({
      reconType: createForm.reconType,
      targetCode: createForm.targetCode,
      targetName: createForm.targetName,
      periodStart: createForm.periodRange[0],
      periodEnd: createForm.periodRange[1],
      ourOrderCount: createForm.ourOrderCount!,
      ourTotalAmount: createForm.ourTotalAmount!,
      theirOrderCount: createForm.theirOrderCount,
      theirTotalAmount: createForm.theirTotalAmount,
      diffCount: createForm.diffCount,
      diffAmount: createForm.diffAmount,
      operatorCode: createForm.operatorCode,
      remark: createForm.remark || undefined
    })
    ElMessage.success('对账记录已创建')
    createVisible.value = false
    loadPage()
  } finally {
    createLoading.value = false
  }
}

// ---------- 通用动作 loading ----------
const actionLoading = ref(false)

// ---------- 完成对账（无差异，path，仅确认） ----------
async function handleComplete(row: FinanceReconciliation) {
  if (!row.reconCode) return
  await ElMessageBox.confirm(`确定将对账「${row.reconCode}」标记为「已完成（无差异）」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  actionLoading.value = true
  try {
    await completeReconciliation(row.reconCode)
    ElMessage.success('已完成对账')
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

// ---------- 提交差异弹窗（0→2 待确认） ----------
const diffVisible = ref(false)
const diffForm = reactive({ reconCode: '', diffDetail: '', handleResult: '', remark: '' })

function openSubmitDiff(row: FinanceReconciliation) {
  if (!row.reconCode) return
  Object.assign(diffForm, { reconCode: row.reconCode, diffDetail: '', handleResult: '', remark: '' })
  diffVisible.value = true
}

async function handleDiffSubmit() {
  if (!diffForm.reconCode) return
  actionLoading.value = true
  try {
    await submitDiffReconciliation({
      reconCode: diffForm.reconCode,
      diffDetail: diffForm.diffDetail || undefined,
      handleResult: diffForm.handleResult || undefined,
      remark: diffForm.remark || undefined
    })
    ElMessage.success('已提交差异，进入待确认')
    diffVisible.value = false
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

// ---------- 确认对账弹窗（2→3 已确认） ----------
const confirmVisible = ref(false)
const confirmForm = reactive({ reconCode: '', handleResult: '', remark: '' })

function openConfirm(row: FinanceReconciliation) {
  if (!row.reconCode) return
  Object.assign(confirmForm, { reconCode: row.reconCode, handleResult: '', remark: '' })
  confirmVisible.value = true
}

async function handleConfirmSubmit() {
  if (!confirmForm.reconCode) return
  actionLoading.value = true
  try {
    await confirmReconciliation({
      reconCode: confirmForm.reconCode,
      handleResult: confirmForm.handleResult || undefined,
      remark: confirmForm.remark || undefined
    })
    ElMessage.success('对账已确认')
    confirmVisible.value = false
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function reconTypeLabel(t?: number): string {
  return RECON_TYPE_OPTIONS.find((o) => o.value === t)?.label ?? '--'
}
function reconResultLabel(r?: number): string {
  return RECON_RESULT_OPTIONS.find((o) => o.value === r)?.label ?? '--'
}
function reconResultTagType(r?: number): 'success' | 'warning' {
  return r === 1 ? 'success' : 'warning'
}
function statusLabel(s?: number): string {
  return RECON_STATUS_OPTIONS.find((o) => o.value === s)?.label ?? '--'
}
function statusTagType(s?: number): 'success' | 'warning' | 'info' | 'primary' {
  switch (s) {
    case ReconStatus.COMPLETED:
    case ReconStatus.CONFIRMED:
      return 'success'
    case ReconStatus.PENDING_CONFIRM:
      return 'warning'
    case ReconStatus.RECONCILING:
      return 'primary'
    default:
      return 'info'
  }
}

loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.reconCode"
          placeholder="对账编号"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.reconType" placeholder="对账类型" clearable style="width: 130px">
          <el-option v-for="o in RECON_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input
          v-model="query.targetCode"
          placeholder="对象编码"
          clearable
          style="width: 140px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.reconResult" placeholder="对账结果" clearable style="width: 120px">
          <el-option v-for="o in RECON_RESULT_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="o in RECON_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span class="card-title">对账管理列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增对账</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="reconCode">
        <el-table-column prop="reconCode" label="对账编号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="reconType" label="类型" width="110" align="center">
          <template #default="{ row }">{{ reconTypeLabel(row.reconType) }}</template>
        </el-table-column>
        <el-table-column prop="targetName" label="对账对象" min-width="140" show-overflow-tooltip />
        <el-table-column prop="periodStart" label="对账周期" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDate(row.periodStart) }} ~ {{ formatDate(row.periodEnd) }}</template>
        </el-table-column>
        <el-table-column prop="ourTotalAmount" label="我方金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.ourTotalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="theirTotalAmount" label="对方金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.theirTotalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="diffAmount" label="差异金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.diffAmount) }}</template>
        </el-table-column>
        <el-table-column prop="reconResult" label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="reconResultTagType(row.reconResult)" size="small">{{ reconResultLabel(row.reconResult) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reconTime" label="对账时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.reconTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === ReconStatus.RECONCILING"
              link
              type="success"
              size="small"
              :disabled="actionLoading"
              @click="handleComplete(row)"
            >
              完成对账
            </el-button>
            <el-button
              v-if="row.status === ReconStatus.RECONCILING"
              link
              type="warning"
              size="small"
              :disabled="actionLoading"
              @click="openSubmitDiff(row)"
            >
              提交差异
            </el-button>
            <el-button
              v-if="row.status === ReconStatus.PENDING_CONFIRM"
              link
              type="primary"
              size="small"
              :disabled="actionLoading"
              @click="openConfirm(row)"
            >
              确认对账
            </el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="对账详情" width="800px" :close-on-click-modal="false">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="对账编号">{{ detail.reconCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="对账类型">{{ reconTypeLabel(detail.reconType) }}</el-descriptions-item>
        <el-descriptions-item label="对账对象">{{ detail.targetName || detail.targetCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="对账周期">{{ formatDate(detail.periodStart) }} ~ {{ formatDate(detail.periodEnd) }}</el-descriptions-item>
        <el-descriptions-item label="我方订单数">{{ detail.ourOrderCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="我方总金额">{{ formatMoney(detail.ourTotalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="对方订单数">{{ detail.theirOrderCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="对方总金额">{{ formatMoney(detail.theirTotalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="差异订单数">{{ detail.diffCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="差异金额">{{ formatMoney(detail.diffAmount) }}</el-descriptions-item>
        <el-descriptions-item label="对账结果">
          <el-tag :type="reconResultTagType(detail.reconResult)" size="small">{{ reconResultLabel(detail.reconResult) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="差异处理结果" :span="2">{{ detail.handleResult || '--' }}</el-descriptions-item>
        <el-descriptions-item label="差异明细" :span="2">{{ detail.diffDetail || '--' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operatorName || detail.operatorCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="对账时间">{{ formatDateTime(detail.reconTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增对账弹窗 -->
    <el-dialog v-model="createVisible" title="新增对账" width="720px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="对账类型" prop="reconType">
              <el-select v-model="createForm.reconType" style="width: 100%">
                <el-option v-for="o in RECON_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对象编码" prop="targetCode">
              <el-input v-model="createForm.targetCode" placeholder="对账对象编码" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对象名称" prop="targetName">
              <el-input v-model="createForm.targetName" placeholder="对账对象名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作人编码" prop="operatorCode">
              <el-input v-model="createForm.operatorCode" placeholder="操作人编码" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="对账周期" required>
              <el-date-picker
                v-model="createForm.periodRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="我方订单数" prop="ourOrderCount">
              <el-input-number v-model="createForm.ourOrderCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="我方总金额" prop="ourTotalAmount">
              <el-input-number v-model="createForm.ourTotalAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对方订单数">
              <el-input-number v-model="createForm.theirOrderCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对方总金额">
              <el-input-number v-model="createForm.theirTotalAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <!-- 差异由后端计算（我方-对方），创建时不允许手填，固定 0 -->
            <el-form-item label="差异订单数">
              <el-input-number v-model="createForm.diffCount" :min="0" disabled controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="差异金额">
              <el-input-number v-model="createForm.diffAmount" :precision="2" disabled controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 提交差异弹窗 -->
    <el-dialog v-model="diffVisible" title="提交差异" width="520px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="对账编号">
          <el-input :model-value="diffForm.reconCode" disabled />
        </el-form-item>
        <el-form-item label="差异明细">
          <el-input v-model="diffForm.diffDetail" type="textarea" :rows="3" placeholder="差异明细（JSON，可选）" />
        </el-form-item>
        <el-form-item label="差异处理">
          <el-input v-model="diffForm.handleResult" type="textarea" :rows="2" placeholder="差异处理结果（可选）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="diffForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="diffVisible = false">取消</el-button>
        <el-button type="warning" :loading="actionLoading" @click="handleDiffSubmit">提交差异</el-button>
      </template>
    </el-dialog>

    <!-- 确认对账弹窗 -->
    <el-dialog v-model="confirmVisible" title="确认对账" width="520px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="对账编号">
          <el-input :model-value="confirmForm.reconCode" disabled />
        </el-form-item>
        <el-form-item label="差异处理">
          <el-input v-model="confirmForm.handleResult" type="textarea" :rows="2" placeholder="差异处理结果（可选）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="confirmForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleConfirmSubmit">确认</el-button>
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
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}
</style>
