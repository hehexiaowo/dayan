<template>
  <view class="steps">
    <view
      v-for="(s, i) in steps"
      :key="i"
      class="step"
      :class="{ done: i < current, active: i === current }"
    >
      <view class="step-dot">
        <text v-if="i < current" class="step-check">✓</text>
        <text v-else class="step-num">{{ i + 1 }}</text>
      </view>
      <text class="step-label">{{ s }}</text>
      <view v-if="i < steps.length - 1" class="step-line" :class="{ on: i < current }" />
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * 文章转写七步流程步骤条。
 *
 * 供七个步骤页共用：current 为当前步骤下标（0-6），
 * 之前的步骤显示完成态（✓ + 品牌色），当前步骤高亮。
 */
const steps = ['获取', '判断', '策略', '转写', '审查', '配图', '完成']

withDefaults(
  defineProps<{
    /** 当前步骤下标（0-6） */
    current: number
  }>(),
  { current: 0 },
)
</script>

<style scoped lang="scss">
.steps {
  display: flex;
  align-items: flex-start;
  padding: 4rpx 0 $spacing-md;
}

.step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  min-width: 0;
}

.step-dot {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  background: #fff;
  border: 2rpx solid $border-base;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8rpx;
  transition: all $transition-fast;
}

.step-num {
  font-size: 20rpx;
  color: $text-secondary;
  line-height: 1;
}

.step-check {
  font-size: 20rpx;
  color: #fff;
  line-height: 1;
}

.step-label {
  font-size: 20rpx;
  color: $text-secondary;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.step-line {
  position: absolute;
  top: 15rpx;
  left: 50%;
  width: 100%;
  height: 2rpx;
  background: $border-base;
  z-index: -1;
}

.step.done .step-dot {
  background: $brand-primary;
  border-color: $brand-primary;
}

.step.done .step-label {
  color: $brand-primary;
}

.step.active .step-dot {
  border-color: $brand-primary;
  box-shadow: 0 0 0 6rpx rgba($brand-primary, 0.12);
}

.step.active .step-num {
  color: $brand-primary;
  font-weight: 700;
}

.step.active .step-label {
  color: $brand-primary;
  font-weight: 600;
}

.step-line.on {
  background: $brand-primary;
}
</style>
