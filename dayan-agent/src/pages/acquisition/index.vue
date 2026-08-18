<template>
  <view class="page dy-safe-bottom">
    <!-- 渐变 header（与展业中心一致） -->
    <view class="header">
      <text class="header-title">获客工具</text>
      <text class="header-sub">内容获客、线索管理、专业工具</text>
    </view>

    <!-- ===== 常用功能（每行 4 个菜单卡） ===== -->
    <view class="tools-card">
      <view class="tools-title">常用功能</view>
      <view class="tools-grid">
        <view class="tool-item dy-clickable" @click="onTool('lead')">
          <DyIconBlock text="线" color="blue" size="md" />
          <text class="tool-label">线索管理</text>
        </view>
        <view class="tool-item dy-clickable" @click="onTool('content')">
          <DyIconBlock text="文" color="green" size="md" />
          <text class="tool-label">内容获客</text>
        </view>
        <view class="tool-item dy-clickable" @click="onTool('poster')">
          <DyIconBlock text="海" color="orange" size="md" />
          <text class="tool-label">营销海报</text>
        </view>
        <view class="tool-item dy-clickable" @click="onTool('card')">
          <DyIconBlock text="名" color="blue" size="md" />
          <text class="tool-label">电子名片</text>
        </view>
        <view class="tool-item dy-clickable" @click="onTool('pensioncal')">
          <DyIconBlock text="退" color="orange" size="md" />
          <text class="tool-label">养老计算</text>
        </view>
        <view class="tool-item dy-clickable" @click="onTool('gapcal')">
          <DyIconBlock text="缺" color="red" size="md" />
          <text class="tool-label">缺口计算</text>
        </view>
        <view class="tool-item dy-clickable" @click="onTool('aiartist')">
          <DyIconBlock text="AI" color="red" size="md" />
          <text class="tool-label">AI 创作</text>
        </view>
        <view class="tool-item dy-clickable" @click="onTool('aichat')">
          <DyIconBlock text="答" color="red" size="md" />
          <text class="tool-label">你问我答</text>
        </view>
        <view class="tool-item dy-clickable" @click="onTool('network')">
          <DyIconBlock text="网" color="green" size="md" />
          <text class="tool-label">养老网络</text>
        </view>
      </view>
    </view>

    <!-- ===== 今日热点 ===== -->
    <view class="hot-section">
      <view class="section-head">
        <text class="section-title">今日热点</text>
        <view class="section-actions">
          <text class="section-more dy-clickable" @click="onTool('content')">内容</text>
          <text class="section-more-sep">·</text>
          <text class="section-more dy-clickable" @click="onTool('poster')">海报</text>
        </view>
      </view>

      <!-- 骨架屏 -->
      <template v-if="hotLoading">
        <DySkeleton v-for="i in 2" :key="i" :rows="2" avatar card />
      </template>

      <!-- 内容热点（横向滑动） -->
      <scroll-view v-if="hotContents.length" scroll-x class="hot-scroll" :show-scrollbar="false">
        <view class="hot-row">
          <view
            v-for="article in hotContents"
            :key="article.contentCode"
            class="hot-item dy-clickable"
            @click="goContent(article.contentCode)"
          >
            <view class="hot-cover">
              <image
                v-if="formatFileUrl(article.coverImage)"
                :src="formatFileUrl(article.coverImage)"
                mode="aspectFill"
                class="hot-img"
              />
              <view v-else class="hot-placeholder">
                <text class="hot-char">{{ (article.title || '?').charAt(0) }}</text>
              </view>
              <text v-if="article.viewCount != null" class="hot-views">{{ formatCount(article.viewCount) }}阅</text>
            </view>
            <text class="hot-name">{{ article.title }}</text>
          </view>
        </view>
      </scroll-view>

      <!-- 海报热点（横向滑动） -->
      <scroll-view v-if="hotPosters.length" scroll-x class="hot-scroll" :show-scrollbar="false">
        <view class="hot-row">
          <view
            v-for="tpl in hotPosters"
            :key="tpl.templateCode"
            class="hot-item dy-clickable"
            @click="goPoster(tpl.templateCode)"
          >
            <view class="hot-cover">
              <image
                :src="formatFileUrl(tpl.coverImage)"
                mode="aspectFill"
                class="hot-img"
              />
            </view>
            <text class="hot-name">{{ tpl.title }}</text>
          </view>
        </view>
      </scroll-view>

      <!-- 空状态 -->
      <DyEmpty v-if="!hotLoading && !hotContents.length && !hotPosters.length" text="暂无热点内容" icon="热" color="orange" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getContentList } from '@/api/content';
import { getPosterTemplates } from '@/api/poster';
import { formatFileUrl } from '@/utils/file';
import type { ContentArticle } from '@/types';
import type { PosterTemplate } from '@/api/poster';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

/**
 * 获客中心首页（tab 页）。
 *
 * - 风格与展业中心统一：渐变 header + 常用功能宫格（含四个工具直达入口）；
 * - 你问我答的问答人物为其二级页面（aichat/index 人物选择），不在本页平铺；
 * - 「今日热点」独立区块：最新/最热内容（前 4）+ 海报模板（前 2），横向滑动；
 * - 线索清单/搜索已拆至独立「线索管理」页（个人中心「线索记录」同入口）。
 */

const hotLoading = ref(false);
const hotContents = ref<ContentArticle[]>([]);
const hotPosters = ref<PosterTemplate[]>([]);

async function loadHot() {
  hotLoading.value = true;
  try {
    // 内容：取最新/推荐前 4（后端按 sortOrder + 浏览量排序）
    const res = await getContentList({ current: 1, size: 4 });
    hotContents.value = res?.records || [];
    // 海报：取前 2
    const posters = await getPosterTemplates();
    hotPosters.value = (posters || []).slice(0, 2);
  } catch {
    hotContents.value = [];
    hotPosters.value = [];
  } finally {
    hotLoading.value = false;
  }
}

function formatCount(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万';
  return String(n);
}

function goContent(contentCode: string) {
  uni.navigateTo({ url: `/pages/acquisition/content/detail?code=${contentCode}` });
}

function goPoster(templateCode: string) {
  uni.navigateTo({ url: `/pages/acquisition/poster/detail?code=${templateCode}` });
}

function onShareCode() {
  uni.showToast({ title: '分享获客码功能开发中', icon: 'none' });
}

function onTool(type: string) {
  if (type === 'lead') {
    uni.navigateTo({ url: '/pages/acquisition/lead/index' });
    return;
  }
  if (type === 'content') {
    uni.navigateTo({ url: '/pages/acquisition/content/index' });
    return;
  }
  if (type === 'card') {
    uni.navigateTo({ url: '/pages/acquisition/card/index' });
    return;
  }
  if (type === 'poster') {
    uni.navigateTo({ url: '/pages/acquisition/poster/index' });
    return;
  }
  if (type === 'pensioncal') {
    uni.navigateTo({ url: '/pages/acquisition/tools/pensioncal' });
    return;
  }
  if (type === 'gapcal') {
    uni.navigateTo({ url: '/pages/acquisition/tools/gapcal' });
    return;
  }
  if (type === 'aiartist') {
    uni.navigateTo({ url: '/pages/acquisition/tools/aiartist/index' });
    return;
  }
  if (type === 'aichat') {
    uni.navigateTo({ url: '/pages/acquisition/aichat/index' });
    return;
  }
  if (type === 'network') {
    uni.navigateTo({ url: '/pages/business/park/index' });
    return;
  }
  uni.showToast({ title: '功能开发中', icon: 'none' });
}

onShow(() => {
  loadHot();
});
</script>

<style lang="scss" scoped>

.page {
  padding: $spacing-md $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* ===== 渐变 header（与展业中心一致） ===== */
.header {
  background: $gradient-blue;
  border-radius: $radius-lg;
  padding: $spacing-xl $spacing-lg;
  margin-bottom: $spacing-md;
}
.header-title {
  display: block;
  font-size: 38rpx;
  font-weight: bold;
  color: #fff;
}
.header-sub {
  display: block;
  margin-top: $spacing-sm;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

/* ===== 获客工具（每行 4 个菜单卡） ===== */
.tools-card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg $spacing-md;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
}
.tools-title {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
  margin-bottom: $spacing-lg;
}
.tools-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  row-gap: $spacing-lg;
}
.tool-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
}
.tool-label {
  font-size: 24rpx;
  color: $text-regular;
}

/* ===== 今日热点 ===== */
.hot-section {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg $spacing-md;
  margin-top: $spacing-lg;
  box-shadow: $shadow-card;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;
}
.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}
.section-actions {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
}
.section-more {
  font-size: 24rpx;
  color: $text-secondary;
}
.section-more-sep {
  font-size: 24rpx;
  color: $text-placeholder;
}
.hot-scroll {
  margin-bottom: $spacing-md;
}
.hot-scroll:last-of-type {
  margin-bottom: 0;
}
.hot-row {
  display: inline-flex;
  gap: $spacing-md;
  padding: 4rpx;
}
.hot-item {
  width: 220rpx;
  flex-shrink: 0;
}
.hot-cover {
  position: relative;
  width: 220rpx;
  height: 150rpx;
  border-radius: $radius-sm;
  overflow: hidden;
  background: $brand-warning-light;
}
.hot-img {
  width: 100%;
  height: 100%;
}
.hot-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $gradient-orange;
}
.hot-char {
  font-size: 56rpx;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.9);
}
.hot-views {
  position: absolute;
  right: 8rpx;
  bottom: 8rpx;
  font-size: 18rpx;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 6rpx;
  padding: 2rpx 8rpx;
}
.hot-name {
  display: block;
  margin-top: $spacing-xs;
  font-size: 24rpx;
  color: $text-regular;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
