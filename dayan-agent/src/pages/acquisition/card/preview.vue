<template>
  <view class="preview-page">
    <!-- 骨架屏 -->
    <view v-if="loading" class="skeleton-wrap">
      <DySkeleton :rows="5" card />
    </view>

    <view v-else-if="card" class="preview-content">
      <!-- 名片主体（渐变背景） -->
      <view class="card-hero">
        <view class="hero-avatar">
          <image
            v-if="avatarUrl"
            class="hero-avatar-img"
            :src="avatarUrl"
            mode="aspectFill"
          />
          <view v-else class="hero-avatar-fallback">
            <text class="hero-avatar-text">{{ card.displayName?.charAt(0) || '?' }}</text>
          </view>
        </view>
        <text class="hero-name">{{ card.displayName }}</text>
        <text v-if="card.title" class="hero-title">{{ card.title }}</text>
        <text v-if="card.company" class="hero-company">{{ card.company }}</text>
      </view>

      <!-- 联系方式 -->
      <view class="info-section">
        <view class="section-label">联系方式</view>
        <view class="info-card">
          <view v-if="card.phone" class="info-row dy-clickable" @click="onCall">
            <text class="info-icon">📱</text>
            <text class="info-text">{{ card.phone }}</text>
            <text class="info-action">拨打</text>
          </view>
          <view v-if="card.wechat" class="info-row" @click="onCopy(card.wechat, '微信号')">
            <text class="info-icon">💬</text>
            <text class="info-text">{{ card.wechat }}</text>
            <text class="info-action">复制</text>
          </view>
          <view v-if="card.email" class="info-row" @click="onCopy(card.email, '邮箱')">
            <text class="info-icon">📧</text>
            <text class="info-text">{{ card.email }}</text>
            <text class="info-action">复制</text>
          </view>
          <view v-if="card.address" class="info-row info-row-address">
            <text class="info-icon">📍</text>
            <text class="info-text">{{ card.address }}</text>
          </view>
        </view>
      </view>

      <!-- 个人简介 -->
      <view v-if="card.intro" class="info-section">
        <view class="section-label">个人简介</view>
        <view class="info-card">
          <text class="intro-text">{{ card.intro }}</text>
        </view>
      </view>

      <!-- 专长标签 -->
      <view v-if="tagList.length" class="info-section">
        <view class="section-label">专长领域</view>
        <view class="tags-wrap">
          <text v-for="tag in tagList" :key="tag" class="tag-chip">{{ tag }}</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="actions">
        <button class="dy-btn dy-btn-outline action-btn" @click="onEdit">编辑</button>
        <button class="dy-btn action-btn btn-delete" @click="onDelete">删除</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getCardDetail, deleteCard } from '@/api/card';
import { formatFileUrl } from '@/utils/file';
import type { BusinessCard } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';

const loading = ref(false);
const card = ref<BusinessCard | null>(null);

const avatarUrl = computed(() => {
  if (!card.value?.avatar) return '';
  return formatFileUrl(card.value.avatar);
});

const tagList = computed(() => {
  if (!card.value?.tags) return [];
  return card.value.tags
    .split(/[，,]/)
    .map((s) => s.trim())
    .filter(Boolean);
});

async function loadDetail(id: string) {
  loading.value = true;
  try {
    card.value = await getCardDetail(id);
  } catch {
    uni.showToast({ title: '加载名片失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function onCall() {
  if (!card.value?.phone) return;
  uni.makePhoneCall({ phoneNumber: card.value.phone }).catch(() => {});
}

function onCopy(text: string, label: string) {
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: `${label}已复制`, icon: 'none' }),
  });
}

function onEdit() {
  if (!card.value) return;
  uni.navigateTo({ url: '/pages/acquisition/card/edit?id=' + card.value.id });
}

function onDelete() {
  if (!card.value) return;
  uni.showModal({
    title: '删除名片',
    content: `确定删除「${card.value.cardName}」吗？`,
    confirmColor: '#fa3534',
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await deleteCard(card.value!.id);
        uni.showToast({ title: '已删除', icon: 'success' });
        setTimeout(() => uni.navigateBack(), 500);
      } catch {
        // 错误已由 request 拦截器提示
      }
    },
  });
}

interface CardPreviewRouteParams {
  id?: string;
}

onLoad((options: CardPreviewRouteParams) => {
  if (options?.id) {
    loadDetail(String(options.id));
  }
});
</script>

<style lang="scss" scoped>

.preview-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 80rpx;
}

.skeleton-wrap {
  padding: $spacing-md;
}

/* 名片主体（渐变背景区） */
.card-hero {
  background: $gradient-blue;
  padding: 60rpx $spacing-lg 50rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.hero-avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.25);
  border: 6rpx solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}

.hero-avatar-img {
  width: 100%;
  height: 100%;
}

.hero-avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-avatar-text {
  font-size: 56rpx;
  font-weight: bold;
  color: #fff;
}

.hero-name {
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
}

.hero-title {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.9);
}

.hero-company {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.75);
  margin-top: 4rpx;
}

/* 信息区块 */
.info-section {
  margin-top: $spacing-md;
  padding: 0 $spacing-lg;
}

.section-label {
  font-size: 26rpx;
  color: $text-secondary;
  margin-bottom: $spacing-sm;
  font-weight: 500;
}

.info-card {
  background: $bg-card;
  border-radius: $radius-md;
  padding: 0 $spacing-lg;
  box-shadow: $shadow-card;
}

.info-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-md 0;
  border-bottom: 2rpx solid $border-light;

  &:last-child {
    border-bottom: none;
  }
}

.info-icon {
  font-size: 32rpx;
  flex-shrink: 0;
}

.info-text {
  flex: 1;
  font-size: 28rpx;
  color: $text-primary;
  word-break: break-all;
}

.info-action {
  font-size: 24rpx;
  color: $brand-primary;
  flex-shrink: 0;
}

.info-row-address {
  align-items: flex-start;
}

/* 简介 */
.intro-text {
  display: block;
  font-size: 28rpx;
  color: $text-regular;
  line-height: 1.7;
  padding: $spacing-md 0;
}

/* 标签 */
.tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.tag-chip {
  font-size: 26rpx;
  color: $brand-primary;
  background: $brand-primary-light;
  padding: 12rpx 28rpx;
  border-radius: $radius-md;
}

/* 操作按钮 */
.actions {
  display: flex;
  gap: $spacing-md;
  padding: $spacing-xl $spacing-lg 0;
}

.action-btn {
  flex: 1;
}

.btn-delete {
  background: $bg-card;
  border: 2rpx solid $brand-error;
  color: $brand-error;
}
</style>
