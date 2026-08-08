<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import {
  cancelOrderCourse,
  cancelOrderEquity,
  cancelOrderScene,
  cancelOrderSojourn,
  getOrderCourse,
  getOrderEquity,
  getOrderScene,
  getOrderSojourn,
  pageOrderCourses,
  pageOrderScenes,
  pageOrderSojourns,
  pageOrders,
  type OrderCancelData
} from '@/api/order'
import { createFinancePayment, markFinancePaymentSuccess } from '@/api/finance'
import {
  ORDER_STATUS_OPTIONS,
  OrderStatus,
  type Order,
  type OrderCourse,
  type OrderCourseQuery,
  type OrderQuery,
  type OrderScene,
  type OrderSceneQuery,
  type OrderSojourn,
  type OrderSojournQuery
} from '@/types/order'
import { PAY_TYPE_OPTIONS, type PayType } from '@/types/finance'

/**
 * 订单管理页（采购结算目录 - 4 类订单统一管理）。
 *
 * 4 个 tab：权益 / 场景 / 课程 / 旅居，每个 tab 一份独立的 useCrud 实例
 * （loading/tableData/total/query 互不影响）。各 tab 搜索条件统一为
 * 「订单编码 + 订单状态」两项（产品裁定：减少代码量，场景/课程/旅居的特有
 * 搜索项 sceneCode/courseCode/parkCode 暂不暴露在搜索栏，仍可通过订单编码精确查询）。
 *
 * 懒加载：权益 tab 默认激活且 onMounted 即加载；其余 tab 首次切换时触发 loadPage
 * （loaded 标记防止重复加载）。
 *
 * 取消订单：仅当 orderStatus ∈ {待支付, 退款中} 时显示「取消」按钮；点击弹 prompt
 * 输入取消原因，调对应 cancelOrderXxx，成功后刷新当前 tab 列表。
 *
 * 内联支付：待支付订单显示「支付」按钮，点击弹窗选支付方式 + 填交易号，
 * 一步完成创建支付单 + 标记成功（模拟支付），替代原跳转收银台方案。
 * 4 个 tab 共用同一个支付弹窗（openPayDialog 传 orderType 区分）。
 *
 * 查看详情：简化方案，用 ElMessageBox.alert 展示后端 VO 原始 JSON 字段。
 */

type TabKey = 'equity' | 'scene' | 'course' | 'sojourn'

// ==================== 4 组 useCrud 实例（状态相互隔离） ====================

const equityCrud = useCrud<Order, OrderQuery>(
  { page: pageOrders },
  { initialQuery: { orderCode: '', orderStatus: undefined } }
)
const sceneCrud = useCrud<OrderScene, OrderSceneQuery>(
  { page: pageOrderScenes },
  { initialQuery: { orderCode: '', orderStatus: undefined } }
)
const courseCrud = useCrud<OrderCourse, OrderCourseQuery>(
  { page: pageOrderCourses },
  { initialQuery: { orderCode: '', orderStatus: undefined } }
)
const sojournCrud = useCrud<OrderSojourn, OrderSojournQuery>(
  { page: pageOrderSojourns },
  { initialQuery: { orderCode: '', orderStatus: undefined } }
)

// ==================== Tab 切换 + 懒加载 ====================

const activeTab = ref<TabKey>('equity')
const loaded = reactive<Record<TabKey, boolean>>({
  equity: false,
  scene: false,
  course: false,
  sojourn: false
})

function loadTab(tab: TabKey): Promise<void> {
  switch (tab) {
    case 'equity':
      return equityCrud.loadPage()
    case 'scene':
      return sceneCrud.loadPage()
    case 'course':
      return courseCrud.loadPage()
    case 'sojourn':
      return sojournCrud.loadPage()
  }
}

watch(activeTab, (tab) => {
  if (loaded[tab]) return
  loaded[tab] = true
  loadTab(tab).catch((err) => {
    // 接口未实现或渠道无数据时降级：留空 + 控制台 warn（错误 toast 由拦截器统一处理）
    console.warn(`[order-manage] 加载 ${tab} 订单列表失败:`, err)
  })
})

onMounted(() => {
  loaded.equity = true
  equityCrud
    .loadPage()
    .catch((err) => console.warn('[order-manage] 加载权益订单列表失败:', err))
})

// ==================== 订单状态展示（照搬 order/index.vue） ====================

function statusTagType(v?: number): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  switch (v) {
    case OrderStatus.COMPLETED:
      return 'success'
    case OrderStatus.PENDING_PAY:
      return 'warning'
    case OrderStatus.PAID:
    case OrderStatus.DELIVERED:
      return 'primary'
    case OrderStatus.PARTIAL_DELIVERED:
      return 'warning'
    case OrderStatus.CANCELLED:
      return 'info'
    case OrderStatus.REFUNDING:
      return 'danger'
    case OrderStatus.REFUNDED:
      return 'info'
    default:
      return 'info'
  }
}

function statusText(v?: number): string {
  const opt = ORDER_STATUS_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

/** 是否可取消（仅待支付 / 退款中可取消） */
function isCancellable(status?: number): boolean {
  return status === OrderStatus.PENDING_PAY || status === OrderStatus.REFUNDING
}

/** 金额格式化（保留 2 位小数，空值返回 '--'） */
function formatAmount(v?: number | null): string {
  return v != null ? Number(v).toFixed(2) : '--'
}

// ==================== 通用搜索栏 handler ====================

/** 重置搜索条件（仅 orderCode + orderStatus 两项）后触发查询 */
function resetSearch(query: { orderCode?: string; orderStatus?: OrderStatus }, search: () => void) {
  query.orderCode = ''
  query.orderStatus = undefined
  search()
}

// ==================== 查看详情（工厂，简化：alert JSON） ====================

/** HTML 转义，防止 JSON 字段在 dangerouslyUseHTMLString 下注入 */
function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => {
    switch (c) {
      case '&':
        return '&amp;'
      case '<':
        return '&lt;'
      case '>':
        return '&gt;'
      case '"':
        return '&quot;'
      default:
        return '&#39;'
    }
  })
}

function makeViewHandler<T>(getter: (code: string) => Promise<T>, title: string) {
  return async (orderCode: string) => {
    if (!orderCode) return
    try {
      const vo = await getter(orderCode)
      await ElMessageBox.alert(
        `<pre class="order-detail-pre">${escapeHtml(JSON.stringify(vo, null, 2))}</pre>`,
        `${title} · ${orderCode}`,
        {
          confirmButtonText: '关闭',
          dangerouslyUseHTMLString: true,
          customClass: 'order-detail-msgbox'
        }
      )
    } catch (err) {
      // 用户关闭弹窗或接口报错：静默（错误已由响应拦截器统一提示）
      void err
    }
  }
}

const viewEquity = makeViewHandler(getOrderEquity, '权益订单详情')
const viewScene = makeViewHandler(getOrderScene, '场景订单详情')
const viewCourse = makeViewHandler(getOrderCourse, '课程订单详情')
const viewSojourn = makeViewHandler(getOrderSojourn, '旅居订单详情')

// ==================== 取消订单（工厂：prompt 输入原因 → cancel → 刷新） ====================

function makeCancelHandler(
  cancel: (code: string, data: OrderCancelData) => Promise<void>,
  refresh: () => void
) {
  return async (orderCode: string) => {
    if (!orderCode) return
    try {
      const { value } = await ElMessageBox.prompt('请输入取消原因', `取消订单 · ${orderCode}`, {
        confirmButtonText: '确定取消',
        cancelButtonText: '返回',
        inputType: 'textarea',
        inputPlaceholder: '取消原因（必填，将通知客户与平台）',
        inputValidator: (v: string) => (v != null && v.trim().length > 0) || '取消原因不能为空'
      })
      await cancel(orderCode, { cancelReason: value.trim() })
      ElMessage.success('订单已取消')
      refresh()
    } catch (err) {
      // 用户点「返回」放弃取消：静默；接口报错已由拦截器统一提示
      void err
    }
  }
}

const cancelEquity = makeCancelHandler(cancelOrderEquity, () => equityCrud.loadPage())
const cancelScene = makeCancelHandler(cancelOrderScene, () => sceneCrud.loadPage())
const cancelCourse = makeCancelHandler(cancelOrderCourse, () => courseCrud.loadPage())
const cancelSojourn = makeCancelHandler(cancelOrderSojourn, () => sojournCrud.loadPage())

// ==================== 内联支付弹窗（4 tab 共用，替代跳转收银台） ====================

/**
 * 订单类型 → 文案映射（支付弹窗头部展示用）。
 * 1=权益 / 2=场景 / 3=课程 / 4=旅居（与 finance_payment.order_type 一致）。
 */
const ORDER_TYPE_LABEL: Record<number, string> = {
  1: '权益订单',
  2: '场景订单',
  3: '课程订单',
  4: '旅居订单'
}

/** 支付弹窗状态（4 tab 共享一份） */
const payDialog = reactive({
  visible: false,
  submitting: false,
  /** 订单类型：1权益/2场景/3课程/4旅居 */
  orderType: 0 as number,
  /** 订单编码 */
  orderCode: '' as string,
  /** 应付金额（从行数据取，权益订单后端权威覆盖，其余类型传给后端） */
  payAmount: undefined as number | undefined,
  /** 支付方式（用户选） */
  payType: undefined as PayType | undefined,
  /** 第三方交易号（用户填，模拟支付） */
  tradeNo: '' as string
})

/**
 * 打开支付弹窗（4 tab 统一入口，orderType 区分订单类型）。
 * row 只取 orderCode + payAmount 两字段，4 类订单行均有。
 */
function openPayDialog(row: { orderCode?: string; payAmount?: number }, orderType: number) {
  payDialog.orderType = orderType
  payDialog.orderCode = row.orderCode ?? ''
  payDialog.payAmount = row.payAmount
  payDialog.payType = undefined
  payDialog.tradeNo = ''
  payDialog.visible = true
}

/**
 * 提交支付：创建支付单 + 标记成功两步合一（模拟支付）。
 * - 创建支付单返回 paymentCode（PAY+序号）；
 * - 标记成功写 tradeNo + payTime，触发订单状态机 0→1（待支付→已支付）+ 记资金流水；
 * - 权益订单（orderType=1）payAmount 由后端从订单表权威解析覆盖，前端不传。
 */
async function handleSubmitPay() {
  if (!payDialog.payType) {
    ElMessage.warning('请选择支付方式')
    return
  }
  const tradeNo = payDialog.tradeNo.trim()
  if (!tradeNo) {
    ElMessage.warning('请输入第三方交易号')
    return
  }
  payDialog.submitting = true
  try {
    // 1. 创建支付单（返回 paymentCode）
    const paymentCode = await createFinancePayment({
      orderType: payDialog.orderType,
      orderCode: payDialog.orderCode,
      payType: payDialog.payType,
      // 权益订单由后端权威覆盖；其余类型传表单值
      payAmount: payDialog.orderType === 1 ? undefined : payDialog.payAmount
    })
    // 2. 标记支付成功（写 tradeNo + payTime，触发订单状态机 + 资金流水）
    await markFinancePaymentSuccess(paymentCode, { tradeNo })
    ElMessage.success('支付完成')
    payDialog.visible = false
    // 刷新当前激活 tab
    await loadTab(activeTab.value)
  } catch (err) {
    // 接口报错已由响应拦截器统一提示；此处静默
    void err
  } finally {
    payDialog.submitting = false
  }
}
</script>

<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" type="border-card" class="order-tabs">
      <!-- ==================== 权益订单 ==================== -->
      <el-tab-pane label="权益订单" name="equity" lazy>
        <div class="tab-inner">
          <el-card shadow="never" class="search-card">
            <el-form :inline="true" :model="equityCrud.query" @submit.prevent>
              <el-form-item label="订单编码">
                <el-input
                  v-model="equityCrud.query.orderCode"
                  placeholder="订单编码"
                  clearable
                  @keyup.enter="equityCrud.handleSearch"
                />
              </el-form-item>
              <el-form-item label="订单状态">
                <el-select
                  v-model="equityCrud.query.orderStatus"
                  placeholder="全部"
                  clearable
                  style="width: 140px"
                >
                  <el-option v-for="o in ORDER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="'Search'" @click="equityCrud.handleSearch">查询</el-button>
                <el-button :icon="'Refresh'" @click="resetSearch(equityCrud.query, equityCrud.handleSearch)">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <div class="card-header"><span>权益订单列表</span></div>
            </template>
            <el-table
              v-loading="equityCrud.loading.value"
              :data="equityCrud.tableData.value"
              border
              stripe
              row-key="orderCode"
            >
              <el-table-column prop="orderCode" label="订单编码" min-width="140" show-overflow-tooltip />
              <el-table-column prop="orderStatus" label="订单状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag
                    v-if="row.orderStatus !== undefined && row.orderStatus !== null"
                    :type="statusTagType(row.orderStatus)"
                  >
                    {{ statusText(row.orderStatus) }}
                  </el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="goodsName" label="商品名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="quantity" label="数量" width="80" align="right" />
              <el-table-column prop="payAmount" label="实付金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatAmount(row.payAmount) }}</template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" min-width="160" />
              <el-table-column label="操作" width="160" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="viewEquity(row.orderCode)">详情</el-button>
                  <el-button
                    v-if="row.orderStatus === OrderStatus.PENDING_PAY"
                    link
                    type="primary"
                    size="small"
                    @click="openPayDialog(row, 1)"
                  >
                    支付
                  </el-button>
                  <el-button
                    v-if="isCancellable(row.orderStatus)"
                    link
                    type="danger"
                    size="small"
                    @click="cancelEquity(row.orderCode)"
                  >
                    取消
                  </el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无数据" /></template>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                :current-page="equityCrud.query.current"
                :page-size="equityCrud.query.size"
                :total="equityCrud.total.value"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                background
                @current-change="equityCrud.handlePageChange"
                @size-change="equityCrud.handleSizeChange"
              />
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ==================== 场景订单 ==================== -->
      <el-tab-pane label="场景订单" name="scene" lazy>
        <div class="tab-inner">
          <el-card shadow="never" class="search-card">
            <el-form :inline="true" :model="sceneCrud.query" @submit.prevent>
              <el-form-item label="订单编码">
                <el-input
                  v-model="sceneCrud.query.orderCode"
                  placeholder="订单编码"
                  clearable
                  @keyup.enter="sceneCrud.handleSearch"
                />
              </el-form-item>
              <el-form-item label="订单状态">
                <el-select
                  v-model="sceneCrud.query.orderStatus"
                  placeholder="全部"
                  clearable
                  style="width: 140px"
                >
                  <el-option v-for="o in ORDER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="'Search'" @click="sceneCrud.handleSearch">查询</el-button>
                <el-button :icon="'Refresh'" @click="resetSearch(sceneCrud.query, sceneCrud.handleSearch)">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <div class="card-header"><span>场景订单列表</span></div>
            </template>
            <el-table
              v-loading="sceneCrud.loading.value"
              :data="sceneCrud.tableData.value"
              border
              stripe
              row-key="orderCode"
            >
              <el-table-column prop="orderCode" label="订单编码" min-width="140" show-overflow-tooltip />
              <el-table-column prop="orderStatus" label="订单状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag
                    v-if="row.orderStatus !== undefined && row.orderStatus !== null"
                    :type="statusTagType(row.orderStatus)"
                  >
                    {{ statusText(row.orderStatus) }}
                  </el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="sceneName" label="场景名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="activityDate" label="活动日期" width="120" align="center" />
              <el-table-column prop="participantCount" label="人数" width="80" align="right" />
              <el-table-column prop="payAmount" label="实付金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatAmount(row.payAmount) }}</template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" min-width="160" />
              <el-table-column label="操作" width="160" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="viewScene(row.orderCode)">详情</el-button>
                  <el-button
                    v-if="row.orderStatus === OrderStatus.PENDING_PAY"
                    link
                    type="primary"
                    size="small"
                    @click="openPayDialog(row, 2)"
                  >
                    支付
                  </el-button>
                  <el-button
                    v-if="isCancellable(row.orderStatus)"
                    link
                    type="danger"
                    size="small"
                    @click="cancelScene(row.orderCode)"
                  >
                    取消
                  </el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无数据" /></template>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                :current-page="sceneCrud.query.current"
                :page-size="sceneCrud.query.size"
                :total="sceneCrud.total.value"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                background
                @current-change="sceneCrud.handlePageChange"
                @size-change="sceneCrud.handleSizeChange"
              />
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ==================== 课程订单 ==================== -->
      <el-tab-pane label="课程订单" name="course" lazy>
        <div class="tab-inner">
          <el-card shadow="never" class="search-card">
            <el-form :inline="true" :model="courseCrud.query" @submit.prevent>
              <el-form-item label="订单编码">
                <el-input
                  v-model="courseCrud.query.orderCode"
                  placeholder="订单编码"
                  clearable
                  @keyup.enter="courseCrud.handleSearch"
                />
              </el-form-item>
              <el-form-item label="订单状态">
                <el-select
                  v-model="courseCrud.query.orderStatus"
                  placeholder="全部"
                  clearable
                  style="width: 140px"
                >
                  <el-option v-for="o in ORDER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="'Search'" @click="courseCrud.handleSearch">查询</el-button>
                <el-button :icon="'Refresh'" @click="resetSearch(courseCrud.query, courseCrud.handleSearch)">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <div class="card-header"><span>课程订单列表</span></div>
            </template>
            <el-table
              v-loading="courseCrud.loading.value"
              :data="courseCrud.tableData.value"
              border
              stripe
              row-key="orderCode"
            >
              <el-table-column prop="orderCode" label="订单编码" min-width="140" show-overflow-tooltip />
              <el-table-column prop="orderStatus" label="订单状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag
                    v-if="row.orderStatus !== undefined && row.orderStatus !== null"
                    :type="statusTagType(row.orderStatus)"
                  >
                    {{ statusText(row.orderStatus) }}
                  </el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="courseName" label="课程名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="quantity" label="数量" width="80" align="right" />
              <el-table-column prop="payAmount" label="实付金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatAmount(row.payAmount) }}</template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" min-width="160" />
              <el-table-column label="操作" width="160" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="viewCourse(row.orderCode)">详情</el-button>
                  <el-button
                    v-if="row.orderStatus === OrderStatus.PENDING_PAY"
                    link
                    type="primary"
                    size="small"
                    @click="openPayDialog(row, 3)"
                  >
                    支付
                  </el-button>
                  <el-button
                    v-if="isCancellable(row.orderStatus)"
                    link
                    type="danger"
                    size="small"
                    @click="cancelCourse(row.orderCode)"
                  >
                    取消
                  </el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无数据" /></template>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                :current-page="courseCrud.query.current"
                :page-size="courseCrud.query.size"
                :total="courseCrud.total.value"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                background
                @current-change="courseCrud.handlePageChange"
                @size-change="courseCrud.handleSizeChange"
              />
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ==================== 旅居订单 ==================== -->
      <el-tab-pane label="旅居订单" name="sojourn" lazy>
        <div class="tab-inner">
          <el-card shadow="never" class="search-card">
            <el-form :inline="true" :model="sojournCrud.query" @submit.prevent>
              <el-form-item label="订单编码">
                <el-input
                  v-model="sojournCrud.query.orderCode"
                  placeholder="订单编码"
                  clearable
                  @keyup.enter="sojournCrud.handleSearch"
                />
              </el-form-item>
              <el-form-item label="订单状态">
                <el-select
                  v-model="sojournCrud.query.orderStatus"
                  placeholder="全部"
                  clearable
                  style="width: 140px"
                >
                  <el-option v-for="o in ORDER_STATUS_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="'Search'" @click="sojournCrud.handleSearch">查询</el-button>
                <el-button :icon="'Refresh'" @click="resetSearch(sojournCrud.query, sojournCrud.handleSearch)">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <div class="card-header"><span>旅居订单列表</span></div>
            </template>
            <el-table
              v-loading="sojournCrud.loading.value"
              :data="sojournCrud.tableData.value"
              border
              stripe
              row-key="orderCode"
            >
              <el-table-column prop="orderCode" label="订单编码" min-width="140" show-overflow-tooltip />
              <el-table-column prop="orderStatus" label="订单状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag
                    v-if="row.orderStatus !== undefined && row.orderStatus !== null"
                    :type="statusTagType(row.orderStatus)"
                  >
                    {{ statusText(row.orderStatus) }}
                  </el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="parkFullName" label="机构" min-width="180" show-overflow-tooltip />
              <el-table-column label="入住日期" width="200" align="center">
                <template #default="{ row }">
                  <span v-if="row.checkinDate || row.checkoutDate">
                    {{ row.checkinDate ?? '--' }} ~ {{ row.checkoutDate ?? '--' }}
                  </span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="stayDays" label="天数" width="80" align="right" />
              <el-table-column prop="payAmount" label="实付金额（元）" width="130" align="right">
                <template #default="{ row }">{{ formatAmount(row.payAmount) }}</template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" min-width="160" />
              <el-table-column label="操作" width="160" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="viewSojourn(row.orderCode)">详情</el-button>
                  <el-button
                    v-if="row.orderStatus === OrderStatus.PENDING_PAY"
                    link
                    type="primary"
                    size="small"
                    @click="openPayDialog(row, 4)"
                  >
                    支付
                  </el-button>
                  <el-button
                    v-if="isCancellable(row.orderStatus)"
                    link
                    type="danger"
                    size="small"
                    @click="cancelSojourn(row.orderCode)"
                  >
                    取消
                  </el-button>
                </template>
              </el-table-column>
              <template #empty><el-empty description="暂无数据" /></template>
            </el-table>

            <div class="pagination-wrap">
              <el-pagination
                :current-page="sojournCrud.query.current"
                :page-size="sojournCrud.query.size"
                :total="sojournCrud.total.value"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                background
                @current-change="sojournCrud.handlePageChange"
                @size-change="sojournCrud.handleSizeChange"
              />
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 内联支付弹窗（4 tab 共用） ==================== -->
    <el-dialog
      v-model="payDialog.visible"
      :title="`订单支付 · ${ORDER_TYPE_LABEL[payDialog.orderType] ?? ''}`"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form label-width="96px" @submit.prevent>
        <el-form-item label="订单编码">
          <span class="pay-order-code">{{ payDialog.orderCode }}</span>
        </el-form-item>
        <el-form-item label="应付金额">
          <span class="pay-amount">{{ formatAmount(payDialog.payAmount) }} 元</span>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="payDialog.payType" placeholder="请选择支付方式" style="width: 100%">
            <el-option v-for="o in PAY_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易号">
          <el-input
            v-model="payDialog.tradeNo"
            placeholder="模拟支付交易号，如 WX202608071234"
            maxlength="64"
          />
        </el-form-item>
        <div v-if="payDialog.orderType === 1" class="pay-hint">
          权益订单以订单实付金额为准，后端将权威解析覆盖。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="payDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="payDialog.submitting" @click="handleSubmitPay">
          确认支付
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  display: flex;
  flex-direction: column;
}

.order-tabs {
  :deep(.el-tabs__content) {
    padding: 16px;
  }
}

.tab-inner {
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

.pay-order-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  color: #303133;
}

.pay-amount {
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
}

.pay-hint {
  margin: -8px 0 0 96px;
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.5;
}
</style>

<!-- 详情 MessageBox 用到的 <pre> 样式（非 scoped：MessageBox 渲染在 body 末尾，scoped 不生效） -->
<style lang="scss">
.order-detail-msgbox {
  .order-detail-pre {
    margin: 0;
    max-height: 60vh;
    overflow: auto;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 12px;
    line-height: 1.6;
    color: #303133;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
