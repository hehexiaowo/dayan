<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { pageOrderSojourns, getOrderSojourn, cancelOrderSojourn } from '@/api/order'
import type { OrderSojourn, OrderSojournQuery } from '@/types/order'
import { formatDateTime, formatMoney } from '@/utils/format'
import {
  SojournOrderStatus,
  SOJOURN_ORDER_STATUS_OPTIONS,
  COURSE_PAY_TYPE_OPTIONS
} from '@/types/order'

/**
 * 旅游短居预订订单管理页（第一版简化）。
 *
 * - 订单由业务流程创建，本页以只读列表为主：搜索 + 表格 + 分页 + 详情查看。
 * - 生命周期操作只保留「取消订单」（pay-callback/complete/apply-refund 暂留 TODO）。
 * - 取消操作走 POST /order/sojourn/cancel，入参含 cancelReason（@RequestBody OrderCancelDTO）。
 *
 * 订单状态（order_sojourn.order_status）：
 *   0待支付 / 1已支付 / 2部分发放 / 3已发放 / 4已完成 / 5已取消 / 6退款中 / 7已退款。
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
} = useCrud<OrderSojourn, OrderSojournQuery>(
  { page: pageOrderSojourns },
  {
    initialQuery: {
      orderCode: '',
      orderType: undefined,
      channelCode: '',
      agentCode: '',
      distributorCode: '',
      clientCode: '',
      parkCode: '',
      roomTypeCode: '',
      couponCode: '',
      equityCode: '',
      checkinDateStart: '',
      checkinDateEnd: '',
      orderStatus: undefined,
      payType: undefined
    }
  }
)

/** 入住日期范围（element-plus 日期选择器返回 [start, end]，同步到 query 的两个字段）。 */
const checkinDateRange = ref<[string, string] | null>(null)

function handleCheckinRangeChange(val: [string, string] | null) {
  checkinDateRange.value = val
  if (val && val.length === 2) {
    query.checkinDateStart = val[0]
    query.checkinDateEnd = val[1]
  } else {
    query.checkinDateStart = ''
    query.checkinDateEnd = ''
  }
}

function handleReset() {
  query.orderCode = ''
  query.orderType = undefined
  query.channelCode = ''
  query.agentCode = ''
  query.distributorCode = ''
  query.clientCode = ''
  query.parkCode = ''
  query.roomTypeCode = ''
  query.couponCode = ''
  query.equityCode = ''
  query.checkinDateStart = ''
  query.checkinDateEnd = ''
  query.orderStatus = undefined
  query.payType = undefined
  checkinDateRange.value = null
  handleSearch()
}

// ---------- 详情弹窗 ----------
const detailVisible = ref(false)
const detail = ref<OrderSojourn>({})

async function openDetail(row: OrderSojourn) {
  if (!row.orderCode) return
  try {
    detail.value = await getOrderSojourn(row.orderCode)
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

function openCancel(row: OrderSojourn) {
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
    await cancelOrderSojourn({
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
function payTypeLabel(t?: number): string {
  const found = COURSE_PAY_TYPE_OPTIONS.find((o) => o.value === t)
  return found ? found.label : t != null ? String(t) : '--'
}

function orderStatusLabel(s?: number): string {
  const found = SOJOURN_ORDER_STATUS_OPTIONS.find((o) => o.value === s)
  return found ? found.label : s != null ? String(s) : '--'
}

/** 订单状态 el-tag type：0待付warning / 1已付primary / 2部分发放info / 3已发放info / 4完成success / 5取消info / 6退款/7已退danger。 */
function orderStatusTagType(status?: number): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch (status) {
    case SojournOrderStatus.PENDING_PAY:
      return 'warning'
    case SojournOrderStatus.PAID:
      return 'primary'
    case SojournOrderStatus.PARTIAL_DELIVERED:
    case SojournOrderStatus.DELIVERED:
      return 'info'
    case SojournOrderStatus.COMPLETED:
      return 'success'
    case SojournOrderStatus.REFUNDING:
    case SojournOrderStatus.REFUNDED:
      return 'danger'
    case SojournOrderStatus.CANCELLED:
    default:
      return 'info'
  }
}

/** 待支付 / 已支付 / 部分发放 / 已发放 可取消（其余状态后端状态机会拒绝）。 */
function canCancel(status?: number): boolean {
  return (
    status === SojournOrderStatus.PENDING_PAY ||
    status === SojournOrderStatus.PAID ||
    status === SojournOrderStatus.PARTIAL_DELIVERED ||
    status === SojournOrderStatus.DELIVERED
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
        <el-form-item label="订单类型">
          <el-input v-model="query.orderType" placeholder="订单类型" clearable @keyup.enter="handleSearch" />
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
        <el-form-item label="客户编码">
          <el-input v-model="query.clientCode" placeholder="客户编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="旅游短居基地">
          <el-input v-model="query.parkCode" placeholder="旅游短居基地编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="入住日期">
          <el-date-picker
            :model-value="checkinDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="入住起"
            end-placeholder="入住止"
            style="width: 240px"
            @update:model-value="handleCheckinRangeChange"
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="query.orderStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="o in SOJOURN_ORDER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="query.payType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in COURSE_PAY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>旅游短居订单列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="orderCode">
        <el-table-column prop="orderCode" label="订单编号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="orderType" label="订单类型" width="100" align="center">
          <template #default="{ row }">{{ row.orderType ?? '--' }}</template>
        </el-table-column>
        <el-table-column prop="channelFullName" label="渠道" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.channelFullName || row.channelCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="clientFullName" label="客户" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.clientFullName || row.clientCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="parkFullName" label="旅游短居基地" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.parkFullName || row.parkCode || '--' }}</template>
        </el-table-column>
        <el-table-column prop="checkinDate" label="入住日期" width="120" align="center">
          <template #default="{ row }">
            <span>{{ row.checkinDate || '--' }}{{ row.checkoutDate ? ' ~ ' + row.checkoutDate : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stayDays" label="天数" width="80" align="center">
          <template #default="{ row }">{{ row.stayDays ?? '--' }}</template>
        </el-table-column>
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
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <!-- TODO 第一版暂不开放的生命周期操作：支付回调 / 完成 / 申请退款 -->
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
    <el-dialog v-model="detailVisible" title="旅游短居订单详情" width="820px" :close-on-click-modal="false">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号">{{ detail.orderCode }}</el-descriptions-item>
        <el-descriptions-item label="订单类型">{{ detail.orderType ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="渠道">{{ detail.channelFullName || detail.channelCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="代理人">{{ detail.agentFullName || detail.agentCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="分销商">{{ detail.distributorFullName || detail.distributorCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.clientFullName || detail.clientCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ detail.goodsName || detail.goodsCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="旅游短居基地">{{ detail.parkFullName || detail.parkCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="房型编码">{{ detail.roomTypeCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="SKU">{{ detail.skuName || detail.skuCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="入住日期">{{ detail.checkinDate || '--' }}</el-descriptions-item>
        <el-descriptions-item label="离店日期">{{ detail.checkoutDate || '--' }}</el-descriptions-item>
        <el-descriptions-item label="入住天数">{{ detail.stayDays ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="入住人数">{{ detail.residentCount ?? '--' }}</el-descriptions-item>
        <el-descriptions-item label="入住人姓名" :span="2">{{ detail.residentNames || '--' }}</el-descriptions-item>
        <el-descriptions-item label="照护类型">{{ detail.careTypeCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="餐食类型">{{ detail.foodTypeCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="房费">{{ formatMoney(detail.roomFee) }}</el-descriptions-item>
        <el-descriptions-item label="照护费">{{ formatMoney(detail.careFee) }}</el-descriptions-item>
        <el-descriptions-item label="餐费">{{ formatMoney(detail.foodFee) }}</el-descriptions-item>
        <el-descriptions-item label="其他费用">{{ formatMoney(detail.otherFee) }}</el-descriptions-item>
        <el-descriptions-item label="订单总额">{{ formatMoney(detail.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">{{ formatMoney(detail.discountAmount) }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">{{ formatMoney(detail.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="押金">{{ formatMoney(detail.depositAmount) }}</el-descriptions-item>
        <el-descriptions-item label="优惠券编码">{{ detail.couponCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="使用的权益">{{ detail.equityCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ payTypeLabel(detail.payType) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDateTime(detail.payTime) }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactPhone || '--' }}</el-descriptions-item>
        <el-descriptions-item label="特殊需求" :span="2">{{ detail.specialNeeds || '--' }}</el-descriptions-item>
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
