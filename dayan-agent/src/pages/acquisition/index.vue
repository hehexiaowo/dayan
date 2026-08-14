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
          <text v-if="keyword" class="search-clear" @click="keyword = ''">×</text>
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
          <view class="tool-item dy-clickable" @click="onTool('content')">
            <DyIconBlock text="文" color="green" size="md" shape="circle" />
            <text class="tool-label">内容获客</text>
          </view>
          <view class="tool-item dy-clickable" @click="onTool('tools')">
            <DyIconBlock text="具" color="blue" size="md" shape="circle" />
            <text class="tool-label">工具获客</text>
          </view>
          <view class="tool-item dy-clickable" @click="onTool('poster')">
            <DyIconBlock text="海" color="orange" size="md" shape="circle" />
            <text class="tool-label">营销海报</text>
          </view>
          <view class="tool-item dy-clickable" @click="onTool('card')">
            <DyIconBlock text="名" color="blue" size="md" shape="circle" />
            <text class="tool-label">电子名片</text>
          </view>
        </view>
      </view>
    </view>

    <!-- ===== 下半部分：线索清单区 ===== -->
    <view class="bottom-section">
      <!-- 线索池（待认领访客线索） -->
      <view v-if="poolLeads.length" class="pool-section">
        <view class="list-header">
          <text class="list-title">线索池</text>
          <text class="list-count">{{ poolLeads.length }} 条待认领</text>
        </view>
        <view
          v-for="item in poolLeads"
          :key="item.leadCode"
          class="card pool-card"
        >
          <view class="card-left">
            <image
              v-if="item.wxAvatar"
              :src="item.wxAvatar"
              mode="aspectFill"
              class="card-avatar-img"
            />
            <DyIconBlock
              v-else
              :text="poolName(item).charAt(0) || '?'"
              color="gray"
              size="sm"
              shape="circle"
            />
          </view>
          <view class="card-main">
            <view class="card-row-top">
              <text class="card-name">{{ poolName(item) }}</text>
            </view>
            <view class="card-row-mid">
              <text v-if="item.phone" class="card-phone">{{ item.phone }}</text>
              <text v-else class="card-phone muted">未留手机号</text>
            </view>
            <view class="card-row-bottom">
              <template v-if="item.lastInteractType">
                <view class="trace-pill" :class="tracePillClass(item.lastInteractType)">
                  <text class="trace-pill-text">{{ traceTypeText(item.lastInteractType) }}</text>
                </view>
                <text class="card-meta-sep">·</text>
              </template>
              <text class="card-meta">{{ item.interactCount || 0 }}次互动</text>
              <text v-if="item.lastInteractTime || item.createdAt" class="card-meta-sep">·</text>
              <text v-if="item.lastInteractTime || item.createdAt" class="card-meta">{{ formatTime(item.lastInteractTime || item.createdAt, true) }}</text>
            </view>
          </view>
          <view class="claim-btn dy-clickable" @click.stop="onClaim(item)">
            <text class="claim-btn-text">认领</text>
          </view>
        </view>
      </view>

      <!-- 线索标题 + 统计 -->
      <view class="list-header">
        <text class="list-title">线索清单</text>
        <text class="list-count">共 {{ leads.length }} 条</text>
      </view>

      <!-- 状态筛选 Tab -->
      <view class="status-tabs">
        <view
          v-for="tab in statusTabs"
          :key="tab.key"
          class="status-tab dy-clickable"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
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
          v-else-if="!filteredLeads.length"
          :text="emptyText"
          icon="线"
          color="blue"
          :action-text="activeTab === 'all' && !keyword ? '新增线索' : ''"
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
              <image
                v-if="lead.wxAvatar"
                :src="lead.wxAvatar"
                mode="aspectFill"
                class="card-avatar-img"
              />
              <DyIconBlock
                v-else
                :text="leadName(lead).charAt(0) || '?'"
                :color="avatarColor(lead.leadStatus)"
                size="sm"
                shape="circle"
              />
            </view>
            <view class="card-main">
              <!-- 第 1 行：姓名 + 客户状态 -->
              <view class="card-row-top">
                <text class="card-name">{{ leadName(lead) }}</text>
                <view class="card-status" :class="statusClass(lead.leadStatus)">
                  {{ statusText(lead.leadStatus) }}
                </view>
              </view>
              <!-- 第 2 行：手机号 -->
              <view class="card-row-mid">
                <text v-if="lead.phone" class="card-phone">{{ lead.phone }}</text>
                <text v-else class="card-phone muted">未留手机号</text>
              </view>
              <!-- 第 3 行：最后互动类型 · 互动次数 · 最后互动时间 -->
              <view class="card-row-bottom">
                <template v-if="lead.lastTraceType">
                  <view
                    class="trace-pill"
                    :class="tracePillClass(lead.lastTraceType)"
                  >
                    <text class="trace-pill-text">{{ traceTypeText(lead.lastTraceType) }}</text>
                  </view>
                  <text class="card-meta-sep">·</text>
                </template>
                <text class="card-meta">{{ lead.traceCount || 0 }}次互动</text>
                <text v-if="lead.lastTraceTime || lead.createdAt" class="card-meta-sep">·</text>
                <text v-if="lead.lastTraceTime || lead.createdAt" class="card-meta">{{ formatTime(lead.lastTraceTime || lead.createdAt, true) }}</text>
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
import { ref, computed } from 'vue';
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app';
import { getLeads, getLeadPool, claimLead } from '@/api/lead';
import { LeadStatus } from '@/types';
import type { Lead, LeadPoolItem } from '@/types';
import { statusText, statusClass, avatarColor, traceTypeText, formatTime } from '@/utils/lead';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const keyword = ref('');
const leads = ref<Lead[]>([]);
const loading = ref(false);
const loadError = ref(false);

/** 当前选中的 Tab key，'all' = 全部 */
const activeTab = ref<string>('all');

/** Tab key → 线索状态映射（all = 不限） */
const tabStatusMap: Record<string, LeadStatus[] | undefined> = {
  all: undefined,
  lead: [LeadStatus.NEW, LeadStatus.FOLLOWING],
  intent: [LeadStatus.INTENDED],
  convert: [LeadStatus.CONVERTED],
  abandon: [LeadStatus.LOST],
};

/** 状态筛选 Tab 定义 */
const statusTabs = computed(() => [
  { key: 'all', label: '全部', count: leads.value.length },
  { key: 'lead', label: '线索', count: countByStatuses(LeadStatus.NEW, LeadStatus.FOLLOWING) },
  { key: 'intent', label: '意向', count: countByStatus(LeadStatus.INTENDED) },
  { key: 'convert', label: '转化', count: countByStatus(LeadStatus.CONVERTED) },
  { key: 'abandon', label: '流失', count: countByStatus(LeadStatus.LOST) },
]);

/** 按当前 Tab + 关键字过滤的线索列表 */
const filteredLeads = computed(() => {
  const kw = keyword.value.trim();
  let list = leads.value;
  const statuses = tabStatusMap[activeTab.value];
  if (statuses) list = list.filter((l) => statuses.some((s) => s === l.leadStatus));
  if (kw) list = list.filter((l) => (l.name || '').includes(kw) || (l.phone || '').includes(kw));
  return list;
});

/** 空状态文案 */
const emptyText = computed(() => {
  if (activeTab.value === 'all') return '暂无线索';
  const tab = statusTabs.value.find((t) => t.key === activeTab.value);
  return `暂无${tab?.label || ''}线索`;
});

function countByStatus(status: LeadStatus): number {
  return leads.value.filter((l) => l.leadStatus === status).length;
}

function countByStatuses(...statuses: LeadStatus[]): number {
  return leads.value.filter((l) => statuses.some((s) => s === l.leadStatus)).length;
}

/** 线索显示名称：name > wxNickname > "匿名访客" */
function leadName(lead: Lead): string {
  return lead.name || lead.wxNickname || '匿名访客';
}

/** 互动类型标签配色（与详情页圆点配色一致：内容蓝/工具绿/海报橙） */
function tracePillClass(type?: number): string {
  switch (type) {
    case 1: return 'pill-content';
    case 2: return 'pill-tool';
    case 3: return 'pill-poster';
    default: return 'pill-default';
  }
}

async function loadList() {
  loading.value = true;
  loadError.value = false;
  try {
    // 一次性加载全部线索（不分状态、不带关键字），筛选 + 搜索均走客户端
    const res = await getLeads({
      size: 999,
    });
    leads.value = res?.records || [];
  } catch (e) {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

// ---------- 线索池（待认领） ----------
const poolLeads = ref<LeadPoolItem[]>([]);

/** 线索池展示名：name > wxNickname > 匿名访客 */
function poolName(item: LeadPoolItem): string {
  return item.name || item.wxNickname || '匿名访客';
}

async function loadPool() {
  try {
    // 只取前 5 条待认领线索做入口展示，失败静默（不阻塞主列表）
    const res = await getLeadPool({ current: 1, size: 5 });
    poolLeads.value = res?.records || [];
  } catch (e) {
    poolLeads.value = [];
  }
}

async function onClaim(item: LeadPoolItem) {
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '认领线索',
      content: `确定认领「${poolName(item)}」吗？认领后将进入你的线索清单。`,
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    });
  });
  if (!confirmed) return;
  try {
    await claimLead(item.leadCode);
    uni.showToast({ title: '认领成功', icon: 'success' });
    await Promise.all([loadPool(), loadList()]);
  } catch (e: any) {
    uni.showToast({ title: e?.message || '认领失败，可能已被他人认领', icon: 'none' });
    loadPool();
  }
}

function onSearch() {
  loadList();
}

function onAdd() {
  uni.navigateTo({ url: '/pages/acquisition/lead/form' });
}

function onShareCode() {
  uni.showToast({ title: '分享获客码功能开发中', icon: 'none' });
}

function onTool(type: string) {
  if (type === 'content') {
    uni.navigateTo({ url: '/pages/acquisition/content/index' });
    return;
  }
  if (type === 'card') {
    uni.navigateTo({ url: '/pages/acquisition/card/index' });
    return;
  }
  if (type === 'tools') {
    uni.navigateTo({ url: '/pages/acquisition/tools/index' });
    return;
  }
  if (type === 'poster') {
    uni.navigateTo({ url: '/pages/acquisition/poster/index' });
    return;
  }
  uni.showToast({ title: '功能开发中', icon: 'none' });
}

function onLeadClick(lead: Lead) {
  uni.navigateTo({ url: '/pages/acquisition/lead/detail?id=' + lead.id });
}

// 每次进入/返回页面统一刷新（首次加载、状态变更、新增、删除、认领后同步）
onShow(() => {
  loadList();
  loadPool();
});

onPullDownRefresh(async () => {
  try {
    await Promise.all([loadList(), loadPool()]);
  } finally {
    uni.stopPullDownRefresh();
  }
});
</script>

<style lang="scss" scoped>

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
  height: $control-height-sm;
  border: none;
  border-radius: $radius-md;
  padding: 0 20rpx 0 56rpx;
  font-size: 28rpx;
  background: rgba(255, 255, 255, 0.9);
  color: $text-primary;
  box-sizing: border-box;
}
.search-placeholder {
  color: $text-placeholder;
}
.search-clear {
  position: absolute;
  right: 20rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 36rpx;
  color: $text-placeholder;
  padding: 0 12rpx;
  z-index: 1;
}
.btn-search {
  background: #fff;
  color: $brand-primary;
  font-size: 26rpx;
  padding: 0 28rpx;
  height: $control-height-sm;
  line-height: $control-height-sm;
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
.card-avatar-img {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
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
.card-row-mid {
  margin-top: 8rpx;
}
.card-phone {
  font-size: 26rpx;
  color: $text-regular;
}
.card-phone.muted {
  color: $text-placeholder;
}
.card-row-bottom {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6rpx;
  margin-top: 8rpx;
}
.card-name {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}
/* 互动类型小标签 */
.trace-pill {
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
  flex-shrink: 0;
}
.trace-pill-text {
  font-size: 20rpx;
}
.pill-content {
  background: $brand-primary-light;
  .trace-pill-text { color: $brand-primary; }
}
.pill-tool {
  background: $brand-success-light;
  .trace-pill-text { color: $brand-success; }
}
.pill-poster {
  background: $brand-warning-light;
  .trace-pill-text { color: $brand-warning; }
}
.pill-default {
  background: $brand-info-light;
  .trace-pill-text { color: $brand-info; }
}
/* 底部元信息（次数/时间） */
.card-meta {
  font-size: 22rpx;
  color: $text-secondary;
}
.card-meta-sep {
  font-size: 22rpx;
  color: $text-placeholder;
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
  background: $brand-error-light;
  color: $brand-error;
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

/* ========== 线索池 ========== */
.pool-section {
  margin-bottom: $spacing-md;
}
.pool-card {
  border: 1rpx dashed $brand-primary;
}
.claim-btn {
  flex-shrink: 0;
  background: $brand-primary;
  border-radius: $radius-md;
  padding: 12rpx 28rpx;
}
.claim-btn-text {
  font-size: 24rpx;
  color: #fff;
}
</style>
