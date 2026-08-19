<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { createFinancePayment, markFinancePaymentSuccess, pageFinancePayments } from '@/api/finance'
import { formatDateTime, formatMoney } from '@/utils/format'
import {
  PAY_TYPE_OPTIONS,
  PAYMENT_STATUS_OPTIONS,
  PaymentStatus,
  type CreatePaymentData,
  type FinancePayment,
  type FinancePaymentQuery
} from '@/types/finance'

/**
 * 财务收银页（采购结算目录，从订单管理「去支付」跳转或侧边栏进入）。
 *
 * 入口：
 * - 菜单 is_visible=0，左侧导航不显示；
 * - 直接访问 URL `/cashier`，或从订单管理「去支付」按钮
 *   `router.push({ path: '/cashier', query: { orderCode, orderType } })` 跳转。
 *
 * 功能：
 * - 支付单列表（useCrud 只读 page）：paymentCode / orderType / orderCode /
 *   payAmount / payType / tradeNo / payStatus / payTime；
 * - 创建支付弹窗：若路由 query 带 orderCode + orderType，onMounted 自动打开并预填；
 * - 标记成功（模拟支付完成）：仅 payStatus===0（待支付）显示，弹 prompt 输入 tradeNo；
 * - 详情：el-dialog + el-descriptions 结构化弹窗展示支付单行数据全字段
 *   （对齐 order-manage 详情模式）。
 *
 * 防越权说明：
 * - 支付单读接口由后端 ChannelFinanceController 反查本渠道 4 类订单的 orderCode
 *   集合做归属过滤（finance_payment 表无 channel_code）；
 * - 权益订单（orderType=1）创建支付时 payAmount 由后端从订单表权威解析覆盖。
 */

/** 订单类型选项（与订单管理 4 类订单统一命名） */
const ORDER_TYPE_OPTIONS = [
  { label: '养老权益订单', value: 1 },
  { label: '场景营销订单', value: 2 },
  { label: '培训课程订单', value: 3 },
  { label: '旅游短居订单', value: 4 }
] as const

const route = useRoute()

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } =
  useCrud<FinancePayment, FinancePaymentQuery>(
    { page: pageFinancePayments },
    {
      initialQuery: {
        paymentCode: '',
        orderCode: '',
        payStatus: undefined,
        payType: undefined
      }
    }
  )

function handleReset() {
  query.paymentCode = ''
  query.orderCode = ''
  query.payStatus = undefined
  query.payType = undefined
  handleSearch()
}

/** 订单类型 tag 颜色：1权益=primary / 2场景=success / 3课程=warning / 4旅游短居=info */
function orderTypeTagType(v?: number): 'info' | 'warning' | 'success' | 'primary' | 'danger' {
  switch (v) {
    case 1:
      return 'primary'
    case 2:
      return 'success'
    case 3:
      return 'warning'
    case 4:
      return 'info'
    default:
      return 'info'
  }
}

/** 订单类型文案 */
function orderTypeText(v?: number): string {
  const opt = ORDER_TYPE_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

/**
 * 支付状态 tag 颜色：0待支付=warning / 1成功=success / 2失败=danger /
 * 3已退款=info / 4部分退款=warning
 */
function payStatusTagType(v?: number): 'info' | 'warning' | 'success' | 'primary' | 'danger' {
  switch (v) {
    case PaymentStatus.PENDING:
      return 'warning'
    case PaymentStatus.SUCCESS:
      return 'success'
    case PaymentStatus.FAILED:
      return 'danger'
    case PaymentStatus.REFUNDED:
      return 'info'
    case PaymentStatus.PARTIAL_REFUND:
      return 'warning'
    default:
      return 'info'
  }
}

/** 支付状态文案 */
function payStatusText(v?: number): string {
  const opt = PAYMENT_STATUS_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

/** 支付方式文案 */
function payTypeText(v?: number): string {
  const opt = PAY_TYPE_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

/** 是否待支付（决定「标记成功」按钮显示） */
function isPending(status?: number): boolean {
  return status === PaymentStatus.PENDING
}

// ==================== 查看详情（el-dialog + el-descriptions） ====================

const detailVisible = ref(false)
const currentRow = ref<FinancePayment | null>(null)

/** 打开详情弹窗：展示行数据全字段 */
function openDetail(row: FinancePayment) {
  currentRow.value = row
  detailVisible.value = true
}

// ==================== 标记支付成功（模拟支付） ====================

async function handleMarkSuccess(row: FinancePayment) {
  const paymentCode = row.paymentCode
  if (!paymentCode) return
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入第三方交易号（模拟支付完成，将写入 tradeNo + payTime）',
      `标记支付成功 · ${paymentCode}`,
      {
        confirmButtonText: '确认成功',
        cancelButtonText: '取消',
        inputPlaceholder: '交易号（必填，例如 WX202608071234）',
        inputValidator: (v: string) =>
          (v != null && v.trim().length > 0) || '交易号不能为空'
      }
    )
    await markFinancePaymentSuccess(paymentCode, { tradeNo: value.trim() })
    ElMessage.success('已标记为支付成功')
    loadPage().catch((err) => { console.warn('[cashier] 加载失败:', err) })
  } catch {
    // 用户点「取消」：静默；接口报错由拦截器统一提示
  }
}

// ==================== 创建支付弹窗 ====================

const createDialogVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref<FormInstance>()

interface CreateForm {
  /** 订单类型：1权益/2场景/3课程/4旅游短居 */
  orderType: number | undefined
  /** 订单编码 */
  orderCode: string
  /** 支付方式 */
  payType: number | undefined
  /** 支付金额（权益订单后端覆盖，可选） */
  payAmount: number
  /** 支付说明 */
  payDescription: string
}

function makeEmptyForm(): CreateForm {
  return {
    orderType: undefined,
    orderCode: '',
    payType: undefined,
    payAmount: 0,
    payDescription: ''
  }
}

const createForm = reactive<CreateForm>(makeEmptyForm())

/** 权益订单（orderType=1）提示：后端会从订单表权威解析 payAmount 覆盖前端值 */
const isEquityOrder = (): boolean => createForm.orderType === 1

const createRules: FormRules<CreateForm> = {
  orderType: [{ required: true, message: '请选择订单类型', trigger: 'change' }],
  orderCode: [{ required: true, message: '请输入订单编码', trigger: 'blur' }],
  payType: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
  payAmount: [
    {
      validator: (_rule, value: number, callback) => {
        // 权益订单 payAmount 由后端权威覆盖，前端值不校验下限
        if (isEquityOrder()) {
          callback()
          return
        }
        if (value == null || isNaN(value) || value < 0.01) {
          callback(new Error('支付金额须不小于 0.01 元'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

/** 重置创建表单 */
function resetCreateForm() {
  Object.assign(createForm, makeEmptyForm())
  createFormRef.value?.clearValidate()
}

/** 打开创建弹窗（无参数 = 手动创建；带参数 = 路由预填） */
function openCreateDialog(prefill?: { orderType?: number; orderCode?: string }) {
  resetCreateForm()
  if (prefill) {
    createForm.orderType = prefill.orderType
    createForm.orderCode = prefill.orderCode ?? ''
  }
  createDialogVisible.value = true
}

/** 关闭创建弹窗 */
function closeCreateDialog() {
  createDialogVisible.value = false
}

/** 提交创建支付 */
async function handleSubmitCreate() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }

  const trim = (s: string): string => (s ? s.trim() : '')
  const data: CreatePaymentData = {
    orderType: createForm.orderType as number,
    orderCode: trim(createForm.orderCode),
    payType: createForm.payType as number,
    // 权益订单由后端覆盖，可不传；其余类型传表单值
    payAmount: isEquityOrder() ? undefined : createForm.payAmount,
    payDescription: trim(createForm.payDescription) || undefined
  }

  submitting.value = true
  try {
    const paymentCode = await createFinancePayment(data)
    ElMessage.success(`支付单已创建：${paymentCode}`)
    closeCreateDialog()
    // 跳转来源（订单管理「去支付」）后，通常需要看到刚创建的支付单 → 刷新列表
    handleSearch()
  } catch {
    /* 拦截器已处理 */
  } finally {
    submitting.value = false
  }
}

// ==================== 初始化：加载列表 + 路由参数预填 ====================

onMounted(() => {
  loadPage().catch((err) => {
    console.warn('[cashier] 加载支付单列表失败（接口可能未实现）:', err)
  })

  // 路由参数：从订单管理「去支付」跳转 → query.orderCode + query.orderType
  const qOrderCode = route.query.orderCode
  const qOrderType = route.query.orderType
  if (qOrderCode && typeof qOrderCode === 'string') {
    let orderType: number | undefined
    if (qOrderType != null) {
      const parsed = Number(qOrderType)
      if (!isNaN(parsed) && [1, 2, 3, 4].includes(parsed)) {
        orderType = parsed
      }
    }
    // 自动打开创建弹窗并预填订单信息
    openCreateDialog({ orderCode: qOrderCode, orderType })
  }
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="toolbar">
        <el-input
          v-model="query.paymentCode"
          placeholder="支付单编码"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model="query.orderCode"
          placeholder="订单编码"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.payStatus" placeholder="支付状态" clearable style="width: 140px">
          <el-option v-for="o in PAYMENT_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="query.payType" placeholder="支付方式" clearable style="width: 140px">
          <el-option v-for="o in PAY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span class="card-title">支付单列表</span>
          <el-button type="primary" :icon="'Plus'" @click="openCreateDialog()">创建支付</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="paymentCode">
        <el-table-column prop="paymentCode" label="支付单编码" min-width="150" show-overflow-tooltip />
        <el-table-column prop="orderType" label="订单类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.orderType !== undefined && row.orderType !== null"
              :type="orderTypeTagType(row.orderType)"
            >
              {{ orderTypeText(row.orderType) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderCode" label="订单编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="payAmount" label="金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column prop="payType" label="支付方式" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.payType !== undefined && row.payType !== null" type="info">
              {{ payTypeText(row.payType) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="tradeNo" label="交易号" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.tradeNo">{{ row.tradeNo }}</span>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.payStatus !== undefined && row.payStatus !== null"
              :type="payStatusTagType(row.payStatus)"
            >
              {{ payStatusText(row.payStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" min-width="160" align="center">
          <template #default="{ row }">{{ formatDateTime(row.payTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看详情</el-button>
            <el-button
              v-if="isPending(row.payStatus)"
              link
              type="success"
              size="small"
              @click="handleMarkSuccess(row)"
            >
              标记成功
            </el-button>
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

    <!-- 创建支付弹窗 -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建支付单"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetCreateForm"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="96px"
        @submit.prevent
      >
        <el-form-item label="订单类型" prop="orderType">
          <el-select v-model="createForm.orderType" placeholder="请选择" style="width: 220px">
            <el-option v-for="o in ORDER_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单编码" prop="orderCode">
          <el-input v-model="createForm.orderCode" placeholder="订单编码" maxlength="40" />
        </el-form-item>
        <el-form-item label="支付方式" prop="payType">
          <el-select v-model="createForm.payType" placeholder="请选择" style="width: 220px">
            <el-option v-for="o in PAY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付金额" prop="payAmount">
          <el-input-number
            v-model="createForm.payAmount"
            :min="0"
            :precision="2"
            :step="100"
            controls-position="right"
            style="width: 220px"
          />
          <span class="form-tip">元</span>
        </el-form-item>
        <el-form-item v-if="isEquityOrder()">
          <div class="equity-hint">提示：权益订单以订单实付为准，后端将权威解析覆盖此处金额。</div>
        </el-form-item>
        <el-form-item label="支付说明">
          <el-input
            v-model="createForm.payDescription"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeCreateDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitCreate">创建支付单</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="支付单详情" width="720px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="支付单编码">{{ currentRow.paymentCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="订单类型">
          <el-tag
            v-if="currentRow.orderType !== undefined && currentRow.orderType !== null"
            :type="orderTypeTagType(currentRow.orderType)"
          >
            {{ orderTypeText(currentRow.orderType) }}
          </el-tag>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="订单编码">{{ currentRow.orderCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ payTypeText(currentRow.payType) }}</el-descriptions-item>
        <el-descriptions-item label="支付金额">{{ formatMoney(currentRow.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">
          <el-tag
            v-if="currentRow.payStatus !== undefined && currentRow.payStatus !== null"
            :type="payStatusTagType(currentRow.payStatus)"
          >
            {{ payStatusText(currentRow.payStatus) }}
          </el-tag>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="第三方交易号">{{ currentRow.tradeNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDateTime(currentRow.payTime) }}</el-descriptions-item>
        <el-descriptions-item label="回调通知时间">{{ formatDateTime(currentRow.notifyTime) }}</el-descriptions-item>
        <el-descriptions-item label="付款方账号">{{ currentRow.payerAccount || '--' }}</el-descriptions-item>
        <el-descriptions-item label="收款方账号">{{ currentRow.payeeAccount || '--' }}</el-descriptions-item>
        <el-descriptions-item label="支付说明" :span="2">{{ currentRow.payDescription || '--' }}</el-descriptions-item>
        <el-descriptions-item label="扩展数据" :span="2">{{ currentRow.extraData || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(currentRow.createdAt) }}</el-descriptions-item>
      </el-descriptions>
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

.text-muted {
  color: #909399;
}

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.equity-hint {
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.5;
}
</style>
