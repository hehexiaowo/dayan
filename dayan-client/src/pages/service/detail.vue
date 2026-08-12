<template>
  <view class="detail-page">
    <view v-if="loading" class="loading"><text>加载中...</text></view>

    <template v-else-if="session">
      <!-- 状态条 -->
      <view class="status-hero" :class="statusClass">
        <view class="status-row">
          <text class="status-badge">{{ statusText }}</text>
          <text v-if="session.butlerFullName || session.butlerName" class="butler">管家：{{ session.butlerFullName || session.butlerName }}</text>
        </view>
        <text class="status-hint">{{ statusHint }}</text>
        <text class="session-title">{{ session.serviceTitle || session.title || '服务会话' }}</text>
      </view>

      <!-- 服务信息 -->
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">服务单号</text>
          <text class="info-val">{{ session.sessionCode }}</text>
        </view>
        <view v-if="session.equityCode" class="info-row">
          <text class="info-label">关联权益</text>
          <text class="info-val">{{ session.equityCode }}</text>
        </view>
        <view v-if="session.serviceDescription" class="info-row">
          <text class="info-label">需求描述</text>
          <text class="info-val desc">{{ session.serviceDescription }}</text>
        </view>
        <view v-if="session.remark" class="info-row">
          <text class="info-label">备注</text>
          <text class="info-val desc">{{ session.remark }}</text>
        </view>
      </view>

      <!-- 进度时间线 -->
      <view class="section-header">
        <text class="sh-title">服务进度</text>
      </view>
      <view v-if="timelineNodes.length === 0" class="empty-timeline">
        <text>暂无进度记录</text>
      </view>
      <view v-else class="timeline">
        <view v-for="(node, i) in timelineNodes" :key="i" class="tl-node">
          <view class="tl-line-col">
            <view class="tl-dot" :class="node.type">{{ dotIcon(node.type) }}</view>
            <view v-if="i < timelineNodes.length - 1" class="tl-line"></view>
          </view>
          <view class="tl-content">
            <text class="tl-title">{{ node.title }}</text>
            <text v-if="node.content" class="tl-desc">{{ node.content }}</text>
            <text v-if="node.time" class="tl-time">{{ formatTime(node.time) }}</text>
          </view>
        </view>
      </view>

      <!-- 操作区（按状态动态） -->
      <view v-if="showActions" class="action-bar">
        <!-- 方案待确认 -->
        <template v-if="session.sessionStatus === 3">
          <button class="btn-outline" @click="onReject">驳回方案</button>
          <button class="btn-primary" @click="onConfirm">确认方案</button>
        </template>
        <!-- 可取消 -->
        <template v-else-if="[1, 2, 4, 5].includes(session.sessionStatus)">
          <button class="btn-danger-outline" @click="onCancel">取消服务</button>
        </template>
        <!-- 已完成待评价 -->
        <template v-else-if="session.sessionStatus === 6 && !evaluated">
          <button class="btn-primary" @click="showEval = true">去评价</button>
        </template>
        <!-- 已取消 -->
        <template v-else-if="session.sessionStatus === 7">
          <text class="closed-tip">服务已取消</text>
        </template>
      </view>
    </template>

    <!-- 评价弹窗 -->
    <view v-if="showEval" class="modal-mask" @click="showEval = false">
      <view class="eval-modal" @click.stop>
        <text class="eval-title">服务评价</text>
        <view class="eval-dim" v-for="dim in evalDims" :key="dim.key">
          <text class="dim-label">{{ dim.label }}</text>
          <view class="stars">
            <text
              v-for="n in 5"
              :key="n"
              class="star"
              :class="{ active: (evalForm[dim.key] || 0) >= n }"
              @click="evalForm[dim.key] = n"
            >★</text>
          </view>
        </view>
        <textarea
          v-model="evalForm.content"
          class="eval-textarea"
          placeholder="说说您的服务体验（选填）"
          placeholder-class="ph"
          :maxlength="500"
        />
        <view class="eval-actions">
          <button class="btn-cancel" @click="showEval = false">取消</button>
          <button class="btn-primary" :disabled="submittingEval" @click="submitEval">
            {{ submittingEval ? '提交中...' : '提交评价' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import {
  getServiceDetail,
  getTimeline,
  confirmSolution,
  rejectSolution,
  cancelSession,
  evaluateSession,
} from '@/api/service';
import type { ServiceSession, Timeline, TimelineNode } from '@/types';

const sessionCode = ref('');
const session = ref<ServiceSession | null>(null);
const timeline = ref<Timeline | null>(null);
const loading = ref(true);
const evaluated = ref(false);

const showEval = ref(false);
const submittingEval = ref(false);
const evalForm = reactive({
  attitudeRating: 0,
  professionalRating: 0,
  responsivenessRating: 0,
  satisfactionRating: 0,
  content: '',
});

const evalDims = [
  { key: 'attitudeRating', label: '服务态度' },
  { key: 'professionalRating', label: '专业水平' },
  { key: 'responsivenessRating', label: '响应速度' },
  { key: 'satisfactionRating', label: '整体满意' },
] as const;

const STATUS_TEXT: Record<number, string> = {
  1: '待分配', 2: '待收集', 3: '方案中', 4: '安排中', 5: '服务中', 6: '已完成', 7: '已取消',
};
const STATUS_HINT: Record<number, string> = {
  1: '您的服务请求已提交，正在为您分配专属管家',
  2: '管家正在收集您的服务需求信息',
  3: '管家已制定服务方案，请查看并确认',
  4: '方案已确认，正在为您安排服务',
  5: '服务进行中，如有问题请联系管家',
  6: '服务已完成，期待您的评价',
  7: '服务已取消',
};
const STATUS_CLASS: Record<number, string> = {
  1: 'hero-pending', 2: 'hero-pending', 3: 'hero-solution',
  4: 'hero-arrange', 5: 'hero-active', 6: 'hero-done', 7: 'hero-cancel',
};

const statusText = computed(() => STATUS_TEXT[session.value?.sessionStatus ?? 0] || '未知');
const statusHint = computed(() => STATUS_HINT[session.value?.sessionStatus ?? 0] || '');
const statusClass = computed(() => STATUS_CLASS[session.value?.sessionStatus ?? 0] || 'hero-pending');

const showActions = computed(() => {
  const s = session.value?.sessionStatus;
  if (s == null) return false;
  if (s === 3) return true;
  if ([1, 2, 4, 5].includes(s)) return true;
  if (s === 6 && !evaluated.value) return true;
  if (s === 7) return true;
  return false;
});

/** 合并时间线四类节点，按时间倒序 */
const timelineNodes = computed<TimelineNode[]>(() => {
  const t = timeline.value;
  if (!t) return [];
  const all = [...(t.demands || []), ...(t.solutions || []), ...(t.arranges || []), ...(t.visits || [])];
  return all
    .filter((n) => n && n.title)
    .sort((a, b) => {
      const ta = a.time ? new Date(a.time).getTime() : 0;
      const tb = b.time ? new Date(b.time).getTime() : 0;
      return tb - ta;
    });
});

function dotIcon(type: string): string {
  const m: Record<string, string> = { demand: '需', solution: '方', arrange: '排', visit: '访' };
  return m[type] || '●';
}

function formatTime(t?: number | string): string {
  if (!t) return '';
  const d = new Date(t);
  if (isNaN(d.getTime())) return String(t);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

async function loadAll() {
  loading.value = true;
  try {
    const [detail, tl] = await Promise.all([
      getServiceDetail(sessionCode.value),
      getTimeline(sessionCode.value).catch(() => null),
    ]);
    session.value = detail;
    timeline.value = tl;
    // 检查是否已评价（通过 remark 或 timeline 中评价节点判断较为间接；这里默认未评价让用户看到按钮）
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false;
  }
}

/** 操作后重新加载本页（redirectTo 自身 = 销毁重建，状态全新拉取） */
function reload() {
  uni.redirectTo({ url: `/pages/service/detail?sessionCode=${sessionCode.value}` });
}

async function onConfirm() {
  uni.showModal({
    title: '确认方案',
    content: '确定接受当前服务方案吗？',
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await confirmSolution(sessionCode.value);
        uni.showToast({ title: '方案已确认', icon: 'success' });
        setTimeout(reload, 800);
      } catch (e) { /* 拦截器已提示 */ }
    },
  });
}

async function onReject() {
  uni.showModal({
    title: '驳回方案',
    content: '确定驳回当前方案吗？管家将重新制定。',
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await rejectSolution(sessionCode.value);
        uni.showToast({ title: '方案已驳回', icon: 'success' });
        setTimeout(reload, 800);
      } catch (e) { /* 拦截器已提示 */ }
    },
  });
}

async function onCancel() {
  uni.showModal({
    title: '取消服务',
    content: '确定取消此服务吗？取消后无法恢复。',
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await cancelSession(sessionCode.value, '客户主动取消');
        uni.showToast({ title: '服务已取消', icon: 'success' });
        setTimeout(reload, 800);
      } catch (e) { /* 拦截器已提示 */ }
    },
  });
}

async function submitEval() {
  if (!evalForm.attitudeRating || !evalForm.professionalRating || !evalForm.responsivenessRating || !evalForm.satisfactionRating) {
    uni.showToast({ title: '请为每项打分', icon: 'none' });
    return;
  }
  submittingEval.value = true;
  try {
    await evaluateSession(sessionCode.value, { ...evalForm });
    uni.showToast({ title: '评价已提交', icon: 'success' });
    showEval.value = false;
    evaluated.value = true;
    setTimeout(reload, 800);
  } catch (e) { /* 拦截器已提示 */ } finally {
    submittingEval.value = false;
  }
}

onLoad((q) => {
  sessionCode.value = q?.sessionCode || '';
  loadAll();
});
onShow(() => {
  // 从其他操作返回时刷新（仅首次不重复加载）
  if (sessionCode.value && session.value) loadAll();
});
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 140rpx;
}

.loading {
  display: flex;
  justify-content: center;
  padding: 120rpx 0;
  color: #909399;
}

/* 状态条 */
.status-hero {
  padding: 40rpx 30rpx;
}
.hero-pending { background: linear-gradient(135deg, #e6a23c, #cf8a2c); }
.hero-solution { background: linear-gradient(135deg, #409eff, #337ecc); }
.hero-arrange { background: linear-gradient(135deg, #67C23A, #4eaf2a); }
.hero-active { background: linear-gradient(135deg, #67C23A, #4eaf2a); }
.hero-done { background: linear-gradient(135deg, #909399, #76797e); }
.hero-cancel { background: linear-gradient(135deg, #909399, #76797e); }
.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.status-badge {
  color: #fff;
  font-size: 34rpx;
  font-weight: bold;
  background: rgba(255, 255, 255, 0.25);
  padding: 6rpx 24rpx;
  border-radius: 8rpx;
}
.butler {
  color: rgba(255, 255, 255, 0.9);
  font-size: 24rpx;
}
.status-hint {
  color: rgba(255, 255, 255, 0.9);
  font-size: 26rpx;
  margin-top: 16rpx;
  display: block;
}
.session-title {
  color: #fff;
  font-size: 30rpx;
  font-weight: 500;
  margin-top: 8rpx;
  display: block;
}

/* 信息卡 */
.info-card {
  background: #fff;
  margin: 20rpx 24rpx 0;
  border-radius: 16rpx;
  padding: 24rpx;
}
.info-row {
  display: flex;
  padding: 12rpx 0;
  border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.info-label {
  font-size: 26rpx;
  color: #909399;
  width: 140rpx;
  flex-shrink: 0;
}
.info-val {
  font-size: 26rpx;
  color: #303133;
  flex: 1;
  &.desc { line-height: 1.6; }
}

/* 时间线 */
.section-header {
  padding: 30rpx 24rpx 16rpx;
}
.sh-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}
.empty-timeline {
  margin: 0 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 60rpx 0;
  text-align: center;
  color: #c0c4cc;
  font-size: 26rpx;
}
.timeline {
  background: #fff;
  margin: 0 24rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}
.tl-node {
  display: flex;
}
.tl-line-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 20rpx;
}
.tl-dot {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: #fff;
  flex-shrink: 0;
}
.tl-dot.demand { background: #e6a23c; }
.tl-dot.solution { background: #409eff; }
.tl-dot.arrange { background: #67C23A; }
.tl-dot.visit { background: #909399; }
.tl-line {
  width: 2rpx;
  flex: 1;
  min-height: 40rpx;
  background: #ebeef5;
  margin: 4rpx 0;
}
.tl-content {
  flex: 1;
  padding-bottom: 30rpx;
}
.tl-title {
  font-size: 28rpx;
  color: #303133;
  font-weight: 500;
  display: block;
}
.tl-desc {
  font-size: 24rpx;
  color: #606266;
  margin-top: 6rpx;
  display: block;
  line-height: 1.5;
}
.tl-time {
  font-size: 22rpx;
  color: #c0c4cc;
  margin-top: 6rpx;
  display: block;
}

/* 操作区 */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 20rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
}
.closed-tip {
  flex: 1;
  text-align: center;
  font-size: 26rpx;
  color: #909399;
  line-height: 80rpx;
}
.btn-primary {
  flex: 1;
  background: #67C23A;
  color: #fff;
  font-size: 30rpx;
  border-radius: 12rpx;
  height: 84rpx;
  line-height: 84rpx;
  margin: 0;
  &[disabled] { background: #a4da89; }
}
.btn-outline {
  flex: 1;
  background: #fff;
  color: #67C23A;
  border: 2rpx solid #67C23A;
  font-size: 30rpx;
  border-radius: 12rpx;
  height: 84rpx;
  line-height: 80rpx;
  margin: 0;
}
.btn-danger-outline {
  flex: 1;
  background: #fff;
  color: #f56c6c;
  border: 2rpx solid #f56c6c;
  font-size: 30rpx;
  border-radius: 12rpx;
  height: 84rpx;
  line-height: 80rpx;
  margin: 0;
}

/* 评价弹窗 */
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.eval-modal {
  background: #fff;
  border-radius: 20rpx;
  width: 90%;
  padding: 40rpx 30rpx;
}
.eval-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #303133;
  text-align: center;
  display: block;
  margin-bottom: 30rpx;
}
.eval-dim {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1px solid #f5f5f5;
}
.dim-label {
  font-size: 28rpx;
  color: #303133;
}
.stars {
  display: flex;
  gap: 8rpx;
}
.star {
  font-size: 40rpx;
  color: #dcdfe6;
  &.active {
    color: #ff9900;
  }
}
.eval-textarea {
  width: 100%;
  min-height: 140rpx;
  background: #f5f6f8;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 26rpx;
  color: #303133;
  margin-top: 20rpx;
  box-sizing: border-box;
}
.ph { color: #c0c4cc; }
.eval-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 30rpx;
}
.btn-cancel {
  flex: 1;
  background: #f5f6f8;
  color: #606266;
  font-size: 30rpx;
  border-radius: 12rpx;
  height: 84rpx;
  line-height: 84rpx;
  margin: 0;
}
</style>
