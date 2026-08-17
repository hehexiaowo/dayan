<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, ShoppingCart, Delete } from '@element-plus/icons-vue'
import { GOODS_TYPE_OPTIONS, pageGoodsInfos, type GoodsInfo } from '@/api/goods'
import { createOrderEquity } from '@/api/order'
import { formatFileUrl } from '@/utils/file'

/**
 * 大雁商城页（采购结算目录 - 卡片商城 + 本地购物车）。
 *
 * 左右分栏布局：
 * - 左侧：商品卡片网格（搜索栏 + 响应式卡片列表），仅权益商品(goodsType=1)可加入购物车；
 * - 右侧：购物车面板（sticky 固定），显示已选商品 + 数量调整 + 结算下单。
 *
 * 购物车用 localStorage 持久化（key: dayan_channel_cart），无需后端建表/接口。
 * 下单循环调用 createOrderEquity（每个商品一个独立订单，后端只支持单商品下单）。
 * 后端防篡改：goodsName/unitPrice 会被服务端权威覆盖。
 */

/** 商品类型 tag 颜色映射：1=primary(权益) / 2=success(场景) / 3=warning(课程) / 4=info(旅游短居) */
const GOODS_TYPE_TAG: Record<number, 'primary' | 'success' | 'warning' | 'info'> = {
  1: 'primary',
  2: 'success',
  3: 'warning',
  4: 'info'
}

/** 是否权益商品（决定可否加购/下单） */
function isEquity(g: GoodsInfo): boolean {
  return g.goodsType === 1
}

function goodsTypeText(v?: number): string {
  const opt = GOODS_TYPE_OPTIONS.find((o) => o.value === v)
  return opt ? opt.label : '-'
}

// ==================== 商品列表 ====================

const loading = ref(false)
const allGoods = ref<GoodsInfo[]>([])

/** 搜索条件 */
const searchName = ref('')
const searchType = ref<number | undefined>(undefined)

/** 前端过滤后的商品列表 */
const filteredGoods = computed(() => {
  return allGoods.value.filter((g) => {
    if (searchName.value && g.goodsName && !g.goodsName.includes(searchName.value)) return false
    if (searchType.value !== undefined && g.goodsType !== searchType.value) return false
    return true
  })
})

async function loadGoods() {
  loading.value = true
  try {
    const result = await pageGoodsInfos({
      goodsStatus: 1,
      goodsName: '',
      goodsType: undefined,
      current: 1,
      size: 100
    })
    allGoods.value = result.records
  } catch (err) {
    console.warn('[mall] 加载商品列表失败:', err)
  } finally {
    loading.value = false
  }
}

function handleReset() {
  searchName.value = ''
  searchType.value = undefined
}

// ==================== 购物车（localStorage 持久化）====================

interface CartItem {
  goodsCode: string
  goodsName: string
  goodsType: number
  salePrice: number
  quantity: number
}

const CART_KEY = 'dayan_channel_cart'
const cart = ref<CartItem[]>([])

/** 从 localStorage 恢复购物车 */
function loadCart() {
  try {
    const raw = localStorage.getItem(CART_KEY)
    if (raw) {
      cart.value = JSON.parse(raw)
    }
  } catch {
    cart.value = []
  }
}

/** 写回 localStorage */
function saveCart() {
  try {
    localStorage.setItem(CART_KEY, JSON.stringify(cart.value))
  } catch {
    // localStorage 不可用时静默降级（仅内存购物车）
  }
}

/** 查找购物车项索引 */
function cartIndex(goodsCode: string): number {
  return cart.value.findIndex((c) => c.goodsCode === goodsCode)
}

/** 加入购物车（默认数量 1） */
function addToCart(g: GoodsInfo) {
  if (!isEquity(g) || !g.goodsCode) return
  const idx = cartIndex(g.goodsCode)
  if (idx >= 0) {
    cart.value[idx].quantity += 1
  } else {
    cart.value.push({
      goodsCode: g.goodsCode,
      goodsName: g.goodsName ?? g.goodsCode,
      goodsType: g.goodsType ?? 1,
      salePrice: g.salePrice ?? g.originalPrice ?? 0,
      quantity: 1
    })
  }
  saveCart()
  ElMessage.success(`已加入购物车：${g.goodsName}`)
}

/** 修改数量 */
function updateQuantity(goodsCode: string, qty: number) {
  const idx = cartIndex(goodsCode)
  if (idx >= 0) {
    if (qty < 1) {
      // 数量减到 0 则移除
      cart.value.splice(idx, 1)
    } else {
      cart.value[idx].quantity = qty
    }
    saveCart()
  }
}

/** 移除购物车项 */
function removeFromCart(goodsCode: string) {
  const idx = cartIndex(goodsCode)
  if (idx >= 0) {
    cart.value.splice(idx, 1)
    saveCart()
  }
}

/** 合计金额 */
const totalPrice = computed(() => {
  return cart.value.reduce((sum, item) => sum + item.salePrice * item.quantity, 0)
})

// ==================== 结算下单 ====================

const submitting = ref(false)
/** 采购来源：1=对公 / 2=个人 */
const orderSource = ref(1)

async function handleCheckout() {
  if (cart.value.length === 0 || submitting.value) return
  // 二次确认：批量下单不可撤销，按购物车总件数提示
  const totalQuantity = cart.value.reduce((sum, item) => sum + item.quantity, 0)
  try {
    await ElMessageBox.confirm(
      `确定批量结算选中的 ${totalQuantity} 件商品吗？`,
      '批量结算',
      { type: 'warning' }
    )
  } catch {
    return
  }
  submitting.value = true
  const successCodes: string[] = []
  const failedItems: string[] = []

  for (const item of cart.value) {
    try {
      const orderCode = await createOrderEquity({
        orderSource: orderSource.value,
        goodsCode: item.goodsCode,
        goodsName: item.goodsName,
        quantity: item.quantity,
        unitPrice: item.salePrice,
        remark: undefined
      })
      successCodes.push(item.goodsCode)
      ElMessage.success(`${item.goodsName} 下单成功：${orderCode}`)
    } catch {
      failedItems.push(item.goodsName)
    }
  }

  // 移除下单成功的项，保留失败的
  if (successCodes.length > 0) {
    cart.value = cart.value.filter((c) => !successCodes.includes(c.goodsCode))
  }
  saveCart()

  if (failedItems.length > 0) {
    ElMessage.warning(`以下商品下单失败：${failedItems.join('、')}`)
  }

  submitting.value = false
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadGoods()
  loadCart()
})
</script>

<template>
  <div class="mall-container">
    <!-- 左侧：商品区 -->
    <div class="goods-section">
      <!-- 搜索栏（前端实时过滤，无需查询按钮） -->
      <el-card shadow="never" class="search-card">
        <div class="toolbar">
          <el-input v-model="searchName" placeholder="商品名称" clearable style="width: 160px" />
          <el-select v-model="searchType" placeholder="商品类型" clearable style="width: 130px">
            <el-option v-for="o in GOODS_TYPE_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <div class="toolbar-actions">
            <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
          </div>
        </div>
      </el-card>

      <!-- 商品卡片网格 -->
      <div v-loading="loading" class="goods-grid">
        <el-row v-if="filteredGoods.length" :gutter="16">
          <el-col
            v-for="goods in filteredGoods"
            :key="goods.goodsCode"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
          >
            <el-card shadow="hover" class="goods-card" :body-style="{ padding: '0' }">
              <!-- 商品图片 -->
              <div class="goods-image">
                <img v-if="goods.coverImage" :src="formatFileUrl(goods.coverImage)" :alt="goods.goodsName" />
                <div v-else class="goods-image-placeholder">
                  <el-icon :size="40"><Picture /></el-icon>
                  <span>暂无图片</span>
                </div>
                <!-- 类型 tag 叠加在图片右上 -->
                <el-tag
                  v-if="goods.goodsType !== undefined"
                  :type="GOODS_TYPE_TAG[goods.goodsType] ?? 'info'"
                  class="goods-type-tag"
                  effect="dark"
                >
                  {{ goodsTypeText(goods.goodsType) }}
                </el-tag>
              </div>

              <!-- 商品信息 -->
              <div class="goods-info">
                <h4 class="goods-name" :title="goods.goodsName">{{ goods.goodsName }}</h4>
                <p v-if="goods.summary" class="goods-summary">{{ goods.summary }}</p>
                <div class="goods-price">
                  <span class="price-sale">
                    ¥{{ goods.salePrice != null ? Number(goods.salePrice).toFixed(2) : '--' }}
                  </span>
                  <span
                    v-if="goods.originalPrice != null && goods.salePrice != null && goods.originalPrice > goods.salePrice"
                    class="price-original"
                  >
                    ¥{{ Number(goods.originalPrice).toFixed(2) }}
                  </span>
                  <span v-if="goods.priceUnit" class="price-unit">/ {{ goods.priceUnit }}</span>
                </div>

                <!-- 操作区 -->
                <div class="goods-actions">
                  <template v-if="isEquity(goods)">
                    <!-- 已在购物车：显示数量调整 -->
                    <template v-if="cartIndex(goods.goodsCode!) >= 0">
                      <el-input-number
                        :model-value="cart[cartIndex(goods.goodsCode!)].quantity"
                        :min="1"
                        :step="1"
                        :precision="0"
                        size="small"
                        controls-position="right"
                        @change="(v: number) => updateQuantity(goods.goodsCode!, v)"
                      />
                    </template>
                    <!-- 未在购物车：显示加购按钮 -->
                    <el-button v-else type="primary" size="small" @click="addToCart(goods)">
                      加入购物车
                    </el-button>
                  </template>
                  <el-tooltip v-else content="非权益商品请联系平台下单" placement="top">
                    <span>
                      <el-button size="small" disabled>联系平台</el-button>
                    </span>
                  </el-tooltip>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-else-if="!loading" description="暂无商品" />
      </div>
    </div>

    <!-- 右侧：购物车面板 -->
    <div class="cart-section">
      <el-card shadow="never" class="cart-card">
        <template #header>
          <div class="cart-header">
            <el-icon :size="18"><ShoppingCart /></el-icon>
            <span>购物车</span>
            <el-badge v-if="cart.length" :value="cart.length" type="primary" />
          </div>
        </template>

        <!-- 空购物车 -->
        <el-empty v-if="cart.length === 0" description="购物车空空如也" :image-size="80" />

        <!-- 购物车列表 -->
        <template v-else>
          <div class="cart-list">
            <div v-for="item in cart" :key="item.goodsCode" class="cart-item">
              <div class="cart-item-info">
                <div class="cart-item-name" :title="item.goodsName">{{ item.goodsName }}</div>
                <div class="cart-item-price">
                  ¥{{ Number(item.salePrice).toFixed(2) }} ×
                  <el-input-number
                    :model-value="item.quantity"
                    :min="1"
                    :step="1"
                    :precision="0"
                    size="small"
                    controls-position="right"
                    style="width: 110px"
                    @change="(v: number) => updateQuantity(item.goodsCode, v)"
                  />
                </div>
                <div class="cart-item-subtotal">
                  小计 ¥{{ (item.salePrice * item.quantity).toFixed(2) }}
                </div>
              </div>
              <el-button
                type="danger"
                link
                size="small"
                :icon="Delete"
                @click="removeFromCart(item.goodsCode)"
              />
            </div>
          </div>

          <!-- 结算区 -->
          <el-divider />
          <div class="checkout-section">
            <div class="checkout-total">
              <span>合计</span>
              <span class="checkout-amount">¥{{ totalPrice.toFixed(2) }}</span>
            </div>
            <el-form-item label="采购来源" label-width="72px" style="margin: 12px 0">
              <el-select v-model="orderSource" style="width: 100%">
                <el-option :value="1" label="对公" />
                <el-option :value="2" label="个人" />
              </el-select>
            </el-form-item>
            <el-button
              type="primary"
              class="checkout-btn"
              :loading="submitting"
              :disabled="cart.length === 0"
              @click="handleCheckout"
            >
              结算下单（{{ cart.length }} 种）
            </el-button>
          </div>
        </template>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.mall-container {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.goods-section {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cart-section {
  width: 320px;
  flex-shrink: 0;
  position: sticky;
  top: 16px;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 2px;
  }
}

// ==================== 商品卡片 ====================

.goods-grid {
  min-height: 200px;
}

.goods-card {
  margin-bottom: 16px;
  overflow: hidden;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-2px);
  }
}

.goods-image {
  position: relative;
  width: 100%;
  height: 160px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: #c0c4cc;

    span {
      font-size: 12px;
    }
  }
}

.goods-type-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}

.goods-info {
  padding: 12px;
}

.goods-name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 42px;
}

.goods-summary {
  margin: 0 0 8px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-price {
  margin-bottom: 12px;

  .price-sale {
    font-size: 20px;
    font-weight: 700;
    color: #f56c6c;
  }

  .price-original {
    margin-left: 6px;
    font-size: 13px;
    color: #c0c4cc;
    text-decoration: line-through;
  }

  .price-unit {
    font-size: 12px;
    color: #909399;
    margin-left: 2px;
  }
}

.goods-actions {
  display: flex;
  justify-content: center;
}

// ==================== 购物车 ====================

.cart-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.cart-list {
  max-height: calc(100vh - 460px);
  overflow-y: auto;
}

.cart-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  &-info {
    flex: 1;
    min-width: 0;
  }

  &-name {
    font-size: 13px;
    font-weight: 500;
    margin-bottom: 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &-price {
    font-size: 12px;
    color: #606266;
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
  }

  &-subtotal {
    margin-top: 4px;
    font-size: 13px;
    color: #f56c6c;
    font-weight: 600;
  }
}

.checkout-section {
  .checkout-total {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;
    color: #303133;
  }

  .checkout-amount {
    font-size: 22px;
    font-weight: 700;
    color: #f56c6c;
  }

  .checkout-btn {
    width: 100%;
    margin-top: 4px;
  }
}
</style>
