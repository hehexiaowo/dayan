<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { applyFinanceInvoice, getFinanceInvoice, pageFinanceInvoices } from '@/api/finance'
import { formatDateTime, formatMoney } from '@/utils/format'
import {
  INVOICE_STATUS_OPTIONS,
  INVOICE_TYPE_OPTIONS,
  TITLE_TYPE_OPTIONS,
  TitleType,
  type ApplyInvoiceData,
  type FinanceInvoice,
  type FinanceInvoiceQuery
} from '@/types/finance'

/**
 * 发票管理页（采购结算目录 - 渠道端申请发票）。
 *
 * - 列表：useCrud 只读 page（不传 create）；
 * - 申请发票弹窗：独立状态，提交调 applyFinanceInvoice，成功后刷新列表；
 * - 详情：el-dialog + el-descriptions 结构化弹窗，调 getFinanceInvoice
 *   拉取完整 VO 后中文化展示全字段（对齐 order-manage 详情模式）。
 *
 * 防越权说明：
 * - applicantCode / applicantType / applicantName 由后端 ChannelInvoiceController 强制注入
 *   （= 当前 channelCode / "channel" / channelName），前端表单不包含这三项；
 * - 列表 / 详情接口在后端按 applicantCode 归属过滤。
 */

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<FinanceInvoice, FinanceInvoiceQuery>(
    { page: pageFinanceInvoices },
    {
      initialQuery: {
        invoiceCode: '',
        invoiceType: undefined,
        invoiceStatus: undefined
      }
    }
  )

function handleReset() {
  query.invoiceCode = ''
  query.invoiceType = undefined
  query.invoiceStatus = undefined
  handleSearch()
}

/** 发票类型 tag 颜色：1普票=info / 2专票=warning / 3电子=success */
function invoiceTypeTagType(v?: number): 'info' | 'warning' | 'success' | 'primary' | 'danger' {
  switch (v) {
    case 1:
      return 'info'
    case 2:
      return 'warning'
    case 3:
      return 'success'
    default:
      return 'info'
  }
}

/** 发票类型文案 */
function invoiceTypeText(v?: number): string {
  const opt = INVOICE_TYPE_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

/**
 * 发票状态 tag 颜色（对齐 admin）：0待审核=info / 1已审核=primary / 2已开票=success /
 * 3已寄出=warning / 4已完成=success / 5已作废=danger / 6已红冲=danger。
 */
function invoiceStatusTagType(v?: number): 'info' | 'warning' | 'success' | 'primary' | 'danger' {
  switch (v) {
    case 0:
      return 'info'
    case 1:
      return 'primary'
    case 2:
      return 'success'
    case 3:
      return 'warning'
    case 4:
      return 'success'
    case 5:
    case 6:
      return 'danger'
    default:
      return 'info'
  }
}

/** 发票状态文案 */
function invoiceStatusText(v?: number): string {
  const opt = INVOICE_STATUS_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

onMounted(() => {
  loadPage().catch((err) => {
    console.warn('[invoice] 加载发票列表失败（接口可能未实现）:', err)
  })
})

// ==================== 查看详情（el-dialog + el-descriptions） ====================

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<FinanceInvoice | null>(null)

/** 抬头类型文案 */
function titleTypeText(v?: number): string {
  const opt = TITLE_TYPE_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

/** 打开详情弹窗：调 getFinanceInvoice 拉取完整 VO 后结构化展示 */
async function viewDetail(invoiceCode?: string) {
  if (!invoiceCode) return
  detailVisible.value = true
  detailLoading.value = true
  currentDetail.value = null
  try {
    currentDetail.value = await getFinanceInvoice(invoiceCode)
  } catch {
    // 接口报错由响应拦截器统一提示；关闭弹窗
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

// ==================== 申请发票弹窗 ====================

const applyDialogVisible = ref(false)
const submitting = ref(false)
const applyFormRef = ref<FormInstance>()

interface ApplyForm {
  /** 发票类型：1普票/2专票/3电子 */
  invoiceType: number | undefined
  /** 抬头类型：1企业/2个人 */
  titleType: number
  /** 发票抬头 */
  invoiceTitle: string
  /** 纳税人识别号（企业必填） */
  taxNo: string
  /** 开户银行 */
  bankName: string
  /** 银行账号 */
  bankAccount: string
  /** 注册地址 */
  registerAddress: string
  /** 注册电话 */
  registerPhone: string
  /** 开票金额 */
  invoiceAmount: number
  /** 发票内容 */
  invoiceContent: string
  /** 收件人姓名 */
  receiverName: string
  /** 收件人电话 */
  receiverPhone: string
  /** 收件地址 */
  receiverAddress: string
  /** 收件邮箱（电子发票必填） */
  receiverEmail: string
  /** 备注 */
  remark: string
}

function makeEmptyForm(): ApplyForm {
  return {
    invoiceType: 1,
    titleType: TitleType.ENTERPRISE,
    invoiceTitle: '',
    taxNo: '',
    bankName: '',
    bankAccount: '',
    registerAddress: '',
    registerPhone: '',
    invoiceAmount: 0,
    invoiceContent: '养老服务费',
    receiverName: '',
    receiverPhone: '',
    receiverAddress: '',
    receiverEmail: '',
    remark: ''
  }
}

const applyForm = reactive<ApplyForm>(makeEmptyForm())

/** 动态校验：企业（titleType=1）taxNo 必填；电子发票（invoiceType=3）receiverEmail 必填 */
const applyRules = computed<FormRules<ApplyForm>>(() => ({
  invoiceType: [{ required: true, message: '请选择发票类型', trigger: 'change' }],
  titleType: [{ required: true, message: '请选择抬头类型', trigger: 'change' }],
  invoiceTitle: [{ required: true, message: '请输入发票抬头', trigger: 'blur' }],
  taxNo: [
    {
      validator: (_rule, value: string, callback) => {
        if (applyForm.titleType === TitleType.ENTERPRISE && (!value || !String(value).trim())) {
          callback(new Error('企业发票请填写纳税人识别号'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  invoiceAmount: [
    { required: true, message: '请输入开票金额', trigger: 'blur' },
    {
      validator: (_rule, value: number, callback) => {
        if (value == null || isNaN(value) || value < 0.01) {
          callback(new Error('开票金额须不小于 0.01 元'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  invoiceContent: [{ required: true, message: '请输入发票内容', trigger: 'blur' }],
  receiverEmail: [
    {
      validator: (_rule, value: string, callback) => {
        if (applyForm.invoiceType === 3 && (!value || !String(value).trim())) {
          callback(new Error('电子发票请填写收件邮箱'))
        } else if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value).trim())) {
          callback(new Error('邮箱格式不正确'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}))

/** 重置申请表单到默认值 */
function resetApplyForm() {
  Object.assign(applyForm, makeEmptyForm())
  applyFormRef.value?.clearValidate()
}

/** 打开申请弹窗 */
function openApplyDialog() {
  resetApplyForm()
  applyDialogVisible.value = true
}

/** 关闭申请弹窗 */
function closeApplyDialog() {
  applyDialogVisible.value = false
}

/** 提交申请发票 */
async function handleSubmitApply() {
  if (!applyFormRef.value) return
  try {
    await applyFormRef.value.validate()
  } catch {
    return
  }

  // 组装 ApplyInvoiceData，过滤空字符串 → undefined（避免传空串触发后端校验歧义）
  const trim = (s: string): string => (s ? s.trim() : '')
  const data: ApplyInvoiceData = {
    invoiceType: applyForm.invoiceType as number,
    titleType: applyForm.titleType,
    invoiceTitle: trim(applyForm.invoiceTitle),
    invoiceAmount: applyForm.invoiceAmount,
    invoiceContent: trim(applyForm.invoiceContent) || '养老服务费',
    taxNo: trim(applyForm.taxNo) || undefined,
    bankName: trim(applyForm.bankName) || undefined,
    bankAccount: trim(applyForm.bankAccount) || undefined,
    registerAddress: trim(applyForm.registerAddress) || undefined,
    registerPhone: trim(applyForm.registerPhone) || undefined,
    receiverName: trim(applyForm.receiverName) || undefined,
    receiverPhone: trim(applyForm.receiverPhone) || undefined,
    receiverAddress: trim(applyForm.receiverAddress) || undefined,
    receiverEmail: trim(applyForm.receiverEmail) || undefined,
    remark: trim(applyForm.remark) || undefined
  }

  submitting.value = true
  try {
    const invoiceCode = await applyFinanceInvoice(data)
    ElMessage.success(`发票申请已提交：${invoiceCode}`)
    closeApplyDialog()
    // 刷新列表以反映新增记录（默认按创建时间倒序，新发票应在首行）
    handleSearch()
  } catch {
    /* 拦截器已处理 */
  } finally {
    submitting.value = false
  }
}
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
        <el-select v-model="query.invoiceType" placeholder="发票类型" clearable style="width: 140px">
          <el-option v-for="o in INVOICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.invoiceStatus" placeholder="发票状态" clearable style="width: 140px">
          <el-option v-for="o in INVOICE_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span class="card-title">发票列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openApplyDialog">申请发票</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="invoiceCode">
        <el-table-column prop="invoiceCode" label="发票编码" min-width="150" show-overflow-tooltip />
        <el-table-column prop="invoiceType" label="类型" width="140" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.invoiceType !== undefined && row.invoiceType !== null"
              :type="invoiceTypeTagType(row.invoiceType)"
            >
              {{ invoiceTypeText(row.invoiceType) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceTitle" label="发票抬头" min-width="180" show-overflow-tooltip />
        <el-table-column prop="invoiceNo" label="发票号码" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.invoiceNo || '--' }}</template>
        </el-table-column>
        <el-table-column prop="invoiceAmount" label="金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.invoiceAmount) }}</template>
        </el-table-column>
        <el-table-column prop="invoiceStatus" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.invoiceStatus !== undefined && row.invoiceStatus !== null"
              :type="invoiceStatusTagType(row.invoiceStatus)"
            >
              {{ invoiceStatusText(row.invoiceStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" min-width="160" align="center">
          <template #default="{ row }">{{ formatDateTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column prop="sendTime" label="寄出时间" min-width="160" align="center">
          <template #default="{ row }">{{ formatDateTime(row.sendTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row.invoiceCode)">查看详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
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

    <!-- 申请发票弹窗 -->
    <el-dialog
      v-model="applyDialogVisible"
      title="申请发票"
      width="640px"
      :close-on-click-modal="false"
      @closed="resetApplyForm"
    >
      <el-form
        ref="applyFormRef"
        :model="applyForm"
        :rules="applyRules"
        label-width="110px"
        @submit.prevent
      >
        <el-form-item label="发票类型" prop="invoiceType">
          <el-select v-model="applyForm.invoiceType" placeholder="请选择" style="width: 200px">
            <el-option v-for="o in INVOICE_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="抬头类型" prop="titleType">
          <el-radio-group v-model="applyForm.titleType">
            <el-radio v-for="o in TITLE_TYPE_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发票抬头" prop="invoiceTitle">
          <el-input v-model="applyForm.invoiceTitle" placeholder="企业全称 / 个人姓名" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="纳税人识别号" prop="taxNo">
          <el-input
            v-model="applyForm.taxNo"
            :placeholder="applyForm.titleType === 1 ? '企业必填（统一社会信用代码）' : '选填'"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item label="开户银行">
          <el-input v-model="applyForm.bankName" placeholder="选填" maxlength="60" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="applyForm.bankAccount" placeholder="选填" maxlength="40" />
        </el-form-item>
        <el-form-item label="注册地址">
          <el-input v-model="applyForm.registerAddress" placeholder="选填" maxlength="120" />
        </el-form-item>
        <el-form-item label="注册电话">
          <el-input v-model="applyForm.registerPhone" placeholder="选填" maxlength="30" />
        </el-form-item>
        <el-form-item label="开票金额" prop="invoiceAmount">
          <el-input-number
            v-model="applyForm.invoiceAmount"
            :min="0.01"
            :precision="2"
            :step="100"
            controls-position="right"
            style="width: 200px"
          />
          <span class="form-tip">元</span>
        </el-form-item>
        <el-form-item label="发票内容" prop="invoiceContent">
          <el-input v-model="applyForm.invoiceContent" placeholder="默认：养老服务费" maxlength="80" />
        </el-form-item>
        <el-form-item label="收件人姓名">
          <el-input v-model="applyForm.receiverName" placeholder="选填" maxlength="30" />
        </el-form-item>
        <el-form-item label="收件人电话">
          <el-input v-model="applyForm.receiverPhone" placeholder="选填" maxlength="20" />
        </el-form-item>
        <el-form-item label="收件地址">
          <el-input v-model="applyForm.receiverAddress" placeholder="选填" maxlength="200" />
        </el-form-item>
        <el-form-item label="收件邮箱" prop="receiverEmail">
          <el-input
            v-model="applyForm.receiverEmail"
            :placeholder="applyForm.invoiceType === 3 ? '电子发票必填' : '选填'"
            maxlength="80"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="applyForm.remark"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeApplyDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="发票详情" width="720px">
      <div v-loading="detailLoading">
        <el-descriptions v-if="currentDetail" :column="2" border>
          <el-descriptions-item label="发票编码">{{ currentDetail.invoiceCode || '--' }}</el-descriptions-item>
          <el-descriptions-item label="发票状态">
            <el-tag
              v-if="currentDetail.invoiceStatus !== undefined && currentDetail.invoiceStatus !== null"
              :type="invoiceStatusTagType(currentDetail.invoiceStatus)"
            >
              {{ invoiceStatusText(currentDetail.invoiceStatus) }}
            </el-tag>
            <span v-else>--</span>
          </el-descriptions-item>
          <el-descriptions-item label="发票类型">
            <el-tag
              v-if="currentDetail.invoiceType !== undefined && currentDetail.invoiceType !== null"
              :type="invoiceTypeTagType(currentDetail.invoiceType)"
            >
              {{ invoiceTypeText(currentDetail.invoiceType) }}
            </el-tag>
            <span v-else>--</span>
          </el-descriptions-item>
          <el-descriptions-item label="抬头类型">{{ titleTypeText(currentDetail.titleType) }}</el-descriptions-item>
          <el-descriptions-item label="发票抬头" :span="2">{{ currentDetail.invoiceTitle || '--' }}</el-descriptions-item>
          <el-descriptions-item label="纳税人识别号">{{ currentDetail.taxNo || '--' }}</el-descriptions-item>
          <el-descriptions-item label="开票金额">{{ formatMoney(currentDetail.invoiceAmount) }}</el-descriptions-item>
          <el-descriptions-item label="发票内容">{{ currentDetail.invoiceContent || '--' }}</el-descriptions-item>
          <el-descriptions-item label="发票号码">{{ currentDetail.invoiceNo || '--' }}</el-descriptions-item>
          <el-descriptions-item label="关联订单编码">{{ currentDetail.orderCode || '--' }}</el-descriptions-item>
          <el-descriptions-item label="关联结算单编码">{{ currentDetail.billCode || '--' }}</el-descriptions-item>
          <el-descriptions-item label="申请方名称">{{ currentDetail.applicantName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="申请方编码">{{ currentDetail.applicantCode || '--' }}</el-descriptions-item>
          <el-descriptions-item label="申请方类型">{{ currentDetail.applicantType || '--' }}</el-descriptions-item>
          <el-descriptions-item label="开户银行">{{ currentDetail.bankName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="银行账号">{{ currentDetail.bankAccount || '--' }}</el-descriptions-item>
          <el-descriptions-item label="注册地址" :span="2">{{ currentDetail.registerAddress || '--' }}</el-descriptions-item>
          <el-descriptions-item label="注册电话">{{ currentDetail.registerPhone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatDateTime(currentDetail.applyTime) }}</el-descriptions-item>
          <el-descriptions-item label="开票时间">{{ formatDateTime(currentDetail.issueTime) }}</el-descriptions-item>
          <el-descriptions-item label="寄出时间">{{ formatDateTime(currentDetail.sendTime) }}</el-descriptions-item>
          <el-descriptions-item label="收件人">{{ currentDetail.receiverName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="收件人电话">{{ currentDetail.receiverPhone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="收件地址" :span="2">{{ currentDetail.receiverAddress || '--' }}</el-descriptions-item>
          <el-descriptions-item label="收件邮箱">{{ currentDetail.receiverEmail || '--' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentDetail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="发票文件" :span="2">
            <span v-if="currentDetail.invoiceUrl">{{ currentDetail.invoiceUrl }}</span>
            <span v-else>--</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentDetail.remark || '--' }}</el-descriptions-item>
        </el-descriptions>
      </div>
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

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
