<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageFlows, getFlow, recordFlow } from '@/api/finance-flow'
import type { FinanceFlow, FinanceFlowQuery } from '@/types/finance'
import {
  FlowType,
  FLOW_TYPE_OPTIONS,
  PayType,
  PAY_TYPE_OPTIONS,
  FlowStatus,
  FLOW_STATUS_OPTIONS
} from '@/types/finance'
import { formatDateTime } from '@/utils/format'

/**
 * 资金流水（FinanceFlow）管理页。
 *
 * - 以只读列表为主（流水通常由系统自动生成），手动录入为辅助；
 * - 搜索栏支持多维度筛选（流水号/类型/业务类型/账号/状态/结算状态）；
 * - 表格展示流水全貌，新增按钮放次要位置；
 * - 无状态流转操作（流水状态仅 正常/已冲正，冲正通常不在此页面操作）。
 *
 * 状态约定：
 * - flowType：1收入 / 2支出 / 3退款 / 4结算
 * - status：0已冲正 / 1正常
 * - isSettled：0未结算 / 1已结算
 */

// ---------------- 业务类型 / 账号类型 常用选项（供 el-select 选择或手动输入） ----------------
const BIZ_TYPE_OPTIONS = [
  { label: '权益订单 equity_order', value: 'equity_order' },
  { label: '场景订单 scene_order', value: 'scene_order' },
  { label: '课程订单 course_order', value: 'course_order' },
  { label: '旅游短居订单 travel_order', value: 'travel_order' },
  { label: '结算 settlement', value: 'settlement' }
] as const

const ACCOUNT_TYPE_OPTIONS = [
  { label: '机构 organ', value: 'organ' },
  { label: '渠道 channel', value: 'channel' },
  { label: '代理 agent', value: 'agent' },
  { label: '客户 client', value: 'client' },
  { label: '供应商 supplier', value: 'supplier' }
] as const

const SETTLED_OPTIONS = [
  { label: '未结算', value: 0 },
  { label: '已结算', value: 1 }
] as const

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
} = useCrud<FinanceFlow, FinanceFlowQuery>(
  { page: pageFlows },
  {
    initialQuery: {
      flowCode: '',
      flowType: undefined,
      bizType: '',
      accountType: '',
      accountCode: '',
      status: undefined,
      isSettled: undefined
    }
  }
)

function handleReset() {
  query.flowCode = ''
  query.flowType = undefined
  query.bizType = ''
  query.accountType = ''
  query.accountCode = ''
  query.status = undefined
  query.isSettled = undefined
  handleSearch()
}

// ---------------- 记录流水弹窗 ----------------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 记录流水表单（对应 RecordFlowDTO，不含 flowCode / flowTime / balanceBefore / balanceAfter） */
const form = reactive({
  flowType: FlowType.INCOME,
  bizType: '',
  bizCode: '',
  accountType: '',
  accountCode: '',
  flowAmount: 0,
  payType: PayType.WECHAT,
  tradeNo: '',
  counterpartyType: '',
  counterpartyCode: '',
  counterpartyName: '',
  flowDescription: '',
  remark: ''
})

const rules: FormRules<typeof form> = {
  flowType: [{ required: true, message: '请选择流水类型', trigger: 'change' }],
  bizType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  accountType: [{ required: true, message: '请选择账号类型', trigger: 'change' }],
  accountCode: [{ required: true, message: '请输入账号编码', trigger: 'blur' }],
  flowAmount: [{ required: true, message: '请输入流水金额', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    flowType: FlowType.INCOME,
    bizType: '',
    bizCode: '',
    accountType: '',
    accountCode: '',
    flowAmount: 0,
    payType: PayType.WECHAT,
    tradeNo: '',
    counterpartyType: '',
    counterpartyCode: '',
    counterpartyName: '',
    flowDescription: '',
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
    await recordFlow({
      flowType: form.flowType,
      bizType: form.bizType,
      bizCode: form.bizCode || undefined,
      accountType: form.accountType,
      accountCode: form.accountCode,
      flowAmount: form.flowAmount,
      payType: form.payType || undefined,
      tradeNo: form.tradeNo || undefined,
      counterpartyType: form.counterpartyType || undefined,
      counterpartyCode: form.counterpartyCode || undefined,
      counterpartyName: form.counterpartyName || undefined,
      flowDescription: form.flowDescription || undefined,
      remark: form.remark || undefined
    })
    ElMessage.success('记录流水成功')
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

// ---------------- 详情弹窗 ----------------
const detailDialogVisible = ref(false)
const detail = ref<FinanceFlow | null>(null)

async function openDetail(row: FinanceFlow) {
  if (!row.flowCode) return
  try {
    detail.value = await getFlow(row.flowCode)
  } catch {
    detail.value = row
  }
  detailDialogVisible.value = true
}

// ---------------- 辅助渲染 ----------------
function flowTypeLabel(t?: number): string {
  const found = FLOW_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function flowTypeTagType(t?: number): 'success' | 'danger' | 'warning' | 'primary' {
  switch (t) {
    case FlowType.INCOME:
      return 'success'
    case FlowType.EXPENSE:
      return 'danger'
    case FlowType.REFUND:
      return 'warning'
    case FlowType.SETTLEMENT:
      return 'primary'
    default:
      return 'primary'
  }
}

function payTypeLabel(p?: number): string {
  const found = PAY_TYPE_OPTIONS.find((o) => o.value === p)
  return found ? found.label : p != null ? String(p) : '--'
}

function flowStatusLabel(s?: number): string {
  const found = FLOW_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/** 流水状态 el-tag：正常 success / 已冲正 danger */
function flowStatusTagType(s?: number): 'success' | 'danger' {
  return s === FlowStatus.NORMAL ? 'success' : 'danger'
}

/** 结算状态展示文字 */
function settledLabel(v?: number): string {
  if (v == null) return '--'
  return v === 1 ? '已结算' : '未结算'
}

/** 结算状态 el-tag：已结算 success / 未结算 info */
function settledTagType(v?: number): 'success' | 'info' {
  return v === 1 ? 'success' : 'info'
}

/**
 * 金额展示：
 * - 收入：绿色 +¥金额
 * - 支出：红色 -¥金额
 * - 退款/结算：按原值展示 ¥金额（退款一般为正数冲减）
 */
function amountLabel(row: FinanceFlow): { text: string; cls: string } {
  if (row.flowAmount == null) return { text: '--', cls: 'amount-zero' }
  const fixed = Math.abs(row.flowAmount).toFixed(2)
  if (row.flowType === FlowType.INCOME) {
    return { text: `+¥${fixed}`, cls: 'amount-income' }
  }
  if (row.flowType === FlowType.EXPENSE) {
    return { text: `-¥${fixed}`, cls: 'amount-expense' }
  }
  return { text: `¥${fixed}`, cls: 'amount-zero' }
}

/** 详情金额展示（带符号规则同上） */
function detailAmountLabel(flow?: FinanceFlow): { text: string; cls: string } {
  if (!flow) return { text: '--', cls: 'amount-zero' }
  return amountLabel(flow)
}

/** 纯金额展示（无符号，用于 balanceBefore/After） */
function plainAmount(v?: number): string {
  if (v == null) return '--'
  return `¥${v.toFixed(2)}`
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.flowCode"
          placeholder="流水编号"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.flowType" placeholder="流水类型" clearable style="width: 130px">
          <el-option v-for="o in FLOW_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input
          v-model="query.bizType"
          placeholder="业务类型 如 equity_order"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.accountType"
          placeholder="账号类型 如 channel"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.accountCode"
          placeholder="账号编码"
          clearable
          style="width: 140px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="流水状态" clearable style="width: 130px">
          <el-option v-for="o in FLOW_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.isSettled" placeholder="结算状态" clearable style="width: 130px">
          <el-option v-for="o in SETTLED_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span class="card-title">资金流水列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">记录流水</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="flowCode">
        <el-table-column prop="flowCode" label="流水编号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="flowType" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="flowTypeTagType(row.flowType)">{{ flowTypeLabel(row.flowType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizType" label="业务类型" min-width="130" show-overflow-tooltip />
        <el-table-column prop="accountCode" label="账号" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.accountCode ?? '--' }}</span>
            <div v-if="row.accountType" class="sub-text">{{ row.accountType }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="flowAmount" label="流水金额" width="140" align="right">
          <template #default="{ row }">
            <span :class="amountLabel(row).cls">{{ amountLabel(row).text }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="payType" label="支付方式" width="110" align="center">
          <template #default="{ row }">
            <span>{{ payTypeLabel(row.payType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="flowTime" label="流水时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.flowTime) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="flowStatusTagType(row.status)">{{ flowStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isSettled" label="结算状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="settledTagType(row.isSettled)" effect="plain">
              {{ settledLabel(row.isSettled) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
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

    <!-- 记录流水弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="记录流水"
      width="860px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="流水类型" prop="flowType">
              <el-select v-model="form.flowType" placeholder="流水类型" style="width: 100%">
                <el-option v-for="o in FLOW_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付方式" prop="payType">
              <el-select v-model="form.payType" placeholder="支付方式" style="width: 100%">
                <el-option v-for="o in PAY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务类型" prop="bizType">
              <el-select v-model="form.bizType" placeholder="选择业务类型" style="width: 100%">
                <el-option v-for="o in BIZ_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务编码">
              <el-input v-model="form.bizCode" placeholder="关联业务单号（可选）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号类型" prop="accountType">
              <el-select v-model="form.accountType" placeholder="选择账号类型" style="width: 100%">
                <el-option v-for="o in ACCOUNT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号编码" prop="accountCode">
              <el-input v-model="form.accountCode" placeholder="账号编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="流水金额" prop="flowAmount">
              <el-input-number
                v-model="form.flowAmount"
                :precision="2"
                :step="100"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="交易流水号">
              <el-input v-model="form.tradeNo" placeholder="第三方交易号（可选）" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="对方类型">
              <el-input v-model="form.counterpartyType" placeholder="如 client" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="对方编码">
              <el-input v-model="form.counterpartyCode" placeholder="对方编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="对方名称">
              <el-input v-model="form.counterpartyName" placeholder="对方名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="流水描述">
              <el-input v-model="form.flowDescription" type="textarea" :rows="2" placeholder="流水描述（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗（只读） -->
    <el-dialog v-model="detailDialogVisible" title="流水详情" width="820px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="流水编号">{{ detail.flowCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="流水类型">
          <el-tag :type="flowTypeTagType(detail.flowType)">{{ flowTypeLabel(detail.flowType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ detail.bizType ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="业务编码">{{ detail.bizCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="账号类型">{{ detail.accountType ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="账号编码">{{ detail.accountCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="流水金额">
          <span :class="detailAmountLabel(detail).cls">{{ detailAmountLabel(detail).text }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ payTypeLabel(detail.payType) }}</el-descriptions-item>
        <el-descriptions-item label="变动前余额">{{ plainAmount(detail.balanceBefore) }}</el-descriptions-item>
        <el-descriptions-item label="变动后余额">{{ plainAmount(detail.balanceAfter) }}</el-descriptions-item>
        <el-descriptions-item label="交易流水号">{{ detail.tradeNo ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="流水时间">{{ formatDateTime(detail.flowTime) }}</el-descriptions-item>
        <el-descriptions-item label="对方类型">{{ detail.counterpartyType ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="对方编码">{{ detail.counterpartyCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="对方名称" :span="2">{{ detail.counterpartyName ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="flowStatusTagType(detail.status)">{{ flowStatusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="结算状态">
          <el-tag :type="settledTagType(detail.isSettled)" effect="plain">{{ settledLabel(detail.isSettled) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="结算单号">{{ detail.settleCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="流水描述" :span="2">{{ detail.flowDescription ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark ?? '--' }}</el-descriptions-item>
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

.sub-text {
  color: #909399;
  font-size: 12px;
  line-height: 1.2;
}

.amount-income {
  color: #67c23a;
  font-weight: 600;
}

.amount-expense {
  color: #f56c6c;
  font-weight: 600;
}

.amount-zero {
  color: #303133;
  font-weight: 600;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}
</style>
