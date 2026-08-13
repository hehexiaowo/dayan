<template>
  <view class="detail-page">
    <!-- 加载骨架 -->
    <template v-if="loading">
      <DySkeleton :rows="3" card />
      <DySkeleton :rows="2" card />
    </template>

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
      <view class="section-card dy-clickable" @click="goUsePersons">
        <view class="section-left">
          <DyIconBlock text="人" color="green" size="sm" shape="circle" />
          <view>
            <text class="section-title">权益人管理</text>
            <text class="section-desc">{{ persons.length }} 位权益人 {{ placeholderCount > 0 ? `（${placeholderCount} 位待补全）` : '' }}</text>
          </view>
        </view>
        <text class="section-arrow">›</text>
      </view>

      <!-- 可用服务项目 -->
      <view class="section-header">
        <text class="sh-title">可用服务项目</text>
        <text class="sh-count">{{ serviceItems.length }} 项</text>
      </view>

      <DyEmpty
        v-if="serviceItems.length === 0"
        text="暂无可用服务项目"
        icon="项"
        color="gray"
      />

      <view v-else class="item-list">
        <view
          v-for="item in serviceItems"
          :key="item.itemCode"
          class="item-card dy-clickable"
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
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';

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
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.detail-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 40rpx;
}

/* 权益信息 */
.equity-hero {
  background: $gradient-brand;
  padding: 40rpx $spacing-md;
}
.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;
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
  border-radius: $radius-sm;
  margin-left: $spacing-sm;
}
/* BUG 修复：状态标签样式（原代码引用但从未定义，导致裸奔） */
.st-active { background: rgba(255, 255, 255, 0.3); }
.st-using { background: rgba(255, 255, 255, 0.25); }
.st-done { background: rgba(255, 255, 255, 0.2); }
.st-expired { background: rgba(250, 53, 52, 0.5); }
.st-void { background: rgba(250, 53, 52, 0.5); }
.st-default { background: rgba(255, 255, 255, 0.2); }
.hero-meta {
  background: rgba(255, 255, 255, 0.15);
  border-radius: $radius-md;
  padding: 20rpx $spacing-md;
}
.meta-item {
  display: flex;
  margin: $spacing-xs 0;
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
  background: $bg-card;
  margin: $spacing-sm $spacing-md 0;
  border-radius: $radius-lg;
  padding: 28rpx $spacing-md;
  box-shadow: $shadow-card;
}
.section-left {
  display: flex;
  align-items: center;
}
.section-title {
  font-size: 30rpx;
  color: $text-primary;
  font-weight: 500;
  display: block;
  margin-left: $spacing-md;
}
.section-desc {
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: 4rpx;
  display: block;
  margin-left: $spacing-md;
}
.section-arrow {
  color: $text-placeholder;
  font-size: 36rpx;
}

/* 服务项目 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-md $spacing-sm;
}
.sh-title {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}
.sh-count {
  font-size: 24rpx;
  color: $text-secondary;
}

.item-list {
  padding: 0 $spacing-md;
}
.item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: 28rpx $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
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
  color: $text-primary;
  font-weight: 500;
  display: block;
}
.item-quota {
  margin-top: $spacing-xs;
}
.quota-text {
  font-size: 24rpx;
  color: $text-secondary;
}
.quota-num {
  color: $brand-primary;
  font-weight: bold;
  font-size: 28rpx;
  &.zero {
    color: $text-placeholder;
  }
}
.btn-start {
  background: $brand-primary;
  color: #fff;
  font-size: 26rpx;
  border-radius: $radius-sm;
  padding: 0 $spacing-md;
  height: 64rpx;
  line-height: 64rpx;
  flex-shrink: 0;
  &[disabled] {
    background: $border-base;
    color: #fff;
  }
}
</style>
