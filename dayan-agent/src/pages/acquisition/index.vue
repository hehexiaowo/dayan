<template>
  <view class="page">
    <!-- 顶部操作条：搜索 + 新增 + 分享码 -->
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
      <view class="actions">
        <button class="btn-outline" size="mini" @click="onShareCode">
          分享获客码
        </button>
        <button class="btn-primary" size="mini" @click="onAdd">新增线索</button>
      </view>
    </view>

    <!-- 获客工具 -->
    <view class="tools">
      <view class="tools-title">获客工具</view>
      <view class="tools-grid">
        <view class="tool-item" @click="onTool('card')">
          <view class="tool-icon" style="background: #409eff">名</view>
          <text class="tool-label">电子名片</text>
        </view>
        <view class="tool-item" @click="onTool('poster')">
          <view class="tool-icon" style="background: #ff9900">海</view>
          <text class="tool-label">营销海报</text>
        </view>
        <view class="tool-item" @click="onTool('content')">
          <view class="tool-icon" style="background: #19be6b">享</view>
          <text class="tool-label">内容分享</text>
        </view>
      </view>
    </view>

    <!-- 线索列表 -->
    <view class="list">
      <view v-if="loading && !leads.length" class="empty">加载中...</view>
      <view v-else-if="!leads.length" class="empty">
        暂无线索（接口待后端提供）
      </view>

      <view v-else>
        <view
          v-for="lead in leads"
          :key="lead.leadId"
          class="card"
          @click="onLeadClick(lead)"
        >
          <view class="card-row">
            <view class="card-name">{{ lead.name || '未命名' }}</view>
            <view
              class="card-status"
              :class="statusClass(lead.leadStatus)"
            >
              {{ statusText(lead.leadStatus) }}
            </view>
          </view>
          <view class="card-row sub">
            <text class="card-phone">{{ formatPhone(lead.phone) }}</text>
            <text class="card-time">{{ lead.createdAt || '' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部新增按钮（固定） -->
    <view class="fab" @click="onAdd">+</view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { onPullDownRefresh } from '@dcloudio/uni-app';
import { getLeads } from '@/api/lead';
import type { Lead, LeadStatus } from '@/types';

const keyword = ref('');
const leads = ref<Lead[]>([]);
const loading = ref(false);

async function loadList() {
  loading.value = true;
  try {
    const res = await getLeads({ keyword: keyword.value || undefined });
    leads.value = res?.records || [];
  } catch (e) {
    leads.value = [];
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  loadList();
}

function onAdd() {
  uni.showModal({
    title: '新增线索',
    content: '线索录入表单开发中',
    showCancel: true,
    confirmText: '我知道了',
    success: () => {},
  });
}

function onShareCode() {
  uni.showToast({ title: '分享获客码功能开发中', icon: 'none' });
}

function onTool(type: string) {
  uni.showToast({ title: '获客工具（Inc 4 上线）', icon: 'none' });
}

function onLeadClick(lead: Lead) {
  uni.showModal({
    title: lead.name || '线索详情',
    content: `手机：${lead.phone || '-'}\n状态：${statusText(lead.leadStatus)}`,
    showCancel: false,
  });
}

function statusText(s?: LeadStatus | number): string {
  switch (s) {
    case LeadStatus.NEW:
    case 1:
      return '新线索';
    case LeadStatus.FOLLOWING:
    case 2:
      return '跟进中';
    case LeadStatus.INTENDED:
    case 3:
      return '意向';
    case LeadStatus.CONVERTED:
    case 4:
      return '已转化';
    case LeadStatus.LOST:
    case 5:
      return '已流失';
    default:
      return '未知';
  }
}

function statusClass(s?: LeadStatus | number): string {
  switch (s) {
    case LeadStatus.NEW:
    case 1:
      return 'st-new';
    case LeadStatus.FOLLOWING:
    case 2:
      return 'st-following';
    case LeadStatus.INTENDED:
    case 3:
      return 'st-intended';
    case LeadStatus.CONVERTED:
    case 4:
      return 'st-converted';
    case LeadStatus.LOST:
    case 5:
      return 'st-lost';
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
  padding: 24rpx 24rpx 160rpx;
  min-height: 100vh;
  background: #f5f7fa;
}

/* 工具条 */
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
.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16rpx;
}
.btn-outline {
  background: #fff;
  color: #409eff;
  border: 1px solid #409eff;
  font-size: 24rpx;
  margin-right: 16rpx;
}
.btn-primary {
  background: #19be6b;
  color: #fff;
  font-size: 24rpx;
}

/* 获客工具 */
.tools {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  margin-top: 20rpx;
}
.tools-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20rpx;
}
.tools-grid {
  display: flex;
  justify-content: space-around;
}
.tool-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.tool-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 36rpx;
  font-weight: bold;
}
.tool-label {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #606266;
}

/* 列表 */
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
.card-status {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}
.st-new {
  background: #ecf5ff;
  color: #409eff;
}
.st-following {
  background: #fff7e6;
  color: #ff9900;
}
.st-converted {
  background: #edfff3;
  color: #19be6b;
}
.st-intended {
  background: #fdf6ec;
  color: #e6a23c;
}
.st-lost {
  background: #fef0f0;
  color: #f56c6c;
}

/* 悬浮按钮 */
.fab {
  position: fixed;
  right: 40rpx;
  bottom: 60rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 60rpx;
  text-align: center;
  line-height: 100rpx;
  box-shadow: 0 8rpx 24rpx rgba(64, 158, 255, 0.5);
}
</style>
