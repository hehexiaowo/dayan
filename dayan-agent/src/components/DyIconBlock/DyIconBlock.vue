<template>
  <view class="dy-icon-block" :class="[sizeClass, shapeClass, colorClass]">
    <text class="dy-icon-text">{{ text }}</text>
  </view>
</template>

<script setup lang="ts">
/**
 * 精致化图标色块组件。
 *
 * 渐变背景 + 微阴影 + 圆角/圆形，替代全项目的 emoji 和手写色块。
 * color 映射到 variables.scss 中的渐变令牌。
 */
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    /** 图标文字（1-2 字） */
    text: string;
    /** 颜色主题 */
    color?: 'blue' | 'green' | 'orange' | 'red' | 'gray';
    /** 尺寸 */
    size?: 'sm' | 'md' | 'lg';
    /** 形状 */
    shape?: 'circle' | 'square';
  }>(),
  {
    color: 'blue',
    size: 'md',
    shape: 'square',
  },
);

const sizeClass = computed(() => `is-${props.size}`);
const shapeClass = computed(() => `is-${props.shape}`);
const colorClass = computed(() => `is-${props.color}`);
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.dy-icon-block {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-weight: bold;
  color: #fff;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.12);
}

/* 尺寸 */
.is-sm {
  width: 56rpx;
  height: 56rpx;
  border-radius: $radius-sm;
  font-size: 26rpx;
}
.is-md {
  width: 80rpx;
  height: 80rpx;
  border-radius: $radius-md;
  font-size: 34rpx;
}
.is-lg {
  width: 104rpx;
  height: 104rpx;
  border-radius: $radius-lg;
  font-size: 42rpx;
}

/* 形状 */
.is-circle {
  border-radius: 50% !important;
}

/* 颜色渐变 */
.is-blue {
  background: $gradient-blue;
  box-shadow: 0 4rpx 12rpx rgba(64, 158, 255, 0.3);
}
.is-green {
  background: $gradient-green;
  box-shadow: 0 4rpx 12rpx rgba(25, 190, 107, 0.3);
}
.is-orange {
  background: $gradient-orange;
  box-shadow: 0 4rpx 12rpx rgba(255, 153, 0, 0.3);
}
.is-red {
  background: $gradient-red;
  box-shadow: 0 4rpx 12rpx rgba(250, 53, 52, 0.3);
}
.is-gray {
  background: $gradient-gray;
  box-shadow: 0 4rpx 12rpx rgba(144, 147, 153, 0.3);
}
</style>
