<template>
  <view class="service">
    <!-- 顶部发起服务入口 -->
    <view class="action-bar">
      <view class="action-info">
        <text class="action-title">需要帮助？</text>
        <text class="action-desc">从我的权益发起专属服务</text>
      </view>
      <button class="start-btn" @click="onStartService">发起服务</button>
    </view>

    <!-- 状态筛选 -->
    <scroll-view scroll-x class="filter-bar" :show-scrollbar="false">
      <view
        v-for="f in filters"
        :key="String(f.value)"
        class="filter-item"
        :class="{ active: query.sessionStatus === f.value }"
        @click="selectFilter(f.value)"
      >{{ f.label }}</view>
    </scroll-view>

    <!-- 服务会话列表 -->
    <view class="list">
      <view v-if="list.length" class="cards">
        <view class="card" v-for="s in list" :key="s.sessionCode" @click="goDetail(s.sessionCode)">
          <view class="card-head">
            <text class="title">{{ s.serviceTitle || s.title || '服务会话' }}</text>
            <text class="status" :class="statusClass(s.sessionStatus)">{{ statusText(s.sessionStatus) }}</text>
          </view>
          <view class="card-row">
            <text class="label">管家</text>
            <text class="value">{{ s.butlerFullName || s.butlerName || '待分配' }}</text>
          </view>
          <view class="card-foot">
            <text class="time">{{ formatTime(s.createdAt) }}</text>
            <text class="code">单号 {{ s.sessionCode }}</text>
          </view>
        </view>
      </view>

      <view v-else class="empty">
        <text class="empty-text">{{ loading ? '加载中...' : '暂无服务会话' }}</text>
        <text v-if="!loading" class="empty-sub">从「我的权益」发起服务后会显示在这里</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app';
import { getServices, type ServiceQuery } from '@/api/service';
import type { ServiceSession, ServiceSessionStatus } from '@/types';

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
.service {
  min-height: 100vh;
  background: #f5f6f8;
}

/* 发起服务入口 */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #67C23A 0%, #4eaf2a 100%);
  padding: 30rpx;
}
.action-info { display: flex; flex-direction: column; }
.action-title { color: #fff; font-size: 32rpx; font-weight: bold; }
.action-desc { color: rgba(255,255,255,0.85); font-size: 24rpx; margin-top: 6rpx; }
.start-btn {
  background: #fff; color: #67C23A; font-size: 26rpx; font-weight: bold;
  border-radius: 32rpx; padding: 0 36rpx; height: 64rpx; line-height: 64rpx; margin: 0;
}

/* 筛选条 */
.filter-bar { white-space: nowrap; background: #fff; padding: 20rpx 24rpx; border-bottom: 1px solid #f0f0f0; }
.filter-item {
  display: inline-block; padding: 10rpx 28rpx; margin-right: 16rpx;
  border-radius: 28rpx; background: #f5f6f8; font-size: 24rpx; color: #606266;
  &.active { background: #67C23A; color: #fff; }
}

/* 列表 */
.list { padding: 20rpx 24rpx; }
.cards { display: flex; flex-direction: column; gap: 20rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 24rpx; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 30rpx; font-weight: bold; color: #303133; flex: 1; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; margin-left: 16rpx; flex-shrink: 0; }
.st-normal { color: #e6a23c; background: #fdf6ec; }
.st-active { color: #67C23A; background: #f0f9eb; }
.st-done { color: #909399; background: #f4f4f5; }
.st-cancel { color: #f56c6c; background: #fef0f0; }
.card-row { display: flex; margin-top: 14rpx; }
.label { font-size: 26rpx; color: #909399; width: 100rpx; }
.value { font-size: 26rpx; color: #303133; flex: 1; }
.card-foot { display: flex; justify-content: space-between; margin-top: 16rpx; padding-top: 16rpx; border-top: 1px solid #f5f5f5; }
.time, .code { font-size: 22rpx; color: #c0c4cc; }

.empty { background: #fff; border-radius: 16rpx; padding: 100rpx 0; text-align: center; }
.empty-text { color: #909399; font-size: 26rpx; display: block; }
.empty-sub { color: #c0c4cc; font-size: 24rpx; margin-top: 12rpx; display: block; }
</style>
