<template>
  <view class="page dy-safe-bottom">
    <!-- 渐变 header（与其他 tab 页统一） -->
    <view class="header">
      <text class="header-title">我的权益</text>
      <text class="header-sub">查询卡号、激活码、绑定码</text>
    </view>

    <!-- 搜索栏 -->
    <view class="toolbar">
      <view class="search">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索卡号 / 激活码 / 绑定码"
          confirm-type="search"
          @confirm="onSearch"
        />
        <text v-if="keyword" class="search-clear" @click="keyword = ''; onSearch()">×</text>
        <view class="btn-search dy-clickable" @click="onSearch">搜索</view>
      </view>
    </view>

    <!-- 统计卡片 -->
    <view class="stats-row">
      <view class="stat-item">
        <text class="stat-num">{{ stats?.total ?? '-' }}</text>
        <text class="stat-label">总数</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ stats?.stock ?? '-' }}</text>
        <text class="stat-label">库存</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ stats?.activated ?? '-' }}</text>
        <text class="stat-label">已激活</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ stats?.inUse ?? '-' }}</text>
        <text class="stat-label">使用中</text>
      </view>
    </view>

    <!-- 状态 Tab -->
    <scroll-view scroll-x class="tab-bar" :show-scrollbar="false">
      <view
        v-for="tab in STATUS_TABS"
        :key="tab.label"
        class="tab-item dy-clickable"
        :class="{ active: activeStatus === tab.value }"
        @click="onTabChange(tab.value)"
      >
        <text>{{ tab.label }}</text>
      </view>
    </scroll-view>

    <!-- 权益卡列表 -->
    <view class="list">
      <!-- 加载骨架 -->
      <template v-if="loading && !cards.length">
        <DySkeleton :rows="2" card />
        <DySkeleton :rows="2" card />
        <DySkeleton :rows="2" card />
      </template>

      <!-- 加载错误态 -->
      <DyEmpty
        v-else-if="loadError"
        text="加载失败，请检查网络后重试"
        icon="!"
        color="gray"
        action-text="重新加载"
        @action="loadAll"
      />

      <!-- 空状态 -->
      <DyEmpty
        v-else-if="!cards.length"
        text="暂无权益卡"
        icon="卡"
        color="orange"
      />

      <!-- 权益卡卡片 -->
      <view v-else>
        <view
          v-for="card in cards"
          :key="card.equityCode"
          class="equity-card dy-clickable"
          @click="onCardClick(card)"
        >
          <!-- 卡片头：卡号 + 标签 -->
          <view class="card-header">
            <text class="card-no">{{ card.equityNo || card.equityCode }}</text>
            <view class="card-badges">
              <view class="badge carrier" :class="carrierCls(card.carrierType)">
                {{ carrierText(card.carrierType) }}
              </view>
              <view class="badge status" :class="statusCls(card.equityStatus)">
                {{ statusText(card.equityStatus) }}
              </view>
            </view>
          </view>

          <!-- 商品名 -->
          <view v-if="card.goodsName" class="card-goods">
            <text>{{ card.goodsName }}</text>
            <text v-if="card.personCount" class="card-persons">· {{ card.personCount }}人</text>
          </view>

          <!-- 客户信息（已激活/使用中/完成） -->
          <view v-if="card.clientName" class="card-client">
            <text class="client-name">{{ card.clientName }}</text>
            <text v-if="card.clientPhone" class="client-phone">{{ maskPhone(card.clientPhone) }}</text>
          </view>

          <!-- 激活/使用信息 -->
          <view class="card-dates">
            <text v-if="card.activateTime" class="date-item">
              激活：{{ formatDate(card.activateTime) }}
            </text>
            <text v-if="card.expireTime" class="date-item">
              有效期至：{{ formatDate(card.expireTime) }}
            </text>
          </view>

          <!-- 激活码/绑定码（库存/出库状态） -->
          <view v-if="shouldShowCode(card)" class="card-code">
            <text v-if="card.activateCode" class="code-text">激活码：{{ card.activateCode }}</text>
            <text v-if="card.bindCode" class="code-text">绑定码：{{ card.bindCode }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
import { getEquityCards, getEquityStats } from '@/api/equity';
import type { EquityCard, EquityStats } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

/** 状态筛选 Tab 配置 */
const STATUS_TABS = [
  { label: '全部', value: null as number | null },
  { label: '库存', value: 0 },
  { label: '赠出', value: 1 },
  { label: '激活', value: 2 },
  { label: '使用', value: 3 },
  { label: '完成', value: 4 },
] as const;

const keyword = ref('');
const activeStatus = ref<number | null>(null);
const cards = ref<EquityCard[]>([]);
const stats = ref<EquityStats | null>(null);
const loading = ref(false);
const loadError = ref(false);

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    const res = await getEquityCards({
      keyword: keyword.value || undefined,
      equityStatus: activeStatus.value,
      current: 1,
      size: 50,
    });
    cards.value = res?.records || [];
  } catch (e) {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

async function loadStats() {
  try {
    stats.value = await getEquityStats();
  } catch (e) {
    // 统计失败不阻断列表
  }
}

async function loadAll() {
  await Promise.all([loadList(), loadStats()]);
}

function onSearch() {
  loadList();
}

function onTabChange(status: number | null) {
  activeStatus.value = status;
  loadList();
}

function onCardClick(card: EquityCard) {
  uni.navigateTo({
    url: `/pages/service/detail?equityCode=${encodeURIComponent(card.equityCode)}`,
  });
}

// ===== 显示辅助函数 =====

function carrierText(t?: number): string {
  return t === 2 ? '函' : '卡';
}

function carrierCls(t?: number): string {
  return t === 2 ? 'carrier-letter' : 'carrier-card';
}

function statusText(s?: number): string {
  const map: Record<number, string> = {
    0: '库存中', 1: '已出库', 2: '已激活', 3: '使用中',
    4: '已完成', 5: '已过期', 6: '已作废', 7: '更换中',
  };
  return map[s ?? -1] || '未知';
}

function statusCls(s?: number): string {
  const map: Record<number, string> = {
    0: 'st-stock', 1: 'st-outbound', 2: 'st-activated', 3: 'st-inuse',
    4: 'st-completed', 5: 'st-expired', 6: 'st-void', 7: 'st-changing',
  };
  return map[s ?? -1] || 'st-stock';
}

function shouldShowCode(card: EquityCard): boolean {
  const s = card.equityStatus;
  // 库存(0)或出库(1)状态时显示激活码/绑定码
  return (s === 0 || s === 1) && !!(card.activateCode || card.bindCode);
}

function formatDate(dt?: string): string {
  if (!dt) return '-';
  return dt.length >= 10 ? dt.substring(0, 10) : dt;
}

function maskPhone(phone?: string): string {
  if (!phone) return '';
  if (phone.length >= 11) {
    return phone.substring(0, 3) + '****' + phone.substring(7);
  }
  return phone;
}

onShow(() => {
  loadAll();
});

onPullDownRefresh(async () => {
  try {
    await loadAll();
  } finally {
    uni.stopPullDownRefresh();
  }
});
</script>

<style lang="scss" scoped>

.page {
  padding: $spacing-md $spacing-md 140rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* 渐变 header（与其他 tab 页统一） */
.header {
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  margin-bottom: $spacing-md;
}
.header-title {
  display: block;
  font-size: 38rpx;
  font-weight: bold;
  color: #fff;
}
.header-sub {
  display: block;
  margin-top: $spacing-sm;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
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
  box-shadow: 0 4rpx 12rpx rgba(64, 158, 255, 0.3);
}

/* 统计卡片 */
.stats-row {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-md;
}
.stat-item {
  flex: 1;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md 0;
  text-align: center;
  box-shadow: $shadow-card;
}
.stat-num {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: $brand-primary;
}
.stat-label {
  display: block;
  font-size: 22rpx;
  color: $text-secondary;
  margin-top: 4rpx;
}

/* 状态 Tab */
.tab-bar {
  display: flex;
  white-space: nowrap;
  margin-top: $spacing-md;
  padding-bottom: 4rpx;
}
.tab-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12rpx 28rpx;
  margin-right: $spacing-sm;
  border-radius: $radius-md;
  font-size: 26rpx;
  color: $text-regular;
  background: $bg-card;
  border: 2rpx solid transparent;
  flex-shrink: 0;
}
.tab-item.active {
  color: #fff;
  background: $gradient-blue;
  font-weight: 500;
}

/* 权益卡列表 */
.list {
  margin-top: $spacing-md;
}
.equity-card {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md $spacing-lg;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}

/* 卡片头 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-no {
  font-size: 28rpx;
  font-weight: bold;
  color: $text-primary;
}
.card-badges {
  display: flex;
  gap: 8rpx;
}
.badge {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
}

/* 载体类型 */
.carrier-card {
  background: $brand-primary-light;
  color: $brand-primary;
}
.carrier-letter {
  background: $brand-warning-light;
  color: $brand-warning;
}

/* 状态标签 */
.st-stock { background: $brand-info-light; color: $brand-info; }
.st-outbound { background: $brand-primary-light; color: $brand-primary; }
.st-activated { background: $brand-success-light; color: $brand-success; }
.st-inuse { background: $brand-warning-light; color: $brand-warning; }
.st-completed { background: $brand-primary-light; color: $brand-primary; }
.st-expired { background: $brand-error-light; color: $brand-error; }
.st-void { background: $brand-error-light; color: $brand-error; }
.st-changing { background: $brand-warning-light; color: $brand-warning; }

/* 商品名 */
.card-goods {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: $text-regular;
}
.card-persons {
  color: $text-secondary;
  margin-left: 8rpx;
}

/* 客户信息 */
.card-client {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: 10rpx;
}
.client-name {
  font-size: 28rpx;
  font-weight: 500;
  color: $text-primary;
}
.client-phone {
  font-size: 24rpx;
  color: $text-secondary;
}

/* 日期信息 */
.card-dates {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 8rpx;
}
.date-item {
  font-size: 24rpx;
  color: $text-placeholder;
}

/* 激活码/绑定码 */
.card-code {
  margin-top: 8rpx;
  display: flex;
  gap: 24rpx;
}
.code-text {
  font-size: 24rpx;
  color: $brand-primary;
  font-family: monospace;
}
</style>
