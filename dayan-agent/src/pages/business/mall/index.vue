<template>
  <view class="page dy-safe-bottom">
    <!-- 搜索栏 -->
    <view class="toolbar">
      <view class="search">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索商品名称"
          confirm-type="search"
          @confirm="onSearch"
        />
        <text v-if="keyword" class="search-clear" @click="keyword = ''">×</text>
        <view class="btn-search dy-clickable" @click="onSearch">搜索</view>
      </view>
    </view>

    <!-- 商品列表 -->
    <view class="list">
      <!-- 加载骨架 -->
      <template v-if="loading && !products.length">
        <view class="grid">
          <view v-for="i in 4" :key="i" class="skeleton-card">
            <DySkeleton :rows="3" card />
          </view>
        </view>
      </template>

      <!-- 加载错误 -->
      <DyEmpty
        v-else-if="loadError"
        text="加载失败，请检查网络后重试"
        icon="!"
        color="gray"
        action-text="重新加载"
        @action="loadList"
      />

      <!-- 空状态 -->
      <DyEmpty
        v-else-if="!filtered.length"
        text="暂无可购商品"
        icon="品"
        color="orange"
      />

      <!-- 商品网格 -->
      <view v-else class="grid">
        <view
          v-for="product in filtered"
          :key="product.goodsCode"
          class="product-card dy-clickable"
          @click="goDetail(product.goodsCode)"
        >
          <view class="product-image">
            <image
              v-if="cardImage(product)"
              :src="formatFileUrl(cardImage(product)!)"
              mode="aspectFill"
              class="cover-img"
            />
            <view v-else class="cover-placeholder">
              <text class="placeholder-text">{{ product.goodsName.charAt(0) }}</text>
            </view>
          </view>
          <view class="product-info">
            <text class="product-name">{{ product.goodsName }}</text>
            <!-- 权益内容摘要（次数 + 人数/期限徽标） -->
            <view v-if="product.equity" class="equity-brief">
              <view v-if="quotaLines(product).length" class="quota-tags">
                <text v-for="q in quotaLines(product)" :key="q" class="quota-tag">{{ q }}</text>
              </view>
              <view class="equity-meta">
                <text class="meta-chip">{{ holderText(product.equity) }}</text>
                <text class="meta-chip">{{ validityText(product.equity) }}</text>
              </view>
            </view>
            <view class="product-bottom">
              <text class="product-price">¥{{ formatPrice(product.salePrice) }}</text>
              <view class="product-tag">权益</view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getGoodsList } from '@/api/goods';
import { formatFileUrl } from '@/utils/file';
import { holderText, validityText, quotaText, parseDisplayConfig } from '@/types';
import type { GoodsProduct } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const keyword = ref('');
const products = ref<GoodsProduct[]>([]);
const loading = ref(false);
const loadError = ref(false);

/** 列表卡片图：展示配置缩略图优先，未配置回退封面图 */
function cardImage(product: GoodsProduct): string | undefined {
  const cfg = parseDisplayConfig(product.displayConfig);
  return cfg.thumbnail || product.coverImage || '';
}

/** 权益次数摘要行：每项服务 "旅居6次/年" 等 */
function quotaLines(product: GoodsProduct): string[] {
  return (product.equity?.serviceItems || []).slice(0, 3).map((it) => quotaText(it));
}

const filtered = computed(() => {
  if (!keyword.value.trim()) return products.value;
  const kw = keyword.value.trim().toLowerCase();
  return products.value.filter((p) => p.goodsName.toLowerCase().includes(kw));
});

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    products.value = await getGoodsList();
  } catch (e) {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  // 前端过滤，computed 自动响应
}

function goDetail(goodsCode: string) {
  uni.navigateTo({ url: `/pages/business/mall/detail?code=${goodsCode}` });
}

function formatPrice(price?: number): string {
  if (price == null) return '-';
  return Number(price).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

onShow(() => {
  loadList();
});
</script>

<style lang="scss" scoped>

.page {
  padding: $spacing-md $spacing-md 140rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* 搜索栏 */
.toolbar {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  box-shadow: $shadow-card;
}
.search {
  display: flex;
  align-items: center;
}
.search-input {
  flex: 1;
  height: $control-height-sm;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  padding: 0 20rpx;
  font-size: 28rpx;
  background: $bg-page;
  box-sizing: border-box;
}
.search-clear {
  padding: 0 16rpx;
  font-size: 36rpx;
  color: $text-placeholder;
}
.btn-search {
  margin-left: $spacing-sm;
  height: $control-height-sm;
  line-height: $control-height-sm;
  background: $gradient-blue;
  color: #fff;
  font-size: 26rpx;
  padding: 0 32rpx;
  border-radius: $radius-md;
}

/* 商品网格 */
.list {
  margin-top: $spacing-md;
}
.grid {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}
.product-card {
  width: calc(50% - #{$spacing-sm} / 2);
  background: $bg-card;
  border-radius: $radius-md;
  overflow: hidden;
  box-shadow: $shadow-card;
}
.skeleton-card {
  width: calc(50% - #{$spacing-sm} / 2);
}

/* 商品图片 */
.product-image {
  width: 100%;
  height: 280rpx;
  background: $bg-page;
}
.cover-img {
  width: 100%;
  height: 100%;
}
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $brand-primary-light;
}
.placeholder-text {
  font-size: 72rpx;
  font-weight: bold;
  color: $brand-primary;
  opacity: 0.4;
}

/* 商品信息 */
.product-info {
  padding: $spacing-sm $spacing-md $spacing-md;
}
.product-name {
  display: block;
  font-size: 26rpx;
  font-weight: 500;
  color: $text-primary;
  line-height: 1.4;
  /* 两行省略 */
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 72rpx;
}
.product-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: $spacing-sm;
}

/* 权益摘要 */
.equity-brief {
  margin-top: $spacing-xs;
}
.quota-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}
.quota-tag {
  font-size: 20rpx;
  color: $brand-primary;
  background: $brand-primary-light;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
  line-height: 30rpx;
}
.equity-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 8rpx;
}
.meta-chip {
  font-size: 20rpx;
  color: $text-secondary;
  background: $bg-page;
  border-radius: 6rpx;
  padding: 2rpx 10rpx;
  line-height: 30rpx;
}
.product-price {
  font-size: 32rpx;
  font-weight: bold;
  color: $brand-error;
}
.product-tag {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 16rpx;
  background: $brand-primary-light;
  color: $brand-primary;
}
</style>
