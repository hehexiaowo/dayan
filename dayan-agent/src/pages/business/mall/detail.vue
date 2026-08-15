<template>
  <view class="page dy-safe-bottom">
    <!-- 加载中 -->
    <template v-if="loading">
      <DySkeleton :rows="6" card />
    </template>

    <template v-else-if="product">
      <!-- 商品图片：展示配置轮播图优先，未配置时回退封面图 -->
      <view v-if="banners.length > 1" class="hero">
        <swiper class="hero-swiper" :indicator-dots="true" :circular="true" indicator-active-color="#10aeff">
          <swiper-item v-for="key in banners" :key="key">
            <image :src="formatFileUrl(key)" mode="aspectFill" class="hero-img" />
          </swiper-item>
        </swiper>
      </view>
      <view v-else class="hero">
        <image
          v-if="heroImage"
          :src="formatFileUrl(heroImage)"
          mode="aspectFill"
          class="hero-img"
        />
        <view v-else class="hero-placeholder">
          <text class="hero-placeholder-text">{{ product.goodsName.charAt(0) }}</text>
        </view>
      </view>

      <!-- 商品信息 -->
      <view class="info-card">
        <view class="price-row">
          <text class="price">¥{{ formatPrice(product.salePrice) }}</text>
          <text v-if="product.originalPrice && product.originalPrice !== product.salePrice" class="original-price">
            ¥{{ formatPrice(product.originalPrice) }}
          </text>
        </view>
        <text class="name">{{ product.goodsName }}</text>
        <view class="meta-row">
          <view class="meta-tag">权益</view>
          <text v-if="stockText" class="meta-text">{{ stockText }}</text>
          <text v-if="product.priceUnit" class="meta-text">· {{ product.priceUnit }}</text>
        </view>
      </view>

      <!-- 权益内容（结构化：构成/期限/共享/转让 + 服务项目次数/入住权/随心住规则/网络范围） -->
      <view v-if="product.equity" class="equity-card">
        <text class="desc-title">权益内容</text>
        <view class="equity-overview">
          <view class="ov-item">
            <text class="ov-label">权益人</text>
            <text class="ov-val">{{ holderText(product.equity) }}（{{ product.equity.personCount || 1 }}人）</text>
          </view>
          <view class="ov-item">
            <text class="ov-label">权益期限</text>
            <text class="ov-val">{{ validityText(product.equity) }}</text>
          </view>
          <view class="ov-item">
            <text class="ov-label">次数口径</text>
            <text class="ov-val">{{ product.equity.shareMode === 0 ? '按人独立配额' : '权益人共享' }}</text>
          </view>
          <view class="ov-item">
            <text class="ov-label">转让</text>
            <text class="ov-val">{{ transferText }}</text>
          </view>
        </view>

        <view
          v-for="it in product.equity.serviceItems || []"
          :key="it.itemCode"
          class="svc-item"
        >
          <view class="svc-head">
            <text class="svc-name">{{ it.itemName || it.itemCode }}</text>
            <text class="svc-quota">{{ it.quantity }}{{ it.quotaType === 1 ? '次/权益期' : '次/年' }}</text>
          </view>
          <view v-if="rightTags(it).length || it.usageRule" class="svc-tags">
            <text v-for="t in rightTags(it)" :key="t" class="svc-tag">{{ t }}</text>
            <text v-if="it.usageRule" class="svc-tag tg-blue">随心住</text>
          </view>
          <text v-if="usageBrief(it)" class="svc-rule">{{ usageBrief(it) }}</text>
          <text class="svc-net">服务网络：{{ networkText(it) }}</text>
        </view>
      </view>

      <!-- 商品描述 -->
      <view v-if="product.goodsDescription || product.summary" class="desc-card">
        <text class="desc-title">商品详情</text>
        <text class="desc-content">{{ product.goodsDescription || product.summary }}</text>
      </view>

      <!-- 结构化展示板块（产品介绍/权益详解/服务流程/常见问题/购买须知，来自 admin 页面配置） -->
      <view
        v-for="(block, bi) in displayBlocks"
        :key="block.id || bi"
        class="desc-card block-card"
      >
        <text class="desc-title">{{ blockTitleOf(block) }}</text>
        <rich-text v-if="block.content" :nodes="block.content" class="block-rich" />
        <view v-if="blockImages(block).length" class="block-images">
          <view
            v-for="(img, ii) in blockImages(block)"
            :key="ii"
            class="block-image-item"
          >
            <image
              :src="formatFileUrl(img)"
              mode="widthFix"
              class="block-img"
              @click="previewImages(block, ii)"
            />
            <text v-if="blockDesc(block, ii)" class="block-img-desc">{{ blockDesc(block, ii) }}</text>
          </view>
        </view>
      </view>
    </template>

    <!-- 加载失败 -->
    <DyEmpty
      v-else
      text="商品不存在或已下架"
      icon="!"
      color="gray"
    />

    <!-- 底部固定栏 -->
    <view v-if="product" class="bottom-bar">
      <view class="quantity-section">
        <text class="quantity-label">数量</text>
        <view class="quantity-control">
          <view
            class="qty-btn dy-clickable"
            :class="{ disabled: quantity <= 1 }"
            @click="changeQty(-1)"
          >-</view>
          <text class="qty-num">{{ quantity }}</text>
          <view
            class="qty-btn dy-clickable"
            :class="{ disabled: !canIncrease }"
            @click="changeQty(1)"
          >+</view>
        </view>
      </view>
      <view class="buy-section">
        <view class="total">
          <text class="total-label">合计</text>
          <text class="total-amount">¥{{ formatPrice(totalAmount) }}</text>
        </view>
        <button
          class="buy-btn"
          :class="{ disabled: !canBuy }"
          :disabled="!canBuy"
          @click="onBuy"
        >{{ canBuy ? '立即购买' : '暂无库存' }}</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getGoodsDetail } from '@/api/goods';
import { createOrderEquity } from '@/api/order';
import { formatFileUrl } from '@/utils/file';
import { holderText, validityText, rightTags, usageBrief, networkText, blockTitleOf, parseImagesArr, parseDisplayConfig } from '@/types';
import type { GoodsProduct, GoodsDisplayBlock } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const product = ref<GoodsProduct | null>(null);
const loading = ref(true);
const quantity = ref(1);
const submitting = ref(false);
const goodsCode = ref('');

/** 展示配置轮播图（无 banners 时不启用轮播） */
const banners = computed(() => parseDisplayConfig(product.value?.displayConfig).banners);
/** hero 单图：banners 单张或未配置时回退封面图 */
const heroImage = computed(() => {
  const cfg = parseDisplayConfig(product.value?.displayConfig);
  if (cfg.banners.length === 1) return cfg.banners[0];
  return product.value?.coverImage || '';
});
/** 详情页展示板块（后端仅返回显示态、按 sortOrder 升序） */
const displayBlocks = computed(() => product.value?.displayBlocks || []);

function blockImages(block: GoodsDisplayBlock): string[] {
  return parseImagesArr(block.images);
}
function blockDesc(block: GoodsDisplayBlock, index: number): string {
  const descs = parseImagesArr(block.imageDescriptions);
  return descs[index] || '';
}
function previewImages(block: GoodsDisplayBlock, index: number) {
  const urls = blockImages(block).map((k) => formatFileUrl(k));
  if (!urls.length) return;
  uni.previewImage({ urls, current: urls[index] || urls[0] });
}

const transferText = computed(() => {
  const mt = product.value?.equity?.maxTransferable ?? 0;
  return mt > 0 ? `可转让${mt}次` : '不可转让';
});

const canIncrease = computed(() => {
  if (!product.value) return false;
  const stock = product.value.stock;
  // stock = -1 表示不限
  if (stock == null || stock < 0) return true;
  return quantity.value < stock;
});

const canBuy = computed(() => {
  if (!product.value) return false;
  const stock = product.value.stock;
  // stock = -1 或 null 表示不限；stock = 0 表示无库存
  return stock == null || stock !== 0;
});

const stockText = computed(() => {
  if (!product.value) return '';
  const stock = product.value.stock;
  if (stock == null) return '';
  if (stock < 0) return '库存充足';
  if (stock === 0) return '暂无库存';
  return `库存 ${stock} 件`;
});

const totalAmount = computed(() => {
  if (!product.value || !product.value.salePrice) return 0;
  return Number(product.value.salePrice) * quantity.value;
});

function formatPrice(price?: number): string {
  if (price == null) return '-';
  return Number(price).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

function changeQty(delta: number) {
  const next = quantity.value + delta;
  if (next < 1) return;
  if (delta > 0 && !canIncrease.value) return;
  quantity.value = next;
}

async function loadDetail() {
  loading.value = true;
  try {
    product.value = await getGoodsDetail(goodsCode.value);
  } catch (e) {
    product.value = null;
  } finally {
    loading.value = false;
  }
}

async function onBuy() {
  if (!product.value || !canBuy.value || submitting.value) return;

  // 确认弹窗
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '确认购买',
      content: `商品：${product.value!.goodsName}\n数量：${quantity.value} 件\n合计：¥${formatPrice(totalAmount.value)}`,
      confirmText: '确认下单',
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    });
  });
  if (!confirmed) return;

  submitting.value = true;
  uni.showLoading({ title: '下单中...' });
  try {
    await createOrderEquity({
      goodsCode: product.value.goodsCode,
      goodsName: product.value.goodsName,
      quantity: quantity.value,
      unitPrice: Number(product.value.salePrice || 0),
    });
    uni.hideLoading();
    uni.showToast({ title: '下单成功', icon: 'success' });
    setTimeout(() => uni.navigateBack(), 1000);
  } catch (e) {
    uni.hideLoading();
    // 错误已由 request 拦截器提示
  } finally {
    submitting.value = false;
  }
}

onLoad((query) => {
  goodsCode.value = query?.code || '';
  if (goodsCode.value) {
    loadDetail();
  } else {
    loading.value = false;
  }
});
</script>

<style lang="scss" scoped>

.page {
  padding-bottom: 140rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* 商品大图 */
.hero {
  width: 100%;
  height: 500rpx;
  background: $bg-card;
}
.hero-swiper {
  width: 100%;
  height: 100%;
}
.hero-img {
  width: 100%;
  height: 100%;
}
.hero-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $gradient-blue;
}
.hero-placeholder-text {
  font-size: 120rpx;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.4);
}

/* 商品信息卡 */
.info-card {
  background: $bg-card;
  padding: $spacing-lg $spacing-md;
  margin-bottom: $spacing-sm;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: $spacing-sm;
}
.price {
  font-size: 48rpx;
  font-weight: bold;
  color: $brand-error;
}
.original-price {
  font-size: 26rpx;
  color: $text-placeholder;
  text-decoration: line-through;
}
.name {
  display: block;
  font-size: 32rpx;
  font-weight: 500;
  color: $text-primary;
  margin-top: $spacing-sm;
  line-height: 1.5;
}
.meta-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
}
.meta-tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 16rpx;
  background: $brand-primary-light;
  color: $brand-primary;
}
.meta-text {
  font-size: 24rpx;
  color: $text-secondary;
}

/* 商品描述 */
.desc-card {
  background: $bg-card;
  padding: $spacing-lg $spacing-md;
}
.desc-title {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: $spacing-sm;
  padding-bottom: $spacing-sm;
  border-bottom: 2rpx solid $border-light;
}
.desc-content {
  font-size: 26rpx;
  color: $text-regular;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* 结构化展示板块 */
.block-card {
  margin-top: $spacing-sm;
}
.block-rich {
  display: block;
  margin-top: $spacing-sm;
  font-size: 26rpx;
  color: $text-regular;
  line-height: 1.8;
}
.block-images {
  margin-top: $spacing-sm;
}
.block-image-item {
  margin-bottom: $spacing-sm;
}
.block-img {
  width: 100%;
  border-radius: $radius-sm;
}
.block-img-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: $text-secondary;
  text-align: center;
}

/* 权益内容 */
.equity-card {
  background: $bg-card;
  padding: $spacing-lg $spacing-md;
  margin-bottom: $spacing-sm;
}
.equity-overview {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx 32rpx;
  margin-bottom: $spacing-md;
}
.ov-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.ov-label {
  font-size: 24rpx;
  color: $text-secondary;
}
.ov-val {
  font-size: 24rpx;
  color: $text-primary;
  font-weight: 500;
}
.svc-item {
  padding: $spacing-sm 0;
  border-top: 2rpx solid $border-light;
}
.svc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.svc-name {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  flex: 1;
}
.svc-quota {
  font-size: 26rpx;
  color: $brand-primary;
  font-weight: bold;
  flex-shrink: 0;
  margin-left: 16rpx;
}
.svc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 10rpx;
}
.svc-tag {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
  line-height: 32rpx;
  color: #07c160;
  background: rgba(7, 193, 96, 0.1);

  &.tg-blue {
    color: #10aeff;
    background: rgba(16, 174, 255, 0.1);
  }
}
/* 入住权首标签着色（保证红/优先橙由行内顺序决定，统一绿色系外再区分太细，保持简洁） */
.svc-rule {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $text-secondary;
  line-height: 1.6;
}
.svc-net {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: $text-placeholder;
}

/* 底部固定栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  padding-bottom: calc(#{$spacing-md} + env(safe-area-inset-bottom));
  background: $bg-card;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
  z-index: 100;
}

/* 数量选择 */
.quantity-section {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}
.quantity-label {
  font-size: 26rpx;
  color: $text-regular;
}
.quantity-control {
  display: flex;
  align-items: center;
  gap: 4rpx;
}
.qty-btn {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid $border-base;
  border-radius: $radius-sm;
  font-size: 32rpx;
  color: $text-primary;
  background: $bg-card;

  &.disabled {
    color: $text-placeholder;
    opacity: 0.5;
  }
}
.qty-num {
  min-width: 60rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 500;
  color: $text-primary;
}

/* 购买区 */
.buy-section {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}
.total {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.total-label {
  font-size: 22rpx;
  color: $text-secondary;
}
.total-amount {
  font-size: 36rpx;
  font-weight: bold;
  color: $brand-error;
}
.buy-btn {
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 48rpx;
  background: $gradient-blue;
  color: #fff;
  font-size: 28rpx;
  font-weight: 500;
  border-radius: $radius-md;
  border: none;

  &.disabled {
    background: $text-placeholder;
  }
}
</style>
