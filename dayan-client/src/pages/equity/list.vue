<template>
  <view class="list-page">
    <view v-if="loading && list.length === 0" class="loading">
      <text>加载中...</text>
    </view>

    <view v-else-if="list.length === 0" class="empty">
      <text class="empty-icon">📋</text>
      <text class="empty-text">还没有权益</text>
      <button class="btn-go-activate" @click="goActivate">去激活权益</button>
    </view>

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
          class="equity-card"
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
      <button class="btn-activate" @click="goActivate">+ 激活新权益</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getEquities } from '@/api/equity';
import type { Equity, EquityStatus } from '@/types';

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
.list-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 140rpx;
}

.loading {
  display: flex;
  justify-content: center;
  padding: 120rpx 0;
  color: #909399;
  font-size: 28rpx;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
  .empty-icon {
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }
  .empty-text {
    font-size: 30rpx;
    color: #606266;
    margin-bottom: 40rpx;
  }
}
.btn-go-activate {
  background: #67C23A;
  color: #fff;
  font-size: 28rpx;
  border-radius: 12rpx;
  padding: 0 60rpx;
  height: 76rpx;
  line-height: 76rpx;
}

.equity-scroll {
  height: calc(100vh - 140rpx);
}
.equity-list {
  padding: 20rpx 24rpx;
}
.equity-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.eq-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.eq-status {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  margin-left: 16rpx;
  flex-shrink: 0;
}
.st-active {
  color: #67C23A;
  background: #f0f9eb;
}
.st-using {
  color: #409eff;
  background: #ecf5ff;
}
.st-done {
  color: #909399;
  background: #f4f4f5;
}
.st-expired,
.st-void {
  color: #f56c6c;
  background: #fef0f0;
}
.st-default {
  color: #909399;
  background: #f4f4f5;
}
.card-body {
  margin-top: 16rpx;
}
.info-row {
  display: flex;
  font-size: 26rpx;
  margin-top: 8rpx;
  .info-label {
    color: #909399;
    width: 140rpx;
  }
  .info-val {
    color: #606266;
    flex: 1;
  }
}
.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1px solid #f5f5f5;
  .expire {
    font-size: 24rpx;
    color: #909399;
  }
  .arrow {
    font-size: 24rpx;
    color: #67C23A;
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 24rpx;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
}
.btn-activate {
  background: #67C23A;
  color: #fff;
  font-size: 30rpx;
  border-radius: 12rpx;
  height: 80rpx;
  line-height: 80rpx;
}
</style>
