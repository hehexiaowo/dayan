<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useCrud } from '@/composables/useCrud'
import { GOODS_TYPE_OPTIONS, pageGoodsInfos, type GoodsInfo, type GoodsInfoQuery } from '@/api/goods'
import { createOrderEquity } from '@/api/order'

/**
 * 大雁商城页（采购结算目录 - 商品浏览 + 权益下单）。
 *
 * 产品裁定（任务1）：本页只支持权益类商品（goodsType=1）下单，
 * 其他类型（场景/课程/旅居）按钮 disabled + tooltip 提示「请联系平台下单」。
 *
 * 后端 GET /channel-api/goods-infos 返回 List<GoodsInfoVO>（非分页，已按
 * 渠道白名单 + 上架状态过滤），由 api/goods.ts 的 pageGoodsInfos 包装为 PageResult。
 *
 * 商品列表用 useCrud 管理（只读，仅传 page）；下单逻辑独立实现（不进 useCrud.create），
 * 成功后不刷新列表（商品目录与订单解耦）。
 */

/** 商品类型 tag 颜色映射：1=primary(权益) / 2=success(场景) / 3=warning(课程) / 4=info(旅居) */
const GOODS_TYPE_TAG: Record<number, 'primary' | 'success' | 'warning' | 'info'> = {
  1: 'primary',
  2: 'success',
  3: 'warning',
  4: 'info'
}

const { loading, tableData, total, query, loadPage, handleSearch, handlePageChange, handleSizeChange } = useCrud<
  GoodsInfo,
  GoodsInfoQuery
>(
  { page: pageGoodsInfos },
  {
    initialQuery: {
      // 默认只查上架商品（goodsStatus=1），符合商城页只卖上架商品的语义
      goodsStatus: 1,
      goodsName: '',
      goodsType: undefined
    }
  }
)

function handleReset() {
  query.goodsName = ''
  query.goodsType = undefined
  // 保留 goodsStatus=1（上架商品），避免重置后看到下架商品
  query.goodsStatus = 1
  handleSearch()
}

/** 商品类型文案（用于 tag / 空值回退） */
function goodsTypeText(v?: number): string {
  const opt = GOODS_TYPE_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

/** 是否权益商品（决定下单按钮是否可点） */
function isEquity(row: GoodsInfo): boolean {
  return row.goodsType === 1
}

// ==================== 下单弹窗 ====================

/** 下单弹窗可见性 */
const orderDialogVisible = ref(false)
const submitting = ref(false)
const orderFormRef = ref<FormInstance>()

/** 当前正在下单的商品（用于弹窗内展示） */
const currentGoods = ref<GoodsInfo | null>(null)

/** 下单表单 */
interface OrderForm {
  /** 采购来源：1=对公 / 2=个人 */
  orderSource: number
  /** 购买数量 */
  quantity: number
  /** 备注 */
  remark: string
}

const orderForm = reactive<OrderForm>({
  orderSource: 1,
  quantity: 1,
  remark: ''
})

const orderRules: FormRules<OrderForm> = {
  orderSource: [{ required: true, message: '请选择采购来源', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入购买数量', trigger: 'blur' },
    {
      validator: (_rule, value: number, callback) => {
        if (!Number.isInteger(value) || value < 1) {
          callback(new Error('数量必须为不小于 1 的整数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

/** 重置下单表单到默认值 */
function resetOrderForm() {
  orderForm.orderSource = 1
  orderForm.quantity = 1
  orderForm.remark = ''
  orderFormRef.value?.clearValidate()
}

/** 打开下单弹窗（仅权益商品可调用） */
function openOrderDialog(row: GoodsInfo) {
  if (!isEquity(row)) return
  currentGoods.value = row
  resetOrderForm()
  orderDialogVisible.value = true
}

/** 关闭下单弹窗 */
function closeOrderDialog() {
  orderDialogVisible.value = false
  currentGoods.value = null
}

/** 提交下单 */
async function handleSubmitOrder() {
  if (!orderFormRef.value || !currentGoods.value) return
  // 校验失败直接返回（错误信息由 el-form 自动显示）
  try {
    await orderFormRef.value.validate()
  } catch {
    return
  }

  const goods = currentGoods.value
  // 防御性校验：商品编码/名称/售价缺失时不可下单
  if (!goods.goodsCode || !goods.goodsName) {
    ElMessage.error('商品信息不完整，无法下单')
    return
  }
  const unitPrice = goods.salePrice ?? goods.originalPrice
  if (unitPrice == null || unitPrice < 0) {
    ElMessage.error('商品未配置价格，无法下单')
    return
  }

  submitting.value = true
  try {
    const orderCode = await createOrderEquity({
      orderSource: orderForm.orderSource,
      goodsCode: goods.goodsCode,
      // goodsName/unitPrice 会被后端权威覆盖，此处仅满足 DTO @NotBlank/@NotNull
      goodsName: goods.goodsName,
      quantity: orderForm.quantity,
      unitPrice,
      remark: orderForm.remark || undefined
    })
    ElMessage.success(`下单成功，订单号：${orderCode}`)
    closeOrderDialog()
    // 不刷新商品列表：商品目录与订单解耦，下单不影响可购商品集合
  } catch (err) {
    // 错误消息已由响应拦截器统一提示，此处不再重复 ElMessage
    void err
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPage().catch((err) => {
    // 后端端点未实现或渠道未配置白名单时，降级：留空 + 控制台 warn（不弹 toast）
    console.warn('[mall] 加载商品列表失败（接口可能未实现或本渠道未配置白名单）:', err)
  })
})
</script>

<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="商品名称">
          <el-input v-model="query.goodsName" placeholder="商品名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="商品类型">
          <el-select v-model="query.goodsType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="o in GOODS_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
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
          <span>商品列表</span>
          <span class="card-header-hint">仅权益类商品支持渠道下单，其他类型请联系平台</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="goodsCode">
        <el-table-column prop="goodsCode" label="商品编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="goodsName" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="goodsType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.goodsType !== undefined && row.goodsType !== null" :type="GOODS_TYPE_TAG[row.goodsType] ?? 'info'">
              {{ goodsTypeText(row.goodsType) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="salePrice" label="售价（元）" width="130" align="right">
          <template #default="{ row }">{{ row.salePrice != null ? Number(row.salePrice).toFixed(2) : '--' }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100" align="right">
          <template #default="{ row }">{{ row.stock != null ? row.stock : '--' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              v-if="!isEquity(row)"
              content="非权益商品请联系平台下单"
              placement="top"
            >
              <span>
                <!-- 用 span 包裹 disabled 按钮，使 tooltip 能在 disabled 状态下触发 -->
                <el-button type="primary" size="small" disabled>下单</el-button>
              </span>
            </el-tooltip>
            <el-button v-else type="primary" size="small" @click="openOrderDialog(row)">下单</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无可购商品" />
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

    <!-- 下单弹窗 -->
    <el-dialog
      v-model="orderDialogVisible"
      title="权益商品下单"
      width="480px"
      :close-on-click-modal="false"
      @closed="resetOrderForm"
    >
      <el-form
        ref="orderFormRef"
        :model="orderForm"
        :rules="orderRules"
        label-width="96px"
        @submit.prevent
      >
        <el-form-item label="商品名称">
          <span class="dialog-static">{{ currentGoods?.goodsName ?? '-' }}</span>
        </el-form-item>
        <el-form-item label="单价">
          <span class="dialog-static">
            {{ currentGoods?.salePrice != null ? `¥ ${Number(currentGoods.salePrice).toFixed(2)}` : '-' }}
            <span v-if="currentGoods?.priceUnit" class="dialog-unit">/ {{ currentGoods.priceUnit }}</span>
          </span>
        </el-form-item>
        <el-form-item label="采购来源" prop="orderSource">
          <el-radio-group v-model="orderForm.orderSource">
            <el-radio :value="1">对公</el-radio>
            <el-radio :value="2">个人</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="购买数量" prop="quantity">
          <el-input-number v-model="orderForm.quantity" :min="1" :step="1" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="orderForm.remark"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeOrderDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitOrder">提交订单</el-button>
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

  .card-header-hint {
    font-size: 12px;
    color: #909399;
    font-weight: normal;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.dialog-static {
  font-size: 14px;
  color: #303133;

  .dialog-unit {
    color: #909399;
    margin-left: 4px;
  }
}
</style>
