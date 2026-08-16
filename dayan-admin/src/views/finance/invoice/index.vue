<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  pageInvoices,
  getInvoice,
  applyInvoice,
  auditInvoice,
  issueInvoice,
  sendInvoice,
  finishInvoice,
  voidInvoice,
  redFlushInvoice
} from '@/api/finance-invoice'
import type { FinanceInvoice, FinanceInvoiceQuery } from '@/types/finance'
import {
  InvoiceType,
  TitleType,
  InvoiceStatus,
  INVOICE_TYPE_OPTIONS,
  TITLE_TYPE_OPTIONS,
  INVOICE_STATUS_OPTIONS
} from '@/types/finance'
import { formatDateTime, formatMoney } from '@/utils/format'
import FileUploader from '@/components/FileUploader/index.vue'

/**
 * 发票（FinanceInvoice）管理页。
 *
 * - 搜索 + 表格 + 分页 + 申请开票弹窗 + 开票/审核/寄出弹窗。
 * - 状态机（invoiceStatus，7 态）：
 *   0 待审核 →[audit]→ 1 已审核 →[issue]→ 2 已开票 →[send]→ 3 已寄出 →[finish]→ 4 已完成
 *   任意 →[void]→ 5 已作废；已开票及之后 →[red-flush]→ 6 已红冲。
 *   操作按钮按 invoiceStatus 动态显示。
 */

// 申请方类型下拉选项（ApplyInvoiceDTO.applicantType：channel/agent/client）
const APPLICANT_TYPE_OPTIONS = [
  { label: '渠道（channel）', value: 'channel' },
  { label: '代理（agent）', value: 'agent' },
  { label: '客户（client）', value: 'client' }
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
} = useCrud<FinanceInvoice, FinanceInvoiceQuery>(
  { page: pageInvoices },
  {
    initialQuery: {
      invoiceCode: '',
      invoiceType: undefined,
      invoiceStatus: undefined,
      applicantType: '',
      invoiceNo: ''
    }
  }
)

function handleReset() {
  query.invoiceCode = ''
  query.invoiceType = undefined
  query.invoiceStatus = undefined
  query.applicantType = ''
  query.invoiceNo = ''
  handleSearch()
}

// ---------------- 申请开票弹窗 ----------------
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

/** 申请开票表单（对应 ApplyInvoiceDTO，不含 invoiceCode / applyTime / issueTime / sendTime / invoiceStatus） */
const form = reactive({
  invoiceType: InvoiceType.GENERAL,
  billCode: '',
  orderCode: '',
  applicantType: 'channel',
  applicantCode: '',
  applicantName: '',
  titleType: TitleType.ENTERPRISE,
  invoiceTitle: '',
  taxNo: '',
  bankName: '',
  bankAccount: '',
  registerAddress: '',
  registerPhone: '',
  invoiceAmount: 0,
  invoiceContent: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  receiverEmail: '',
  remark: ''
})

/** 企业抬头 / 电子发票时部分字段建议必填，故 rules 动态计算 */
const isEnterprise = computed(() => form.titleType === TitleType.ENTERPRISE)
const isElectronic = computed(() => form.invoiceType === InvoiceType.ELECTRONIC)

const rules = computed<FormRules<typeof form>>(() => ({
  invoiceType: [{ required: true, message: '请选择发票类型', trigger: 'change' }],
  applicantType: [{ required: true, message: '请选择申请方类型', trigger: 'change' }],
  applicantCode: [{ required: true, message: '请输入申请方编码', trigger: 'blur' }],
  applicantName: [{ required: true, message: '请输入申请方名称', trigger: 'blur' }],
  invoiceTitle: [{ required: true, message: '请输入发票抬头', trigger: 'blur' }],
  // 企业必填纳税人识别号
  taxNo: isEnterprise.value
    ? [{ required: true, message: '企业抬头请填写纳税人识别号', trigger: 'blur' }]
    : [],
  invoiceAmount: [{ required: true, message: '请输入开票金额', trigger: 'blur' }],
  invoiceContent: [{ required: true, message: '请输入发票内容', trigger: 'blur' }],
  // 电子发票建议必填邮箱
  receiverEmail: isElectronic.value
    ? [
        {
          required: true,
          message: '电子发票请填写收件邮箱',
          trigger: 'blur'
        },
        { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
      ]
    : []
}))

function resetForm() {
  Object.assign(form, {
    invoiceType: InvoiceType.GENERAL,
    billCode: '',
    orderCode: '',
    applicantType: 'channel',
    applicantCode: '',
    applicantName: '',
    titleType: TitleType.ENTERPRISE,
    invoiceTitle: '',
    taxNo: '',
    bankName: '',
    bankAccount: '',
    registerAddress: '',
    registerPhone: '',
    invoiceAmount: 0,
    invoiceContent: '',
    receiverName: '',
    receiverPhone: '',
    receiverAddress: '',
    receiverEmail: '',
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
    await applyInvoice({
      invoiceType: form.invoiceType,
      billCode: form.billCode || undefined,
      orderCode: form.orderCode || undefined,
      applicantType: form.applicantType,
      applicantCode: form.applicantCode,
      applicantName: form.applicantName,
      titleType: form.titleType,
      invoiceTitle: form.invoiceTitle,
      taxNo: form.taxNo || undefined,
      bankName: form.bankName || undefined,
      bankAccount: form.bankAccount || undefined,
      registerAddress: form.registerAddress || undefined,
      registerPhone: form.registerPhone || undefined,
      invoiceAmount: form.invoiceAmount,
      invoiceContent: form.invoiceContent,
      receiverName: form.receiverName || undefined,
      receiverPhone: form.receiverPhone || undefined,
      receiverAddress: form.receiverAddress || undefined,
      receiverEmail: form.receiverEmail || undefined,
      remark: form.remark || undefined
    })
    ElMessage.success('申请开票成功')
    dialogVisible.value = false
    loadPage()
  } finally {
    submitLoading.value = false
  }
}

// ---------------- 审核弹窗（audit：0 → 1） ----------------
const auditDialogVisible = ref(false)
const auditSubmitLoading = ref(false)
const auditForm = reactive<{ invoiceCode: string; invoiceTitle: string; remark: string }>({
  invoiceCode: '',
  invoiceTitle: '',
  remark: ''
})

function openAudit(row: FinanceInvoice) {
  if (!row.invoiceCode) return
  auditForm.invoiceCode = row.invoiceCode
  auditForm.invoiceTitle = row.invoiceTitle ?? ''
  auditForm.remark = ''
  auditDialogVisible.value = true
}

async function handleAuditSubmit() {
  auditSubmitLoading.value = true
  try {
    await auditInvoice({
      invoiceCode: auditForm.invoiceCode,
      remark: auditForm.remark || undefined
    })
    ElMessage.success('已审核')
    auditDialogVisible.value = false
    loadPage()
  } finally {
    auditSubmitLoading.value = false
  }
}

// ---------------- 开票弹窗（issue：1 → 2） ----------------
const issueDialogVisible = ref(false)
const issueSubmitLoading = ref(false)
const issueFormRef = ref<FormInstance>()
const issueForm = reactive<{
  invoiceCode: string
  invoiceTitle: string
  invoiceNo: string
  invoiceUrl: string
  remark: string
}>({
  invoiceCode: '',
  invoiceTitle: '',
  invoiceNo: '',
  invoiceUrl: '',
  remark: ''
})

const issueRules: FormRules<typeof issueForm> = {
  invoiceNo: [{ required: true, message: '请输入税务发票号码', trigger: 'blur' }]
}

function openIssue(row: FinanceInvoice) {
  if (!row.invoiceCode) return
  issueForm.invoiceCode = row.invoiceCode
  issueForm.invoiceTitle = row.invoiceTitle ?? ''
  issueForm.invoiceNo = ''
  issueForm.invoiceUrl = ''
  issueForm.remark = ''
  issueDialogVisible.value = true
}

async function handleIssueSubmit() {
  if (!issueFormRef.value) return
  try {
    await issueFormRef.value.validate()
  } catch {
    return
  }
  issueSubmitLoading.value = true
  try {
    await issueInvoice({
      invoiceCode: issueForm.invoiceCode,
      invoiceNo: issueForm.invoiceNo,
      invoiceUrl: issueForm.invoiceUrl || undefined,
      remark: issueForm.remark || undefined
    })
    ElMessage.success('已开票')
    issueDialogVisible.value = false
    loadPage()
  } finally {
    issueSubmitLoading.value = false
  }
}

// ---------------- 寄出弹窗（send：2 → 3） ----------------
const sendDialogVisible = ref(false)
const sendSubmitLoading = ref(false)
const sendForm = reactive<{ invoiceCode: string; invoiceTitle: string; remark: string }>({
  invoiceCode: '',
  invoiceTitle: '',
  remark: ''
})

function openSend(row: FinanceInvoice) {
  if (!row.invoiceCode) return
  sendForm.invoiceCode = row.invoiceCode
  sendForm.invoiceTitle = row.invoiceTitle ?? ''
  sendForm.remark = ''
  sendDialogVisible.value = true
}

async function handleSendSubmit() {
  sendSubmitLoading.value = true
  try {
    await sendInvoice({
      invoiceCode: sendForm.invoiceCode,
      remark: sendForm.remark || undefined
    })
    ElMessage.success('已寄出')
    sendDialogVisible.value = false
    loadPage()
  } finally {
    sendSubmitLoading.value = false
  }
}

// ---------------- 完成 / 作废 / 红冲（纯确认） ----------------
async function handleFinish(row: FinanceInvoice) {
  if (!row.invoiceCode) return
  await ElMessageBox.confirm(
    `确定完成发票「${row.invoiceTitle ?? row.invoiceCode}」吗？（状态：已寄出 → 已完成）`,
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await finishInvoice({ invoiceCode: row.invoiceCode })
  ElMessage.success('已完成')
  loadPage()
}

async function handleVoid(row: FinanceInvoice) {
  if (!row.invoiceCode) return
  await ElMessageBox.confirm(
    `确定作废发票「${row.invoiceTitle ?? row.invoiceCode}」吗？作废后不可恢复。`,
    '作废确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await voidInvoice({ invoiceCode: row.invoiceCode })
  ElMessage.success('已作废')
  loadPage()
}

async function handleRedFlush(row: FinanceInvoice) {
  if (!row.invoiceCode) return
  await ElMessageBox.confirm(
    `确定对发票「${row.invoiceTitle ?? row.invoiceCode}」进行红冲吗？红冲后不可恢复。`,
    '红冲确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await redFlushInvoice({ invoiceCode: row.invoiceCode })
  ElMessage.success('已红冲')
  loadPage()
}

// ---------------- 详情弹窗 ----------------
const detailDialogVisible = ref(false)
const detail = ref<FinanceInvoice | null>(null)

async function openDetail(row: FinanceInvoice) {
  if (!row.invoiceCode) return
  try {
    detail.value = await getInvoice(row.invoiceCode)
  } catch {
    detail.value = row
  }
  detailDialogVisible.value = true
}

// ---------------- 辅助渲染 ----------------
function invoiceTypeLabel(t?: number): string {
  const found = INVOICE_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function invoiceStatusLabel(s?: number): string {
  const found = INVOICE_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

function titleTypeLabel(t?: number): string {
  const found = TITLE_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function applicantTypeLabel(t?: string): string {
  if (!t) return '--'
  const found = APPLICANT_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t
}

/**
 * 根据发票状态返回 el-tag type：
 * 待审核 info / 已审核 primary / 已开票 success / 已寄出 warning
 * 已完成 success / 已作废 danger / 已红冲 danger。
 */
function invoiceStatusTagType(
  status?: number
): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (status) {
    case InvoiceStatus.AUDITED:
      return 'primary'
    case InvoiceStatus.ISSUED:
      return 'success'
    case InvoiceStatus.SENT:
      return 'warning'
    case InvoiceStatus.FINISHED:
      return 'success'
    case InvoiceStatus.VOID:
    case InvoiceStatus.RED_FLUSH:
      return 'danger'
    case InvoiceStatus.PENDING_AUDIT:
    default:
      return 'info'
  }
}

/** 金额展示：千分位 + ¥ 前缀（统一走 formatMoney） */
function amountLabel(amount?: number): string {
  return formatMoney(amount)
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
          v-model="query.invoiceCode"
          placeholder="发票编码"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.invoiceType" placeholder="发票类型" clearable style="width: 160px">
          <el-option v-for="o in INVOICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.invoiceStatus" placeholder="发票状态" clearable style="width: 130px">
          <el-option
            v-for="o in INVOICE_STATUS_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
        <el-select v-model="query.applicantType" placeholder="申请方类型" clearable style="width: 140px">
          <el-option v-for="o in APPLICANT_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input
          v-model="query.invoiceNo"
          placeholder="税务发票号码"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
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
          <span class="card-title">发票列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreate">申请开票</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="invoiceCode">
        <el-table-column prop="invoiceCode" label="发票编码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="invoiceType" label="类型" width="140" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ invoiceTypeLabel(row.invoiceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceTitle" label="发票抬头" min-width="180" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申请方" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.applicantName ?? '--' }}</span>
            <div class="sub-text">{{ applicantTypeLabel(row.applicantType) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceAmount" label="开票金额" width="130" align="right">
          <template #default="{ row }">
            <span class="amount-text">{{ amountLabel(row.invoiceAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceNo" label="发票号码" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.invoiceNo ?? '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="invoiceStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="invoiceStatusTagType(row.invoiceStatus)">
              {{ invoiceStatusLabel(row.invoiceStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.invoiceStatus === InvoiceStatus.PENDING_AUDIT"
              link
              type="warning"
              size="small"
              @click="openAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.invoiceStatus === InvoiceStatus.AUDITED"
              link
              type="success"
              size="small"
              @click="openIssue(row)"
            >
              开票
            </el-button>
            <el-button
              v-if="row.invoiceStatus === InvoiceStatus.ISSUED"
              link
              type="primary"
              size="small"
              @click="openSend(row)"
            >
              寄出
            </el-button>
            <el-button
              v-if="row.invoiceStatus === InvoiceStatus.SENT"
              link
              type="success"
              size="small"
              @click="handleFinish(row)"
            >
              完成
            </el-button>
            <el-button
              v-if="row.invoiceStatus !== InvoiceStatus.VOID && row.invoiceStatus !== InvoiceStatus.RED_FLUSH && row.invoiceStatus !== InvoiceStatus.FINISHED"
              link
              type="danger"
              size="small"
              @click="handleVoid(row)"
            >
              作废
            </el-button>
            <el-button
              v-if="
                row.invoiceStatus === InvoiceStatus.ISSUED ||
                row.invoiceStatus === InvoiceStatus.SENT ||
                row.invoiceStatus === InvoiceStatus.FINISHED
              "
              link
              type="danger"
              size="small"
              @click="handleRedFlush(row)"
            >
              红冲
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

    <!-- 申请开票弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="申请开票"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="发票类型" prop="invoiceType">
              <el-select v-model="form.invoiceType" placeholder="发票类型" style="width: 100%">
                <el-option
                  v-for="o in INVOICE_TYPE_OPTIONS"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="抬头类型" prop="titleType">
              <el-radio-group v-model="form.titleType">
                <el-radio v-for="o in TITLE_TYPE_OPTIONS" :key="o.value" :value="o.value">
                  {{ o.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开票金额" prop="invoiceAmount">
              <el-input-number
                v-model="form.invoiceAmount"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发票抬头" prop="invoiceTitle">
              <el-input v-model="form.invoiceTitle" placeholder="发票抬头" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col v-if="isEnterprise" :span="12">
            <el-form-item label="纳税人识别号" prop="taxNo">
              <el-input v-model="form.taxNo" placeholder="企业必填" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="isEnterprise ? 12 : 24">
            <el-form-item label="发票内容" prop="invoiceContent">
              <el-input v-model="form.invoiceContent" placeholder="如：养老服务费" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="申请方类型" prop="applicantType">
              <el-select v-model="form.applicantType" placeholder="申请方类型" style="width: 100%">
                <el-option
                  v-for="o in APPLICANT_TYPE_OPTIONS"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="申请方编码" prop="applicantCode">
              <el-input v-model="form.applicantCode" placeholder="申请方编码" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="申请方名称" prop="applicantName">
              <el-input v-model="form.applicantName" placeholder="申请方名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联结算单">
              <el-input v-model="form.billCode" placeholder="结算单编码（可选）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联订单">
              <el-input v-model="form.orderCode" placeholder="订单编码（可选）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col v-if="isEnterprise" :span="12">
            <el-form-item label="开户银行">
              <el-input v-model="form.bankName" placeholder="开户银行（可选）" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col v-if="isEnterprise" :span="12">
            <el-form-item label="银行账号">
              <el-input v-model="form.bankAccount" placeholder="银行账号（可选）" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col v-if="isEnterprise" :span="12">
            <el-form-item label="注册地址">
              <el-input v-model="form.registerAddress" placeholder="注册地址（可选）" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col v-if="isEnterprise" :span="12">
            <el-form-item label="注册电话">
              <el-input v-model="form.registerPhone" placeholder="注册电话（可选）" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收件人">
              <el-input v-model="form.receiverName" placeholder="收件人姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收件电话">
              <el-input v-model="form.receiverPhone" placeholder="收件人电话" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收件邮箱" prop="receiverEmail">
              <el-input
                v-model="form.receiverEmail"
                :placeholder="isElectronic ? '电子发票必填' : '收件邮箱（可选）'"
                maxlength="100"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="收件地址">
              <el-input v-model="form.receiverAddress" placeholder="收件地址（纸质发票寄送地址）" maxlength="200" />
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
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定申请</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="发票审核" width="520px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="发票编码">
          <span>{{ auditForm.invoiceCode }}</span>
        </el-form-item>
        <el-form-item label="发票抬头">
          <span>{{ auditForm.invoiceTitle }}</span>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="3"
            placeholder="审核备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitLoading" @click="handleAuditSubmit">
          确定审核
        </el-button>
      </template>
    </el-dialog>

    <!-- 开票弹窗 -->
    <el-dialog v-model="issueDialogVisible" title="开票" width="560px" :close-on-click-modal="false">
      <el-form ref="issueFormRef" :model="issueForm" :rules="issueRules" label-width="100px">
        <el-form-item label="发票编码">
          <span>{{ issueForm.invoiceCode }}</span>
        </el-form-item>
        <el-form-item label="发票抬头">
          <span>{{ issueForm.invoiceTitle }}</span>
        </el-form-item>
        <el-form-item label="发票号码" prop="invoiceNo">
          <el-input v-model="issueForm.invoiceNo" placeholder="税务发票号码" maxlength="50" />
        </el-form-item>
        <el-form-item label="发票文件URL">
          <FileUploader v-model="issueForm.invoiceUrl" type="file" module="finance" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="issueForm.remark"
            type="textarea"
            :rows="3"
            placeholder="备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="issueSubmitLoading" @click="handleIssueSubmit">
          确定开票
        </el-button>
      </template>
    </el-dialog>

    <!-- 寄出弹窗 -->
    <el-dialog v-model="sendDialogVisible" title="发票寄出" width="520px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="发票编码">
          <span>{{ sendForm.invoiceCode }}</span>
        </el-form-item>
        <el-form-item label="发票抬头">
          <span>{{ sendForm.invoiceTitle }}</span>
        </el-form-item>
        <el-form-item label="寄出备注">
          <el-input
            v-model="sendForm.remark"
            type="textarea"
            :rows="3"
            placeholder="如快递单号等（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendSubmitLoading" @click="handleSendSubmit">
          确定寄出
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗（只读） -->
    <el-dialog v-model="detailDialogVisible" title="发票详情" width="820px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="发票编码">{{ detail.invoiceCode ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="发票类型">
          {{ invoiceTypeLabel(detail.invoiceType) }}
        </el-descriptions-item>
        <el-descriptions-item label="抬头类型">
          {{ titleTypeLabel(detail.titleType) }}
        </el-descriptions-item>
        <el-descriptions-item label="发票状态">
          <el-tag :type="invoiceStatusTagType(detail.invoiceStatus)">
            {{ invoiceStatusLabel(detail.invoiceStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发票抬头" :span="2">
          {{ detail.invoiceTitle ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="纳税人识别号">
          {{ detail.taxNo ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="开票金额">
          <span class="amount-text">{{ amountLabel(detail.invoiceAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="发票内容" :span="2">
          {{ detail.invoiceContent ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="申请方类型">
          {{ applicantTypeLabel(detail.applicantType) }}
        </el-descriptions-item>
        <el-descriptions-item label="申请方编码">
          {{ detail.applicantCode ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="申请方名称" :span="2">
          {{ detail.applicantName ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="关联结算单">
          {{ detail.billCode ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="关联订单">
          {{ detail.orderCode ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="开户银行">
          {{ detail.bankName ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="银行账号">
          {{ detail.bankAccount ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="注册地址" :span="2">
          {{ detail.registerAddress ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="注册电话">
          {{ detail.registerPhone ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="发票号码">
          {{ detail.invoiceNo ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="发票文件URL" :span="2">
          {{ detail.invoiceUrl ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="收件人">
          {{ detail.receiverName ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="收件电话">
          {{ detail.receiverPhone ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="收件邮箱" :span="2">
          {{ detail.receiverEmail ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="收件地址" :span="2">
          {{ detail.receiverAddress ?? '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">
          {{ formatDateTime(detail.applyTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="开票时间">
          {{ formatDateTime(detail.issueTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="寄出时间">
          {{ formatDateTime(detail.sendTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
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

.amount-text {
  color: #e6a23c;
  font-weight: 600;
}

.sub-text {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}
</style>
