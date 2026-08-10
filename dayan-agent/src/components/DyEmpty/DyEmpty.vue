<template>
  <view class="dy-empty">
    <view class="empty-icon" :class="`is-${color}`">
      <text class="empty-icon-text">{{ icon || defaultIcon }}</text>
    </view>
    <text class="empty-text">{{ text }}</text>
    <view
      v-if="actionText"
      class="empty-action"
      @click="$emit('action')"
    >
      <text class="action-text">{{ actionText }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * 空状态组件。
 *
 * 大号单字色块 + 提示文案 + 可选操作按钮，替代全项目的纯文字空状态。
 */
withDefaults(
  defineProps<{
    /** 提示文案 */
    text?: string;
    /** 图标文字（单字，如"空"） */
    icon?: string;
    /** 图标颜色主题 */
    color?: 'blue' | 'green' | 'orange' | 'gray';
    /** 操作按钮文字（不传则不显示按钮） */
    actionText?: string;
  }>(),
  {
    text: '暂无数据',
    icon: '',
    color: 'gray',
    actionText: '',
  },
);

defineEmits<{
  action: [];
}>();

const defaultIcon = '空';
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.dy-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0 80rpx;
}

.empty-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-lg;

  &.is-blue {
    background: $brand-primary-light;
  }
  &.is-green {
    background: $brand-success-light;
  }
  &.is-orange {
    background: $brand-warning-light;
  }
  &.is-gray {
    background: $brand-info-light;
  }
}

.empty-icon-text {
  font-size: 52rpx;
  font-weight: bold;
}
.is-blue .empty-icon-text {
  color: $brand-primary;
}
.is-green .empty-icon-text {
  color: $brand-success;
}
.is-orange .empty-icon-text {
  color: $brand-warning;
}
.is-gray .empty-icon-text {
  color: $brand-info;
}

.empty-text {
  font-size: 28rpx;
  color: $text-secondary;
  line-height: 1.6;
}

.empty-action {
  margin-top: $spacing-lg;
  padding: 16rpx 48rpx;
  background: $gradient-blue;
  border-radius: 40rpx;
  box-shadow: $shadow-fab;

  &:active {
    opacity: 0.85;
  }
}

.action-text {
  color: #fff;
  font-size: 28rpx;
  font-weight: 500;
}
</style>
