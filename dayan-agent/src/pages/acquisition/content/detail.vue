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

/**
 * Canvas 绘制海报 — 品牌化专业排版。
 *
 * 布局（600×960）：
 *  ┌────────────────────────────┐
 *  │     封面图（全出血 600×340） │  Hero
 *  │     ◐ 渐变过渡到白色         │
 *  ├────────────────────────────┤
 *  │  [分类标签]                 │
 *  │  文章标题（大号加粗）         │
 *  │  摘要（灰色小字）            │
 *  │  ── 品牌色短装饰线           │
 *  ├────────────────────────────┤
 *  │ ┌─── 代理人卡片（圆角背景）─┐│
 *  │ │ ●  姓名 · 职务           ││
 *  │ │    📱 电话号码     ┌───┐ ││
 *  │ │                  │QR │ ││
 *  │ │  长按识别阅读全文  └───┘ ││
 *  └─┴──────────────────────────┴┘
 *  │   养老宝典 · 专业养老服务平台  │  Footer
 *  └────────────────────────────┘
 */
async function drawPoster(): Promise<string> {
  const W = 600, H = 960;
  const PAD = 40;
  const BRAND = '#337ecc';

  const canvas = document.createElement('canvas');
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d')!;

  const a = article.value!;

  // ===== 背景 =====
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, W, H);

  // ===== 1. 封面图（全出血，居中裁切到 600×340） =====
  const COVER_H = 340;
  const coverSrc = formatFileUrl(a.coverImage);
  if (coverSrc) {
    try {
      const img = await loadImage(coverSrc);
      drawImageCover(ctx, img, 0, 0, W, COVER_H);
    } catch {
      drawCoverPlaceholder(ctx, W, COVER_H, BRAND);
    }
  } else {
    drawCoverPlaceholder(ctx, W, COVER_H, BRAND);
  }

  // 封面底部渐变过渡到白色
  const fade = ctx.createLinearGradient(0, COVER_H - 50, 0, COVER_H);
  fade.addColorStop(0, 'rgba(255,255,255,0)');
  fade.addColorStop(1, 'rgba(255,255,255,1)');
  ctx.fillStyle = fade;
  ctx.fillRect(0, COVER_H - 50, W, 50);

  // ===== 2. 分类标签（小药丸） =====
  let y = COVER_H + 20;
  if (a.categoryName) {
    const badgeText = a.categoryName;
    ctx.font = '18px sans-serif';
    const badgeW = ctx.measureText(badgeText).width + 28;
    ctx.fillStyle = BRAND;
    roundRect(ctx, PAD, y, badgeW, 30, 15);
    ctx.fill();
    ctx.fillStyle = '#ffffff';
    ctx.textBaseline = 'middle';
    ctx.fillText(badgeText, PAD + 14, y + 16);
    ctx.textBaseline = 'alphabetic';
    y += 46;
  } else {
    y += 10;
  }

  // ===== 3. 标题（大号加粗） =====
  ctx.fillStyle = '#1a1a2e';
  ctx.font = 'bold 30px sans-serif';
  y = wrapText(ctx, a.title || '', PAD, y + 30, W - PAD * 2, 44, 2) + 14;

  // ===== 4. 摘要（灰色，最多 2 行） =====
  if (a.summary) {
    ctx.fillStyle = '#888888';
    ctx.font = '20px sans-serif';
    y = wrapText(ctx, a.summary, PAD, y + 20, W - PAD * 2, 32, 2) + 10;
  }

  // ===== 5. 装饰短线（品牌色） =====
  y += 16;
  ctx.fillStyle = BRAND;
  roundRect(ctx, PAD, y, 48, 4, 2);
  ctx.fill();

  // ===== 6. 代理人卡片 =====
  const agentUser = uni.getStorageSync('agent_user') as any;
  const agentName = agentUser?.realName || a.authorName || '养老顾问';
  const agentPhone = agentUser?.phone || '';

  const CARD_X = 24, CARD_Y = y + 24, CARD_W = W - 48, CARD_H = 250;
  const CARD_R = 20;

  // 卡片背景 + 阴影
  ctx.shadowColor = 'rgba(0,0,0,0.06)';
  ctx.shadowBlur = 12;
  ctx.shadowOffsetY = 4;
  ctx.fillStyle = '#f7f8fa';
  roundRect(ctx, CARD_X, CARD_Y, CARD_W, CARD_H, CARD_R);
  ctx.fill();
  ctx.shadowColor = 'transparent';
  ctx.shadowBlur = 0;
  ctx.shadowOffsetY = 0;

  // 6a. 头像（圆形）
  const AVATAR = 76;
  const ax = CARD_X + 28;
  const ay = CARD_Y + 28;
  const avatarSrc = formatFileUrl(agentUser?.avatar);
  if (avatarSrc) {
    try {
      const avatar = await loadImage(avatarSrc);
      ctx.save();
      ctx.beginPath();
      ctx.arc(ax + AVATAR / 2, ay + AVATAR / 2, AVATAR / 2, 0, Math.PI * 2);
      ctx.closePath();
      ctx.clip();
      ctx.drawImage(avatar, ax, ay, AVATAR, AVATAR);
      ctx.restore();
    } catch {
      drawAvatarFallback(ctx, ax, ay, AVATAR, agentName, BRAND);
    }
  } else {
    drawAvatarFallback(ctx, ax, ay, AVATAR, agentName, BRAND);
  }

  // 6b. 姓名 + 职务
  const nameX = ax + AVATAR + 20;
  ctx.fillStyle = '#1a1a2e';
  ctx.font = 'bold 27px sans-serif';
  ctx.fillText(agentName, nameX, ay + 32);

  ctx.fillStyle = '#909399';
  ctx.font = '19px sans-serif';
  ctx.fillText('专业养老顾问 · 为您服务', nameX, ay + 60);

  // 6c. 电话
  if (agentPhone) {
    ctx.fillStyle = '#555555';
    ctx.font = '22px sans-serif';
    ctx.fillText('📱  ' + agentPhone, nameX, ay + 95);
  }

  // 6d. 卡内分割线
  const dividerY = CARD_Y + CARD_H - 110;
  ctx.strokeStyle = '#e8eaed';
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(CARD_X + 28, dividerY);
  ctx.lineTo(CARD_X + CARD_W - 28, dividerY);
  ctx.stroke();

  // 6e. QR 码（右对齐）
  const QR = 88;
  const qrX = CARD_X + CARD_W - QR - 28;
  const qrY = dividerY + 12;
  try {
    const qrDataUrl = await QRCode.toDataURL(getShareUrl(), { width: QR, margin: 0 });
    const qrImg = await loadImage(qrDataUrl);
    // 白底框
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(qrX - 4, qrY - 4, QR + 8, QR + 8);
    ctx.drawImage(qrImg, qrX, qrY, QR, QR);
  } catch {
    ctx.strokeStyle = BRAND;
    ctx.lineWidth = 2;
    ctx.strokeRect(qrX, qrY, QR, QR);
  }

  // 6f. QR 左侧提示文字
  ctx.fillStyle = '#1a1a2e';
  ctx.font = 'bold 20px sans-serif';
  ctx.fillText('扫码阅读全文', CARD_X + 28, dividerY + 42);
  ctx.fillStyle = '#999999';
  ctx.font = '17px sans-serif';
  ctx.fillText('了解更多详情', CARD_X + 28, dividerY + 68);

  // ===== 7. 品牌页脚 =====
  ctx.textAlign = 'center';
  ctx.font = 'bold 20px sans-serif';
  const brandText = '养老宝典';
  const subText = ' · 专业养老服务平台';
  const brandW = ctx.measureText(brandText).width;
  ctx.font = '20px sans-serif';
  const subW = ctx.measureText(subText).width;
  const totalW = brandW + subW;
  const startX = (W - totalW) / 2;

  ctx.textAlign = 'left';
  ctx.fillStyle = BRAND;
  ctx.font = 'bold 20px sans-serif';
  ctx.fillText(brandText, startX, H - 36);
  ctx.fillStyle = '#c0c4cc';
  ctx.font = '20px sans-serif';
  ctx.fillText(subText, startX + brandW, H - 36);

  return canvas.toDataURL('image/png');
}

/** 居中裁切绘制图片（类似 CSS object-fit: cover） */
function drawImageCover(ctx: CanvasRenderingContext2D, img: HTMLImageElement, dx: number, dy: number, dw: number, dh: number) {
  const imgRatio = img.width / img.height;
  const destRatio = dw / dh;
  let sx = 0, sy = 0, sw = img.width, sh = img.height;
  if (imgRatio > destRatio) {
    sw = img.height * destRatio;
    sx = (img.width - sw) / 2;
  } else {
    sh = img.width / destRatio;
    sy = (img.height - sh) / 2;
  }
  ctx.drawImage(img, sx, sy, sw, sh, dx, dy, dw, dh);
}

/** 圆角矩形路径（不 fill，由调用方 fill/stroke） */
function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.lineTo(x + w - r, y);
  ctx.arcTo(x + w, y, x + w, y + r, r);
  ctx.lineTo(x + w, y + h - r);
  ctx.arcTo(x + w, y + h, x + w - r, y + h, r);
  ctx.lineTo(x + r, y + h);
  ctx.arcTo(x, y + h, x, y + h - r, r);
  ctx.lineTo(x, y + r);
  ctx.arcTo(x, y, x + r, y, r);
  ctx.closePath();
}

/** 无封面时的品牌渐变占位 */
function drawCoverPlaceholder(ctx: CanvasRenderingContext2D, W: number, H: number, brand: string) {
  const grad = ctx.createLinearGradient(0, 0, W, H);
  grad.addColorStop(0, '#2c6faf');
  grad.addColorStop(0.5, brand);
  grad.addColorStop(1, '#5ba3d8');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, W, H);

  // 装饰圆
  ctx.fillStyle = 'rgba(255,255,255,0.08)';
  ctx.beginPath();
  ctx.arc(W * 0.2, H * 0.3, 80, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.arc(W * 0.85, H * 0.7, 120, 0, Math.PI * 2);
  ctx.fill();

  // 品牌文字
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 36px sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('养老宝典', W / 2, H / 2 - 6);
  ctx.font = '20px sans-serif';
  ctx.fillStyle = 'rgba(255,255,255,0.75)';
  ctx.fillText('专业养老服务平台', W / 2, H / 2 + 28);
  ctx.textAlign = 'left';
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

/** Canvas 文字换行（支持最大行数截断） */
function wrapText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, maxWidth: number, lineHeight: number, maxLines = 99): number {
  const chars = text.split('');
  let line = '';
  let yPos = y;
  let lineCount = 0;
  for (const char of chars) {
    const testLine = line + char;
    if (ctx.measureText(testLine).width > maxWidth && line.length > 0) {
      lineCount++;
      if (lineCount >= maxLines) {
        // 截断加省略号
        let truncated = line;
        while (ctx.measureText(truncated + '…').width > maxWidth && truncated.length > 0) {
          truncated = truncated.slice(0, -1);
        }
        ctx.fillText(truncated + '…', x, yPos);
        return yPos;
      }
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

/** 头像加载失败的文字占位 */
function drawAvatarFallback(ctx: CanvasRenderingContext2D, x: number, y: number, size: number, name: string, brand = '#409eff') {
  ctx.fillStyle = brand;
  ctx.beginPath();
  ctx.arc(x + size / 2, y + size / 2, size / 2, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = '#ffffff';
  ctx.font = `bold ${Math.floor(size * 0.4)}px sans-serif`;
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
