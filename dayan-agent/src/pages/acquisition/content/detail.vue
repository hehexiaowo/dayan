<template>
  <view class="page dy-safe-bottom">
    <template v-if="loading">
      <DySkeleton :rows="8" card />
    </template>

    <template v-else-if="article">
      <!-- 视频类型：播放器置顶 -->
      <view v-if="article.contentType === 2 && mediaUrl" class="video-top">
        <video
          :src="mediaUrl"
          controls
          class="video-player-top"
          object-fit="contain"
        />
      </view>

      <!-- 信息区（标题/副标题/作者/标签） -->
      <view class="info-section">
        <view v-if="getBadges(article).length" class="badge-row">
          <text
            v-for="badge in getBadges(article)"
            :key="badge.cls"
            class="badge"
            :class="badge.cls"
          >{{ badge.text }}</text>
        </view>
        <text class="title">{{ article.title }}</text>
        <text v-if="article.subtitle" class="subtitle">{{ article.subtitle }}</text>
        <view class="meta-row">
          <text v-if="article.authorName" class="author-name">{{ article.authorName }}</text>
          <text v-if="article.publishTime" class="meta-text">{{ formatDate(article.publishTime) }}</text>
          <text v-if="article.viewCount != null" class="meta-text">
            {{ formatCount(article.viewCount) }} {{ article.contentType === 2 ? '播放' : article.contentType === 6 ? '下载' : '阅读' }}
          </text>
          <text v-if="article.collectCount" class="meta-text">{{ article.collectCount }} 收藏</text>
        </view>
        <view v-if="tagList.length" class="tag-row">
          <text v-for="tag in tagList" :key="tag" class="tag-chip">{{ tag }}</text>
        </view>
      </view>

      <!-- 正文区（与信息区无间隙衔接） -->
      <view class="body-section">
        <!-- 视频：描述文字 -->
        <template v-if="article.contentType === 2">
          <text v-if="mediaDesc" class="body-text">{{ mediaDesc }}</text>
        </template>

        <!-- 图集：2列大图墙 -->
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
          <text v-if="article.summary" class="body-text gallery-caption">{{ article.summary }}</text>
        </template>

        <!-- 文件：大号下载卡 -->
        <template v-else-if="article.contentType === 6">
          <view class="file-card-large">
            <view class="file-card-header">
              <view class="file-icon-big">
                <text class="file-icon-big-text">📄</text>
                <text v-if="fileInfo.ext" class="file-ext-tag">{{ fileInfo.ext }}</text>
              </view>
              <view class="file-card-info">
                <text class="file-card-name">{{ fileInfo.name }}</text>
                <text class="file-card-meta">{{ fileInfo.ext && fileInfo.size ? fileInfo.ext + ' · ' + fileInfo.size : (fileInfo.ext || fileInfo.size) }}</text>
              </view>
            </view>
            <view class="download-btn dy-clickable" @click="openFile">
              <text class="download-text">📥 下载文件</text>
            </view>
          </view>
          <text v-if="article.summary" class="body-text">{{ article.summary }}</text>
        </template>

        <!-- 图文（默认） -->
        <template v-else>
          <rich-text v-if="isHtmlBody" :nodes="article.contentBody || ''" />
          <text v-else class="body-text">{{ article.contentBody || article.summary || '暂无内容' }}</text>
        </template>

        <!-- 转载来源（嵌在正文末尾） -->
        <view v-if="article.sourceUrl" class="source-bar dy-clickable" @click="openSource">
          <text class="source-label">{{ article.sourceType === 2 ? '转载自' : '原文链接' }}</text>
          <text class="source-url one-line">{{ article.sourceUrl }}</text>
          <text class="source-arrow">↗</text>
        </view>
      </view>

      <!-- 底部操作栏 -->
      <view class="bottom-bar">
        <view class="action-btn fav-btn dy-clickable" @click="toggleFavorite">
          <text class="action-icon" :class="{ favorited: isFavorited }">{{ isFavorited ? '♥' : '♡' }}</text>
          <text class="action-text">{{ isFavorited ? '已收藏' : '收藏' }}</text>
        </view>
        <view class="action-btn share-btn dy-clickable" @click="onShare">
          <text class="action-icon">↗</text>
          <text class="action-text">分享给客户</text>
        </view>
      </view>
    </template>

    <DyEmpty
      v-else
      text="内容不存在或已下线"
      icon="!"
      color="gray"
    />

    <!-- 海报弹窗 -->
    <view v-if="showPoster" class="poster-mask" @click="showPoster = false">
      <view class="poster-container" @click.stop>
        <image
          v-if="posterImage"
          :src="posterImage"
          mode="widthFix"
          class="poster-image"
          :show-menu-by-longpress="true"
        />
        <view class="poster-actions">
          <view class="poster-save-btn dy-clickable" @click="savePoster">
            <text>💾 保存到相册</text>
          </view>
          <text class="poster-hint">长按图片可保存或转发</text>
        </view>
        <view class="poster-close dy-clickable" @click="showPoster = false">
          <text>✕</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import QRCode from 'qrcode';
import { getContentDetail } from '@/api/content';
import { addFavoriteApi, removeFavoriteApi, getFavoritedCodesApi, TARGET_TYPE } from '@/api/favorite';
import { formatFileUrl } from '@/utils/file';
import type { ContentArticle } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const article = ref<ContentArticle | null>(null);
const loading = ref(true);
const contentCode = ref('');
const isFavorited = ref(false);

// 海报
const showPoster = ref(false);
const posterImage = ref('');
const posterLoading = ref(false);

interface Badge {
  text: string;
  cls: string;
}

const isHtmlBody = computed(() => {
  const body = article.value?.contentBody;
  return !!body && body.trim().startsWith('<');
});

/** 视频地址（type=2，content_body 存 URL 或 JSON {url,desc}） */
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
  const body = article.value.contentBody || '';
  try {
    const parsed = JSON.parse(body);
    return parsed.desc || '';
  } catch {
    return '';
  }
});

/** 图片集（type=3，content_body 存 JSON 数组） */
const galleryImages = computed<string[]>(() => {
  if (article.value?.contentType !== 3) return [];
  try {
    const parsed = JSON.parse(article.value.contentBody || '[]');
    return Array.isArray(parsed) ? parsed.map((s: unknown) => formatFileUrl(String(s))) : [];
  } catch {
    return [];
  }
});

/** 文件信息（type=6，content_body 存 JSON {fileName,fileUrl,fileSize}） */
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

const tagList = computed<string[]>(() => {
  const tags = article.value?.tags;
  if (!tags) return [];
  try {
    const parsed = JSON.parse(tags);
    return Array.isArray(parsed) ? parsed.map(String) : [];
  } catch {
    return tags.split(/[,，]/).map((t) => t.trim()).filter(Boolean);
  }
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

function getBadges(a: ContentArticle): Badge[] {
  const badges: Badge[] = [];
  if (a.isTop === 1) badges.push({ text: '置顶', cls: 'badge-top' });
  if (a.isRecommend === 1) badges.push({ text: '热', cls: 'badge-hot' });
  if (isNew(a.publishTime)) badges.push({ text: '新', cls: 'badge-new' });
  return badges;
}

function isNew(publishTime?: string): boolean {
  if (!publishTime) return false;
  const diff = Date.now() - new Date(publishTime.replace(/-/g, '/')).getTime();
  return diff < 7 * 24 * 60 * 60 * 1000;
}

async function loadDetail() {
  loading.value = true;
  try {
    article.value = await getContentDetail(contentCode.value);
    // 动态导航栏标题
    const titleMap: Record<number, string> = { 1: '文章详情', 2: '视频', 3: '图集', 6: '文件详情' };
    uni.setNavigationBarTitle({ title: titleMap[article.value.contentType ?? 1] || '详情' });
    // 查收藏状态
    const codes = await getFavoritedCodesApi(TARGET_TYPE.CONTENT);
    isFavorited.value = codes.includes(contentCode.value);
  } catch {
    article.value = null;
  } finally {
    loading.value = false;
  }
}

async function toggleFavorite() {
  const was = isFavorited.value;
  isFavorited.value = !was; // 乐观
  try {
    if (was) {
      await removeFavoriteApi(TARGET_TYPE.CONTENT, contentCode.value);
    } else {
      await addFavoriteApi(TARGET_TYPE.CONTENT, contentCode.value);
    }
  } catch {
    isFavorited.value = was; // 回滚
  }
}

/** 分享 — 弹出选择 */
function onShare() {
  uni.showActionSheet({
    itemList: ['📄 生成海报', '🔗 复制链接'],
    success: (res) => {
      if (res.tapIndex === 0) generatePoster();
      else if (res.tapIndex === 1) copyShareLink();
    },
  });
}

/** 转发：复制分享链接 */
function copyShareLink() {
  // #ifdef H5
  const agentCode = (uni.getStorageSync('agent_user') as any)?.accountCode || '';
  const url = `${window.location.origin}/#/pages/share/content?code=${contentCode.value}&agent=${agentCode}`;
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: '链接已复制，可粘贴发给客户', icon: 'none', duration: 2500 }),
  });
  // #endif
}

/** 获取分享链接 */
function getShareUrl(): string {
  // #ifdef H5
  const agentCode = (uni.getStorageSync('agent_user') as any)?.accountCode || '';
  return `${window.location.origin}/#/pages/share/content?code=${contentCode.value}&agent=${agentCode}`;
  // #endif
  // #ifndef H5
  return '';
  // #endif
}

/** 生成海报 */
async function generatePoster() {
  if (posterLoading.value) return;
  posterLoading.value = true;
  uni.showLoading({ title: '生成海报...' });
  try {
    const dataUrl = await drawPoster();
    posterImage.value = dataUrl;
    showPoster.value = true;
  } catch (e) {
    uni.showToast({ title: '海报生成失败', icon: 'none' });
  } finally {
    posterLoading.value = false;
    uni.hideLoading();
  }
}

/** Canvas 绘制海报 */
async function drawPoster(): Promise<string> {
  const W = 600, H = 900;
  const canvas = document.createElement('canvas');
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d')!;

  const a = article.value!;

  // 1. 白色背景
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, W, H);

  let y = 40;

  // 2. 封面图（16:9）
  const coverSrc = formatFileUrl(a.coverImage);
  if (coverSrc) {
    try {
      const img = await loadImage(coverSrc);
      const imgH = Math.min(300, (img.height / img.width) * (W - 80));
      ctx.drawImage(img, 40, y, W - 80, imgH);
      y += imgH + 20;
    } catch {
      y = drawPlaceholderHeader(ctx, W, y);
    }
  } else {
    y = drawPlaceholderHeader(ctx, W, y);
  }

  // 3. 标题（加粗，自动换行）
  ctx.fillStyle = '#303133';
  ctx.font = 'bold 28px sans-serif';
  y = wrapText(ctx, a.title || '', 40, y, W - 80, 40) + 16;

  // 4. 摘要（灰色）
  if (a.summary) {
    ctx.fillStyle = '#909399';
    ctx.font = '22px sans-serif';
    y = wrapText(ctx, a.summary, 40, y, W - 80, 34) + 20;
  }

  // 5. 分割线
  y += 10;
  ctx.strokeStyle = '#ebeef5';
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(40, y);
  ctx.lineTo(W - 40, y);
  ctx.stroke();
  y += 30;

  // 6. 分享人信息（从 localStorage 获取当前代理人）
  const agentUser = uni.getStorageSync('agent_user') as any;
  const agentName = agentUser?.realName || a.authorName || '养老顾问';

  // 头像（圆形）
  const avatarSize = 70;
  const avatarX = 40;
  const avatarY = y;
  const avatarSrc = formatFileUrl(agentUser?.avatar);
  if (avatarSrc) {
    try {
      const avatar = await loadImage(avatarSrc);
      ctx.save();
      ctx.beginPath();
      ctx.arc(avatarX + avatarSize / 2, avatarY + avatarSize / 2, avatarSize / 2, 0, Math.PI * 2);
      ctx.closePath();
      ctx.clip();
      ctx.drawImage(avatar, avatarX, avatarY, avatarSize, avatarSize);
      ctx.restore();
    } catch {
      drawAvatarFallback(ctx, avatarX, avatarY, avatarSize, agentName);
    }
  } else {
    drawAvatarFallback(ctx, avatarX, avatarY, avatarSize, agentName);
  }

  // 姓名 / 电话
  ctx.fillStyle = '#303133';
  ctx.font = 'bold 26px sans-serif';
  ctx.fillText(agentName, avatarX + avatarSize + 20, avatarY + 30);

  ctx.fillStyle = '#909399';
  ctx.font = '20px sans-serif';
  ctx.fillText('专业养老顾问 · 为您服务', avatarX + avatarSize + 20, avatarY + 58);

  y += avatarSize + 40;

  // 7. QR 码
  const qrSize = 120;
  const qrX = W - qrSize - 40;
  const qrY = y;
  try {
    const qrDataUrl = await QRCode.toDataURL(getShareUrl(), { width: qrSize, margin: 1 });
    const qrImg = await loadImage(qrDataUrl);
    ctx.drawImage(qrImg, qrX, qrY, qrSize, qrSize);
  } catch {
    // QR 失败画占位框
    ctx.strokeStyle = '#409eff';
    ctx.strokeRect(qrX, qrY, qrSize, qrSize);
  }

  // QR 提示文字
  ctx.fillStyle = '#606266';
  ctx.font = '18px sans-serif';
  ctx.fillText('长按识别', qrX, qrY + qrSize + 26);
  ctx.fillText('阅读全文', qrX, qrY + qrSize + 50);

  // 8. 底部品牌
  ctx.fillStyle = '#c0c4cc';
  ctx.font = '16px sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('大雁养老 · 专业养老服务平台', W / 2, H - 24);
  ctx.textAlign = 'left';

  return canvas.toDataURL('image/png');
}

/** 加载图片（支持 CORS） */
function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = src;
  });
}

/** Canvas 文字换行 */
function wrapText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, maxWidth: number, lineHeight: number): number {
  const chars = text.split('');
  let line = '';
  let yPos = y;
  for (const char of chars) {
    const testLine = line + char;
    if (ctx.measureText(testLine).width > maxWidth && line.length > 0) {
      ctx.fillText(line, x, yPos);
      line = char;
      yPos += lineHeight;
    } else {
      line = testLine;
    }
  }
  ctx.fillText(line, x, yPos);
  return yPos;
}

/** 无封面时的占位头部 */
function drawPlaceholderHeader(ctx: CanvasRenderingContext2D, W: number, y: number): number {
  const grad = ctx.createLinearGradient(40, y, W - 40, y + 160);
  grad.addColorStop(0, '#337ecc');
  grad.addColorStop(1, '#409eff');
  ctx.fillStyle = grad;
  ctx.fillRect(40, y, W - 80, 160);
  ctx.fillStyle = '#ffffff';
  ctx.font = '24px sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('大雁养老', W / 2, y + 90);
  ctx.textAlign = 'left';
  return y + 180;
}

/** 头像加载失败的文字占位 */
function drawAvatarFallback(ctx: CanvasRenderingContext2D, x: number, y: number, size: number, name: string) {
  ctx.fillStyle = '#409eff';
  ctx.beginPath();
  ctx.arc(x + size / 2, y + size / 2, size / 2, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 28px sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(name.charAt(0) || '?', x + size / 2, y + size / 2);
  ctx.textAlign = 'left';
  ctx.textBaseline = 'alphabetic';
}

/** 保存海报到相册/H5下载 */
function savePoster() {
  // #ifdef H5
  const a = document.createElement('a');
  a.href = posterImage.value;
  a.download = `海报_${article.value?.title?.substring(0, 10) || '文章'}.png`;
  a.click();
  uni.showToast({ title: '海报已保存', icon: 'success' });
  // #endif
}

/** 图片预览（全屏左右滑动） */
function previewImage(index: number) {
  uni.previewImage({ urls: galleryImages.value, current: galleryImages.value[index] });
}

/** 打开文件 */
function openFile() {
  if (!fileInfo.value.url) {
    uni.showToast({ title: '文件地址无效', icon: 'none' });
    return;
  }
  // #ifdef H5
  window.open(fileInfo.value.url, '_blank');
  // #endif
  // #ifndef H5
  uni.downloadFile({
    url: fileInfo.value.url,
    success: (res) => {
      uni.openDocument({
        filePath: res.tempFilePath,
        fail: () => uni.showToast({ title: '无法打开此文件', icon: 'none' }),
      });
    },
    fail: () => uni.showToast({ title: '下载失败', icon: 'none' }),
  });
  // #endif
}

/** 打开转载原文 */
function openSource() {
  const url = article.value?.sourceUrl;
  if (!url) return;
  // #ifdef H5
  window.open(url, '_blank');
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: url,
    success: () => uni.showToast({ title: '链接已复制', icon: 'none' }),
  });
  // #endif
}

onLoad((query) => {
  contentCode.value = query?.code || '';
  if (contentCode.value) {
    loadDetail();
  } else {
    loading.value = false;
  }
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.page {
  padding-bottom: 140rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* ====== 视频置顶 ====== */
.video-top {
  width: 100%;
  background: #000;
}
.video-player-top {
  width: 100%;
  height: 420rpx;
}

/* ====== 信息区 ====== */
.info-section {
  background: $bg-card;
  padding: $spacing-lg $spacing-md $spacing-md;
}
.badge-row {
  display: flex;
  gap: 8rpx;
  margin-bottom: $spacing-sm;
}
.badge {
  display: inline-flex;
  align-items: center;
  font-size: 20rpx;
  line-height: 1;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
  font-weight: 500;
}
.badge-top { background: $brand-error-light; color: $brand-error; }
.badge-hot { background: $brand-warning-light; color: $brand-warning; }
.badge-new { background: $brand-primary-light; color: $brand-primary; }

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
  line-height: 1.5;
}
.meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
}
.author-name {
  font-size: 24rpx;
  color: $brand-primary;
  font-weight: 500;
}
.meta-text {
  font-size: 24rpx;
  color: $text-placeholder;
}
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
}
.tag-chip {
  font-size: 22rpx;
  color: $text-secondary;
  background: $bg-page;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}

/* ====== 正文区（与信息区无缝衔接） ====== */
.body-section {
  background: $bg-card;
  padding: $spacing-md $spacing-md $spacing-lg;
  margin-top: 0;
}
.body-text {
  display: block;
  font-size: 30rpx;
  color: $text-primary;
  line-height: 2;
  white-space: pre-wrap;
}
.gallery-caption {
  margin-top: $spacing-md;
  font-size: 26rpx;
  color: $text-secondary;
}

/* ====== 图集 2列大图 ====== */
.gallery-grid {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}
.gallery-grid-img {
  width: calc((100% - 12rpx) / 2);
  height: 340rpx;
  border-radius: $radius-sm;
  background: $bg-page;
}

/* ====== 文件大号下载卡 ====== */
.file-card-large {
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  overflow: hidden;
}
.file-card-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-lg $spacing-md;
}
.file-icon-big {
  position: relative;
  width: 80rpx;
  height: 80rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $brand-primary-light;
  border-radius: $radius-sm;
}
.file-icon-big-text {
  font-size: 40rpx;
}
.file-ext-tag {
  position: absolute;
  bottom: -6rpx;
  left: 50%;
  transform: translateX(-50%);
  font-size: 16rpx;
  line-height: 1;
  padding: 2rpx 10rpx;
  background: $brand-primary;
  color: #fff;
  border-radius: 4rpx;
}
.file-card-info {
  flex: 1;
  min-width: 0;
}
.file-card-name {
  display: block;
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-card-meta {
  font-size: 24rpx;
  color: $text-placeholder;
  margin-top: 4rpx;
}
.download-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: $spacing-md;
  background: $gradient-blue;
}
.download-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: 500;
}

/* ====== 转载来源 ====== */
.source-bar {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-md;
  padding: $spacing-md;
  background: $brand-primary-light;
  border-radius: $radius-md;
}
.source-label {
  flex-shrink: 0;
  font-size: 24rpx;
  color: $brand-primary;
  font-weight: 500;
}
.source-url {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
  color: $text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.source-arrow {
  flex-shrink: 0;
  font-size: 28rpx;
  color: $brand-primary;
}

/* ====== 底部操作栏 ====== */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: $spacing-md;
  padding: $spacing-md $spacing-lg;
  padding-bottom: calc(#{$spacing-md} + env(safe-area-inset-bottom));
  background: $bg-card;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
  z-index: 100;
}
.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-sm;
  height: 80rpx;
  border-radius: $radius-md;
  font-size: 28rpx;
  font-weight: 500;
}
.fav-btn {
  flex: 1;
  background: $brand-info-light;
  color: $text-regular;
}
.share-btn {
  flex: 2;
  background: $gradient-blue;
  color: #fff;
}
.action-icon {
  font-size: 32rpx;
  color: $text-secondary;
}
.action-icon.favorited {
  color: $brand-error;
}
.share-btn .action-icon {
  color: #fff;
}
.action-text {
  color: inherit;
}

/* 海报弹窗 */
.poster-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx;
}
.poster-container {
  position: relative;
  width: 100%;
  max-width: 580rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.poster-image {
  width: 100%;
  border-radius: 16rpx;
  background: #fff;
}
.poster-actions {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.poster-save-btn {
  background: #fff;
  color: #337ecc;
  padding: 18rpx 56rpx;
  border-radius: 40rpx;
  font-size: 30rpx;
  font-weight: 600;
}
.poster-hint {
  color: rgba(255, 255, 255, 0.7);
  font-size: 24rpx;
}
.poster-close {
  position: absolute;
  top: -60rpx;
  right: 0;
  width: 56rpx;
  height: 56rpx;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32rpx;
}
</style>
