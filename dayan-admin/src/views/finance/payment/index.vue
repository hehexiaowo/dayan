<script setup lang="ts">
/**
 * 支付记录管理页（状态机驱动）。
 *
 * 后端 FinancePaymentAdminController（/admin-api/finance/payment）：
 * - page / list / getDetail 查询；
 * - create 创建（→0 待支付）；
 * - mark-success（0→1 支付成功，body：tradeNo）；
 * - mark-failed（0→2 支付失败，body：payDescription）。
 */
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pagePayments,
  getPayment,
  createPayment,
  markPaymentSuccess,
  markPaymentFailed
} from '@/api/finance-payment'
import type { FinancePayment, FinancePaymentQuery } from '@/types/finance-payment'
import {
  PayStatus,
  PAY_STATUS_OPTIONS,
  ORDER_TYPE_OPTIONS,
  PAY_TYPE_OPTIONS
} from '@/types/finance-payment'
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
} = useCrud<FinancePayment, FinancePaymentQuery>(
  { page: pagePayments },
  {
    initialQuery: {
      paymentCode: '',
      orderType: undefined,
      orderCode: '',
      payType: undefined,
      payStatus: undefined,
      tradeNo: ''
    }
  }
)

function handleReset() {
  query.paymentCode = ''
  query.orderType = undefined
  query.orderCode = ''
  query.payType = undefined
  query.payStatus = undefined
  query.tradeNo = ''
  handleSearch()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const detail = ref<FinancePayment>({})

async function openDetail(row: FinancePayment) {
  if (!row.paymentCode) return
  try {
    detail.value = await getPayment(row.paymentCode)
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

// ---------- 新增支付记录弹窗 ----------
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  orderType: 1,
  orderCode: '',
  payType: 1,
  payAmount: undefined as number | undefined,
  payerAccount: '',
  payeeAccount: '',
  payDescription: ''
})
const createRules: FormRules<typeof createForm> = {
  orderType: [{ required: true, message: '请选择订单类型', trigger: 'change' }],
  orderCode: [{ required: true, message: '请输入订单编号', trigger: 'blur' }],
  payType: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

function openCreate() {
  Object.assign(createForm, {
    orderType: 1,
    orderCode: '',
    payType: 1,
    payAmount: undefined,
    payerAccount: '',
    payeeAccount: '',
    payDescription: ''
  })
  createVisible.value = true
}

async function handleCreateSubmit() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  createLoading.value = true
  try {
    await createPayment({
      orderType: createForm.orderType,
      orderCode: createForm.orderCode,
      payType: createForm.payType,
      payAmount: createForm.payAmount,
      payerAccount: createForm.payerAccount || undefined,
      payeeAccount: createForm.payeeAccount || undefined,
      payDescription: createForm.payDescription || undefined
    })
    ElMessage.success('支付记录已创建')
    createVisible.value = false
    loadPage()
  } finally {
    createLoading.value = false
  }
}

// ---------- 通用动作 loading ----------
const actionLoading = ref(false)

// ---------- 标记成功弹窗（body：tradeNo） ----------
const successVisible = ref(false)
const successForm = reactive({ paymentCode: '', tradeNo: '', payTime: '', payDescription: '' })
const successRules: FormRules<typeof successForm> = {
  tradeNo: [{ required: true, message: '请输入第三方交易号', trigger: 'blur' }]
}

function openMarkSuccess(row: FinancePayment) {
  if (!row.paymentCode) return
  Object.assign(successForm, { paymentCode: row.paymentCode, tradeNo: '', payTime: '', payDescription: '' })
  successVisible.value = true
}

async function handleMarkSuccessSubmit() {
  if (!successForm.paymentCode || !successForm.tradeNo.trim()) {
    ElMessage.warning('请输入第三方交易号')
    return
  }
  actionLoading.value = true
  try {
    await markPaymentSuccess({
      paymentCode: successForm.paymentCode,
      tradeNo: successForm.tradeNo.trim(),
      payTime: successForm.payTime || undefined,
      payDescription: successForm.payDescription || undefined
    })
    ElMessage.success('已标记支付成功')
    successVisible.value = false
    loadPage()
  } finally {
    actionLoading.value = false
  }
}

// ---------- 标记失败弹窗（body：payDescription） ----------
const failedVisible = ref(false)
const failedForm = reactive({ paymentCode: '', payDescription: '', notifyTime: '' })

function openMarkFailed(row: FinancePayment) {
  if (!row.paymentCode) return
  Object.assign(failedForm, { paymentCode: row.paymentCode, payDescription: '', notifyTime: '' })
  failedVisible.value = true
}

async function handleMarkFailedSubmit() {
  if (!failedForm.paymentCode) return
  actionLoading.value = true
  try {
    await markPaymentFailed({
      paymentCode: failedForm.paymentCode,
      payDescription: failedForm.payDescription || undefined,
      notifyTime: failedForm.notifyTime || undefined
    })
    ElMessage.success('已标记支付失败')
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
function payTypeLabel(t?: number): string {
  return PAY_TYPE_OPTIONS.find((o) => o.value === t)?.label ?? '--'
}
function statusLabel(s?: number): string {
  return PAY_STATUS_OPTIONS.find((o) => o.value === s)?.label ?? '--'
}
function statusTagType(s?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (s) {
    case PayStatus.SUCCESS:
      return 'success'
    case PayStatus.PENDING:
      return 'warning'
    case PayStatus.FAILED:
      return 'danger'
    case PayStatus.REFUNDED:
    case PayStatus.PARTIAL_REFUND:
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
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="支付流水号">
          <el-input v-model="query.paymentCode" placeholder="支付流水号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="订单类型">
          <el-select v-model="query.orderType" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="o in ORDER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单编号">
          <el-input v-model="query.orderCode" placeholder="订单编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="query.payType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in PAY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="query.payStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in PAY_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>支付记录列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">新增支付记录</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="paymentCode">
        <el-table-column prop="paymentCode" label="支付流水号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="orderType" label="订单类型" width="100" align="center">
          <template #default="{ row }">{{ orderTypeLabel(row.orderType) }}</template>
        </el-table-column>
        <el-table-column prop="orderCode" label="订单编号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="payType" label="支付方式" width="110" align="center">
          <template #default="{ row }">{{ payTypeLabel(row.payType) }}</template>
        </el-table-column>
        <el-table-column prop="payAmount" label="支付金额" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column prop="tradeNo" label="第三方交易号" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.tradeNo || '--' }}</template>
        </el-table-column>
        <el-table-column prop="payStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.payStatus)" size="small">{{ statusLabel(row.payStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.payTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.payStatus === PayStatus.PENDING"
              link
              type="success"
              size="small"
              :disabled="actionLoading"
              @click="openMarkSuccess(row)"
            >
              标记成功
            </el-button>
            <el-button
              v-if="row.payStatus === PayStatus.PENDING"
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
    <el-dialog v-model="detailVisible" title="支付记录详情" width="760px" :close-on-click-modal="false">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="支付流水号">{{ detail.paymentCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="订单类型">{{ orderTypeLabel(detail.orderType) }}</el-descriptions-item>
        <el-descriptions-item label="订单编号">{{ detail.orderCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ payTypeLabel(detail.payType) }}</el-descriptions-item>
        <el-descriptions-item label="支付金额">{{ formatMoney(detail.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="第三方交易号">{{ detail.tradeNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="付款方账号">{{ detail.payerAccount || '--' }}</el-descriptions-item>
        <el-descriptions-item label="收款方账号">{{ detail.payeeAccount || '--' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDateTime(detail.payTime) }}</el-descriptions-item>
        <el-descriptions-item label="通知时间">{{ formatDateTime(detail.notifyTime) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.payStatus)" size="small">{{ statusLabel(detail.payStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付说明" :span="2">{{ detail.payDescription || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增支付记录弹窗 -->
    <el-dialog v-model="createVisible" title="新增支付记录" width="620px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="订单类型" prop="orderType">
              <el-select v-model="createForm.orderType" style="width: 100%">
                <el-option v-for="o in ORDER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单编号" prop="orderCode">
              <el-input v-model="createForm.orderCode" placeholder="订单编号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付方式" prop="payType">
              <el-select v-model="createForm.payType" style="width: 100%">
                <el-option v-for="o in PAY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付金额">
              <el-input-number v-model="createForm.payAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款方账号">
              <el-input v-model="createForm.payerAccount" placeholder="付款方账号（可选）" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收款方账号">
              <el-input v-model="createForm.payeeAccount" placeholder="收款方账号（可选）" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="支付说明">
              <el-input v-model="createForm.payDescription" type="textarea" :rows="2" placeholder="支付说明（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 标记成功弹窗 -->
    <el-dialog v-model="successVisible" title="标记支付成功" width="500px" :close-on-click-modal="false">
      <el-form ref="successFormRef" :model="successForm" :rules="successRules" label-width="110px">
        <el-form-item label="支付流水号">
          <el-input :model-value="successForm.paymentCode" disabled />
        </el-form-item>
        <el-form-item label="第三方交易号" prop="tradeNo">
          <el-input v-model="successForm.tradeNo" placeholder="第三方交易号" maxlength="64" />
        </el-form-item>
        <el-form-item label="支付时间">
          <el-date-picker v-model="successForm.payTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="留空取当前时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="successForm.payDescription" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="successVisible = false">取消</el-button>
        <el-button type="success" :loading="actionLoading" @click="handleMarkSuccessSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 标记失败弹窗 -->
    <el-dialog v-model="failedVisible" title="标记支付失败" width="500px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="支付流水号">
          <el-input :model-value="failedForm.paymentCode" disabled />
        </el-form-item>
        <el-form-item label="失败原因">
          <el-input v-model="failedForm.payDescription" type="textarea" :rows="3" placeholder="支付失败原因（可选）" />
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
