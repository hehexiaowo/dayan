<template>
  <view class="skeleton-list">
    <view v-for="i in rows" :key="i" class="skeleton-row" :class="{ avatar: showAvatar }">
      <view v-if="showAvatar" class="sk-avatar shimmer" />
      <view class="sk-lines">
        <view class="sk-line shimmer w60" />
        <view class="sk-line shimmer w85" />
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
/** 列表骨架屏：加载中替代"加载中…"文字，避免内容跳动。 */
withDefaults(
  defineProps<{
    /** 骨架行数 */
    rows?: number
    /** 是否展示左侧头像占位（列表项带头像时开启） */
    showAvatar?: boolean
  }>(),
  { rows: 4, showAvatar: false },
)
</script>

<style scoped lang="scss">
.skeleton-list {
  padding: $spacing-md;
}

.skeleton-row {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}

.sk-avatar {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  margin-right: $spacing-md;
  flex-shrink: 0;
}

.sk-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.sk-line {
  height: 28rpx;
  border-radius: 999rpx;
}

.w60 {
  width: 60%;
}

.w85 {
  width: 85%;
}

.shimmer {
  background: linear-gradient(90deg, #f0f2f5 25%, #e4e7ed 37%, #f0f2f5 63%);
  background-size: 400% 100%;
  animation: sk-shimmer 1.4s ease infinite;
}

@keyframes sk-shimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
