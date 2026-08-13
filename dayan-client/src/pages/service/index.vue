<template>
  <view class="service dy-safe-bottom">
    <!-- 顶部发起服务入口 -->
    <view class="action-bar">
      <view class="action-info">
        <text class="action-title">需要帮助？</text>
        <text class="action-desc">从我的权益发起专属服务</text>
      </view>
      <button class="start-btn dy-clickable" @click="onStartService">发起服务</button>
    </view>

    <!-- 状态筛选 -->
    <scroll-view scroll-x class="filter-bar" :show-scrollbar="false">
      <view
        v-for="f in filters"
        :key="String(f.value)"
        class="filter-item dy-clickable"
        :class="{ active: query.sessionStatus === f.value }"
        @click="selectFilter(f.value)"
      >{{ f.label }}</view>
    </scroll-view>

    <!-- 服务会话列表 -->
    <view class="list">
      <!-- 加载骨架 -->
      <template v-if="loading && list.length === 0">
        <DySkeleton :rows="2" card />
        <DySkeleton :rows="2" card />
      </template>

      <template v-else-if="list.length">
        <view class="cards">
          <view class="card dy-clickable" v-for="s in list" :key="s.sessionCode" @click="goDetail(s.sessionCode)">
            <view class="card-head">
              <text class="title">{{ s.serviceTitle || s.title || '服务会话' }}</text>
              <text class="status" :class="statusClass(s.sessionStatus)">{{ statusText(s.sessionStatus) }}</text>
            </view>
            <view class="card-row">
              <text class="row-label">管家</text>
              <text class="row-value">{{ s.butlerFullName || s.butlerName || '待分配' }}</text>
            </view>
            <view class="card-foot">
              <text class="time">{{ formatTime(s.createdAt) }}</text>
              <text class="code">单号 {{ s.sessionCode }}</text>
            </view>
          </view>
        </view>
      </template>

      <DyEmpty
        v-else
        text="暂无服务会话"
        icon="务"
        color="green"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
import { getServices, type ServiceQuery } from '@/api/service';
import type { ServiceSession, ServiceSessionStatus } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const query = ref<ServiceQuery>({ current: 1, size: 20 });
const list = ref<ServiceSession[]>([]);
const loading = ref(false);

const filters = [
  { label: '全部', value: undefined as number | undefined },
  { label: '待分配', value: 1 },
  { label: '方案中', value: 3 },
  { label: '安排中', value: 4 },
  { label: '服务中', value: 5 },
  { label: '已完成', value: 6 },
  { label: '已取消', value: 7 },
];

function selectFilter(v?: number) {
  query.value.sessionStatus = v;
  loadData();
}

async function loadData() {
  loading.value = true;
  try {
    const res = await getServices(query.value);
    list.value = res?.records || [];
  } catch {
    list.value = [];
  } finally {
    loading.value = false;
  }
}

function goDetail(sessionCode: string) {
  uni.navigateTo({ url: `/pages/service/detail?sessionCode=${sessionCode}` });
}

function onStartService() {
  // 发起服务统一从权益详情进入：选权益 → 详情 → 发起
  uni.navigateTo({ url: '/pages/equity/list' });
}

function statusText(st: ServiceSessionStatus): string {
  const map: Record<number, string> = {
    1: '待分配', 2: '待收集', 3: '方案中', 4: '安排中',
    5: '服务中', 6: '已完成', 7: '已取消',
  };
  return map[st] || '未知';
}

function statusClass(st: ServiceSessionStatus): string {
  if (st === 5) return 'st-active';
  if (st === 6) return 'st-done';
  if (st === 7) return 'st-cancel';
  return 'st-normal';
}

function formatTime(t?: number | string): string {
  if (!t) return '';
  const d = new Date(t);
  if (isNaN(d.getTime())) return String(t);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

onPullDownRefresh(() => {
  loadData().finally(() => uni.stopPullDownRefresh());
});

// 仅 onShow：返回本 tab 时刷新（如提交服务后、确认方案后）
onShow(loadData);
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.service {
  min-height: 100vh;
  background: $bg-page;
}

/* 发起服务入口 */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $gradient-brand;
  padding: $spacing-md;
}
.action-info { display: flex; flex-direction: column; }
.action-title { color: #fff; font-size: 32rpx; font-weight: bold; }
.action-desc { color: rgba(255, 255, 255, 0.85); font-size: 24rpx; margin-top: 6rpx; }
.start-btn {
  background: $bg-card; color: $brand-primary; font-size: 26rpx; font-weight: bold;
  border-radius: 32rpx; padding: 0 36rpx; height: 64rpx; line-height: 64rpx; margin: 0;
}

/* 筛选条 */
.filter-bar {
  white-space: nowrap;
  background: $bg-card;
  padding: $spacing-sm $spacing-md;
  border-bottom: 1px solid $border-light;
}
.filter-item {
  display: inline-block;
  padding: 10rpx 28rpx;
  margin-right: $spacing-sm;
  border-radius: 28rpx;
  background: $bg-page;
  font-size: 24rpx;
  color: $text-regular;
  &.active { background: $gradient-brand; color: #fff; }
}

/* 列表 */
.list { padding: $spacing-sm $spacing-md; }
.cards { display: flex; flex-direction: column; gap: $spacing-sm; }
.card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-md;
  box-shadow: $shadow-card;
}
.card-head { display: flex; align-items: center; justify-content: space-between; }
.title {
  font-size: 30rpx; font-weight: bold; color: $text-primary;
  flex: 1; overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}
.status {
  font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx;
  margin-left: $spacing-sm; flex-shrink: 0;
}
.st-normal { color: $brand-warning; background: $brand-warning-light; }
.st-active { color: $brand-primary; background: $brand-primary-light; }
.st-done { color: $brand-info; background: $brand-info-light; }
.st-cancel { color: $brand-error; background: $brand-error-light; }
.card-row { display: flex; margin-top: 14rpx; }
.row-label { font-size: 26rpx; color: $text-secondary; width: 100rpx; }
.row-value { font-size: 26rpx; color: $text-primary; flex: 1; }
.card-foot {
  display: flex; justify-content: space-between;
  margin-top: $spacing-sm; padding-top: $spacing-sm;
  border-top: 1px solid $border-light;
}
.time, .code { font-size: 22rpx; color: $text-placeholder; }
</style>
