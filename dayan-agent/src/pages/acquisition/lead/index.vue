<template>
  <view class="page dy-safe-bottom">
    <!-- ===== 渐变 header（与其他 tab 页统一） ===== -->
    <view class="header">
      <text class="header-title">线索管理</text>
      <text class="header-sub">认领公共线索，跟进客户转化</text>
    </view>

    <!-- ===== 搜索栏（白卡，与服务页一致） ===== -->
    <view class="toolbar">
      <view class="search">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索客户名/手机号"
          placeholder-class="search-placeholder"
          confirm-type="search"
          @confirm="onSearch"
        />
        <text v-if="keyword" class="search-clear" @click="clearKeyword">×</text>
        <button class="btn-search" size="mini" @click="onSearch">搜索</button>
      </view>
    </view>

    <!-- ===== 内容区 ===== -->
    <view class="bottom-section">
      <!-- Tab：线索池靠左（灰色区隔），状态 Tab 靠右 -->
      <view class="tabs-row">
        <view
          class="status-tab pool-tab dy-clickable"
          :class="{ active: activeTab === 'pool' }"
          @click="activeTab = 'pool'"
        >
          <text class="status-tab-text">线索池</text>
          <text v-if="poolLeads.length > 0" class="status-tab-count">{{ poolLeads.length }}</text>
        </view>
        <view class="tabs-right">
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
      </view>

      <!-- 内容区 -->
      <view class="list-body">
        <!-- 加载骨架屏 -->
        <template v-if="loading && !leads.length && !poolLeads.length">
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

        <!-- ===== 线索池 Tab：待认领访客 ===== -->
        <template v-else-if="activeTab === 'pool'">
          <DyEmpty
            v-if="!poolLeads.length"
            text="暂无可认领线索"
            icon="线"
            color="blue"
          />
          <view
            v-for="item in poolLeads"
            :key="item.leadCode"
            class="card pool-card dy-clickable"
            @click="onClaim(item)"
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
            <view class="claim-btn">
              <text class="claim-btn-text">认领</text>
            </view>
          </view>
        </template>

        <!-- ===== 状态 Tab：空状态 ===== -->
        <DyEmpty
          v-else-if="!filteredLeads.length"
          :text="emptyText"
          icon="线"
          color="blue"
          :action-text="activeTab === 'all' && !keyword ? '新增线索' : ''"
          @action="onAdd"
        />

        <!-- ===== 状态 Tab：线索卡片 ===== -->
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

/**
 * 线索管理页（独立页面）。
 *
 * - 从获客中心首页拆出：搜索 + 线索池（待认领）+ 线索清单（状态筛选）；
 * - 一次性加载全部线索，筛选 + 搜索均走客户端；
 * - 入口：获客中心首页「线索管理」工具卡、个人中心「线索记录」。
 */

const keyword = ref('');
const leads = ref<Lead[]>([]);
const loading = ref(false);
const loadError = ref(false);

/** 当前选中的 Tab key：'pool' = 公共线索池，其余为状态 Tab */
const activeTab = ref<string>('pool');

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

function clearKeyword() {
  keyword.value = '';
}

function onSearch() {
  loadList();
}

function onAdd() {
  uni.navigateTo({ url: '/pages/acquisition/lead/form' });
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

/* ===== 渐变 header（与其他 tab 页统一） ===== */
.header {
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  margin: $spacing-md $spacing-md 0;
}
.header-title {
  display: block;
  font-size: 38rpx;
  font-weight: bold;
  color: #fff;
}
.header-sub {
  display: block;
  margin-top: $spacing-sm;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

/* ===== 搜索栏（白卡，与服务页一致） ===== */
.toolbar {
  background: $bg-card;
  border-radius: $radius-md;
  margin: $spacing-md $spacing-md 0;
  padding: $spacing-md;
  box-shadow: $shadow-card;
}
.search {
  display: flex;
  align-items: center;
}
.search-input {
  flex: 1;
  height: $control-height-sm;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  padding: 0 20rpx;
  font-size: 28rpx;
  background: $bg-page;
  box-sizing: border-box;
}
.search-placeholder {
  color: $text-placeholder;
}
.search-clear {
  padding: 0 16rpx;
  font-size: 36rpx;
  color: $text-placeholder;
}
.btn-search {
  margin-left: $spacing-sm;
  height: $control-height-sm;
  line-height: $control-height-sm;
  background: $gradient-blue;
  color: #fff;
  font-size: 26rpx;
  padding: 0 32rpx;
  border-radius: $radius-md;
  box-shadow: 0 4rpx 12rpx rgba(64, 158, 255, 0.3);
}

/* ========== 内容区 ========== */
.bottom-section {
  flex: 1;
  background: $bg-page;
  border-radius: $radius-lg $radius-lg 0 0;
  margin-top: $spacing-sm;
  padding: $spacing-lg $spacing-md 0;
  position: relative;
  z-index: 1;
}

/* Tab 行：线索池靠左（灰色区隔），状态 Tab 组靠右 */
.tabs-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
  border-bottom: 1rpx solid $border-light;
  padding-bottom: $spacing-sm;
}
.tabs-right {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  margin-left: auto;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  white-space: nowrap;
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
/* 线索池 Tab：浅灰底，与右侧状态 Tab 视觉区隔 */
.pool-tab {
  background: $bg-page;
  border: 1rpx solid $border-base;
}
.pool-tab.active {
  background: $brand-warning;
  border-color: transparent;
  .status-tab-text {
    color: #fff;
    font-weight: bold;
  }
  .status-tab-count {
    background: rgba(255, 255, 255, 0.3);
    color: #fff;
  }
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

/* ========== 线索池卡片（虚线边框 + 认领按钮） ========== */
.pool-card {
  border: 1rpx dashed $brand-warning;
}
.claim-btn {
  flex-shrink: 0;
  background: $brand-warning;
  border-radius: $radius-md;
  padding: 12rpx 28rpx;
}
.claim-btn-text {
  font-size: 24rpx;
  color: #fff;
}
</style>
