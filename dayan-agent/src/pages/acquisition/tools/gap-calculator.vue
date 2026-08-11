<template>
  <view class="page dy-safe-bottom">
    <!-- 表单区 -->
    <view class="form-card">
      <text class="card-title">基本信息</text>

      <view class="form-row">
        <text class="form-label">当前年龄</text>
        <view class="form-input-wrap">
          <input v-model.number="form.age" class="form-input" type="number" placeholder="如 35" />
          <text class="form-unit">岁</text>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">退休年龄</text>
        <view class="form-input-wrap">
          <input v-model.number="form.retireAge" class="form-input" type="number" placeholder="如 60" />
          <text class="form-unit">岁</text>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">预期寿命</text>
        <view class="form-input-wrap">
          <input v-model.number="form.lifeExpectancy" class="form-input" type="number" placeholder="如 85" />
          <text class="form-unit">岁</text>
        </view>
      </view>
    </view>

    <!-- 收入侧 -->
    <view class="form-card">
      <text class="card-title">退休收入预估</text>

      <view class="form-row">
        <text class="form-label">预估月养老金</text>
        <view class="form-input-wrap">
          <input v-model.number="form.pensionIncome" class="form-input" type="digit" placeholder="可用养老金计算器算出" />
          <text class="form-unit">元/月</text>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">现有养老储备</text>
        <view class="form-input-wrap">
          <input v-model.number="form.savings" class="form-input" type="digit" placeholder="存款、理财、投资等" />
          <text class="form-unit">元</text>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">每月定投金额</text>
        <view class="form-input-wrap">
          <input v-model.number="form.monthlySave" class="form-input" type="digit" placeholder="退休前每月存入" />
          <text class="form-unit">元</text>
        </view>
      </view>
      <view class="form-row form-row-block">
        <view class="form-label-row">
          <text class="form-label">预期年化收益率</text>
          <text class="form-value-tag">{{ form.returnRate.toFixed(1) }}%</text>
        </view>
        <slider
          :value="form.returnRate"
          :min="0"
          :max="10"
          :step="0.5"
          active-color="#409eff"
          block-color="#409eff"
          @change="onRateChange"
        />
        <view class="slider-hints">
          <text class="slider-hint">保守 2%</text>
          <text class="slider-hint">均衡 5%</text>
          <text class="slider-hint">进取 8%+</text>
        </view>
      </view>
    </view>

    <!-- 支出侧 -->
    <view class="form-card">
      <text class="card-title">退休生活期望</text>

      <view class="form-row">
        <text class="form-label">期望月支出</text>
        <view class="form-input-wrap">
          <input v-model.number="form.monthlyExpense" class="form-input" type="digit" placeholder="含食宿、医疗、护理等" />
          <text class="form-unit">元/月</text>
        </view>
      </view>
    </view>

    <!-- 结果区 -->
    <view v-if="canCalc" class="result-card">
      <!-- 主结果：缺口 -->
      <view class="result-main" :class="hasGap ? 'result-gap' : 'result-surplus'">
        <text class="result-label">{{ hasGap ? '养老资金缺口' : '养老资金盈余' }}</text>
        <view class="result-amount-row">
          <text class="result-currency">¥</text>
          <text class="result-amount">{{ formatNum(Math.abs(result.totalGap)) }}</text>
        </view>
        <text class="result-subtitle">
          每月{{ hasGap ? '缺口约' : '盈余约' }} ¥{{ formatNum(Math.abs(result.monthlyGap)) }}
        </text>
      </view>

      <view class="result-divider" />

      <!-- 天平对比 -->
      <view class="balance-section">
        <text class="balance-title">资金天平</text>

        <!-- 需求侧 -->
        <view class="balance-item">
          <view class="balance-header">
            <text class="balance-icon">需求</text>
            <text class="balance-amount">¥{{ formatNum(result.totalNeed) }}</text>
          </view>
          <view class="bar-track">
            <view class="bar-fill bar-need" :style="{ width: needBarWidth }" />
          </view>
          <text class="balance-hint">
            {{ form.monthlyExpense }}元/月 × {{ result.retireYears }}年退休期
          </text>
        </view>

        <!-- 供给侧 -->
        <view class="balance-item">
          <view class="balance-header">
            <text class="balance-icon">供给</text>
            <text class="balance-amount">¥{{ formatNum(result.totalSupply) }}</text>
          </view>
          <view class="bar-track">
            <view class="bar-fill bar-supply" :style="{ width: supplyBarWidth }" />
          </view>
          <text class="balance-hint">养老金 + 储备金增值 + 定投增值</text>
        </view>
      </view>

      <view class="result-divider" />

      <!-- 供给明细 -->
      <view class="breakdown">
        <view class="breakdown-item">
          <text class="breakdown-label">养老金总收入</text>
          <text class="breakdown-value">¥{{ formatNum(result.pensionTotal) }}</text>
        </view>
        <view class="breakdown-item">
          <text class="breakdown-label">储备金增值（{{ result.yearsToRetire }}年）</text>
          <text class="breakdown-value">¥{{ formatNum(result.savingsFV) }}</text>
        </view>
        <view class="breakdown-item">
          <text class="breakdown-label">每月定投增值</text>
          <text class="breakdown-value">¥{{ formatNum(result.monthlySaveFV) }}</text>
        </view>
      </view>

      <!-- 建议 -->
      <view v-if="hasGap" class="advice-box">
        <text class="advice-title">💡 规划建议</text>
        <text class="advice-text">{{ advice }}</text>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-hint">
      <text class="empty-text">请完善上方信息查看资金缺口分析</text>
    </view>

    <view class="disclaimer">
      <text class="disclaimer-text">
        ※ 以上为简化估算，实际支出受通胀、医疗费用、政策变化等影响，仅供参考。
      </text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, computed } from 'vue';

// ===== 表单 =====
const form = reactive({
  age: 35,
  retireAge: 60,
  lifeExpectancy: 85,
  pensionIncome: 3000,
  savings: 100000,
  monthlySave: 2000,
  monthlyExpense: 6000,
  returnRate: 4.0,
});

function onRateChange(e: any) {
  form.returnRate = e.detail.value;
}

// ===== 计算逻辑 =====
const canCalc = computed(() => {
  return form.age > 0
    && form.retireAge > form.age
    && form.lifeExpectancy > form.retireAge
    && form.monthlyExpense > 0;
});

const result = computed(() => {
  const yearsToRetire = form.retireAge - form.age;
  const retireYears = form.lifeExpectancy - form.retireAge;
  const annualRate = form.returnRate / 100;

  // 需求侧
  const totalNeed = form.monthlyExpense * 12 * retireYears;

  // 供给侧
  const pensionTotal = (form.pensionIncome || 0) * 12 * retireYears;

  // 储备金终值 = 现值 × (1+r)^n
  const savingsFV = form.savings > 0 && annualRate > 0
    ? form.savings * Math.pow(1 + annualRate, yearsToRetire)
    : form.savings;

  // 每月定投终值（年金终值）
  // FV = PMT × [((1+r_month)^n_months - 1) / r_month]
  let monthlySaveFV = 0;
  if (form.monthlySave > 0) {
    const months = yearsToRetire * 12;
    if (annualRate > 0) {
      const rMonth = annualRate / 12;
      monthlySaveFV = form.monthlySave * ((Math.pow(1 + rMonth, months) - 1) / rMonth);
    } else {
      monthlySaveFV = form.monthlySave * months;
    }
  }

  const totalSupply = pensionTotal + savingsFV + monthlySaveFV;
  const totalGap = totalNeed - totalSupply;
  const monthlyGap = totalGap / (retireYears * 12);

  return {
    yearsToRetire,
    retireYears,
    totalNeed: Math.round(totalNeed),
    pensionTotal: Math.round(pensionTotal),
    savingsFV: Math.round(savingsFV),
    monthlySaveFV: Math.round(monthlySaveFV),
    totalSupply: Math.round(totalSupply),
    totalGap: Math.round(totalGap),
    monthlyGap: Math.round(monthlyGap),
  };
});

const hasGap = computed(() => result.value.totalGap > 0);

// 天平柱状图宽度
const needBarWidth = computed(() => {
  const { totalNeed, totalSupply } = result.value;
  const max = Math.max(totalNeed, totalSupply, 1);
  return Math.min((totalNeed / max) * 100, 100) + '%';
});
const supplyBarWidth = computed(() => {
  const { totalNeed, totalSupply } = result.value;
  const max = Math.max(totalNeed, totalSupply, 1);
  return Math.min((totalSupply / max) * 100, 100) + '%';
});

// 建议
const advice = computed(() => {
  const r = result.value;
  if (r.monthlyGap <= 0) return '当前规划充足，可考虑配置重疾险和医疗险提升保障。';

  // 需要每月多存多少钱（用定投来填补缺口）
  const yearsToRetire = r.yearsToRetire;
  const annualRate = form.returnRate / 100;
  const rMonth = annualRate > 0 ? annualRate / 12 : 0;
  const months = yearsToRetire * 12;
  let extraMonthly = 0;
  if (rMonth > 0) {
    const annuityFactor = (Math.pow(1 + rMonth, months) - 1) / rMonth;
    extraMonthly = annuityFactor > 0 ? r.totalGap / annuityFactor : r.totalGap / months;
  } else {
    extraMonthly = r.totalGap / months;
  }

  return `距退休还有 ${yearsToRetire} 年，建议每月额外储备约 ¥${formatNum(Math.round(extraMonthly))} 元（按 ${form.returnRate}% 年化），或通过商业养老年金险弥补缺口。`;
});

// ===== 工具函数 =====
function formatNum(n: number): string {
  if (!n || isNaN(n)) return '0';
  return Math.abs(n).toLocaleString('zh-CN');
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

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
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
}
.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: $brand-primary;
  margin-bottom: $spacing-sm;
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
  border-bottom: none;
}
.form-label {
  font-size: 28rpx;
  color: $text-regular;
  flex-shrink: 0;
}
.form-input-wrap {
  display: flex;
  align-items: center;
}
.form-input {
  text-align: right;
  font-size: 30rpx;
  color: $text-primary;
  font-weight: 500;
  min-width: 220rpx;
  height: 60rpx;
}
.form-unit {
  margin-left: $spacing-xs;
  font-size: 24rpx;
  color: $text-secondary;
}
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
  padding: $spacing-lg $spacing-md;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.result-gap {
  background: linear-gradient(135deg, #fa3534, #ff6b6b);
}
.result-surplus {
  background: $gradient-green;
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
  font-size: 64rpx;
  color: #fff;
  font-weight: bold;
  line-height: 1.1;
}
.result-subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.result-divider {
  height: 1rpx;
  background: $border-light;
  margin: 0 $spacing-md;
}

/* 天平 */
.balance-section {
  padding: $spacing-md;
}
.balance-title {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
  display: block;
  margin-bottom: $spacing-md;
}
.balance-item {
  margin-bottom: $spacing-md;
}
.balance-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-xs;
}
.balance-icon {
  font-size: 26rpx;
  font-weight: 500;
  color: $text-regular;
  padding: 4rpx 20rpx;
  border-radius: $radius-sm;
  background: $brand-info-light;
}
.balance-amount {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}
.bar-track {
  height: 20rpx;
  background: $brand-info-light;
  border-radius: 10rpx;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 10rpx;
  transition: width 0.5s ease;
}
.bar-need {
  background: $gradient-red;
}
.bar-supply {
  background: $gradient-blue;
}
.balance-hint {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $text-placeholder;
}

/* 明细 */
.breakdown {
  padding: $spacing-md;
}
.breakdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-xs 0;
}
.breakdown-label {
  font-size: 26rpx;
  color: $text-secondary;
}
.breakdown-value {
  font-size: 28rpx;
  font-weight: 500;
  color: $text-primary;
}

/* 建议 */
.advice-box {
  margin: 0 $spacing-md $spacing-md;
  padding: $spacing-md;
  background: $brand-warning-light;
  border-radius: $radius-md;
  border-left: 6rpx solid $brand-warning;
}
.advice-title {
  font-size: 28rpx;
  font-weight: bold;
  color: $brand-warning;
  display: block;
  margin-bottom: 8rpx;
}
.advice-text {
  font-size: 26rpx;
  color: $text-regular;
  line-height: 1.6;
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
</style>
