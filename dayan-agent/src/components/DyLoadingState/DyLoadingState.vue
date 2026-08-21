<template>
  <view class="loading-state">
    <view class="loading-spinner"></view>
    <text class="loading-text">{{ text }}</text>
    <text v-if="hint" class="loading-hint">{{ hint }}</text>
  </view>
</template>

<script setup lang="ts">
/**
 * 统一加载状态：品牌色旋转指示器 + 主文案 + 辅助提示。
 *
 * 用于 LLM 生成等耗时操作的等待反馈。
 */
withDefaults(
  defineProps<{
    /** 主文案（当前正在执行的操作） */
    text: string
    /** 辅助提示 */
    hint?: string
  }>(),
  { text: '加载中...' },
)
</script>

<style scoped lang="scss">
.loading-state {
  padding: 100rpx 0;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-md;
}

.loading-spinner {
  width: 88rpx;
  height: 88rpx;
  border: 6rpx solid $brand-primary-light;
  border-top-color: $brand-primary;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 32rpx;
  color: $text-primary;
  font-weight: 600;
}

.loading-hint {
  font-size: 26rpx;
  color: $text-secondary;
}
</style>
