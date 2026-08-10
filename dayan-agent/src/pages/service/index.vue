<template>
  <view class="page">
    <!-- 搜索栏 -->
    <view class="toolbar">
      <view class="search">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索客户名/手机号"
          confirm-type="search"
          @confirm="onSearch"
        />
        <button class="btn-search" size="mini" @click="onSearch">搜索</button>
      </view>
    </view>

    <!-- 客户列表 -->
    <view class="list">
      <view v-if="loading && !customers.length" class="empty">加载中...</view>
      <view v-else-if="!customers.length" class="empty">
        暂无客户（接口待 Inc 5 提供）
      </view>

      <view v-else>
        <view
          v-for="c in customers"
          :key="c.clientCode"
          class="card"
          @click="onCustomerClick(c)"
        >
          <view class="card-row">
            <view class="card-name">{{ c.clientName || '未命名' }}</view>
            <view class="card-type" :class="typeClass(c.clientType)">
              {{ typeText(c.clientType) }}
            </view>
          </view>
          <view class="card-row sub">
            <text class="card-phone">{{ formatPhone(c.phone) }}</text>
            <text class="card-time">绑定：{{ c.bindTime || '-' }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { onPullDownRefresh } from '@dcloudio/uni-app';
import { getCustomers } from '@/api/customer';
import type { Customer, ClientType } from '@/types';

const keyword = ref('');
const customers = ref<Customer[]>([]);
const loading = ref(false);

async function loadList() {
  loading.value = true;
  try {
    const res = await getCustomers({ keyword: keyword.value || undefined });
    customers.value = res?.records || [];
  } catch (e) {
    customers.value = [];
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  loadList();
}

function onCustomerClick(c: Customer) {
  uni.showToast({
    title: `客户详情：${c.clientName}（Inc 5 上线）`,
    icon: 'none',
  });
}

function typeText(t?: ClientType | number): string {
  switch (t) {
    case ClientType.SELF:
    case 1:
      return '本人';
    case ClientType.FAMILY:
    case 2:
      return '家属';
    case ClientType.ELDER:
    case 3:
      return '老人';
    default:
      return '未知';
  }
}

function typeClass(t?: ClientType | number): string {
  switch (t) {
    case ClientType.SELF:
    case 1:
      return 'tp-self';
    case ClientType.FAMILY:
    case 2:
      return 'tp-family';
    case ClientType.ELDER:
    case 3:
      return 'tp-elder';
    default:
      return '';
  }
}

function formatPhone(phone?: string): string {
  if (!phone) return '手机：-';
  return `手机：${phone}`;
}

onMounted(() => {
  loadList();
});

onPullDownRefresh(async () => {
  try {
    await loadList();
  } finally {
    uni.stopPullDownRefresh();
  }
});
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx 24rpx 60rpx;
  min-height: 100vh;
  background: #f5f7fa;
}
.toolbar {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
}
.search {
  display: flex;
  align-items: center;
}
.search-input {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 8rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
}
.btn-search {
  margin-left: 16rpx;
  background: #409eff;
  color: #fff;
  font-size: 26rpx;
}
.list {
  margin-top: 24rpx;
}
.empty {
  text-align: center;
  color: #909399;
  font-size: 26rpx;
  padding: 80rpx 0;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 20rpx;
}
.card-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-row.sub {
  margin-top: 12rpx;
}
.card-name {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}
.card-phone,
.card-time {
  font-size: 26rpx;
  color: #606266;
}
.card-type {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}
.tp-self {
  background: #ecf5ff;
  color: #409eff;
}
.tp-family {
  background: #fff7e6;
  color: #ff9900;
}
.tp-elder {
  background: #edfff3;
  color: #19be6b;
}
</style>
