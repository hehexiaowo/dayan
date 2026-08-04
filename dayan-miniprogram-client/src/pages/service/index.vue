<template>
  <view class="service">
    <!-- 顶部发起服务入口 -->
    <view class="action-bar">
      <view class="action-info">
        <text class="action-title">需要帮助？</text>
        <text class="action-desc">激活权益后即可发起专属服务</text>
      </view>
      <button class="start-btn" @click="onStartService">发起服务</button>
    </view>

    <!-- 状态筛选 -->
    <scroll-view scroll-x class="filter-bar" :show-scrollbar="false">
      <view
        v-for="f in filters"
        :key="f.value"
        class="filter-item"
        :class="{ active: query.status === f.value }"
        @click="selectFilter(f.value)"
      >{{ f.label }}</view>
    </scroll-view>

    <!-- 服务会话列表 -->
    <view class="list">
      <view v-if="list.length" class="cards">
        <view class="card" v-for="s in list" :key="s.sessionCode" @click="onTapSession(s)">
          <view class="card-head">
            <text class="title">{{ s.title }}</text>
            <text class="status" :class="statusClass(s.sessionStatus)">{{ statusText(s.sessionStatus) }}</text>
          </view>
          <view class="card-row">
            <text class="label">管家</text>
            <text class="value">{{ s.butlerName || '未分配' }}</text>
          </view>
          <view v-if="s.subStatus" class="card-row">
            <text class="label">进度</text>
            <text class="value">{{ s.subStatus }}</text>
          </view>
          <view v-if="s.progress != null" class="progress-bar">
            <view class="progress-inner" :style="{ width: s.progress + '%' }"></view>
          </view>
          <view class="card-foot">
            <text class="time">{{ formatTime(s.createdAt) }}</text>
            <text class="code">单号 {{ s.sessionCode }}</text>
          </view>
        </view>
      </view>

      <view v-else class="empty">
        <text class="empty-text">{{ loading ? '加载中...' : '暂无服务会话，接口待后端提供' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { onPullDownRefresh } from '@dcloudio/uni-app';
import { getServices, type ServiceQuery } from '@/api/service';
import type { ServiceSession, ServiceSessionStatus } from '@/types';

const query = ref<ServiceQuery>({ page: 1, size: 20 });
const list = ref<ServiceSession[]>([]);
const loading = ref(false);

const filters = [
  { label: '全部', value: undefined as number | undefined },
  { label: '待受理', value: 1 },
  { label: '服务中', value: 2 },
  { label: '待评价', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已取消', value: 5 },
];

function selectFilter(v?: number) {
  query.value.status = v;
  loadData();
}

async function loadData() {
  loading.value = true;
  try {
    const res = await getServices(query.value);
    list.value = res?.list || [];
  } catch (e) {
    // 后端业务接口未实现，降级为空列表
    list.value = [];
  } finally {
    loading.value = false;
  }
}

function onStartService() {
  uni.showToast({ title: '请先激活权益', icon: 'none' });
}

function onTapSession(s: ServiceSession) {
  uni.showToast({ title: `会话 ${s.sessionCode}（详情待开放）`, icon: 'none' });
}

function statusText(st: ServiceSessionStatus): string {
  const map: Record<number, string> = {
    1: '待受理',
    2: '服务中',
    3: '待评价',
    4: '已完成',
    5: '已取消',
    6: '暂停',
    7: '异常',
  };
  return map[st] || '未知';
}

function statusClass(st: ServiceSessionStatus): string {
  // normal/urgent/完成/取消/进度 等样式分类
  if (st === 2) return 'st-urgent'; // 服务中（进行中，强调）
  if (st === 4) return 'st-done'; // 完成
  if (st === 5) return 'st-cancel'; // 取消
  if (st === 7) return 'st-cancel'; // 异常
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

onMounted(loadData);
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
  padding: 30rpx 30rpx;
}
.action-info {
  display: flex;
  flex-direction: column;
}
.action-title {
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
}
.action-desc {
  color: rgba(255, 255, 255, 0.85);
  font-size: 24rpx;
  margin-top: 6rpx;
}
.start-btn {
  background: #fff;
  color: #67C23A;
  font-size: 26rpx;
  font-weight: bold;
  border-radius: 32rpx;
  padding: 0 36rpx;
  height: 64rpx;
  line-height: 64rpx;
  margin: 0;
}

/* 筛选条 */
.filter-bar {
  white-space: nowrap;
  background: #fff;
  padding: 20rpx 24rpx;
  border-bottom: 1px solid #f0f0f0;
}
.filter-item {
  display: inline-block;
  padding: 10rpx 28rpx;
  margin-right: 16rpx;
  border-radius: 28rpx;
  background: #f5f6f8;
  font-size: 24rpx;
  color: #606266;
  &.active {
    background: #67C23A;
    color: #fff;
  }
}

/* 列表 */
.list {
  padding: 20rpx 24rpx;
}
.cards {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.status {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  margin-left: 16rpx;
  flex-shrink: 0;
}
.st-normal {
  color: #e6a23c;
  background: #fdf6ec;
}
.st-urgent {
  color: #67C23A;
  background: #f0f9eb;
}
.st-done {
  color: #909399;
  background: #f4f4f5;
}
.st-cancel {
  color: #f56c6c;
  background: #fef0f0;
}
.card-row {
  display: flex;
  margin-top: 14rpx;
}
.label {
  font-size: 26rpx;
  color: #909399;
  width: 100rpx;
}
.value {
  font-size: 26rpx;
  color: #303133;
  flex: 1;
}
.progress-bar {
  margin-top: 16rpx;
  height: 8rpx;
  background: #f0f0f0;
  border-radius: 4rpx;
  overflow: hidden;
}
.progress-inner {
  height: 100%;
  background: #67C23A;
  border-radius: 4rpx;
}
.card-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1px solid #f5f5f5;
}
.time {
  font-size: 22rpx;
  color: #c0c4cc;
}
.code {
  font-size: 22rpx;
  color: #c0c4cc;
}

/* 空状态 */
.empty {
  background: #fff;
  border-radius: 16rpx;
  padding: 100rpx 0;
  text-align: center;
}
.empty-text {
  color: #909399;
  font-size: 26rpx;
}
</style>
