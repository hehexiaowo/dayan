<template>
  <view class="page dy-safe-bottom">
    <template v-if="loading">
      <DySkeleton :rows="8" card />
    </template>

    <template v-else-if="article">
      <!-- 视频置顶 -->
      <view v-if="article.contentType === 2 && mediaUrl" class="video-top">
        <video :src="mediaUrl" controls class="video-player-top" object-fit="contain" />
      </view>

      <!-- 信息区 -->
      <view class="info-section">
        <text class="title">{{ article.title }}</text>
        <text v-if="article.subtitle" class="subtitle">{{ article.subtitle }}</text>
        <view class="meta-row">
          <text v-if="article.authorName" class="author-name">{{ article.authorName }}</text>
          <text v-if="article.publishTime" class="meta-text">{{ formatDate(article.publishTime) }}</text>
          <text v-if="article.viewCount != null" class="meta-text">{{ formatCount(article.viewCount) }} 阅读</text>
        </view>
      </view>

      <!-- 正文 -->
      <view class="body-section">
        <template v-if="article.contentType === 2">
          <text v-if="mediaDesc" class="body-text">{{ mediaDesc }}</text>
        </template>
        <template v-else-if="article.contentType === 3">
          <view class="gallery-grid">
            <image
              v-for="(img, i) in galleryImages"
              :key="i"
              :src="img"
              mode="aspectFill"
              class="gallery-grid-img dy-clickable"
              @click="previewImage(i)"
            />
          </view>
          <text v-if="article.summary" class="body-text">{{ article.summary }}</text>
        </template>
        <template v-else-if="article.contentType === 6">
          <view v-if="fileInfo.url" class="file-download dy-clickable" @click="openFile">
            <text class="file-dl-icon">📄</text>
            <view class="file-dl-info">
              <text class="file-dl-name">{{ fileInfo.name }}</text>
              <text class="file-dl-size">{{ fileInfo.ext }}{{ fileInfo.ext && fileInfo.size ? ' · ' : '' }}{{ fileInfo.size }}</text>
            </view>
            <text class="file-dl-btn">下载</text>
          </view>
          <text v-if="article.summary" class="body-text">{{ article.summary }}</text>
        </template>
        <template v-else>
          <rich-text v-if="isHtmlBody" :nodes="article.contentBody || ''" />
          <text v-else class="body-text">{{ article.contentBody || article.summary || '暂无内容' }}</text>
        </template>
      </view>

      <!-- 分享人名片 -->
      <view v-if="card" class="agent-card">
        <view class="card-top">
          <view class="card-avatar">
            <image v-if="formatFileUrl(card.avatar)" :src="formatFileUrl(card.avatar)" mode="aspectFill" class="avatar-img" />
            <text v-else class="avatar-fallback">{{ card.displayName?.charAt(0) || '?' }}</text>
          </view>
          <view class="card-info">
            <view class="name-row">
              <text class="agent-name">{{ card.displayName }}</text>
              <text v-if="card.title" class="agent-title">{{ card.title }}</text>
            </view>
            <text v-if="card.company" class="agent-company">{{ card.company }}</text>
            <view v-if="card.tags" class="agent-tags">
              <text v-for="tag in cardTags" :key="tag" class="agent-tag">{{ tag }}</text>
            </view>
          </view>
        </view>
        <view v-if="card.intro" class="card-intro">{{ card.intro }}</view>
        <!-- 联系按钮 -->
        <view class="contact-row">
          <view v-if="card.phone" class="contact-btn call-btn dy-clickable" @click="callPhone">
            <text class="contact-icon">📱</text>
            <text class="contact-text">拨打电话</text>
          </view>
          <view v-if="card.wechat" class="contact-btn wechat-btn dy-clickable" @click="copyWechat">
            <text class="contact-icon">💬</text>
            <text class="contact-text">复制微信</text>
          </view>
        </view>
      </view>

      <!-- 底部品牌 -->
      <view class="brand-footer">
        <text class="brand-text">大雁养老 · 专业养老服务平台</text>
      </view>
    </template>

    <DyEmpty v-else text="内容不存在或已下线" icon="!" color="gray" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getShareContent } from '@/api/share';
import { formatFileUrl } from '@/utils/file';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const loading = ref(true);
const article = ref<any>(null);
const card = ref<any>(null);

const isHtmlBody = computed(() => {
  const body = article.value?.contentBody;
  return !!body && body.trim().startsWith('<');
});

const mediaUrl = computed(() => {
  if (article.value?.contentType !== 2) return '';
  const body = article.value.contentBody || '';
  try {
    const parsed = JSON.parse(body);
    return formatFileUrl(parsed.url || '');
  } catch {
    return formatFileUrl(body.trim());
  }
});

const mediaDesc = computed(() => {
  if (article.value?.contentType !== 2) return '';
  try {
    return JSON.parse(article.value.contentBody || '{}').desc || '';
  } catch {
    return '';
  }
});

const galleryImages = computed<string[]>(() => {
  if (article.value?.contentType !== 3) return [];
  try {
    const parsed = JSON.parse(article.value.contentBody || '[]');
    return Array.isArray(parsed) ? parsed.map((s: unknown) => formatFileUrl(String(s))) : [];
  } catch {
    return [];
  }
});

const fileInfo = computed(() => {
  const fallback = { name: '附件', url: '', size: '', ext: '' };
  if (article.value?.contentType !== 6) return fallback;
  try {
    const parsed = JSON.parse(article.value.contentBody || '{}');
    const name = parsed.fileName || '附件';
    return {
      name,
      url: formatFileUrl(parsed.fileUrl || ''),
      size: parsed.fileSize || '',
      ext: name.includes('.') ? name.split('.').pop()!.toUpperCase() : '',
    };
  } catch {
    return fallback;
  }
});

const cardTags = computed<string[]>(() => {
  const tags = card.value?.tags;
  if (!tags) return [];
  return tags.split(/[,，]/).map((t: string) => t.trim()).filter(Boolean).slice(0, 3);
});

function formatDate(dt?: string): string {
  if (!dt) return '';
  return dt.length >= 10 ? dt.substring(0, 10) : dt;
}

function formatCount(n?: number): string {
  if (n == null) return '0';
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w';
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return String(n);
}

function previewImage(index: number) {
  uni.previewImage({ urls: galleryImages.value, current: galleryImages.value[index] });
}

function openFile() {
  if (!fileInfo.value.url) return;
  // #ifdef H5
  window.open(fileInfo.value.url, '_blank');
  // #endif
}

function callPhone() {
  if (card.value?.phone) {
    uni.makePhoneCall({ phoneNumber: card.value.phone });
  }
}

function copyWechat() {
  if (card.value?.wechat) {
    uni.setClipboardData({
      data: card.value.wechat,
      success: () => uni.showToast({ title: '微信号已复制', icon: 'none' }),
    });
  }
}

onLoad(async (query) => {
  const code = query?.code || '';
  const agent = query?.agent || '';
  if (!code) {
    loading.value = false;
    return;
  }
  const result = await getShareContent(code, agent);
  article.value = result.content;
  card.value = result.card;
  loading.value = false;
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page {
  padding-bottom: 60rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* 视频置顶 */
.video-top { width: 100%; background: #000; }
.video-player-top { width: 100%; height: 420rpx; }

/* 信息区 */
.info-section {
  background: $bg-card;
  padding: $spacing-lg $spacing-md $spacing-md;
}
.title {
  display: block;
  font-size: 36rpx;
  font-weight: bold;
  color: $text-primary;
  line-height: 1.5;
}
.subtitle {
  display: block;
  font-size: 26rpx;
  color: $text-secondary;
  margin-top: 8rpx;
}
.meta-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
}
.author-name { font-size: 24rpx; color: $brand-primary; font-weight: 500; }
.meta-text { font-size: 24rpx; color: $text-placeholder; }

/* 正文区 */
.body-section {
  background: $bg-card;
  padding: $spacing-md $spacing-md $spacing-lg;
  margin-bottom: $spacing-sm;
}
.body-text {
  display: block;
  font-size: 30rpx;
  color: $text-primary;
  line-height: 2;
  white-space: pre-wrap;
}
.gallery-grid { display: flex; flex-wrap: wrap; gap: $spacing-sm; }
.gallery-grid-img {
  width: calc((100% - 12rpx) / 2);
  height: 340rpx;
  border-radius: $radius-sm;
  background: $bg-page;
}
.file-download {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
}
.file-dl-icon { font-size: 40rpx; }
.file-dl-info { flex: 1; min-width: 0; }
.file-dl-name {
  display: block;
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-dl-size { font-size: 22rpx; color: $text-placeholder; }
.file-dl-btn {
  flex-shrink: 0;
  font-size: 26rpx;
  color: $brand-primary;
  font-weight: 500;
  padding: 8rpx 24rpx;
  border: 2rpx solid $brand-primary;
  border-radius: $radius-sm;
}

/* 分享人名片 */
.agent-card {
  background: $bg-card;
  margin: 0 $spacing-md;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-card;
}
.card-top {
  display: flex;
  gap: $spacing-md;
  padding: $spacing-lg $spacing-md $spacing-sm;
}
.card-avatar {
  width: 100rpx;
  height: 100rpx;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
  background: $gradient-blue;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-img { width: 100%; height: 100%; }
.avatar-fallback { font-size: 40rpx; color: #fff; font-weight: bold; }
.card-info { flex: 1; min-width: 0; padding-top: 4rpx; }
.name-row { display: flex; align-items: center; gap: $spacing-sm; }
.agent-name { font-size: 32rpx; font-weight: bold; color: $text-primary; }
.agent-title { font-size: 24rpx; color: $brand-primary; }
.agent-company { font-size: 24rpx; color: $text-secondary; margin-top: 4rpx; }
.agent-tags { display: flex; gap: $spacing-xs; margin-top: $spacing-sm; flex-wrap: wrap; }
.agent-tag {
  font-size: 20rpx;
  color: $text-secondary;
  background: $bg-page;
  padding: 2rpx 12rpx;
  border-radius: 4rpx;
}
.card-intro {
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.6;
  padding: 0 $spacing-md $spacing-sm;
}

/* 联系按钮 */
.contact-row {
  display: flex;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md $spacing-lg;
}
.contact-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  height: 76rpx;
  border-radius: $radius-md;
  font-size: 28rpx;
  font-weight: 500;
}
.call-btn {
  background: $gradient-blue;
  color: #fff;
}
.wechat-btn {
  background: #07c160;
  color: #fff;
}
.contact-icon { font-size: 30rpx; }
.contact-text { color: inherit; }

/* 底部品牌 */
.brand-footer {
  text-align: center;
  padding: $spacing-lg 0;
}
.brand-text {
  font-size: 22rpx;
  color: $text-placeholder;
}
</style>
