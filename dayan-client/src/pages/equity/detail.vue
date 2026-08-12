<template>
  <view class="detail-page">
    <view v-if="loading" class="loading"><text>加载中...</text></view>

    <template v-else>
      <!-- 权益信息卡 -->
      <view class="equity-hero">
        <view class="hero-top">
          <text class="eq-name">{{ equity?.goodsName || equity?.equityName || '养老权益' }}</text>
          <text class="eq-status" :class="statusClass">{{ statusText }}</text>
        </view>
        <view class="hero-meta">
          <view class="meta-item">
            <text class="meta-label">权益编码</text>
            <text class="meta-val">{{ equity?.equityCode }}</text>
          </view>
          <view v-if="equity?.equityType" class="meta-item">
            <text class="meta-label">权益类型</text>
            <text class="meta-val">{{ equity.equityType }}</text>
          </view>
          <view class="meta-item">
            <text class="meta-label">有效期</text>
            <text class="meta-val">{{ validText }}</text>
          </view>
        </view>
      </view>

      <!-- 权益人入口 -->
      <view class="section-card" @click="goUsePersons">
        <view class="section-left">
          <text class="section-icon" style="color: #67C23A">人</text>
          <view>
            <text class="section-title">权益人管理</text>
            <text class="section-desc">{{ persons.length }} 位权益人 {{ placeholderCount > 0 ? `（${placeholderCount} 位待补全）` : '' }}</text>
          </view>
        </view>
        <text class="section-arrow">></text>
      </view>

      <!-- 可用服务项目 -->
      <view class="section-header">
        <text class="sh-title">可用服务项目</text>
        <text class="sh-count">{{ serviceItems.length }} 项</text>
      </view>

      <view v-if="serviceItems.length === 0" class="empty-section">
        <text class="empty-text">暂无可用服务项目</text>
      </view>

      <view v-else class="item-list">
        <view
          v-for="item in serviceItems"
          :key="item.itemCode"
          class="item-card"
          :class="{ disabled: item.remaining <= 0 }"
        >
          <view class="item-info">
            <text class="item-name">{{ item.itemName }}</text>
            <view class="item-quota">
              <text class="quota-text">
                剩余 <text class="quota-num" :class="{ zero: item.remaining <= 0 }">{{ item.remaining }}</text> / {{ item.quantity }} {{ item.quotaType === 1 ? '次' : '次/年' }}
              </text>
            </view>
          </view>
          <button
            class="btn-start"
            :disabled="item.remaining <= 0"
            @click="goCreateService(item.itemCode)"
          >
            {{ item.remaining > 0 ? '发起' : '已用完' }}
          </button>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getEquityDetail, getServiceItems, getUsePersons } from '@/api/equity';
import type { Equity, ClientServiceItem, EquityUsePerson } from '@/types';

const equityCode = ref('');
const equity = ref<Equity | null>(null);
const serviceItems = ref<ClientServiceItem[]>([]);
const persons = ref<EquityUsePerson[]>([]);
const loading = ref(true);

const STATUS_TEXT: Record<number, string> = {
  2: '已激活', 3: '使用中', 4: '已完成', 5: '已过期', 6: '已作废',
};
const STATUS_CLASS: Record<number, string> = {
  2: 'st-active', 3: 'st-using', 4: 'st-done', 5: 'st-expired', 6: 'st-void',
};

const statusText = computed(() => {
  const s = equity.value?.equityStatus;
  return s != null ? (STATUS_TEXT[s] || '未知') : '';
});
const statusClass = computed(() => {
  const s = equity.value?.equityStatus;
  return s != null ? (STATUS_CLASS[s] || 'st-default') : 'st-default';
});

const validText = computed(() => {
  if (equity.value?.expireTime) {
    const d = new Date(equity.value.expireTime);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
  if (equity.value?.validDays) return `${equity.value.validDays} 天`;
  return '长期有效';
});

const placeholderCount = computed(() =>
  persons.value.filter((p) => !p.usePersonName || p.usePersonName.startsWith('待填写')).length,
);

async function loadAll() {
  loading.value = true;
  try {
    const [eq, items, ps] = await Promise.all([
      getEquityDetail(equityCode.value),
      getServiceItems(equityCode.value).catch(() => []),
      getUsePersons(equityCode.value).catch(() => []),
    ]);
    equity.value = eq;
    serviceItems.value = items;
    persons.value = ps;
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false;
  }
}

function goUsePersons() {
  uni.navigateTo({ url: `/pages/equity/use-persons/index?equityCode=${equityCode.value}` });
}
function goCreateService(itemCode: string) {
  uni.navigateTo({ url: `/pages/service/create?equityCode=${equityCode.value}&itemCode=${itemCode}` });
}

onLoad((q) => {
  equityCode.value = q?.equityCode || '';
  loadAll();
});
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 40rpx;
}

.loading {
  display: flex;
  justify-content: center;
  padding: 120rpx 0;
  color: #909399;
}

/* 权益信息 */
.equity-hero {
  background: linear-gradient(135deg, #67C23A 0%, #4eaf2a 100%);
  padding: 40rpx 30rpx;
}
.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}
.eq-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
  flex: 1;
}
.eq-status {
  font-size: 24rpx;
  color: #fff;
  background: rgba(255, 255, 255, 0.25);
  padding: 6rpx 20rpx;
  border-radius: 8rpx;
  margin-left: 16rpx;
}
.hero-meta {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
}
.meta-item {
  display: flex;
  margin: 8rpx 0;
  .meta-label {
    color: rgba(255, 255, 255, 0.75);
    font-size: 26rpx;
    width: 140rpx;
  }
  .meta-val {
    color: #fff;
    font-size: 26rpx;
    flex: 1;
  }
}

/* 权益人入口卡 */
.section-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  margin: 20rpx 24rpx 0;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
}
.section-left {
  display: flex;
  align-items: center;
}
.section-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #f0f9eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: bold;
  margin-right: 16rpx;
}
.section-title {
  font-size: 30rpx;
  color: #303133;
  font-weight: 500;
  display: block;
}
.section-desc {
  font-size: 24rpx;
  color: #909399;
  margin-top: 4rpx;
  display: block;
}
.section-arrow {
  color: #c0c4cc;
  font-size: 32rpx;
}

/* 服务项目 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 24rpx 16rpx;
}
.sh-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}
.sh-count {
  font-size: 24rpx;
  color: #909399;
}

.empty-section {
  margin: 0 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 60rpx 0;
  text-align: center;
  .empty-text {
    font-size: 26rpx;
    color: #c0c4cc;
  }
}

.item-list {
  padding: 0 24rpx;
}
.item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
  &.disabled {
    opacity: 0.6;
  }
}
.item-info {
  flex: 1;
  overflow: hidden;
}
.item-name {
  font-size: 30rpx;
  color: #303133;
  font-weight: 500;
  display: block;
}
.item-quota {
  margin-top: 8rpx;
}
.quota-text {
  font-size: 24rpx;
  color: #909399;
}
.quota-num {
  color: #67C23A;
  font-weight: bold;
  font-size: 28rpx;
  &.zero {
    color: #c0c4cc;
  }
}
.btn-start {
  background: #67C23A;
  color: #fff;
  font-size: 26rpx;
  border-radius: 8rpx;
  padding: 0 32rpx;
  height: 64rpx;
  line-height: 64rpx;
  flex-shrink: 0;
  &[disabled] {
    background: #dcdfe6;
    color: #fff;
  }
}
</style>
