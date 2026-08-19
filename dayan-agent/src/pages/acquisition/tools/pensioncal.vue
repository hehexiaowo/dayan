<template>
  <view class="page dy-safe-bottom">
    <!-- 表单区 -->
    <view class="form-card">
      <text class="card-title">输入信息</text>

      <!-- 性别 -->
      <view class="form-row">
        <text class="form-label">性别</text>
        <view class="gender-btns">
          <view
            class="gender-btn dy-clickable"
            :class="{ active: form.gender === 'male' }"
            @click="form.gender = 'male'"
          >
            <text>男</text>
          </view>
          <view
            class="gender-btn dy-clickable"
            :class="{ active: form.gender === 'female' }"
            @click="form.gender = 'female'"
          >
            <text>女</text>
          </view>
        </view>
      </view>

      <!-- 当前年龄 -->
      <view class="form-row">
        <text class="form-label">当前年龄</text>
        <view class="form-input-wrap">
          <input
            v-model.number="form.age"
            class="form-input"
            type="number"
            placeholder="如 35"
          />
          <text class="form-unit">岁</text>
        </view>
      </view>

      <!-- 退休年龄 -->
      <view class="form-row">
        <text class="form-label">退休年龄</text>
        <view class="form-input-wrap">
          <input
            v-model.number="form.retireAge"
            class="form-input"
            type="number"
            placeholder="男60 / 女55"
          />
          <text class="form-unit">岁</text>
        </view>
      </view>

      <!-- 当前月工资 -->
      <view class="form-row">
        <text class="form-label">当前月税前工资</text>
        <view class="form-input-wrap">
          <input
            v-model.number="form.salary"
            class="form-input"
            type="digit"
            placeholder="如 8000"
          />
          <text class="form-unit">元</text>
        </view>
      </view>

      <!-- 已缴年限 -->
      <view class="form-row">
        <text class="form-label">已缴费年限</text>
        <view class="form-input-wrap">
          <input
            v-model.number="form.contributedYears"
            class="form-input"
            type="number"
            placeholder="如 10"
          />
          <text class="form-unit">年</text>
        </view>
      </view>

      <!-- 缴费指数 -->
      <view class="form-row form-row-block">
        <view class="form-label-row">
          <text class="form-label">缴费指数</text>
          <text class="form-value-tag">{{ form.index.toFixed(1) }}</text>
        </view>
        <slider
          :value="form.index"
          :min="0.6"
          :max="3.0"
          :step="0.1"
          active-color="#409eff"
          block-color="#409eff"
          @change="onIndexChange"
        />
        <view class="slider-hints">
          <text class="slider-hint">最低 0.6</text>
          <text class="slider-hint">社会平均 1.0</text>
          <text class="slider-hint">最高 3.0</text>
        </view>
      </view>
    </view>

    <!-- 结果区 -->
    <view v-if="canCalc" class="result-card">
      <view class="result-main">
        <text class="result-label">预计退休后每月可领</text>
        <view class="result-amount-row">
          <text class="result-currency">¥</text>
          <text class="result-amount">{{ formatNum(result.monthlyPension) }}</text>
        </view>
        <text class="result-subtitle">基础养老金 + 个人账户养老金</text>
      </view>

      <view class="result-divider" />

      <!-- 明细 -->
      <view class="breakdown">
        <view class="breakdown-item">
          <view class="breakdown-left">
            <text class="breakdown-label">基础养老金</text>
            <text class="breakdown-hint">按社平 {{ formatNum(SOCIAL_AVG_SALARY) }} 元估算</text>
          </view>
          <view class="breakdown-right">
            <text class="breakdown-value">¥{{ formatNum(result.basicPension) }}</text>
          </view>
        </view>
        <view class="breakdown-item">
          <view class="breakdown-left">
            <text class="breakdown-label">个人账户养老金</text>
            <text class="breakdown-hint">账户约 ¥{{ formatNum(result.accountTotal) }} ÷ {{ result.months }} 月</text>
          </view>
          <view class="breakdown-right">
            <text class="breakdown-value">¥{{ formatNum(result.accountPension) }}</text>
          </view>
        </view>
        <view class="breakdown-item">
          <view class="breakdown-left">
            <text class="breakdown-label">总缴费年限</text>
          </view>
          <view class="breakdown-right">
            <text class="breakdown-value-sm">{{ result.totalYears }} 年</text>
          </view>
        </view>
      </view>

      <view class="result-divider" />

      <!-- 替代率 -->
      <view class="replacement-bar">
        <view class="replacement-header">
          <text class="replacement-label">养老金替代率</text>
          <text class="replacement-value">{{ result.replacementRate }}%</text>
        </view>
        <view class="bar-track">
          <view
            class="bar-fill"
            :class="replacementLevel.cls"
            :style="{ width: Math.min(result.replacementRate, 100) + '%' }"
          />
        </view>
        <text class="replacement-hint">{{ replacementLevel.text }}</text>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-hint">
      <text class="empty-text">请填写左侧信息查看计算结果</text>
    </view>

    <!-- 底部提示 -->
    <view class="disclaimer">
      <text class="disclaimer-text">
        ※ 以上为简化估算，实际金额受当地社平工资、缴费基数、政策调整等因素影响，仅供参考。
      </text>
    </view>

    <!-- 分享模式：代理人名片 -->
    <view v-if="isShareMode && agentCard" class="agent-card">
      <view class="agent-card-header">
        <view v-if="agentCard.avatar" class="agent-avatar">
          <image :src="formatFileUrl(agentCard.avatar)" mode="aspectFill" class="avatar-img" />
        </view>
        <view v-else class="agent-avatar">
          <text class="agent-avatar-text">{{ (agentCard.displayName || '顾问').charAt(0) }}</text>
        </view>
        <view class="agent-info">
          <text class="agent-name">{{ agentCard.displayName || '养老顾问' }}</text>
          <text v-if="agentCard.title" class="agent-job">{{ agentCard.title }}</text>
        </view>
      </view>
      <view class="agent-contact">
        <view v-if="agentCard.phone" class="contact-btn contact-phone dy-clickable" @click="onCall(agentCard.phone)">
          <text>📞 联系顾问</text>
        </view>
        <view v-if="agentCard.wechat" class="contact-btn contact-wechat dy-clickable" @click="onCopyWechat(agentCard.wechat)">
          <text>💬 复制微信</text>
        </view>
      </view>
    </view>

    <!-- 正常模式：分享按钮 -->
    <view v-if="!isShareMode" class="share-btn-bar">
      <view class="share-action-btn dy-clickable" @click="onShareTool">
        <text>↗ 分享给客户</text>
      </view>
    </view>

    <!-- 分享模式：留资卡片 -->
    <DyContactForm v-if="isShareMode" />

    <!-- 品牌页脚 -->
    <view v-if="isShareMode" class="brand-footer">
      <text>养老宝典 · 专业养老服务平台</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref, computed, watch } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getShareAgentCard, trackShare } from '@/api/share';
import { savePensionCalculatorRecord } from '@/api/tool';
import { formatFileUrl } from '@/utils/file';
import DyContactForm from '@/components/DyContactForm/DyContactForm.vue';

// ===== 分享模式 =====
const isShareMode = ref(false);
const agentCard = ref<import('@/api/share').ShareCard | null>(null);

// ===== 常量 =====
/** 社会平均工资（全国近似值） */
const SOCIAL_AVG_SALARY = 8000;
/** 计发月数映射 */
const PENSION_MONTHS: Record<number, number> = {
  60: 139,
  55: 170,
  50: 195,
};

// ===== 表单 =====
const form = reactive({
  gender: 'male' as 'male' | 'female',
  age: 35,
  retireAge: 60,
  salary: 8000,
  contributedYears: 10,
  index: 1.0,
});

function onIndexChange(e: { detail: { value: number } }) {
  form.index = e.detail.value;
}

// ===== 计算逻辑 =====
const canCalc = computed(() => {
  return form.age > 0 && form.retireAge > form.age && form.salary > 0 && form.contributedYears >= 0;
});

const result = computed(() => {
  const retireAge = form.retireAge;
  const futureYears = retireAge - form.age;
  const totalYears = form.contributedYears + futureYears;

  // 基础养老金 = (社平 + 社平×指数) / 2 × 缴费年限 × 1%
  const indexedSalary = SOCIAL_AVG_SALARY * form.index;
  const basicPension = ((SOCIAL_AVG_SALARY + indexedSalary) / 2) * totalYears * 0.01;

  // 个人账户储存额 ≈ 月工资 × 8% × 12 × 缴费年限（简化，忽略利息）
  const accountTotal = form.salary * 0.08 * 12 * totalYears;
  const months = PENSION_MONTHS[retireAge] || 139;
  const accountPension = accountTotal / months;

  const monthlyPension = basicPension + accountPension;
  const replacementRate = form.salary > 0
    ? Math.round((monthlyPension / form.salary) * 100)
    : 0;

  return {
    basicPension: Math.round(basicPension),
    accountPension: Math.round(accountPension),
    accountTotal: Math.round(accountTotal),
    monthlyPension: Math.round(monthlyPension),
    months,
    totalYears,
    replacementRate,
  };
});

const replacementLevel = computed(() => {
  const r = result.value.replacementRate;
  if (r >= 70) return { cls: 'bar-good', text: '替代率较高，退休后可维持较好的生活水平' };
  if (r >= 50) return { cls: 'bar-ok', text: '替代率适中，建议适当补充商业养老保障' };
  return { cls: 'bar-low', text: '替代率偏低，需要额外规划养老收入来源' };
});

// ===== 使用记录：计算结果稳定后保存（指纹去重，避免输入过程重复记录） =====
let lastSavedFingerprint = '';
watch(
  result,
  (r) => {
    if (!canCalc.value) return;
    const fingerprint = JSON.stringify({ input: form, result: r });
    if (fingerprint === lastSavedFingerprint) return;
    lastSavedFingerprint = fingerprint;
    savePensionCalculatorRecord({ ...form }, r).catch(() => { /* 记录失败不阻塞使用 */ });
  },
  { flush: 'post' },
);

// ===== 工具函数 =====
function formatNum(n: number): string {
  if (!n || isNaN(n)) return '0';
  return n.toLocaleString('zh-CN');
}

// ===== 分享相关 =====
function onShareTool() {
  // #ifdef H5
  const agentUser = uni.getStorageSync('agent_user') as { accountCode?: string } | undefined;
  const agentCode = agentUser?.accountCode || '';
  const url = `${window.location.origin}/#/pages/acquisition/tools/pensioncal?agent=${agentCode}`;
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: '链接已复制，可粘贴发给客户', icon: 'none', duration: 2500 }),
  });
  // #endif
}

function onCall(phone: string) {
  uni.makePhoneCall({ phoneNumber: phone });
}

function onCopyWechat(wechat: string) {
  uni.setClipboardData({ data: wechat, success: () => uni.showToast({ title: '微信号已复制', icon: 'none' }) });
}

onLoad((opts) => {
  const agent = opts?.agent;
  if (agent) {
    isShareMode.value = true;
    getShareAgentCard(agent).then((card) => { agentCard.value = card; });
    // 追踪访客打开
    trackShare({
      agentCode: agent,
      shareType: 2,
      bizCode: 'pensioncal',
      bizTitle: '社保养老计算器',
    });
  }
});
</script>

<style lang="scss" scoped>

.page {
  min-height: 100vh;
  background: $bg-page;
  padding: $spacing-md;
  padding-bottom: 60rpx;
}

/* ===== 表单卡片 ===== */
.form-card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg $spacing-md;
  box-shadow: $shadow-card;
}
.card-title {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
  margin-bottom: $spacing-md;
  display: block;
}

/* 表单行 */
.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-sm 0;
  border-bottom: 1rpx solid $border-light;
  min-height: $control-height;
}
.form-row-block {
  flex-direction: column;
  align-items: stretch;
}
.form-label {
  font-size: 28rpx;
  color: $text-regular;
  flex-shrink: 0;
}

/* 性别按钮 */
.gender-btns {
  display: flex;
  gap: $spacing-sm;
}
.gender-btn {
  padding: 8rpx 40rpx;
  border-radius: $radius-sm;
  background: $brand-info-light;
  font-size: 28rpx;
  color: $text-secondary;
  transition: all $transition-base;
}
.gender-btn.active {
  background: $brand-primary;
  color: #fff;
  font-weight: 500;
}

/* 输入框 */
.form-input-wrap {
  display: flex;
  align-items: center;
}
.form-input {
  text-align: right;
  font-size: 30rpx;
  color: $text-primary;
  font-weight: 500;
  min-width: 200rpx;
  height: 60rpx;
}
.form-unit {
  margin-left: $spacing-xs;
  font-size: 26rpx;
  color: $text-secondary;
}

/* Slider 区 */
.form-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-sm;
}
.form-value-tag {
  font-size: 28rpx;
  font-weight: bold;
  color: $brand-primary;
  background: $brand-primary-light;
  padding: 4rpx 20rpx;
  border-radius: $radius-sm;
}
slider {
  width: 100%;
  margin: 0;
}
.slider-hints {
  display: flex;
  justify-content: space-between;
  margin-top: 8rpx;
}
.slider-hint {
  font-size: 22rpx;
  color: $text-placeholder;
}

/* ===== 结果卡片 ===== */
.result-card {
  background: $bg-card;
  border-radius: $radius-lg;
  margin-top: $spacing-md;
  box-shadow: $shadow-card;
  overflow: hidden;
}
.result-main {
  background: $gradient-blue;
  padding: $spacing-lg $spacing-md;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.result-label {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}
.result-amount-row {
  display: flex;
  align-items: baseline;
  margin-top: $spacing-xs;
}
.result-currency {
  font-size: 36rpx;
  color: #fff;
  font-weight: bold;
  margin-right: 4rpx;
}
.result-amount {
  font-size: 72rpx;
  color: #fff;
  font-weight: bold;
  line-height: 1.1;
}
.result-subtitle {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.65);
}

.result-divider {
  height: 1rpx;
  background: $border-light;
  margin: 0 $spacing-md;
}

/* 明细 */
.breakdown {
  padding: $spacing-md;
}
.breakdown-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: $spacing-sm 0;
}
.breakdown-left {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.breakdown-label {
  font-size: 28rpx;
  color: $text-regular;
}
.breakdown-hint {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: $text-placeholder;
}
.breakdown-right {
  margin-left: $spacing-md;
  flex-shrink: 0;
}
.breakdown-value {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}
.breakdown-value-sm {
  font-size: 28rpx;
  font-weight: 500;
  color: $text-regular;
}

/* 替代率 */
.replacement-bar {
  padding: $spacing-md;
}
.replacement-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: $spacing-sm;
}
.replacement-label {
  font-size: 28rpx;
  color: $text-regular;
}
.replacement-value {
  font-size: 36rpx;
  font-weight: bold;
  color: $brand-primary;
}
.bar-track {
  height: 16rpx;
  background: $brand-info-light;
  border-radius: 8rpx;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 8rpx;
  transition: width 0.5s ease;
}
.bar-good {
  background: $gradient-green;
}
.bar-ok {
  background: $gradient-orange;
}
.bar-low {
  background: $gradient-red;
}
.replacement-hint {
  margin-top: $spacing-sm;
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.5;
}

/* 空状态 */
.empty-hint {
  background: $bg-card;
  border-radius: $radius-lg;
  margin-top: $spacing-md;
  padding: $spacing-xl $spacing-md;
  display: flex;
  justify-content: center;
  box-shadow: $shadow-card;
}
.empty-text {
  font-size: 28rpx;
  color: $text-placeholder;
}

/* 免责 */
.disclaimer {
  padding: $spacing-md $spacing-sm;
}
.disclaimer-text {
  font-size: 22rpx;
  color: $text-placeholder;
  line-height: 1.6;
}

/* 分享按钮 */
.share-btn-bar {
  padding: 0 $spacing-sm $spacing-md;
}
.share-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: $control-height;
  background: $gradient-blue;
  color: #fff;
  border-radius: $radius-md;
  font-size: 30rpx;
  font-weight: 500;
  box-shadow: $shadow-fab;
}

/* 代理人名片 */
.agent-card {
  background: $bg-card;
  margin: $spacing-sm $spacing-sm 0;
  border-radius: $radius-lg;
  padding: $spacing-lg $spacing-md;
  box-shadow: $shadow-card;
  border-top: 4rpx solid $brand-primary;
}
.agent-card-header {
  display: flex;
  align-items: center;
}
.agent-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: $gradient-blue;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.avatar-img { width: 100%; height: 100%; }
.agent-avatar-text {
  color: #fff;
  font-size: 36rpx;
  font-weight: bold;
}
.agent-info {
  margin-left: $spacing-md;
  display: flex;
  flex-direction: column;
}
.agent-name {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}
.agent-job {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.agent-contact {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-md;
}
.contact-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: $control-height;
  border-radius: $radius-md;
  font-size: 26rpx;
}
.contact-phone { background: $brand-primary; color: #fff; }
.contact-wechat { background: $brand-success-light; color: $brand-success; }

/* 品牌页脚 */
.brand-footer {
  text-align: center;
  padding: $spacing-md;
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>
