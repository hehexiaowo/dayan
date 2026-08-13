<template>
  <view class="dy-skeleton" :class="{ 'is-card': card }">
    <!-- 头像占位 -->
    <view v-if="avatar" class="skeleton-avatar skeleton-pulse" />

    <view class="skeleton-body">
      <view
        v-for="i in rows"
        :key="i"
        class="skeleton-line skeleton-pulse"
        :style="{ width: i === rows ? '60%' : '100%' }"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
/**
 * 骨架屏组件。
 *
 * 列表/详情页加载时替代「加载中...」纯文本，提供内容结构预览。
 */
withDefaults(
  defineProps<{
    /** 占位行数 */
    rows?: number;
    /** 是否显示头像占位 */
    avatar?: boolean;
    /** 是否渲染为卡片样式（白底+圆角+阴影） */
    card?: boolean;
  }>(),
  {
    rows: 3,
    avatar: false,
    card: true,
  },
);
</script>

<style lang="scss" scoped>

.dy-skeleton {
  display: flex;
  align-items: center;
  padding: $spacing-lg;

  &.is-card {
    background: $bg-card;
    border-radius: $radius-md;
    box-shadow: $shadow-card;
    margin-bottom: $spacing-md;
  }
}

.skeleton-pulse {
  background: linear-gradient(90deg, #f0f0f0 25%, #e6e6e6 37%, #f0f0f0 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

.skeleton-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  flex-shrink: 0;
  margin-right: $spacing-md;
}

.skeleton-body {
  flex: 1;
}

.skeleton-line {
  height: 28rpx;
  border-radius: $radius-sm;
  margin-bottom: $spacing-sm;

  &:last-child {
    margin-bottom: 0;
  }
}

@keyframes skeleton-loading {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
