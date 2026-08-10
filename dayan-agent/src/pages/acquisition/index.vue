<template>
  <view class="page dy-safe-bottom">
    <!-- ===== 上半部分：获客工具区 ===== -->
    <view class="top-section">
      <!-- 搜索栏 -->
      <view class="search-bar">
        <view class="search-input-wrap">
          <text class="search-icon">搜</text>
          <input
            v-model="keyword"
            class="search-input"
            placeholder="搜索客户名/手机号"
            placeholder-class="search-placeholder"
            confirm-type="search"
            @confirm="onSearch"
          />
        </view>
        <button class="btn-search" size="mini" @click="onSearch">搜索</button>
      </view>

      <!-- 获客工具 -->
      <view class="tools-card">
        <view class="tools-header">
          <text class="tools-title">获客工具</text>
          <view class="actions">
            <view class="btn-action btn-share dy-clickable" @click="onShareCode">
              <text class="btn-action-text">分享获客码</text>
            </view>
          </view>
        </view>
        <view class="tools-grid">
          <view class="tool-item dy-clickable" @click="onTool('card')">
            <DyIconBlock text="名" color="blue" size="md" shape="circle" />
            <text class="tool-label">电子名片</text>
          </view>
          <view class="tool-item dy-clickable" @click="onTool('poster')">
            <DyIconBlock text="海" color="orange" size="md" shape="circle" />
            <text class="tool-label">营销海报</text>
          </view>
          <view class="tool-item dy-clickable" @click="onTool('content')">
            <DyIconBlock text="享" color="green" size="md" shape="circle" />
            <text class="tool-label">内容分享</text>
          </view>
        </view>
      </view>
    </view>

    <!-- ===== 下半部分：线索清单区 ===== -->
    <view class="bottom-section">
      <!-- 线索标题 + 统计 -->
      <view class="list-header">
        <text class="list-title">线索清单</text>
        <text class="list-count">共 {{ leads.length }} 条</text>
      </view>

      <!-- 状态筛选 Tab -->
      <view class="status-tabs">
        <view
          v-for="tab in statusTabs"
          :key="String(tab.value)"
          class="status-tab dy-clickable"
          :class="{ active: activeStatus === tab.value }"
          @click="activeStatus = tab.value"
        >
          <text class="status-tab-text">{{ tab.label }}</text>
          <text v-if="tab.count > 0" class="status-tab-count">{{ tab.count }}</text>
        </view>
      </view>

      <!-- 线索列表 -->
      <view class="list-body">
        <!-- 加载骨架屏 -->
        <template v-if="loading && !leads.length">
          <DySkeleton v-for="i in 3" :key="i" :rows="2" avatar />
        </template>

        <!-- 空状态 -->
        <DyEmpty
          v-else-if="!filteredLeads.length"
          :text="emptyText"
          icon="线"
          color="blue"
          :action-text="activeStatus === null ? '新增线索' : ''"
          @action="onAdd"
        />

        <!-- 线索卡片 -->
        <view v-else>
          <view
            v-for="lead in filteredLeads"
            :key="lead.id"
            class="card dy-clickable"
            @click="onLeadClick(lead)"
          >
            <view class="card-left">
              <DyIconBlock
                :text="lead.name?.charAt(0) || '?'"
                :color="avatarColor(lead.leadStatus)"
                size="sm"
                shape="circle"
              />
            </view>
            <view class="card-main">
              <view class="card-row-top">
                <text class="card-name">{{ lead.name || '未命名' }}</text>
                <view class="card-status" :class="statusClass(lead.leadStatus)">
                  {{ statusText(lead.leadStatus) }}
                </view>
              </view>
              <view class="card-row-bottom">
                <text class="card-phone">{{ formatPhone(lead.phone) }}</text>
                <text v-if="lead.createdAt" class="card-time">{{ lead.createdAt }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 悬浮按钮 -->
    <view class="fab dy-clickable" @click="onAdd">
      <text class="fab-icon">+</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app';
import { getLeads } from '@/api/lead';
import { LeadStatus } from '@/types';
import type { Lead } from '@/types';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const keyword = ref('');
const leads = ref<Lead[]>([]);
const loading = ref(false);

/** 当前选中的状态筛选，null = 全部 */
const activeStatus = ref<LeadStatus | null>(null);

/** 状态筛选 Tab 定义 */
const statusTabs = computed(() => [
  { label: '全部', value: null as LeadStatus | null, count: leads.value.length },
  { label: '新线索', value: LeadStatus.NEW, count: countByStatus(LeadStatus.NEW) },
  { label: '跟进中', value: LeadStatus.FOLLOWING, count: countByStatus(LeadStatus.FOLLOWING) },
  { label: '意向', value: LeadStatus.INTENDED, count: countByStatus(LeadStatus.INTENDED) },
  { label: '已转化', value: LeadStatus.CONVERTED, count: countByStatus(LeadStatus.CONVERTED) },
  { label: '已流失', value: LeadStatus.LOST, count: countByStatus(LeadStatus.LOST) },
]);

/** 按当前 Tab 过滤的线索列表 */
const filteredLeads = computed(() => {
  if (activeStatus.value === null) return leads.value;
  return leads.value.filter((l) => l.leadStatus === activeStatus.value);
});

/** 空状态文案 */
const emptyText = computed(() => {
  if (activeStatus.value === null) return '暂无线索';
  return `暂无${statusText(activeStatus.value)}线索`;
});

function countByStatus(status: LeadStatus): number {
  return leads.value.filter((l) => l.leadStatus === status).length;
}

async function loadList() {
  loading.value = true;
  try {
    // 一次性加载全部线索（不分状态），状态 Tab 切换纯客户端筛选 + 计数
    const res = await getLeads({
      keyword: keyword.value || undefined,
      size: 999,
    });
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
  uni.navigateTo({ url: '/pages/acquisition/form' });
}

function onShareCode() {
  uni.showToast({ title: '分享获客码功能开发中', icon: 'none' });
}

function onTool(type: string) {
  uni.showToast({ title: '获客工具（Inc 4 上线）', icon: 'none' });
}

function onLeadClick(lead: Lead) {
  uni.navigateTo({ url: '/pages/acquisition/detail?id=' + lead.id });
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

/** 根据线索状态返回头像色块颜色 */
function avatarColor(s?: LeadStatus | number): 'blue' | 'green' | 'orange' | 'red' | 'gray' {
  switch (s) {
    case LeadStatus.NEW:
    case 1:
      return 'blue';
    case LeadStatus.FOLLOWING:
    case 2:
      return 'orange';
    case LeadStatus.INTENDED:
    case 3:
      return 'orange';
    case LeadStatus.CONVERTED:
    case 4:
      return 'green';
    case LeadStatus.LOST:
    case 5:
      return 'gray';
    default:
      return 'blue';
  }
}

function formatPhone(phone?: string): string {
  if (!phone) return '手机：-';
  return `手机：${phone}`;
}

onMounted(() => {
  loadList();
});

// 从详情/表单页返回时刷新列表（状态变更、新增、删除后同步）
onShow(() => {
  if (leads.value.length > 0) {
    loadList();
  }
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
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

/* ========== 上半部分：获客工具区 ========== */
.top-section {
  background: $gradient-blue;
  padding: $spacing-md $spacing-md $spacing-lg;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}
.search-input-wrap {
  flex: 1;
  position: relative;
}
.search-icon {
  position: absolute;
  left: 24rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  z-index: 1;
}
.search-input {
  width: 100%;
  border: none;
  border-radius: $radius-md;
  padding: 18rpx 20rpx 18rpx 56rpx;
  font-size: 28rpx;
  background: rgba(255, 255, 255, 0.9);
  color: $text-primary;
}
.search-placeholder {
  color: $text-placeholder;
}
.btn-search {
  background: #fff;
  color: $brand-primary;
  font-size: 26rpx;
  padding: 0 28rpx;
  height: 68rpx;
  line-height: 68rpx;
  border-radius: $radius-md;
  font-weight: 500;
}

/* 获客工具卡片 */
.tools-card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg $spacing-md;
  margin-top: $spacing-md;
  box-shadow: $shadow-card;
}
.tools-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;
}
.tools-title {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}
.actions {
  display: flex;
  gap: $spacing-sm;
}
.btn-action {
  background: $brand-primary-light;
  border-radius: $radius-sm;
  padding: 8rpx 20rpx;
}
.btn-action-text {
  font-size: 24rpx;
  color: $brand-primary;
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
.tool-label {
  margin-top: $spacing-sm;
  font-size: 24rpx;
  color: $text-regular;
}

/* ========== 下半部分：线索清单区 ========== */
.bottom-section {
  flex: 1;
  background: $bg-page;
  border-radius: $radius-lg $radius-lg 0 0;
  margin-top: -$spacing-sm;
  padding: $spacing-lg $spacing-md 0;
  position: relative;
  z-index: 1;
}

/* 列表头 */
.list-header {
  display: flex;
  align-items: baseline;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}
.list-title {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}
.list-count {
  font-size: 24rpx;
  color: $text-secondary;
}

/* 状态筛选 Tab */
.status-tabs {
  display: flex;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  white-space: nowrap;
  margin-bottom: $spacing-md;
  border-bottom: 1rpx solid $border-light;
  padding-bottom: $spacing-sm;
  gap: $spacing-xs;
}
.status-tab {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 12rpx 24rpx;
  border-radius: $radius-md;
  flex-shrink: 0;
  transition: all $transition-base;
}
.status-tab-text {
  font-size: 26rpx;
  color: $text-regular;
}
.status-tab-count {
  font-size: 20rpx;
  color: $text-placeholder;
  background: $border-light;
  border-radius: 16rpx;
  padding: 2rpx 10rpx;
  line-height: 1.4;
}
.status-tab.active {
  background: $brand-primary-light;
  .status-tab-text {
    color: $brand-primary;
    font-weight: bold;
  }
  .status-tab-count {
    background: $brand-primary;
    color: #fff;
  }
}

/* 列表区 */
.list-body {
  min-height: 300rpx;
}

/* 线索卡片 */
.card {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md $spacing-lg;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}
.card-left {
  flex-shrink: 0;
  margin-right: $spacing-md;
}
.card-main {
  flex: 1;
  min-width: 0;
}
.card-row-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-row-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8rpx;
}
.card-name {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}
.card-phone,
.card-time {
  font-size: 24rpx;
  color: $text-secondary;
}
.card-status {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  font-weight: 500;
  flex-shrink: 0;
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
  background: $brand-info-light;
  color: $brand-info;
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
}
.fab-icon {
  color: #fff;
  font-size: 56rpx;
  font-weight: 300;
}
</style>
