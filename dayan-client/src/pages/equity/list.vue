<template>
  <view class="list-page dy-safe-bottom">
    <!-- 加载骨架 -->
    <template v-if="loading && list.length === 0">
      <DySkeleton :rows="2" card />
      <DySkeleton :rows="2" card />
      <DySkeleton :rows="2" card />
    </template>

    <!-- 空状态 -->
    <DyEmpty
      v-else-if="list.length === 0"
      text="还没有权益"
      icon="卡"
      color="green"
      action-text="去激活权益"
      @action="goActivate"
    />

    <scroll-view
      v-else
      scroll-y
      class="equity-scroll"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="equity-list">
        <view
          v-for="eq in list"
          :key="eq.equityCode"
          class="equity-card dy-clickable"
          @click="goDetail(eq.equityCode)"
        >
          <view class="card-top">
            <text class="eq-name">{{ eq.goodsName || eq.equityName || '养老权益' }}</text>
            <text class="eq-status" :class="statusClass(eq.equityStatus)">{{ statusText(eq.equityStatus) }}</text>
          </view>
          <view class="card-body">
            <view class="info-row">
              <text class="info-label">权益编码</text>
              <text class="info-val">{{ eq.equityCode }}</text>
            </view>
            <view v-if="eq.equityType" class="info-row">
              <text class="info-label">类型</text>
              <text class="info-val">{{ eq.equityType }}</text>
            </view>
          </view>
          <view class="card-bottom">
            <text v-if="eq.expireTime" class="expire">有效期至 {{ formatDate(eq.expireTime) }}</text>
            <text v-else-if="eq.validDays" class="expire">有效期 {{ eq.validDays }} 天</text>
            <text class="arrow">查看详情 ></text>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="bottom-bar">
      <button class="dy-btn dy-btn-primary" @click="goActivate">+ 激活新权益</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getEquities } from '@/api/equity';
import type { Equity, EquityStatus } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const list = ref<Equity[]>([]);
const loading = ref(false);
const refreshing = ref(false);

const STATUS_TEXT: Record<number, string> = {
  2: '已激活', 3: '使用中', 4: '已完成', 5: '已过期', 6: '已作废',
};
const STATUS_CLASS: Record<number, string> = {
  2: 'st-active', 3: 'st-using', 4: 'st-done', 5: 'st-expired', 6: 'st-void',
};

function statusText(s: EquityStatus) {
  return STATUS_TEXT[s] || '未知';
}
function statusClass(s: EquityStatus) {
  return STATUS_CLASS[s] || 'st-default';
}

function formatDate(t: any): string {
  if (!t) return '';
  const d = new Date(t);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

async function loadData() {
  loading.value = true;
  try {
    const res = await getEquities({ current: 1, size: 50 });
    list.value = res.records ?? [];
  } catch (e) {
    list.value = [];
  } finally {
    loading.value = false;
  }
}

async function onRefresh() {
  refreshing.value = true;
  await loadData();
  refreshing.value = false;
}

function goDetail(code: string) {
  uni.navigateTo({ url: `/pages/equity/detail?equityCode=${code}` });
}
function goActivate() {
  uni.navigateTo({ url: '/pages/equity/activate' });
}

onShow(loadData);
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.list-page {
  min-height: 100vh;
  background: $bg-page;
  padding: $spacing-sm $spacing-sm;
}

.equity-scroll {
  height: calc(100vh - 200rpx);
}
.equity-list {
  padding-bottom: $spacing-sm;
}
.equity-card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: 28rpx $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.eq-name {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.eq-status {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: $radius-sm;
  margin-left: $spacing-sm;
  flex-shrink: 0;
}
.st-active {
  color: $brand-primary;
  background: $brand-primary-light;
}
.st-using {
  color: $network-blue;
  background: $network-blue-light;
}
.st-done {
  color: $brand-info;
  background: $brand-info-light;
}
.st-expired,
.st-void {
  color: $brand-error;
  background: $brand-error-light;
}
.st-default {
  color: $brand-info;
  background: $brand-info-light;
}
.card-body {
  margin-top: $spacing-sm;
}
.info-row {
  display: flex;
  font-size: 26rpx;
  margin-top: $spacing-xs;
  .info-label {
    color: $text-secondary;
    width: 140rpx;
  }
  .info-val {
    color: $text-regular;
    flex: 1;
  }
}
.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px solid $border-light;
  .expire {
    font-size: 24rpx;
    color: $text-secondary;
  }
  .arrow {
    font-size: 24rpx;
    color: $brand-primary;
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: $spacing-sm $spacing-md;
  background: $bg-card;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
}
</style>
