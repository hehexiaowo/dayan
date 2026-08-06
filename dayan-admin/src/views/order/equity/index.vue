<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageOrderEquitys, getOrderEquity, cancelOrderEquity } from '@/api/order'
import type { OrderEquity, OrderEquityQuery } from '@/types/order'
import { formatDateTime, formatMoney } from '@/utils/format'
import {
  EquityOrderStatus,
  EQUITY_ORDER_STATUS_OPTIONS,
  ORDER_SOURCE_OPTIONS,
  PAY_TYPE_OPTIONS
} from '@/types/order'

/**
 * 权益采购订单管理页（第一版简化）。
 *
 * - 订单由业务流程创建，本页以只读列表为主：搜索 + 表格 + 分页 + 详情查看。
 * - 生命周期操作只保留「取消订单」（pay/deliver/complete/apply-refund 暂留 TODO）。
 * - 取消操作走 POST /order/equity/cancel，入参含 cancelReason（@RequestBody OrderCancelDTO）。
 *
 * 订单状态（order_equity.order_status）：
 *   0待支付 / 1已支付 / 2已发货 / 3已完成 / 4已取消 / 5退款中 / 6已退款。
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
} = useCrud<OrderEquity, OrderEquityQuery>(
  { page: pageOrderEquitys },
  {
    initialQuery: {
      orderCode: '',
      orderSource: undefined,
      channelCode: '',
      agentCode: '',
      distributorCode: '',
      goodsCode: '',
      organCode: '',
      orderStatus: undefined,
      payType: undefined
    }
  }
)

function handleReset() {
  query.orderCode = ''
  query.orderSource = undefined
  query.channelCode = ''
  query.agentCode = ''
  query.distributorCode = ''
  query.goodsCode = ''
  query.organCode = ''
  query.orderStatus = undefined
  query.payType = undefined
  handleSearch()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const detail = ref<OrderEquity>({})

async function openDetail(row: OrderEquity) {
  if (!row.orderCode) return
  try {
    detail.value = await getOrderEquity(row.orderCode)
  } catch {
    detail.value = row
  }
  detailVisible.value = true
}

// ---------- 取消订单弹窗 ----------
const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelForm = reactive<{ orderCode: string; cancelReason: string }>({
  orderCode: '',
  cancelReason: ''
})

function openCancel(row: OrderEquity) {
  if (!row.orderCode) return
  cancelForm.orderCode = row.orderCode
  cancelForm.cancelReason = ''
  cancelVisible.value = true
}

async function handleCancelSubmit() {
  if (!cancelForm.cancelReason.trim()) {
    ElMessage.warning('请填写取消原因')
    return
  }
  cancelLoading.value = true
  try {
    await cancelOrderEquity({
      orderCode: cancelForm.orderCode,
      cancelReason: cancelForm.cancelReason.trim()
    })
    ElMessage.success('取消成功')
    cancelVisible.value = false
    loadPage()
  } finally {
    cancelLoading.value = false
  }
}

// ---------- 辅助渲染 ----------
function orderSourceLabel(s?: number): string {
  const found = ORDER_SOURCE_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

function payTypeLabel(t?: number): string {
  const found = PAY_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function orderStatusLabel(s?: number): string {
  const found = EQUITY_ORDER_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/** 订单状态 el-tag type：0待付warning / 1已付primary / 2发货/3完成success / 4取消info / 5退款/6已退danger。 */
function orderStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (status) {
    case EquityOrderStatus.PENDING:
      return 'warning'
    case EquityOrderStatus.PAID:
      return 'primary'
    case EquityOrderStatus.DELIVERED:
    case EquityOrderStatus.COMPLETED:
      return 'success'
    case EquityOrderStatus.REFUNDING:
    case EquityOrderStatus.REFUNDED:
      return 'danger'
    case EquityOrderStatus.CANCELLED:
    default:
      return 'info'
  }
}

/** 待支付 / 退款中 可取消（其余状态后端状态机会拒绝）。 */
function canCancel(status?: number): boolean {
  return (
    status === EquityOrderStatus.PENDING ||
    status === EquityOrderStatus.PAID ||
    status === EquityOrderStatus.DELIVERED ||
    status === EquityOrderStatus.REFUNDING
  )
}

// 初始化加载
loadPage()
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="订单编号">
          <el-input v-model="query.orderCode" placeholder="订单编号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="采购来源">
          <el-select v-model="query.orderSource" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in ORDER_SOURCE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道编码">
          <el-input v-model="query.channelCode" placeholder="渠道编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="代理人编码">
          <el-input v-model="query.agentCode" placeholder="代理人编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分销商编码">
          <el-input v-model="query.distributorCode" placeholder="分销商编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="商品编码">
          <el-input v-model="query.goodsCode" placeholder="商品编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.orderStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in EQUITY_ORDER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="query.payType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in PAY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>权益订单列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="orderCode">
        <el-table-column prop="orderCode" label="订单编号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="orderSource" label="采购来源" width="100" align="center">
          <template #default="{ row }">{{ orderSourceLabel(row.orderSource) }}</template>
        </el-table-column>
        <el-table-column prop="channelFullName" label="渠道" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.channelFullName || row.channelCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="goodsName" label="商品" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.goodsName || row.goodsCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="skuName" label="SKU" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.skuName || row.skuCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="center" />
        <el-table-column prop="payAmount" label="实付金额" width="130" align="right">
          <template #default="{ row }">{{ formatMoney(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column prop="payType" label="支付方式" width="100" align="center">
          <template #default="{ row }">{{ payTypeLabel(row.payType) }}</template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.payTime) }}</template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="订单状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="orderStatusTagType(row.orderStatus)">{{ orderStatusLabel(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <!-- TODO 第一版暂不开放的生命周期操作：支付回调 / 发货 / 完成 / 申请退款 -->
            <el-button
              v-if="canCancel(row.orderStatus)"
              link
              type="danger"
              size="small"
              @click="openCancel(row)"
            >
              取消订单
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
    <el-dialog v-model="detailVisible" title="权益订单详情" width="780px" :close-on-click-modal="false">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号">{{ detail.orderCode }}</el-descriptions-item>
        <el-descriptions-item label="采购来源">{{ orderSourceLabel(detail.orderSource) }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ detail.channelFullName || detail.channelCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="代理人">{{ detail.agentFullName || detail.agentCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="分销商">{{ detail.distributorFullName || detail.distributorCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="平台运营方">{{ detail.organCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ detail.goodsName || detail.goodsCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="SKU">{{ detail.skuName || detail.skuCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ detail.quantity ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="单价">{{ formatMoney(detail.unitPrice) }}</el-descriptions-item>
        <el-descriptions-item label="订单总额">{{ formatMoney(detail.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">{{ formatMoney(detail.discountAmount) }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">{{ formatMoney(detail.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ payTypeLabel(detail.payType) }}</el-descriptions-item>
        <el-descriptions-item label="支付流水号">{{ detail.payTradeNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDateTime(detail.payTime) }}</el-descriptions-item>
        <el-descriptions-item label="入库方式">{{ detail.deliverType ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="已入库数量">{{ detail.deliverCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="入库完成时间">{{ formatDateTime(detail.deliverTime) }}</el-descriptions-item>
        <el-descriptions-item label="过期时间">{{ formatDateTime(detail.expireTime) }}</el-descriptions-item>
        <el-descriptions-item label="发票状态">{{ detail.invoiceStatus ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="orderStatusTagType(detail.orderStatus)">{{ orderStatusLabel(detail.orderStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="取消原因">{{ detail.cancelReason || '--' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updatedAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 取消订单弹窗 -->
    <el-dialog v-model="cancelVisible" title="取消订单" width="520px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="订单编号">
          <span>{{ cancelForm.orderCode }}</span>
        </el-form-item>
        <el-form-item label="取消原因" required>
          <el-input
            v-model="cancelForm.cancelReason"
            type="textarea"
            :rows="3"
            placeholder="请填写取消原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelVisible = false">返回</el-button>
        <el-button type="danger" :loading="cancelLoading" @click="handleCancelSubmit">确认取消</el-button>
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
