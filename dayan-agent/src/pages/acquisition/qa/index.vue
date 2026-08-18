<template>
  <view class="page">
    <!-- 头部 -->
    <view class="header">
      <text class="header-title">你问我答</text>
      <text class="header-subtitle">AI 智能问答 · 展业答疑助手</text>
    </view>

    <!-- 加载中 -->
    <view v-if="loading" class="empty">
      <text class="empty-text">加载中…</text>
    </view>

    <!-- 空态 -->
    <view v-else-if="configs.length === 0" class="empty">
      <DyIconBlock text="答" color="gray" size="lg" shape="circle" />
      <text class="empty-title">暂无可用的 AI 问答助手</text>
      <text class="empty-desc">更多展业助手持续上线中</text>
    </view>

    <!-- 人物卡片列表 -->
    <view v-else class="persona-list">
      <view
        v-for="c in configs"
        :key="c.id"
        class="persona-card dy-clickable"
        @click="openChat(c)"
      >
        <DyIconBlock
          :text="iconText(c)"
          :color="iconColor(c)"
          size="lg"
          shape="circle"
        />
        <view class="persona-info">
          <text class="persona-name">{{ c.personaName }}</text>
          <text class="persona-desc">{{ c.welcomeMsg || 'AI 展业答疑助手' }}</text>
        </view>
        <text class="persona-arrow">›</text>
      </view>
    </view>

    <!-- 底部提示 -->
    <view class="bottom-tip">
      <text class="tip-text">所有回答均来自大雁养老知识库，附引用出处</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import { getQaConfigs } from '@/api/toolQa';
import type { QaConfig } from '@/types';

const configs = ref<QaConfig[]>([]);
const loading = ref(false);

onShow(async () => {
  loading.value = true;
  try {
    configs.value = await getQaConfigs();
  } catch {
    configs.value = [];
  } finally {
    loading.value = false;
  }
});

function openChat(c: QaConfig) {
  uni.navigateTo({ url: `/pages/acquisition/qa/chat?configId=${c.id}` });
}

/** 头像文字：优先 icon，回退人物名首字 */
function iconText(c: QaConfig): string {
  if (c.icon) return c.icon;
  return c.personaName ? c.personaName.charAt(0) : '问';
}

/** 头像颜色：映射 DyIconBlock 支持的主题色，未知色回退 blue */
const COLOR_SET = ['blue', 'green', 'orange', 'red', 'gray'] as const;
function iconColor(c: QaConfig) {
  const hit = COLOR_SET.find((x) => x === c.iconColor);
  return hit || 'blue';
}
</script>

<style lang="scss" scoped>

.page {
  min-height: 100vh;
  background: $bg-page;
}

/* 头部 */
.header {
  background: $gradient-blue;
  padding: $spacing-xl $spacing-lg;
  display: flex;
  flex-direction: column;
}
.header-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
}
.header-subtitle {
  margin-top: $spacing-xs;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 空态 / 加载 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx $spacing-lg 0;
}
.empty-text {
  font-size: 26rpx;
  color: $text-secondary;
}
.empty-title {
  margin-top: $spacing-lg;
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}
.empty-desc {
  margin-top: $spacing-sm;
  font-size: 26rpx;
  color: $text-secondary;
}

/* 人物卡片列表 */
.persona-list {
  padding: $spacing-md;
}
.persona-card {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-lg;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}
.persona-info {
  flex: 1;
  margin-left: $spacing-md;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.persona-name {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}
.persona-desc {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.persona-arrow {
  font-size: 40rpx;
  color: $text-placeholder;
  margin-left: $spacing-sm;
}

/* 底部提示 */
.bottom-tip {
  padding: $spacing-xl 0 80rpx;
  display: flex;
  justify-content: center;
}
.tip-text {
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>