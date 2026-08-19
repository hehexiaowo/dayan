<template>
  <view class="page">
    <!-- 头部 -->
    <view class="hero">
      <text class="hero-title">你问我答</text>
      <text class="hero-sub">AI 智能问答 · 展业答疑助手</text>
    </view>

    <!-- 加载中 -->
    <DySkeletonList v-if="loading" :rows="4" show-avatar />

    <!-- 加载失败 -->
    <view v-else-if="loadError" class="empty">
      <text class="empty-title">加载失败</text>
      <text class="empty-desc">网络异常，请稍后重试</text>
      <view class="retry-btn dy-clickable" @click="loadConfigs"><text class="retry-btn-text">重新加载</text></view>
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
        :key="c.toolCode"
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
import DySkeletonList from '@/components/DySkeletonList/DySkeletonList.vue';
import { getAichatPersonas } from '@/api/toolChat';
import type { AichatPersona } from '@/types';

const configs = ref<AichatPersona[]>([]);
const loading = ref(false);
const loadError = ref(false);

onShow(loadConfigs);

async function loadConfigs() {
  loading.value = true;
  loadError.value = false;
  try {
    configs.value = await getAichatPersonas();
  } catch {
    configs.value = [];
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function openChat(c: AichatPersona) {
  uni.navigateTo({ url: `/pages/acquisition/aichat/chat?toolCode=${c.toolCode}` });
}

/** 头像文字：优先 icon，回退人物名首字 */
function iconText(c: AichatPersona): string {
  if (c.icon) return c.icon;
  return c.personaName ? c.personaName.charAt(0) : '问';
}

/** 头像颜色：映射 DyIconBlock 支持的主题色，未知色回退 blue */
const COLOR_SET = ['blue', 'green', 'orange', 'red', 'gray'] as const;
function iconColor(c: AichatPersona) {
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
.hero {
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  margin: $spacing-md;
}
.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
}
.hero-sub {
  display: block;
  margin-top: $spacing-xs;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* 空态 / 错误态 */
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
.retry-btn {
  margin-top: $spacing-lg;
  height: $control-height-sm;
  padding: 0 48rpx;
  background: $gradient-blue;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
}
.retry-btn-text {
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;
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