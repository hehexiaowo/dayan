<template>
  <view class="page">
    <!-- 状态筛选 tabs -->
    <view class="tabs">
      <text
        v-for="t in tabs"
        :key="t.group"
        class="tab-item"
        :class="{ active: currentTab === t.group }"
        @click="switchTab(t.group)"
      >
        {{ t.label }}
      </text>
    </view>

    <!-- 订单列表 -->
    <view v-if="orders.length" class="order-list">
      <view v-for="o in orders" :key="o.orderCode" class="order-card">
        <view class="card-head">
          <text class="park-name">{{ o.parkName || o.title || '旅居订单' }}</text>
          <text class="status-tag" :class="statusClass(o.orderStatus)">{{ o.statusText || statusFallback(o.orderStatus) }}</text>
        </view>
        <view v-if="o.skuName" class="card-row">
          <text class="row-label">房型</text>
          <text class="row-value">{{ o.skuName }}</text>
        </view>
        <view class="card-row">
          <text class="row-label">入住</text>
          <text class="row-value">{{ o.checkinDate || '-' }} ~ {{ o.checkoutDate || '-' }}</text>
        </view>
        <view class="card-row">
          <text class="row-label">天数</text>
          <text class="row-value">{{ o.stayDays ?? '-' }} 晚</text>
        </view>
        <view class="card-foot">
          <text class="order-no">订单号：{{ o.orderCode }}</text>
          <view class="amount-wrap">
            <text v-if="o.payAmount && o.payAmount > 0" class="amount">¥{{ formatAmount(o.payAmount) }}</text>
            <text v-else class="amount unpaid">待付款 ¥{{ formatAmount(o.totalAmount) }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 空态 -->
    <DyEmpty v-else-if="!loading" text="暂无相关订单" />

    <!-- 底部提示 -->
    <view v-if="orders.length" class="list-footer">
      <text class="footer-text">仅显示旅居订单 · 共 {{ total }} 条</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getOrders } from '@/api/order';
import type { Order, OrderStatus } from '@/types';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const tabs = [
  { group: 'ALL', label: '全部' },
  { group: 'PENDING', label: '待支付' },
  { group: 'ACTIVE', label: '进行中' },
  { group: 'DONE', label: '已完成' },
];

const currentTab = ref('ALL');
const orders = ref<Order[]>([]);
const total = ref(0);
const loading = ref(true);

async function loadOrders() {
  loading.value = true;
  try {
    const res = await getOrders({ group: currentTab.value, current: 1, size: 50 });
    orders.value = res.records ?? [];
    total.value = res.total ?? 0;
  } catch {
    orders.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function switchTab(group: string) {
  if (currentTab.value === group) return;
  currentTab.value = group;
  loadOrders();
}

/** 状态色标：待支付橙 / 进行中蓝 / 已完成绿 / 已取消灰 / 退款红 */
function statusClass(s?: number): string {
  switch (s) {
    case 0:
      return 'tag-warning';
    case 1:
    case 2:
    case 3:
      return 'tag-primary';
    case 4:
      return 'tag-success';
    case 5:
      return 'tag-info';
    case 6:
    case 7:
      return 'tag-error';
    default:
      return 'tag-info';
  }
}

/** statusText 缺失时的前端兜底文案（与后端一致） */
function statusFallback(s?: number): string {
  const map: Record<number, string> = {
    0: '待支付', 1: '已支付', 2: '部分发放', 3: '已发放',
    4: '已完成', 5: '已取消', 6: '退款中', 7: '已退款',
  };
  return s == null ? '' : map[s] ?? '';
}

function formatAmount(v?: number): string {
  if (v == null) return '0.00';
  return Number(v).toFixed(2);
}

onShow(() => {
  loadOrders();
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 60rpx;
}

/* tabs */
.tabs {
  display: flex;
  background: $bg-card;
  padding: 0 $spacing-sm;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: $shadow-card;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: $text-secondary;
  position: relative;

  &.active {
    color: $brand-primary;
    font-weight: bold;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 48rpx;
      height: 4rpx;
      background: $brand-primary;
      border-radius: 2rpx;
    }
  }
}

/* 订单列表 */
.order-list {
  padding: $spacing-md $spacing-lg;
}
.order-card {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: $spacing-sm;
  border-bottom: 1rpx solid $border-light;
}
.park-name {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
  flex: 1;
  margin-right: $spacing-sm;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.status-tag {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
}
.tag-warning {
  color: $brand-warning;
  background: $brand-warning-light;
}
.tag-primary {
  color: $brand-primary-dark;
  background: $brand-primary-light;
}
.tag-success {
  color: $brand-success;
  background: $brand-success-light;
}
.tag-info {
  color: $brand-info;
  background: $brand-info-light;
}
.tag-error {
  color: $brand-error;
  background: $brand-error-light;
}

.card-row {
  display: flex;
  align-items: center;
  padding: 14rpx 0;
}
.row-label {
  width: 100rpx;
  font-size: 26rpx;
  color: $text-secondary;
  flex-shrink: 0;
}
.row-value {
  flex: 1;
  font-size: 26rpx;
  color: $text-regular;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: $spacing-sm;
  margin-top: $spacing-xs;
  border-top: 1rpx solid $border-light;
}
.order-no {
  font-size: 22rpx;
  color: $text-placeholder;
}
.amount {
  font-size: 32rpx;
  font-weight: bold;
  color: $brand-error;
}
.amount.unpaid {
  color: $brand-warning;
}

.list-footer {
  text-align: center;
  padding: $spacing-lg 0;
}
.footer-text {
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>
