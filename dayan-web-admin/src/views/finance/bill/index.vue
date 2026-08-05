<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageBills,
  getBill,
  generateBill,
  auditBill,
  startSettleBill,
  finishSettleBill
} from '@/api/finance'
import type { FinanceBill, FinanceBillQuery } from '@/types/finance'
import {
  BillType,
  BillStatus,
  BILL_TYPE_OPTIONS,
  BILL_STATUS_OPTIONS,
  SETTLEMENT_METHOD_OPTIONS
} from '@/types/finance'
import { formatDateTime } from '@/utils/format'

/**
 * 结算单（FinanceBill）管理页。
 *
 * - 搜索 + 表格 + 分页 + 生成弹窗。
 * - 结算流：生成（待审核）→ 审核（通过/拒绝）→ 开始结算 → 完成结算。
 *   操作按钮按 billStatus 动态显示。
 *
 * 状态约定（billStatus）：
 * - 0 待审核 / 1 审核通过 / 2 结算中 / 3 已结算 / 4 审核拒绝
 */

// ---------------- 分页 / 搜索 ----------------
const {
  loading,
  tableData,
  total,
  query,
  loadPage,
  handleSearch,
  handlePageChange,
  handleSizeChange
} = useCrud<FinanceBill, FinanceBillQuery>(
  { page: pageBills },
  {
    initialQuery: {
      billCode: '',
      billType: undefined,
      billStatus: undefined,
      targetType: ''
    }
  }
)

function handleReset() {
  query.billCode = ''
  query.billType = undefined
  query.billStatus = undefined
  query.targetType = ''
  handleSearch()
}

// ---------------- 生成结算单弹窗 ----------------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 生成结算单表单（对应 GenerateBillDTO，不含 billCode / finalAmount） */
const form = reactive({
  billType: BillType.CHANNEL,
  targetType: '',
  targetCode: '',
  targetName: '',
  periodStart: '',
  periodEnd: '',
  orderCount: 0,
  totalAmount: 0,
  commissionAmount: 0,
  refundAmount: 0,
  adjustAmount: 0,
  flowIds: '',
  settlementMethod: 1,
  bankInfo: '',
  remark: ''
})

const rules: FormRules<typeof form> = {
  billType: [{ required: true, message: '请选择结算类型', trigger: 'change' }],
  targetType: [{ required: true, message: '请输入结算对象类型', trigger: 'blur' }],
  targetCode: [{ required: true, message: '请输入结算对象编码', trigger: 'blur' }],
  targetName: [{ required: true, message: '请输入结算对象名称', trigger: 'blur' }],
  periodStart: [{ required: true, message: '请选择结算周期开始日期', trigger: 'change' }],
  periodEnd: [{ required: true, message: '请选择结算周期结束日期', trigger: 'change' }],
  orderCount: [{ required: true, message: '请输入订单数量', trigger: 'blur' }],
  totalAmount: [{ required: true, message: '请输入结算总额', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    billType: BillType.CHANNEL,
    targetType: '',
    targetCode: '',
    targetName: '',
    periodStart: '',
    periodEnd: '',
    orderCount: 0,
    totalAmount: 0,
    commissionAmount: 0,
    refundAmount: 0,
    adjustAmount: 0,
    flowIds: '',
    settlementMethod: 1,
    bankInfo: '',
    remark: ''
  })
}

function openCreate() {
  resetForm()
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
    await generateBill({
      billType: form.billType,
      targetType: form.targetType,
      targetCode: form.targetCode,
      targetName: form.targetName,
      periodStart: form.periodStart,
      periodEnd: form.periodEnd,
      orderCount: form.orderCount,
      totalAmount: form.totalAmount,
      commissionAmount: form.commissionAmount || undefined,
      refundAmount: form.refundAmount || undefined,
      adjustAmount: form.adjustAmount || undefined,
      flowIds: form.flowIds || undefined,
      settlementMethod: form.settlementMethod || undefined,
      bankInfo: form.bankInfo || undefined,
      remark: form.remark || undefined
    })
    ElMessage.success('生成结算单成功')
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

// ---------------- 审核弹窗 ----------------
const auditDialogVisible = ref(false)
const auditSubmitLoading = ref(false)
const auditForm = reactive<{ billCode: string; targetName: string; pass: boolean; auditRemark: string }>({
  billCode: '',
  targetName: '',
  // true=通过 → 状态1，false=拒绝 → 状态4
  pass: true,
  auditRemark: ''
})

function openAudit(row: FinanceBill) {
  if (!row.billCode) return
  auditForm.billCode = row.billCode
  auditForm.targetName = row.targetName ?? ''
  auditForm.pass = true
  auditForm.auditRemark = ''
  auditDialogVisible.value = true
}

async function handleAuditSubmit() {
  auditSubmitLoading.value = true
  try {
    await auditBill({
      billCode: auditForm.billCode,
      pass: auditForm.pass,
      auditRemark: auditForm.auditRemark || undefined
    })
    ElMessage.success(auditForm.pass ? '已通过' : '已拒绝')
    auditDialogVisible.value = false
    loadPage()
  } finally {
    auditSubmitLoading.value = false
  }
}

// ---------------- 完成结算弹窗 ----------------
const settleDialogVisible = ref(false)
const settleSubmitLoading = ref(false)
const settleForm = reactive<{ billCode: string; finalAmount: number | undefined; settleTime: string; remark: string }>({
  billCode: '',
  finalAmount: undefined,
  settleTime: '',
  remark: ''
})

function openFinishSettle(row: FinanceBill) {
  if (!row.billCode) return
  settleForm.billCode = row.billCode
  settleForm.finalAmount = row.finalAmount
  settleForm.settleTime = ''
  settleForm.remark = ''
  settleDialogVisible.value = true
}

async function handleSettleSubmit() {
  settleSubmitLoading.value = true
  try {
    await finishSettleBill({
      billCode: settleForm.billCode,
      settleTime: settleForm.settleTime || undefined,
      remark: settleForm.remark || undefined
    })
    ElMessage.success('已完成结算')
    settleDialogVisible.value = false
    loadPage()
  } finally {
    settleSubmitLoading.value = false
  }
}

// ---------------- 开始结算（仅二次确认，无弹窗） ----------------
async function handleStartSettle(row: FinanceBill) {
  if (!row.billCode) return
  await ElMessageBox.confirm(
    `确定开始结算「${row.targetName ?? row.billCode}」吗？（状态：审核通过 → 结算中）`,
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await startSettleBill(row.billCode)
  ElMessage.success('已开始结算')
  loadPage()
}

// ---------------- 详情弹窗 ----------------
const detailDialogVisible = ref(false)
const detail = ref<FinanceBill | null>(null)

async function openDetail(row: FinanceBill) {
  if (!row.billCode) return
  try {
    detail.value = await getBill(row.billCode)
  } catch {
    detail.value = row
  }
  detailDialogVisible.value = true
}

// ---------------- 辅助渲染 ----------------
function billTypeLabel(t?: number): string {
  const found = BILL_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function billStatusLabel(s?: number): string {
  const found = BILL_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/**
 * 根据结算单状态返回 el-tag type：
 * 待审核 info / 审核通过 success / 结算中 warning / 已结算 primary / 审核拒绝 danger。
 */
function billStatusTagType(
  status?: number
): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (status) {
    case BillStatus.AUDIT_PASS:
      return 'success'
    case BillStatus.SETTLING:
      return 'warning'
    case BillStatus.SETTLED:
      return 'primary'
    case BillStatus.AUDIT_REJECT:
      return 'danger'
    case BillStatus.PENDING_AUDIT:
    default:
      return 'info'
  }
}

/** 金额展示：¥ + toFixed(2) */
function amountLabel(amount?: number): string {
  if (amount == null) return '--'
  return `¥${amount.toFixed(2)}`
}

/** 结算周期合并显示 */
function periodLabel(row: FinanceBill): string {
  if (!row.periodStart && !row.periodEnd) return '--'
  return `${row.periodStart ?? '--'} ~ ${row.periodEnd ?? '--'}`
}

/** 结算方式 label */
function settlementMethodLabel(m?: number): string {
  const found = SETTLEMENT_METHOD_OPTIONS.find((o) => o.value === m)
  return found ? found.label : m != null ? String(m) : '--'
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="结算单号">
          <el-input
            v-model="query.billCode"
            placeholder="结算单编号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="结算类型">
          <el-select v-model="query.billType" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="o in BILL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="结算状态">
          <el-select v-model="query.billStatus" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in BILL_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象类型">
          <el-input
            v-model="query.targetType"
            placeholder="channel/supplier/..."
            clearable
            @keyup.enter="handleSearch"
          />
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
          <span>结算单列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">生成结算单</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="billCode">
        <el-table-column prop="billCode" label="结算单号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="billType" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ billTypeLabel(row.billType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="结算对象" min-width="160" show-overflow-tooltip />
        <el-table-column label="结算周期" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ periodLabel(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="finalAmount" label="最终结算金额" width="140" align="right">
          <template #default="{ row }">
            <span class="amount-text">{{ amountLabel(row.finalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="billStatus" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="billStatusTagType(row.billStatus)">
              {{ billStatusLabel(row.billStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.billStatus === BillStatus.PENDING_AUDIT"
              link
              type="warning"
              size="small"
              @click="openAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.billStatus === BillStatus.AUDIT_PASS"
              link
              type="success"
              size="small"
              @click="handleStartSettle(row)"
            >
              开始结算
            </el-button>
            <el-button
              v-if="row.billStatus === BillStatus.SETTLING"
              link
              type="primary"
              size="small"
              @click="openFinishSettle(row)"
            >
              完成结算
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

    <!-- 生成结算单弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="生成结算单"
      width="860px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="结算类型" prop="billType">
              <el-select v-model="form.billType" placeholder="结算类型" style="width: 100%">
                <el-option v-for="o in BILL_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结算方式">
              <el-select v-model="form.settlementMethod" placeholder="结算方式" style="width: 100%">
                <el-option
                  v-for="o in SETTLEMENT_METHOD_OPTIONS"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="对象类型" prop="targetType">
              <el-input v-model="form.targetType" placeholder="channel/supplier/..." maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="对象编码" prop="targetCode">
              <el-input v-model="form.targetCode" placeholder="结算对象编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="对象名称" prop="targetName">
              <el-input v-model="form.targetName" placeholder="结算对象名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="周期开始" prop="periodStart">
              <el-date-picker
                v-model="form.periodStart"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="周期结束" prop="periodEnd">
              <el-date-picker
                v-model="form.periodEnd"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="订单数量" prop="orderCount">
              <el-input-number
                v-model="form.orderCount"
                :min="0"
                :max="999999"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结算总额" prop="totalAmount">
              <el-input-number
                v-model="form.totalAmount"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分销手续费">
              <el-input-number
                v-model="form.commissionAmount"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="退款金额">
              <el-input-number
                v-model="form.refundAmount"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="调整金额">
              <el-input-number
                v-model="form.adjustAmount"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收款银行信息">
              <el-input v-model="form.bankInfo" placeholder="开户行 / 户名 / 账号" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="关联流水ID">
              <el-input
                v-model="form.flowIds"
                type="textarea"
                :rows="2"
                placeholder="多个流水ID用逗号分隔，例如：1001, 1002, 1003"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="2"
                placeholder="备注（可选）"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定生成</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="结算单审核" width="520px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="结算单号">
          <span>{{ auditForm.billCode }}</span>
        </el-form-item>
        <el-form-item label="结算对象">
          <span>{{ auditForm.targetName }}</span>
        </el-form-item>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.pass">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="3"
            placeholder="审核备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitLoading" @click="handleAuditSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 完成结算弹窗 -->
    <el-dialog v-model="settleDialogVisible" title="完成结算" width="520px" :close-on-click-modal="false">
      <el-form label-width="110px">
        <el-form-item label="结算单号">
          <span>{{ settleForm.billCode }}</span>
        </el-form-item>
        <el-form-item label="最终结算金额">
          <span class="amount-text">{{ amountLabel(settleForm.finalAmount) }}</span>
        </el-form-item>
        <el-form-item label="结算完成时间">
          <el-date-picker
            v-model="settleForm.settleTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空则取服务端当前时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="settleForm.remark"
            type="textarea"
            :rows="3"
            placeholder="备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settleSubmitLoading" @click="handleSettleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗（只读） -->
    <el-dialog v-model="detailDialogVisible" title="结算单详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="结算单号">{{ detail.billCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="结算类型">
          {{ billTypeLabel(detail.billType) }}
        </el-descriptions-item>
        <el-descriptions-item label="结算对象类型">{{ detail.targetType ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="结算对象编码">{{ detail.targetCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="结算对象名称" :span="2">
          {{ detail.targetName ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="结算周期" :span="2">
          {{ periodLabel(detail) }}
        </el-descriptions-item>
        <el-descriptions-item label="订单数量">{{ detail.orderCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="结算总额">
          <span class="amount-text">{{ amountLabel(detail.totalAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="分销手续费">
          {{ amountLabel(detail.commissionAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="退款金额">
          {{ amountLabel(detail.refundAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="调整金额">
          {{ amountLabel(detail.adjustAmount) }}
        </el-descriptions-item>
        <el-descriptions-item label="最终结算金额">
          <span class="amount-text">{{ amountLabel(detail.finalAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="结算方式">
          {{ settlementMethodLabel(detail.settlementMethod) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="billStatusTagType(detail.billStatus)">
            {{ billStatusLabel(detail.billStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收款银行信息" :span="2">
          {{ detail.bankInfo ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="关联流水ID" :span="2">
          {{ detail.flowIds ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="审核人">
          {{ detail.auditorName ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="审核时间">
          {{ formatDateTime(detail.auditTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="审核备注" :span="2">
          {{ detail.auditRemark ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">
          {{ formatDateTime(detail.applyTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="结算完成时间">
          {{ formatDateTime(detail.settleTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">
          {{ formatDateTime(detail.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ detail.remark ?? '--' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
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

.amount-text {
  color: #e6a23c;
  font-weight: 600;
}
</style>
