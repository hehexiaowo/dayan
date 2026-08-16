<script setup lang="ts">
/**
 * 退款管理页（状态机驱动）。
 *
 * 后端 FinanceRefundAdminController（/admin-api/finance/refund）：
 * - page / list / getDetail 查询；
 * - apply 创建（→0 待审核）；
 * - audit（0→1 通过 / 0→4 拒绝，body）；
 * - mark-refunding（1→2 退款中，path）；
 * - mark-success（2→3 退款成功，body：refundTradeNo）；
 * - mark-failed（2→5 退款失败，body：remark）。
 *
 * 操作列按 refundStatus 动态展示可执行动作；申请退款/审核/标记成功/失败均走独立弹窗。
 */
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageRefunds,
  getRefund,
  applyRefund,
  auditRefund,
  markRefunding,
  markRefundSuccess,
  markRefundFailed
} from '@/api/finance-refund'
import type { FinanceRefund, FinanceRefundQuery } from '@/types/finance-refund'
import {
  RefundStatus,
  REFUND_STATUS_OPTIONS,
  ORDER_TYPE_OPTIONS,
  REFUND_TYPE_OPTIONS,
  REFUND_CHANNEL_OPTIONS
} from '@/types/finance-refund'
import { formatMoney, formatDateTime } from '@/utils/format'

const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<FinanceRefund, FinanceRefundQuery>(
  { page: pageRefunds },
  {
    initialQuery: {
      refundCode: '',
      orderType: undefined,
      orderCode: '',
      refundStatus: undefined,
      refundType: undefined,
      refundChannel: undefined
    }
  }
)

function handleReset() {
  query.refundCode = ''
  query.orderType = undefined
  query.orderCode = ''
  query.refundStatus = undefined
  query.refundType = undefined
  query.refundChannel = undefined
  handleSearch()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const detail = ref<FinanceRefund>({})

async function openDetail(row: FinanceRefund) {
  if (!row.refundCode) return
  try {
    detail.value = await getRefund(row.refundCode)
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

// ---------- 申请退款弹窗 ----------
const applyVisible = ref(false)
const applyLoading = ref(false)
const applyFormRef = ref<FormInstance>()
const applyForm = reactive({
  orderType: 1,
  orderCode: '',
  paymentCode: '',
  refundAmount: undefined as number | undefined,
  refundReason: '',
  refundType: 1,
  refundChannel: 1,
  remark: ''
})
const applyRules: FormRules<typeof applyForm> = {
  orderType: [{ required: true, message: '请选择订单类型', trigger: 'change' }],
  orderCode: [{ required: true, message: '请输入订单编号', trigger: 'blur' }],
  refundAmount: [{ required: true, message: '请输入退款金额', trigger: 'blur' }],
  refundReason: [{ required: true, message: '请输入退款原因', trigger: 'blur' }]
}

function openApply() {
  Object.assign(applyForm, {
    orderType: 1,
    orderCode: '',
    paymentCode: '',
    refundAmount: undefined,
    refundReason: '',
    refundType: 1,
    refundChannel: 1,
    remark: ''
  })
  applyVisible.value = true
}

async function handleApplySubmit() {
  if (!applyFormRef.value) return
  try {
    await applyFormRef.value.validate()
  } catch {
    return
  }
  applyLoading.value = true
  try {
    await applyRefund({
      orderType: applyForm.orderType,
      orderCode: applyForm.orderCode,
      paymentCode: applyForm.paymentCode || undefined,
      refundAmount: applyForm.refundAmount!,
      refundReason: applyForm.refundReason,
      refundType: applyForm.refundType,
      refundChannel: applyForm.refundChannel,
      remark: applyForm.remark || undefined
    })
    ElMessage.success('退款申请已提交')
    applyVisible.value = false
    loadPage()
  } finally {
    applyLoading.value = false
  }
}

// ---------- 通用动作 loading ----------
const actionLoading = ref(false)

// ---------- 审核弹窗 ----------
const auditVisible = ref(false)
const auditForm = reactive({ refundCode: '', pass: true, auditRemark: '' })

function openAudit(row: FinanceRefund) {
  if (!row.refundCode) return
  auditForm.refundCode = row.refundCode
  auditForm.pass = true
  auditForm.auditRemark = ''
  auditVisible.value = true
}

async function handleAuditSubmit() {
  if (!auditForm.refundCode) return
  actionLoading.value = true
  try {
    await auditRefund({
      refundCode: auditForm.refundCode,
      pass: auditForm.pass,
      auditRemark: auditForm.auditRemark || undefined
    })
    ElMessage.success('审核已提交')
    auditVisible.value = false
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

// ---------- 进入退款中（path，仅确认） ----------
async function handleMarkRefunding(row: FinanceRefund) {
  if (!row.refundCode) return
  await ElMessageBox.confirm(`确定将退款「${row.refundCode}」置为「退款中」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  actionLoading.value = true
  try {
    await markRefunding(row.refundCode)
    ElMessage.success('已置为退款中')
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

// ---------- 标记退款成功弹窗（body：refundTradeNo） ----------
const successVisible = ref(false)
const successFormRef = ref<FormInstance>()
const successForm = reactive({ refundCode: '', refundTradeNo: '', refundTime: '', remark: '' })
const successRules: FormRules<typeof successForm> = {
  refundTradeNo: [{ required: true, message: '请输入退款交易号', trigger: 'blur' }]
}

function openMarkSuccess(row: FinanceRefund) {
  if (!row.refundCode) return
  Object.assign(successForm, { refundCode: row.refundCode, refundTradeNo: '', refundTime: '', remark: '' })
  successVisible.value = true
}

async function handleMarkSuccessSubmit() {
  if (!successFormRef.value) return
  const valid = await successFormRef.value.validate().catch(() => false)
  if (!valid) return
  actionLoading.value = true
  try {
    await markRefundSuccess({
      refundCode: successForm.refundCode,
      refundTradeNo: successForm.refundTradeNo.trim(),
      refundTime: successForm.refundTime || undefined,
      remark: successForm.remark || undefined
    })
    ElMessage.success('已标记退款成功')
    successVisible.value = false
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

// ---------- 标记退款失败弹窗（body：remark） ----------
const failedVisible = ref(false)
const failedForm = reactive({ refundCode: '', remark: '' })

function openMarkFailed(row: FinanceRefund) {
  if (!row.refundCode) return
  Object.assign(failedForm, { refundCode: row.refundCode, remark: '' })
  failedVisible.value = true
}

async function handleMarkFailedSubmit() {
  if (!failedForm.refundCode) return
  actionLoading.value = true
  try {
    await markRefundFailed({
      refundCode: failedForm.refundCode,
      remark: failedForm.remark || undefined
    })
    ElMessage.success('已标记退款失败')
    failedVisible.value = false
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function orderTypeLabel(t?: number): string {
  return ORDER_TYPE_OPTIONS.find((o) => o.value === t)?.label ?? '--'
}
function refundTypeLabel(t?: number): string {
  return REFUND_TYPE_OPTIONS.find((o) => o.value === t)?.label ?? '--'
}
function refundChannelLabel(c?: number): string {
  return REFUND_CHANNEL_OPTIONS.find((o) => o.value === c)?.label ?? '--'
}
function statusLabel(s?: number): string {
  return REFUND_STATUS_OPTIONS.find((o) => o.value === s)?.label ?? '--'
}
function statusTagType(s?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (s) {
    case RefundStatus.SUCCESS:
      return 'success'
    case RefundStatus.PENDING_AUDIT:
    case RefundStatus.AUDITED:
    case RefundStatus.REFUNDING:
      return 'warning'
    case RefundStatus.AUDIT_REJECT:
    case RefundStatus.FAILED:
      return 'danger'
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
          v-model="query.refundCode"
          placeholder="退款编号"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.orderType" placeholder="订单类型" clearable style="width: 120px">
          <el-option v-for="o in ORDER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input
          v-model="query.orderCode"
          placeholder="订单编号"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.refundStatus" placeholder="退款状态" clearable style="width: 130px">
          <el-option v-for="o in REFUND_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.refundType" placeholder="退款类型" clearable style="width: 120px">
          <el-option v-for="o in REFUND_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span class="card-title">退款管理列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openApply">申请退款</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="refundCode">
        <el-table-column prop="refundCode" label="退款编号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="orderType" label="订单类型" width="100" align="center">
          <template #default="{ row }">{{ orderTypeLabel(row.orderType) }}</template>
        </el-table-column>
        <el-table-column prop="orderCode" label="订单编号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="refundAmount" label="退款金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.refundAmount) }}</template>
        </el-table-column>
        <el-table-column prop="refundType" label="退款类型" width="100" align="center">
          <template #default="{ row }">{{ refundTypeLabel(row.refundType) }}</template>
        </el-table-column>
        <el-table-column prop="refundChannel" label="退款渠道" width="110" align="center">
          <template #default="{ row }">{{ refundChannelLabel(row.refundChannel) }}</template>
        </el-table-column>
        <el-table-column prop="refundStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.refundStatus)" size="small">{{ statusLabel(row.refundStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column prop="refundTime" label="完成时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.refundTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.refundStatus === RefundStatus.PENDING_AUDIT"
              link
              type="warning"
              size="small"
              :disabled="actionLoading"
              @click="openAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.refundStatus === RefundStatus.AUDITED"
              link
              type="primary"
              size="small"
              :disabled="actionLoading"
              @click="handleMarkRefunding(row)"
            >
              进入退款中
            </el-button>
            <el-button
              v-if="row.refundStatus === RefundStatus.REFUNDING"
              link
              type="success"
              size="small"
              :disabled="actionLoading"
              @click="openMarkSuccess(row)"
            >
              标记成功
            </el-button>
            <el-button
              v-if="row.refundStatus === RefundStatus.REFUNDING"
              link
              type="danger"
              size="small"
              :disabled="actionLoading"
              @click="openMarkFailed(row)"
            >
              标记失败
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
    <el-dialog v-model="detailVisible" title="退款详情" width="760px" :close-on-click-modal="false">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="退款编号">{{ detail.refundCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="订单类型">{{ orderTypeLabel(detail.orderType) }}</el-descriptions-item>
        <el-descriptions-item label="订单编号">{{ detail.orderCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="原支付记录">{{ detail.paymentCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">{{ formatMoney(detail.refundAmount) }}</el-descriptions-item>
        <el-descriptions-item label="退款类型">{{ refundTypeLabel(detail.refundType) }}</el-descriptions-item>
        <el-descriptions-item label="退款渠道">{{ refundChannelLabel(detail.refundChannel) }}</el-descriptions-item>
        <el-descriptions-item label="退款交易号">{{ detail.refundTradeNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.refundStatus)" size="small">{{ statusLabel(detail.refundStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="退款原因">{{ detail.refundReason || '--' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatDateTime(detail.applyTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ formatDateTime(detail.auditTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDateTime(detail.refundTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detail.auditorName || detail.auditorCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="审核备注" :span="2">{{ detail.auditRemark || '--' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 申请退款弹窗 -->
    <el-dialog v-model="applyVisible" title="申请退款" width="600px" :close-on-click-modal="false">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="订单类型" prop="orderType">
              <el-select v-model="applyForm.orderType" style="width: 100%">
                <el-option v-for="o in ORDER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单编号" prop="orderCode">
              <el-input v-model="applyForm.orderCode" placeholder="订单编号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原支付记录">
              <el-input v-model="applyForm.paymentCode" placeholder="支付记录编码（可选）" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退款金额" prop="refundAmount">
              <el-input-number v-model="applyForm.refundAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退款类型">
              <el-select v-model="applyForm.refundType" style="width: 100%">
                <el-option v-for="o in REFUND_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退款渠道">
              <el-select v-model="applyForm.refundChannel" style="width: 100%">
                <el-option v-for="o in REFUND_CHANNEL_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="退款原因" prop="refundReason">
              <el-input v-model="applyForm.refundReason" type="textarea" :rows="2" placeholder="退款原因" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="applyForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applyLoading" @click="handleApplySubmit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="退款审核" width="480px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="退款编号">
          <el-input :model-value="auditForm.refundCode" disabled />
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.pass">
            <el-radio :value="true">审核通过</el-radio>
            <el-radio :value="false">审核拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="2" placeholder="审核备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleAuditSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 标记成功弹窗 -->
    <el-dialog v-model="successVisible" title="标记退款成功" width="480px" :close-on-click-modal="false">
      <el-form ref="successFormRef" :model="successForm" :rules="successRules" label-width="100px">
        <el-form-item label="退款编号">
          <el-input :model-value="successForm.refundCode" disabled />
        </el-form-item>
        <el-form-item label="退款交易号" prop="refundTradeNo">
          <el-input v-model="successForm.refundTradeNo" placeholder="第三方退款交易号" maxlength="64" />
        </el-form-item>
        <el-form-item label="完成时间">
          <el-date-picker v-model="successForm.refundTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="留空取当前时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="successForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="successVisible = false">取消</el-button>
        <el-button type="success" :loading="actionLoading" @click="handleMarkSuccessSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 标记失败弹窗 -->
    <el-dialog v-model="failedVisible" title="标记退款失败" width="480px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="退款编号">
          <el-input :model-value="failedForm.refundCode" disabled />
        </el-form-item>
        <el-form-item label="失败原因">
          <el-input v-model="failedForm.remark" type="textarea" :rows="3" placeholder="退款失败原因（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="failedVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="handleMarkFailedSubmit">确定</el-button>
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
