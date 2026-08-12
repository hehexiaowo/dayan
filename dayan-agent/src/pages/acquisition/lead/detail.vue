<template>
  <view class="detail-page">
    <!-- 骨架屏 -->
    <view v-if="loading" class="skeleton-wrap">
      <DySkeleton :rows="1" avatar card />
      <DySkeleton :rows="4" card />
      <DySkeleton :rows="3" card />
    </view>

    <view v-else-if="lead" class="detail-content">
      <!-- 线索头部 -->
      <view class="header-section">
        <view class="header-info">
          <view class="header-left">
            <DyIconBlock
              :text="lead.name?.charAt(0) || '?'"
              :color="avatarColor(lead.leadStatus)"
              size="lg"
              shape="circle"
            />
          </view>
          <view class="header-main">
            <view class="header-row-top">
              <text class="lead-name">{{ lead.name || '未命名' }}</text>
              <view class="lead-status" :class="statusClass(lead.leadStatus)">
                {{ statusText(lead.leadStatus) }}
              </view>
            </view>
            <view class="header-row-bottom">
              <text v-if="lead.phone" class="lead-phone" @click="onCall">{{ lead.phone }}</text>
              <text v-else class="lead-phone muted">未填手机号</text>
              <view v-if="lead.phone" class="btn-call dy-clickable" @click="onCall">
                <text class="btn-call-text">拨打</text>
              </view>
            </view>
            <text v-if="lead.leadCode" class="lead-code">编号：{{ lead.leadCode }}</text>
          </view>
        </view>
      </view>

      <!-- 基本信息 -->
      <view class="section-title">基本信息</view>
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">性别</text>
          <text class="info-value">{{ genderText(lead.gender) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">年龄</text>
          <text class="info-value">{{ lead.age ? lead.age + ' 岁' : '-' }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">意向等级</text>
          <text class="info-value">
            <text v-if="lead.intentionLevel" class="intention-tag" :class="intentionClass(lead.intentionLevel)">
              {{ intentionText(lead.intentionLevel) }}
            </text>
            <text v-else>-</text>
          </text>
        </view>
        <view class="info-row">
          <text class="info-label">来源</text>
          <text class="info-value">{{ sourceText(lead.sourceType) }}</text>
        </view>
        <view v-if="lead.interestType" class="info-row">
          <text class="info-label">关注类型</text>
          <text class="info-value">{{ lead.interestType }}</text>
        </view>
        <view v-if="lead.region" class="info-row">
          <text class="info-label">关注区域</text>
          <text class="info-value">{{ lead.region }}</text>
        </view>
      </view>

      <!-- 时间信息 -->
      <view class="section-title">时间信息</view>
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">创建时间</text>
          <text class="info-value">{{ formatTime(lead.createdAt) }}</text>
        </view>
        <view v-if="lead.lastFollowTime" class="info-row">
          <text class="info-label">最后跟进</text>
          <text class="info-value">{{ formatTime(lead.lastFollowTime) }}</text>
        </view>
        <view v-if="lead.convertedAt" class="info-row">
          <text class="info-label">转化时间</text>
          <text class="info-value">{{ formatTime(lead.convertedAt) }}</text>
        </view>
      </view>

      <!-- 备注 -->
      <view v-if="lead.remark" class="section-title">备注</view>
      <view v-if="lead.remark" class="info-card">
        <text class="remark-text">{{ lead.remark }}</text>
      </view>

      <!-- 互动记录 -->
      <view class="section-title">互动记录<text v-if="traces.length" class="section-count">{{ traces.length }}</text></view>
      <view class="info-card">
        <view v-if="tracesLoading && !traces.length" class="trace-empty">加载中...</view>
        <view v-else-if="!traces.length" class="trace-empty">暂无互动记录</view>
        <view v-else class="trace-list">
          <view v-for="(trace, idx) in traces" :key="trace.id" class="trace-item">
            <view v-if="idx < traces.length - 1" class="trace-line" />
            <view class="trace-dot" :class="traceDotClass(trace.traceType)">
              <text class="trace-dot-text">{{ traceIcon(trace.traceType) }}</text>
            </view>
            <view class="trace-content">
              <text class="trace-action">{{ traceAction(trace.traceType) }}</text>
              <text v-if="trace.bizTitle" class="trace-title">「{{ trace.bizTitle }}」</text>
              <text class="trace-time">{{ formatTime(trace.traceTime) }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 底部操作栏 -->
      <view class="action-bar">
        <view class="action-btn dy-clickable" @click="onEdit">
          <text class="action-btn-text">编辑</text>
        </view>
        <view class="action-btn dy-clickable" @click="onChangeStatus">
          <text class="action-btn-text">变更状态</text>
        </view>
        <view class="action-btn action-danger dy-clickable" @click="onDelete">
          <text class="action-btn-text">删除</text>
        </view>
      </view>
    </view>

    <!-- 空状态（线索不存在或已删除） -->
    <DyEmpty
      v-else
      text="线索不存在或已被删除"
      icon="线"
      color="gray"
    />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { getLeadDetail, updateLead, deleteLead, getLeadTraces } from '@/api/lead';
import { LeadStatus } from '@/types';
import type { Lead, LeadTrace } from '@/types';
import { statusText, statusClass, avatarColor, genderText, intentionText, intentionClass, sourceText, formatTime } from '@/utils/lead';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const lead = ref<Lead | null>(null);
const loading = ref(true);
const traces = ref<LeadTrace[]>([]);
const tracesLoading = ref(false);
let leadId: string | null = null;

const STATUS_OPTIONS: { label: string; value: number }[] = [
  { label: '新线索', value: LeadStatus.NEW },
  { label: '跟进中', value: LeadStatus.FOLLOWING },
  { label: '意向', value: LeadStatus.INTENDED },
  { label: '已转化', value: LeadStatus.CONVERTED },
  { label: '已流失', value: LeadStatus.LOST },
];

async function loadDetail() {
  if (!leadId) {
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    lead.value = await getLeadDetail(leadId);
    // 加载互动记录
    loadTraces();
  } catch {
    lead.value = null;
  } finally {
    loading.value = false;
  }
}

async function loadTraces() {
  if (!leadId) return;
  tracesLoading.value = true;
  try {
    traces.value = await getLeadTraces(leadId);
  } catch {
    traces.value = [];
  } finally {
    tracesLoading.value = false;
  }
}

/** 互动类型图标 */
function traceIcon(type?: number): string {
  if (type === 1) return '📄';
  if (type === 2) return '🔧';
  if (type === 3) return '🖼️';
  return '•';
}

/** 互动类型动作文案 */
function traceAction(type?: number): string {
  if (type === 1) return '浏览内容';
  if (type === 2) return '使用工具';
  if (type === 3) return '查看海报';
  return '访问';
}

/** 互动圆点样式 */
function traceDotClass(type?: number): string {
  if (type === 1) return 'dot-content';
  if (type === 2) return 'dot-tool';
  if (type === 3) return 'dot-poster';
  return 'dot-default';
}

function onCall() {
  if (!lead.value?.phone) return;
  uni.makePhoneCall({ phoneNumber: lead.value.phone });
}

function onEdit() {
  if (!leadId) return;
  uni.navigateTo({ url: '/pages/acquisition/lead/form?id=' + leadId });
}

function onChangeStatus() {
  if (!leadId || !lead.value) return;
  const currentStatus = lead.value.leadStatus;
  // 过滤掉当前状态，只展示可变更的状态
  const items = STATUS_OPTIONS.filter((s) => s.value !== currentStatus);
  uni.showActionSheet({
    itemList: items.map((s) => s.label),
    success: async (res) => {
      const chosen = items[res.tapIndex];
      if (!chosen) return;
      // 终态（已转化/已流失）需二次确认，防止误触
      const needConfirm = chosen.value === LeadStatus.CONVERTED || chosen.value === LeadStatus.LOST;
      const doUpdate = async () => {
        try {
          await updateLead(leadId!, { leadStatus: chosen.value });
          uni.showToast({ title: '已更新', icon: 'success' });
          // 本地立即更新，无需整页重载
          lead.value!.leadStatus = chosen.value;
          // 状态变跟进中时后端更新了 lastFollowTime，重新拉取以同步
          if (chosen.value === LeadStatus.FOLLOWING) {
            await loadDetail();
          }
        } catch {
          // 错误已由 request 拦截器提示
        }
      };
      if (needConfirm) {
        uni.showModal({
          title: chosen.value === LeadStatus.CONVERTED ? '确认转化' : '确认流失',
            content:
              chosen.value === LeadStatus.CONVERTED
                ? '确认将此线索标记为已转化？'
                : '确认将此线索标记为流失？标记后可在状态中变更。',
          confirmColor: chosen.value === LeadStatus.LOST ? '#fa3534' : '#409eff',
          success: (r) => {
            if (r.confirm) doUpdate();
          },
        });
      } else {
        doUpdate();
      }
    },
  });
}

function onDelete() {
  if (!leadId) return;
  uni.showModal({
    title: '删除线索',
    content: '确认删除此线索？删除后不可恢复。',
    confirmColor: '#fa3534',
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await deleteLead(leadId!);
        uni.showToast({ title: '已删除', icon: 'success' });
        setTimeout(() => uni.navigateBack(), 500);
      } catch {
        // 错误已由 request 拦截器提示
      }
    },
  });
}

onLoad((options: any) => {
  leadId = options?.id ? String(options.id) : null;
  loadDetail();
});

// 从编辑页返回时刷新详情
onShow(() => {
  if (leadId && !loading.value) {
    loadDetail();
  }
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.detail-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 160rpx;
}

.skeleton-wrap {
  padding: $spacing-md;
}

/* 线索头部 */
.header-section {
  background: $gradient-blue;
  padding: $spacing-xl $spacing-lg $spacing-lg;
}
.header-info {
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
}
.header-left {
  flex-shrink: 0;
  padding-top: 8rpx;
}
.header-main {
  flex: 1;
  min-width: 0;
}
.header-row-top {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: 8rpx;
}
.lead-name {
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
}
.lead-status {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
  font-weight: 500;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}
.header-row-bottom {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}
.lead-phone {
  font-size: 30rpx;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}
.lead-phone.muted {
  color: rgba(255, 255, 255, 0.5);
  font-weight: normal;
}
.btn-call {
  background: rgba(255, 255, 255, 0.2);
  border-radius: $radius-sm;
  padding: 12rpx 28rpx;
  display: flex;
  align-items: center;
  min-height: 88rpx;
}
.btn-call-text {
  font-size: 24rpx;
  color: #fff;
}
.lead-code {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
}

/* 区块标题 */
.section-title {
  padding: $spacing-lg $spacing-lg $spacing-sm;
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}

/* 信息卡片 */
.info-card {
  background: $bg-card;
  margin: 0 $spacing-lg $spacing-sm;
  border-radius: $radius-md;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
}
.info-row {
  display: flex;
  padding: 12rpx 0;
}
.info-label {
  width: 140rpx;
  font-size: 26rpx;
  color: $text-secondary;
  flex-shrink: 0;
}
.info-value {
  flex: 1;
  font-size: 26rpx;
  color: $text-primary;
}

/* 意向等级标签 */
.intention-tag {
  display: inline-block;
  font-size: 24rpx;
  padding: 2rpx 16rpx;
  border-radius: 20rpx;
  font-weight: 500;
}
.it-high {
  background: $brand-error-light;
  color: $brand-error;
}
.it-mid {
  background: $brand-warning-light;
  color: $brand-warning;
}
.it-low {
  background: $brand-info-light;
  color: $brand-info;
}

/* 备注 */
.remark-text {
  font-size: 26rpx;
  color: $text-regular;
  line-height: 1.6;
}

/* 底部操作栏 */
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  padding-bottom: calc($spacing-sm + env(safe-area-inset-bottom));
  background: $bg-card;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.action-btn {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  border-radius: $radius-md;
  background: $brand-primary-light;
}
.action-btn-text {
  font-size: 28rpx;
  font-weight: 500;
  color: $brand-primary;
}
.action-danger {
  background: $brand-error-light;
  .action-btn-text {
    color: $brand-error;
  }
}

/* 互动记录时间线 */
.section-count {
  margin-left: $spacing-xs;
  font-size: 24rpx;
  color: $text-secondary;
  font-weight: normal;
}
.trace-empty {
  padding: $spacing-lg 0;
  text-align: center;
  font-size: 26rpx;
  color: $text-secondary;
}
.trace-list {
  padding: $spacing-sm 0;
}
.trace-item {
  display: flex;
  align-items: flex-start;
  position: relative;
  padding-bottom: $spacing-md;
}
.trace-line {
  position: absolute;
  left: 26rpx;
  top: 48rpx;
  bottom: 0;
  width: 2rpx;
  background: $border-base;
}
.trace-dot {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: $spacing-md;
}
.dot-content { background: $brand-primary-light; }
.dot-tool { background: $brand-success-light; }
.dot-poster { background: $brand-warning-light; }
.dot-default { background: $brand-info-light; }
.trace-dot-text {
  font-size: 28rpx;
}
.trace-content {
  flex: 1;
  padding-top: 4rpx;
}
.trace-action {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}
.trace-title {
  font-size: 28rpx;
  color: $text-regular;
}
.trace-time {
  display: block;
  margin-top: 4rpx;
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>
