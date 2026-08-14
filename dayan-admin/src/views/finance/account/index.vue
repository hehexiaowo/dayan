<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageAccounts, getAccount, createAccount, receiveAccount } from '@/api/finance-account'
import type { FinanceAccount, FinanceAccountQuery } from '@/types/finance-account'
import {
  AccountDirection,
  AccountStatus,
  ACCOUNT_DIRECTION_OPTIONS,
  ACCOUNT_STATUS_OPTIONS
} from '@/types/finance-account'
import { formatDateTime, formatDate } from '@/utils/format'

/**
 * 应收应付账目（FinanceAccount）管理页。
 *
 * - 搜索 + 表格 + 分页 + 创建账目弹窗 + 收/付款弹窗。
 * - 后端无 update / delete，账目创建后通过 receive 推进 account_status。
 * - 状态约定（accountStatus）：
 *   0 待收付 →[receive remain>0]→ 1 部分收付 →[receive remain≤0]→ 2 已结清；
 *   另有 3 已逾期 / 4 已坏账（人工/系统核销）。
 * - 方向：1 应收 / 2 应付。
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
} = useCrud<FinanceAccount, FinanceAccountQuery>(
  { page: pageAccounts },
  {
    initialQuery: {
      accountCode: '',
      direction: undefined,
      accountType: '',
      targetCode: '',
      bizType: '',
      bizCode: '',
      accountStatus: undefined,
      dueDateTo: ''
    }
  }
)

function handleReset() {
  query.accountCode = ''
  query.direction = undefined
  query.accountType = ''
  query.targetCode = ''
  query.bizType = ''
  query.bizCode = ''
  query.accountStatus = undefined
  query.dueDateTo = ''
  handleSearch()
}

// ---------------- 创建账目弹窗 ----------------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 创建账目表单（对应 CreateAccountDTO，不含 accountCode / received / remain / status） */
const form = reactive({
  direction: AccountDirection.RECEIVABLE,
  accountType: '',
  targetCode: '',
  targetName: '',
  bizType: '',
  bizCode: '',
  totalAmount: 0,
  dueDate: '',
  remark: ''
})

const rules: FormRules<typeof form> = {
  direction: [{ required: true, message: '请选择账目方向', trigger: 'change' }],
  accountType: [{ required: true, message: '请输入对象类型', trigger: 'blur' }],
  targetCode: [{ required: true, message: '请输入对象编码', trigger: 'blur' }],
  targetName: [{ required: true, message: '请输入对象名称', trigger: 'blur' }],
  bizType: [{ required: true, message: '请输入业务类型', trigger: 'blur' }],
  totalAmount: [{ required: true, message: '请输入总额', trigger: 'blur' }]
}

function resetForm() {
  Object.assign(form, {
    direction: AccountDirection.RECEIVABLE,
    accountType: '',
    targetCode: '',
    targetName: '',
    bizType: '',
    bizCode: '',
    totalAmount: 0,
    dueDate: '',
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
    await createAccount({
      direction: form.direction,
      accountType: form.accountType,
      targetCode: form.targetCode,
      targetName: form.targetName,
      bizType: form.bizType,
      bizCode: form.bizCode || undefined,
      totalAmount: form.totalAmount,
      dueDate: form.dueDate || undefined,
      remark: form.remark || undefined
    })
    ElMessage.success('创建账目成功')
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

// ---------------- 收/付款弹窗（receive） ----------------
const receiveDialogVisible = ref(false)
const receiveSubmitLoading = ref(false)
const receiveFormRef = ref<FormInstance>()
const receiveForm = reactive<{
  accountCode: string
  targetName: string
  remainAmount: number | undefined
  amount: number
  receiveTime: string
  remark: string
}>({
  accountCode: '',
  targetName: '',
  remainAmount: undefined,
  amount: 0,
  receiveTime: '',
  remark: ''
})

const receiveRules: FormRules<typeof receiveForm> = {
  amount: [{ required: true, message: '请输入本笔收/付金额', trigger: 'blur' }]
}

function openReceive(row: FinanceAccount) {
  if (!row.accountCode) return
  receiveForm.accountCode = row.accountCode
  receiveForm.targetName = row.targetName ?? ''
  receiveForm.remainAmount = row.remainAmount
  receiveForm.amount = 0
  receiveForm.receiveTime = ''
  receiveForm.remark = ''
  receiveDialogVisible.value = true
}

async function handleReceiveSubmit() {
  if (!receiveFormRef.value) return
  try {
    await receiveFormRef.value.validate()
  } catch {
    return
  }
  receiveSubmitLoading.value = true
  try {
    await receiveAccount({
      accountCode: receiveForm.accountCode,
      amount: receiveForm.amount,
      receiveTime: receiveForm.receiveTime || undefined,
      remark: receiveForm.remark || undefined
    })
    ElMessage.success('收/付款成功')
    receiveDialogVisible.value = false
    loadPage()
  } finally {
    receiveSubmitLoading.value = false
  }
}

// ---------------- 详情弹窗 ----------------
const detailDialogVisible = ref(false)
const detail = ref<FinanceAccount | null>(null)

async function openDetail(row: FinanceAccount) {
  if (!row.accountCode) return
  try {
    detail.value = await getAccount(row.accountCode)
  } catch {
    detail.value = row
  }
  detailDialogVisible.value = true
}

// ---------------- 辅助渲染 ----------------
function directionLabel(d?: number): string {
  const found = ACCOUNT_DIRECTION_OPTIONS.find((o) => o.value === d)
  return found ? found.label : d != null ? String(d) : '--'
}

function directionTagType(d?: number): 'success' | 'danger' {
  return d === AccountDirection.PAYABLE ? 'danger' : 'success'
}

function accountStatusLabel(s?: number): string {
  const found = ACCOUNT_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

function accountStatusTagType(
  s?: number
): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (s) {
    case AccountStatus.SETTLED:
      return 'success'
    case AccountStatus.PARTIAL:
      return 'warning'
    case AccountStatus.OVERDUE:
    case AccountStatus.BAD_DEBT:
      return 'danger'
    case AccountStatus.PENDING:
    default:
      return 'info'
  }
}

/** 是否可继续收/付款：待收付 / 部分收付 */
function canReceive(row: FinanceAccount): boolean {
  return (
    row.accountStatus === AccountStatus.PENDING || row.accountStatus === AccountStatus.PARTIAL
  )
}

/** 金额展示：¥ + toFixed(2) */
function amountLabel(amount?: number): string {
  if (amount == null) return '--'
  return `¥${amount.toFixed(2)}`
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="账目编号">
          <el-input
            v-model="query.accountCode"
            placeholder="账目编号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="账目方向">
          <el-select v-model="query.direction" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="o in ACCOUNT_DIRECTION_OPTIONS"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账目状态">
          <el-select v-model="query.accountStatus" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="o in ACCOUNT_STATUS_OPTIONS"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="对象类型">
          <el-input
            v-model="query.accountType"
            placeholder="channel/supplier/agent"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="对象编码">
          <el-input
            v-model="query.targetCode"
            placeholder="对象编码"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-input
            v-model="query.bizType"
            placeholder="如 equity_purchase"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="业务编码">
          <el-input
            v-model="query.bizCode"
            placeholder="业务编码"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="到期日期≤">
          <el-date-picker
            v-model="query.dueDateTo"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="到期日期"
            style="width: 160px"
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
          <span>应收应付账目列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">创建账目</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="accountCode">
        <el-table-column prop="accountCode" label="账目编号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="direction" label="方向" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="directionTagType(row.direction)">
              {{ directionLabel(row.direction) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="对象" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.targetName ?? '--' }}</span>
            <div class="sub-text">{{ row.accountType ?? '--' }} · {{ row.targetCode ?? '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="bizType" label="业务" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.bizType ?? '--' }}</span>
            <div v-if="row.bizCode" class="sub-text">{{ row.bizCode }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount-text">{{ amountLabel(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="receivedAmount" label="已收/付" width="120" align="right">
          <template #default="{ row }">{{ amountLabel(row.receivedAmount) }}</template>
        </el-table-column>
        <el-table-column prop="remainAmount" label="剩余" width="120" align="right">
          <template #default="{ row }">
            <span :class="row.remainAmount ? 'amount-danger' : ''">
              {{ amountLabel(row.remainAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="到期日期" width="120" align="center">
          <template #default="{ row }">{{ formatDate(row.dueDate) }}</template>
        </el-table-column>
        <el-table-column prop="accountStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="accountStatusTagType(row.accountStatus)">
              {{ accountStatusLabel(row.accountStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastReceiveTime" label="最近收/付时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.lastReceiveTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="canReceive(row)"
              link
              type="success"
              size="small"
              @click="openReceive(row)"
            >
              收/付款
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

    <!-- 创建账目弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="创建账目"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="账目方向" prop="direction">
              <el-select v-model="form.direction" placeholder="账目方向" style="width: 100%">
                <el-option
                  v-for="o in ACCOUNT_DIRECTION_OPTIONS"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总额" prop="totalAmount">
              <el-input-number
                v-model="form.totalAmount"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对象类型" prop="accountType">
              <el-input v-model="form.accountType" placeholder="channel/supplier/agent" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对象编码" prop="targetCode">
              <el-input v-model="form.targetCode" placeholder="对象编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对象名称" prop="targetName">
              <el-input v-model="form.targetName" placeholder="对象名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期">
              <el-date-picker
                v-model="form.dueDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="到期日期（可选）"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务类型" prop="bizType">
              <el-input v-model="form.bizType" placeholder="如 equity_purchase" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务编码">
              <el-input v-model="form.bizCode" placeholder="业务编码（可选）" maxlength="50" />
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
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定创建</el-button>
      </template>
    </el-dialog>

    <!-- 收/付款弹窗 -->
    <el-dialog v-model="receiveDialogVisible" title="账目收/付款" width="520px" :close-on-click-modal="false">
      <el-form ref="receiveFormRef" :model="receiveForm" :rules="receiveRules" label-width="120px">
        <el-form-item label="账目编号">
          <span>{{ receiveForm.accountCode }}</span>
        </el-form-item>
        <el-form-item label="对象">
          <span>{{ receiveForm.targetName }}</span>
        </el-form-item>
        <el-form-item label="剩余应收/应付">
          <span class="amount-text">{{ amountLabel(receiveForm.remainAmount) }}</span>
        </el-form-item>
        <el-form-item label="本笔收/付金额" prop="amount">
          <el-input-number
            v-model="receiveForm.amount"
            :min="0"
            :precision="2"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="收/付款时间">
          <el-date-picker
            v-model="receiveForm.receiveTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="留空则取服务端当前时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="receiveForm.remark"
            type="textarea"
            :rows="3"
            placeholder="备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="receiveSubmitLoading" @click="handleReceiveSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗（只读） -->
    <el-dialog v-model="detailDialogVisible" title="账目详情" width="760px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="账目编号">{{ detail.accountCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="方向">
          <el-tag :type="directionTagType(detail.direction)">
            {{ directionLabel(detail.direction) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="对象类型">{{ detail.accountType ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="对象编码">{{ detail.targetCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="对象名称" :span="2">{{ detail.targetName ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ detail.bizType ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="业务编码">{{ detail.bizCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="总额">
          <span class="amount-text">{{ amountLabel(detail.totalAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="已收/付">{{ amountLabel(detail.receivedAmount) }}</el-descriptions-item>
        <el-descriptions-item label="剩余" :span="2">
          <span class="amount-text">{{ amountLabel(detail.remainAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ formatDate(detail.dueDate) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="accountStatusTagType(detail.accountStatus)">
            {{ accountStatusLabel(detail.accountStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近收/付时间">
          {{ formatDateTime(detail.lastReceiveTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
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

.sub-text {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.amount-text {
  color: #e6a23c;
  font-weight: 600;
}

.amount-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
