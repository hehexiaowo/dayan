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
        <text v-if="keyword" class="search-clear" @click="keyword = ''; onSearch()">×</text>
        <view class="btn-search dy-clickable" @click="onSearch">搜索</view>
      </view>
    </view>

    <!-- 客户列表 -->
    <view class="list">
      <!-- 加载骨架 -->
      <template v-if="loading && !customers.length">
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
        @action="loadList"
      />

      <!-- 空状态 -->
      <DyEmpty
        v-else-if="!customers.length"
        text="暂无客户"
        icon="客"
        color="orange"
      />

      <!-- 客户卡片 -->
      <view v-else>
        <view
          v-for="c in customers"
          :key="c.clientCode"
          class="card dy-clickable"
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
import type { Customer } from '@/types';
import { ClientType } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const keyword = ref('');
const customers = ref<Customer[]>([]);
const loading = ref(false);
const loadError = ref(false);

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    const res = await getCustomers({ keyword: keyword.value || undefined });
    customers.value = res?.records || [];
  } catch (e) {
    loadError.value = true;
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
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  padding: $spacing-md $spacing-md 60rpx;
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
.search-icon {
  font-size: 28rpx;
  margin-right: $spacing-xs;
}
.search-input {
  flex: 1;
  border: 1rpx solid $border-base;
  border-radius: $radius-sm;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
  background: $bg-page;
}
.search-clear {
  padding: 0 16rpx;
  font-size: 36rpx;
  color: $text-placeholder;
}
.btn-search {
  margin-left: $spacing-sm;
  background: $gradient-blue;
  color: #fff;
  font-size: 26rpx;
  padding: 14rpx 32rpx;
  border-radius: $radius-sm;
  box-shadow: 0 4rpx 12rpx rgba(64, 158, 255, 0.3);
}

/* list */
.list {
  margin-top: $spacing-md;
}
.card {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md $spacing-lg;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
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
  color: $text-primary;
}
.card-phone,
.card-time {
  font-size: 26rpx;
  color: $text-regular;
}
.card-type {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}
.tp-self {
  background: $brand-primary-light;
  color: $brand-primary;
}
.tp-family {
  background: $brand-warning-light;
  color: $brand-warning;
}
.tp-elder {
  background: $brand-success-light;
  color: $brand-success;
}
</style>
