<template>
  <view class="page dy-safe-bottom">
    <!-- 骨架屏 -->
    <template v-if="loading">
      <DySkeleton :rows="1" card />
      <DySkeleton :rows="6" card />
    </template>

    <!-- 加载失败 -->
    <DyEmpty
      v-else-if="loadError"
      text="加载失败，请检查网络后重试"
      icon="!"
      color="gray"
      action-text="重新加载"
      @action="loadDetail"
    />

    <template v-else-if="content">
      <!-- ===== 文章头部（公众号样式：标题 + meta 行） ===== -->
      <view class="article-head">
        <text class="article-title">{{ content.title }}</text>
        <view class="article-meta">
          <text v-if="content.author" class="meta-author">{{ content.author }}</text>
          <text class="meta-item">{{ formatDate(content.publishTime) }}</text>
          <text v-if="content.viewCount != null" class="meta-item">{{ formatViews(content.viewCount) }} 阅读</text>
          <text v-if="content.badge" class="meta-badge">{{ content.badge }}</text>
        </view>
        <view v-if="content.summary" class="article-summary">
          <text class="summary-text">{{ content.summary }}</text>
        </view>
      </view>

      <!-- ===== 正文（纯文本按空行分段） ===== -->
      <view v-if="paragraphs.length" class="article-body">
        <text v-for="(p, i) in paragraphs" :key="i" class="paragraph">{{ p }}</text>
      </view>
      <DyEmpty v-else text="正文整理中" icon="文" color="blue" />

      <!-- ===== 底部提示 ===== -->
      <view class="article-foot">
        <text class="foot-text">内容来自养老宝典学习中心</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getLearningDetail } from '@/api/learning';
import type { LearningContent } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

/**
 * 学习中心内容详情页（渠道课程 / 外部课程 / 雁鸣中国共用）。
 *
 * - 数据走真实详情接口（GET /agent-api/learning/contents/{code}，浏览量累加）；
 * - 正文 body 为纯文本，按空行分段渲染（公众号文章样式）；
 * - onLoad 仅解析深链 query，状态只存内存。
 */

const content = ref<LearningContent | null>(null);
const loading = ref(false);
const loadError = ref(false);

/** 正文分段：按空行拆分为段，过滤纯空白段 */
const paragraphs = computed<string[]>(() => {
  if (!content.value?.body) return [];
  return content.value.body
    .split(/\n\s*\n/)
    .map((p) => p.trim())
    .filter(Boolean);
});

async function loadDetail() {
  if (!content.value?.contentCode) return;
  loading.value = true;
  loadError.value = false;
  try {
    content.value = await getLearningDetail(content.value.contentCode);
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function formatViews(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万';
  return String(n);
}

function formatDate(dt?: string): string {
  if (!dt) return '';
  const norm = dt.replace('T', ' ');
  return norm.length >= 10 ? norm.slice(0, 10) : norm;
}

onLoad((options) => {
  const code = options?.code;
  if (!code) return;
  // 先占位 contentCode 供 loadDetail 使用，成功后整对象替换
  content.value = { id: '', contentCode: code, title: '', category: 0 };
  loadDetail();
});
</script>

<style lang="scss" scoped>

.page {
  padding: $spacing-lg $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-card;
}

/* ===== 文章头部 ===== */
.article-head {
  padding-bottom: $spacing-md;
  border-bottom: 1rpx solid $border-light;
}

.article-title {
  font-size: 40rpx;
  font-weight: bold;
  color: $text-primary;
  line-height: 1.5;
}

.article-meta {
  margin-top: $spacing-sm;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
}

.meta-author {
  font-size: 24rpx;
  color: $brand-primary;
}

.meta-item {
  font-size: 24rpx;
  color: $text-placeholder;
}

.meta-badge {
  font-size: 20rpx;
  color: $brand-error;
  background: $brand-error-light;
  border-radius: 999rpx;
  padding: 2rpx 14rpx;
}

.article-summary {
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: $bg-page;
  border-radius: $radius-md;
}

.summary-text {
  font-size: 26rpx;
  color: $text-secondary;
  line-height: 1.7;
}

/* ===== 正文 ===== */
.article-body {
  padding-top: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.paragraph {
  font-size: 30rpx;
  color: $text-regular;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* ===== 底部 ===== */
.article-foot {
  margin-top: $spacing-xl;
  padding-top: $spacing-md;
  border-top: 1rpx solid $border-light;
  display: flex;
  justify-content: center;
}

.foot-text {
  font-size: 22rpx;
  color: $text-placeholder;
}
</style>
