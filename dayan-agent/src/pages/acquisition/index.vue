<template>
  <view class="page dy-safe-bottom">
    <!-- 顶部操作条 -->
    <view class="toolbar">
      <view class="search">
        <text class="search-icon">搜</text>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索客户名/手机号"
          placeholder-class="search-placeholder"
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
          <DyIconBlock text="名" color="blue" size="md" shape="circle" />
          <text class="tool-label">电子名片</text>
        </view>
        <view class="tool-item" @click="onTool('poster')">
          <DyIconBlock text="海" color="orange" size="md" shape="circle" />
          <text class="tool-label">营销海报</text>
        </view>
        <view class="tool-item" @click="onTool('content')">
          <DyIconBlock text="享" color="green" size="md" shape="circle" />
          <text class="tool-label">内容分享</text>
        </view>
      </view>
    </view>

    <!-- 线索列表 -->
    <view class="list">
      <!-- 加载骨架屏 -->
      <template v-if="loading && !leads.length">
        <DySkeleton v-for="i in 3" :key="i" :rows="2" avatar />
      </template>

      <!-- 空状态 -->
      <DyEmpty
        v-else-if="!leads.length"
        text="暂无线索"
        icon="线"
        color="blue"
        action-text="新增线索"
        @action="onAdd"
      />

      <!-- 线索卡片 -->
      <view v-else>
        <view
          v-for="lead in leads"
          :key="lead.leadId"
          class="card dy-clickable"
          @click="onLeadClick(lead)"
        >
          <view class="card-row">
            <view class="card-name">{{ lead.name || '未命名' }}</view>
            <view class="card-status" :class="statusClass(lead.leadStatus)">
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

    <!-- 悬浮按钮 -->
    <view class="fab" @click="onAdd">
      <text class="fab-icon">+</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { onPullDownRefresh } from '@dcloudio/uni-app';
import { getLeads } from '@/api/lead';
import type { Lead, LeadStatus } from '@/types';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

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
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  padding: $spacing-md $spacing-md 0;
  min-height: 100vh;
  background: $bg-page;
}

/* 工具条 */
.toolbar {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  box-shadow: $shadow-card;
}
.search {
  display: flex;
  align-items: center;
  position: relative;
}
.search-icon {
  position: absolute;
  left: 24rpx;
  font-size: 22rpx;
  color: $text-placeholder;
  z-index: 1;
}
.search-input {
  flex: 1;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  padding: 18rpx 20rpx 18rpx 56rpx;
  font-size: 28rpx;
  transition: border-color $transition-base;

  &:focus {
    border-color: $brand-primary;
  }
}
.search-placeholder {
  color: $text-placeholder;
}
.btn-search {
  margin-left: $spacing-sm;
  background: $brand-primary;
  color: #fff;
  font-size: 26rpx;
  padding: 0 24rpx;
  height: 68rpx;
  line-height: 68rpx;
  border-radius: $radius-md;
}
.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-sm;
  gap: $spacing-sm;
}
.btn-outline {
  background: $bg-card;
  color: $brand-primary;
  border: 2rpx solid $brand-primary;
  font-size: 24rpx;
  padding: 0 20rpx;
  height: 56rpx;
  line-height: 56rpx;
  border-radius: $radius-sm;
}
.btn-primary {
  background: $brand-success;
  color: #fff;
  font-size: 24rpx;
  padding: 0 20rpx;
  height: 56rpx;
  line-height: 56rpx;
  border-radius: $radius-sm;
}

/* 获客工具 */
.tools {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-lg $spacing-md;
  margin-top: $spacing-md;
  box-shadow: $shadow-card;
}
.tools-title {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
  margin-bottom: $spacing-md;
}
.tools-grid {
  display: flex;
  justify-content: space-around;
}
.tool-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: transform $transition-fast;

  &:active {
    transform: scale(0.95);
  }
}
.tool-label {
  margin-top: $spacing-sm;
  font-size: 24rpx;
  color: $text-regular;
}

/* 列表 */
.list {
  margin-top: $spacing-md;
}
.card {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
}
.card-row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  &.sub {
    margin-top: 12rpx;
  }
}
.card-name {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}
.card-phone,
.card-time {
  font-size: 26rpx;
  color: $text-secondary;
}
.card-status {
  font-size: 24rpx;
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  font-weight: 500;
}
.st-new {
  background: $brand-primary-light;
  color: $brand-primary;
}
.st-following {
  background: $brand-warning-light;
  color: $brand-warning;
}
.st-converted {
  background: $brand-success-light;
  color: $brand-success;
}
.st-intended {
  background: #fdf6ec;
  color: #e6a23c;
}
.st-lost {
  background: $brand-error-light;
  color: $brand-error;
}

/* 悬浮按钮 */
.fab {
  position: fixed;
  right: 40rpx;
  bottom: 160rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: $gradient-blue;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-fab;
  z-index: 100;
  transition: transform $transition-fast;

  &:active {
    transform: scale(0.9);
  }
}
.fab-icon {
  color: #fff;
  font-size: 56rpx;
  font-weight: 300;
}
</style>
