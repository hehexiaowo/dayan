<template>
  <view class="step-progress">
    <view
      v-for="(s, i) in steps"
      :key="s.name"
      class="step"
      :class="{ done: i + 1 < current, active: i + 1 === current }"
    >
      <view class="step-dot">
        <text v-if="i + 1 < current" class="step-check">✓</text>
        <text v-else class="step-num">{{ i + 1 }}</text>
      </view>
      <text class="step-label">{{ s.label }}</text>
      <view v-if="i < steps.length - 1" class="step-line" :class="{ on: i + 1 < current }" />
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * 六阶段流水线步骤条：素材 → 策略 → 大纲 → 正文 → 配图 → 成品。
 * current 为当前步骤序号（1-6）；已过步骤打勾，当前步骤高亮。
 */
withDefaults(
  defineProps<{
    /** 当前步骤序号（1-6） */
    current: number
  }>(),
  { current: 1 },
)

const steps = [
  { name: 'material', label: '素材' },
  { name: 'strategy', label: '策略' },
  { name: 'outline', label: '大纲' },
  { name: 'body', label: '正文' },
  { name: 'image', label: '配图' },
  { name: 'done', label: '成品' },
] as const
</script>

<style scoped lang="scss">
.step-progress {
  display: flex;
  align-items: flex-start;
  padding: 4rpx 0;
}

.step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.step-dot {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: $bg-card;
  border: 3rpx solid $border-base;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  transition: all $transition-fast;
}

.step-num {
  font-size: 22rpx;
  color: $text-placeholder;
}

.step-check {
  font-size: 22rpx;
  color: #fff;
  font-weight: bold;
}

.step-label {
  margin-top: 8rpx;
  font-size: 20rpx;
  color: $text-placeholder;
}

/* 已完成 */
.step.done .step-dot {
  background: $gradient-blue;
  border-color: transparent;
}

.step.done .step-label {
  color: $brand-primary;
}

/* 当前步骤 */
.step.active .step-dot {
  background: #fff;
  border-color: $brand-primary;
}

.step.active .step-num {
  color: $brand-primary;
  font-weight: bold;
}

.step.active .step-label {
  color: $brand-primary;
  font-weight: 600;
}

/* 连接线 */
.step-line {
  position: absolute;
  top: 19rpx;
  left: 50%;
  width: 100%;
  height: 4rpx;
  background: $border-base;
  z-index: 0;
}

.step-line.on {
  background: $brand-primary;
}
</style>
