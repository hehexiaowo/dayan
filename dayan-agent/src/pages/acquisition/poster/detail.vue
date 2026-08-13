<template>
  <view class="page dy-safe-bottom">
    <template v-if="loading">
      <DySkeleton :rows="8" card />
    </template>

    <template v-else-if="poster">
      <!-- 封面图 -->
      <view v-if="coverUrl" class="cover-section">
        <image
          :src="coverUrl"
          mode="widthFix"
          class="cover-image"
        />
      </view>

      <!-- 内容区 -->
      <view class="content-section">
        <view v-if="poster.categoryName" class="badge-row">
          <text class="badge">{{ poster.categoryName }}</text>
        </view>
        <text class="title">{{ poster.title }}</text>
        <text v-if="poster.subtitle" class="subtitle">{{ poster.subtitle }}</text>
        <view class="divider" />
        <text class="body-text">{{ poster.bodyText }}</text>
      </view>

      <!-- 代理人名片 -->
      <view class="agent-card">
        <view class="agent-card-header">
          <view class="agent-avatar">
            <text class="agent-avatar-text">{{ agentName.charAt(0) }}</text>
          </view>
          <view class="agent-info">
            <text class="agent-name">{{ agentName }}</text>
            <text class="agent-title">专业养老顾问 · 为您服务</text>
          </view>
        </view>
        <view class="agent-contact">
          <view v-if="agentPhone" class="contact-btn contact-phone dy-clickable" @click="onCall">
            <text>📞 拨打电话</text>
          </view>
          <view class="contact-btn contact-wechat dy-clickable" @click="onCopyWechat">
            <text>💬 复制微信</text>
          </view>
        </view>
      </view>

      <!-- 品牌页脚 -->
      <view class="brand-footer">
        <text>大雁养老 · 专业养老服务平台</text>
      </view>
    </template>

    <DyEmpty v-else text="海报模板不存在或已下线" icon="!" color="gray" />

    <!-- 底部操作栏 -->
    <view v-if="poster" class="bottom-bar">
      <view class="action-btn share-btn dy-clickable" @click="onShare">
        <text class="action-icon">↗</text>
        <text class="action-text">分享给客户</text>
      </view>
    </view>

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
import { getPosterDetail } from '@/api/poster';
import type { PosterTemplate } from '@/api/poster';
import { formatFileUrl } from '@/utils/file';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const poster = ref<PosterTemplate | null>(null);
const loading = ref(true);
const templateCode = ref('');
const showPoster = ref(false);
const posterImage = ref('');
const posterLoading = ref(false);

const coverUrl = computed(() => poster.value?.coverImage ? formatFileUrl(poster.value.coverImage) : '');

const agentUser = computed(() => uni.getStorageSync('agent_user') as any);
const agentName = computed(() => agentUser.value?.realName || '养老顾问');
const agentPhone = computed(() => agentUser.value?.phone || '');

async function loadDetail() {
  loading.value = true;
  try {
    poster.value = await getPosterDetail(templateCode.value);
  } catch {
    poster.value = null;
  } finally {
    loading.value = false;
  }
}

function onShare() {
  uni.showActionSheet({
    itemList: ['📄 生成海报', '🔗 复制链接'],
    success: (res) => {
      if (res.tapIndex === 0) generatePoster();
      else if (res.tapIndex === 1) copyShareLink();
    },
  });
}

function getShareUrl(): string {
  // #ifdef H5
  const agentCode = agentUser.value?.accountCode || '';
  return `${window.location.origin}/#/pages/share/poster?code=${templateCode.value}&agent=${agentCode}`;
  // #endif
  // #ifndef H5
  return '';
  // #endif
}

function copyShareLink() {
  // #ifdef H5
  uni.setClipboardData({
    data: getShareUrl(),
    success: () => uni.showToast({ title: '链接已复制，可粘贴发给客户', icon: 'none', duration: 2500 }),
  });
  // #endif
}

async function generatePoster() {
  if (posterLoading.value) return;
  posterLoading.value = true;
  uni.showLoading({ title: '生成海报...' });
  try {
    const dataUrl = await drawPoster();
    posterImage.value = dataUrl;
    showPoster.value = true;
  } catch {
    uni.showToast({ title: '海报生成失败', icon: 'none' });
  } finally {
    posterLoading.value = false;
    uni.hideLoading();
  }
}

async function drawPoster(): Promise<string> {
  const W = 600, H = 960;
  const PAD = 40;
  const BRAND = '#337ecc';
  const canvas = document.createElement('canvas');
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d')!;
  const p = poster.value!;

  // 背景
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, W, H);

  // 1. 封面图（全出血）
  const COVER_H = 340;
  const coverSrc = coverUrl.value;
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

  // 封面底部渐变
  const fade = ctx.createLinearGradient(0, COVER_H - 50, 0, COVER_H);
  fade.addColorStop(0, 'rgba(255,255,255,0)');
  fade.addColorStop(1, 'rgba(255,255,255,1)');
  ctx.fillStyle = fade;
  ctx.fillRect(0, COVER_H - 50, W, 50);

  // 2. 分类标签
  let y = COVER_H + 20;
  if (p.categoryName) {
    ctx.font = '18px sans-serif';
    const badgeW = ctx.measureText(p.categoryName).width + 28;
    ctx.fillStyle = BRAND;
    roundRect(ctx, PAD, y, badgeW, 30, 15);
    ctx.fill();
    ctx.fillStyle = '#ffffff';
    ctx.textBaseline = 'middle';
    ctx.fillText(p.categoryName, PAD + 14, y + 16);
    ctx.textBaseline = 'alphabetic';
    y += 46;
  }

  // 3. 标题
  ctx.fillStyle = '#1a1a2e';
  ctx.font = 'bold 30px sans-serif';
  y = wrapText(ctx, p.title, PAD, y + 30, W - PAD * 2, 44, 2) + 8;

  // 4. 副标题
  if (p.subtitle) {
    ctx.fillStyle = BRAND;
    ctx.font = '22px sans-serif';
    y = wrapText(ctx, p.subtitle, PAD, y + 16, W - PAD * 2, 34, 1) + 14;
  }

  // 5. 正文（前6行）
  if (p.bodyText) {
    ctx.fillStyle = '#555555';
    ctx.font = '20px sans-serif';
    y = wrapText(ctx, p.bodyText.replace(/\\n/g, '\n'), PAD, y + 10, W - PAD * 2, 32, 6) + 10;
  }

  // 6. 装饰短线
  y += 12;
  ctx.fillStyle = BRAND;
  roundRect(ctx, PAD, y, 48, 4, 2);
  ctx.fill();

  // 7. 代理人卡片
  const CARD_X = 24, CARD_W = W - 48, CARD_H = 250;
  const CARD_Y = y + 24, CARD_R = 20;

  ctx.shadowColor = 'rgba(0,0,0,0.06)';
  ctx.shadowBlur = 12;
  ctx.shadowOffsetY = 4;
  ctx.fillStyle = '#f7f8fa';
  roundRect(ctx, CARD_X, CARD_Y, CARD_W, CARD_H, CARD_R);
  ctx.fill();
  ctx.shadowColor = 'transparent';
  ctx.shadowBlur = 0;
  ctx.shadowOffsetY = 0;

  // 头像
  const AVATAR = 76;
  const ax = CARD_X + 28;
  const ay = CARD_Y + 28;
  ctx.fillStyle = BRAND;
  ctx.beginPath();
  ctx.arc(ax + AVATAR / 2, ay + AVATAR / 2, AVATAR / 2, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = '#ffffff';
  ctx.font = `bold ${Math.floor(AVATAR * 0.4)}px sans-serif`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(agentName.value.charAt(0), ax + AVATAR / 2, ay + AVATAR / 2);
  ctx.textAlign = 'left';
  ctx.textBaseline = 'alphabetic';

  // 姓名 + 职务
  const nameX = ax + AVATAR + 20;
  ctx.fillStyle = '#1a1a2e';
  ctx.font = 'bold 27px sans-serif';
  ctx.fillText(agentName.value, nameX, ay + 32);
  ctx.fillStyle = '#909399';
  ctx.font = '19px sans-serif';
  ctx.fillText('专业养老顾问 · 为您服务', nameX, ay + 60);

  // 电话
  if (agentPhone.value) {
    ctx.fillStyle = '#555555';
    ctx.font = '22px sans-serif';
    ctx.fillText('📱  ' + agentPhone.value, nameX, ay + 95);
  }

  // 卡内分割线
  const dividerY = CARD_Y + CARD_H - 110;
  ctx.strokeStyle = '#e8eaed';
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(CARD_X + 28, dividerY);
  ctx.lineTo(CARD_X + CARD_W - 28, dividerY);
  ctx.stroke();

  // QR 码
  const QR = 88;
  const qrX = CARD_X + CARD_W - QR - 28;
  const qrY = dividerY + 12;
  try {
    const qrDataUrl = await QRCode.toDataURL(getShareUrl(), { width: QR, margin: 0 });
    const qrImg = await loadImage(qrDataUrl);
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(qrX - 4, qrY - 4, QR + 8, QR + 8);
    ctx.drawImage(qrImg, qrX, qrY, QR, QR);
  } catch {
    ctx.strokeStyle = BRAND;
    ctx.lineWidth = 2;
    ctx.strokeRect(qrX, qrY, QR, QR);
  }

  ctx.fillStyle = '#1a1a2e';
  ctx.font = 'bold 20px sans-serif';
  ctx.fillText('扫码查看详情', CARD_X + 28, dividerY + 42);
  ctx.fillStyle = '#999999';
  ctx.font = '17px sans-serif';
  ctx.fillText('了解更多内容', CARD_X + 28, dividerY + 68);

  // 8. 品牌页脚
  ctx.textAlign = 'center';
  ctx.font = 'bold 20px sans-serif';
  ctx.fillStyle = BRAND;
  ctx.fillText('大雁养老', W / 2 - 50, H - 36);
  ctx.font = '20px sans-serif';
  ctx.fillStyle = '#c0c4cc';
  ctx.fillText(' · 专业养老服务平台', W / 2 + 14, H - 36);
  ctx.textAlign = 'left';

  return canvas.toDataURL('image/png');
}

// ===== Canvas 辅助函数 =====
function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = src;
  });
}

function wrapText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, maxWidth: number, lineHeight: number, maxLines = 99): number {
  const paragraphs = text.split('\n');
  let yPos = y;
  let lineCount = 0;
  for (const para of paragraphs) {
    if (lineCount >= maxLines) break;
    const chars = para.split('');
    let line = '';
    for (const char of chars) {
      const testLine = line + char;
      if (ctx.measureText(testLine).width > maxWidth && line.length > 0) {
        lineCount++;
        if (lineCount >= maxLines) {
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
    if (line) {
      lineCount++;
      if (lineCount > maxLines) break;
      ctx.fillText(line, x, yPos);
      yPos += lineHeight;
    }
  }
  return yPos;
}

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

function drawCoverPlaceholder(ctx: CanvasRenderingContext2D, W: number, H: number, brand: string) {
  const grad = ctx.createLinearGradient(0, 0, W, H);
  grad.addColorStop(0, '#2c6faf');
  grad.addColorStop(0.5, brand);
  grad.addColorStop(1, '#5ba3d8');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, W, H);
  ctx.fillStyle = 'rgba(255,255,255,0.08)';
  ctx.beginPath();
  ctx.arc(W * 0.2, H * 0.3, 80, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.arc(W * 0.85, H * 0.7, 120, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 36px sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('大雁养老', W / 2, H / 2 - 6);
  ctx.font = '20px sans-serif';
  ctx.fillStyle = 'rgba(255,255,255,0.75)';
  ctx.fillText('专业养老服务平台', W / 2, H / 2 + 28);
  ctx.textAlign = 'left';
}

function onCall() {
  if (agentPhone.value) {
    uni.makePhoneCall({ phoneNumber: agentPhone.value });
  }
}

function onCopyWechat() {
  const wechat = agentUser.value?.wechat || agentUser.value?.username || '';
  if (wechat) {
    uni.setClipboardData({ data: wechat, success: () => uni.showToast({ title: '微信号已复制', icon: 'none' }) });
  } else {
    uni.showToast({ title: '暂无微信号', icon: 'none' });
  }
}

function savePoster() {
  // #ifdef H5
  const a = document.createElement('a');
  a.href = posterImage.value;
  a.download = `海报_${poster.value?.title?.substring(0, 10) || '模板'}.png`;
  a.click();
  uni.showToast({ title: '海报已保存', icon: 'success' });
  // #endif
}

onLoad((opts) => {
  templateCode.value = opts?.code || '';
  if (templateCode.value) loadDetail();
});
</script>

<style lang="scss" scoped>

.page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 140rpx;
}

/* 封面 */
.cover-section {
  width: 100%;
}
.cover-image {
  width: 100%;
}

/* 内容区 */
.content-section {
  background: $bg-card;
  margin: -$spacing-sm $spacing-md 0;
  border-radius: $radius-lg $radius-lg 0 0;
  padding: $spacing-lg $spacing-md;
  position: relative;
  z-index: 1;
}
.badge-row {
  margin-bottom: $spacing-sm;
}
.badge {
  display: inline-block;
  background: $brand-primary-light;
  color: $brand-primary;
  font-size: 22rpx;
  padding: 4rpx 20rpx;
  border-radius: 20rpx;
}
.title {
  font-size: 36rpx;
  font-weight: bold;
  color: $text-primary;
  line-height: 1.4;
  display: block;
}
.subtitle {
  margin-top: $spacing-xs;
  font-size: 26rpx;
  color: $brand-primary;
  display: block;
}
.divider {
  height: 1rpx;
  background: $border-base;
  margin: $spacing-md 0;
}
.body-text {
  font-size: 28rpx;
  color: $text-regular;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* 代理人名片 */
.agent-card {
  background: $bg-card;
  margin: $spacing-sm $spacing-md;
  border-radius: $radius-lg;
  padding: $spacing-lg $spacing-md;
  box-shadow: $shadow-card;
  border-top: 4rpx solid $brand-primary;
}
.agent-card-header {
  display: flex;
  align-items: center;
}
.agent-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: $gradient-blue;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.agent-avatar-text {
  color: #fff;
  font-size: 40rpx;
  font-weight: bold;
}
.agent-info {
  margin-left: $spacing-md;
  display: flex;
  flex-direction: column;
}
.agent-name {
  font-size: 34rpx;
  font-weight: bold;
  color: $text-primary;
}
.agent-title {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: $text-secondary;
}
.agent-contact {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-md;
}
.contact-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: $control-height;
  border-radius: $radius-md;
  font-size: 28rpx;
}
.contact-phone {
  background: $brand-primary;
  color: #fff;
}
.contact-wechat {
  background: $brand-success-light;
  color: $brand-success;
}

/* 品牌页脚 */
.brand-footer {
  text-align: center;
  padding: $spacing-md;
  font-size: 24rpx;
  color: $text-placeholder;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $bg-card;
  padding: $spacing-sm $spacing-lg;
  display: flex;
  gap: $spacing-sm;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
  z-index: 100;
}
.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  height: $control-height;
  border-radius: $radius-md;
  font-size: 28rpx;
}
.share-btn {
  background: $gradient-blue;
  color: #fff;
}
.action-icon {
  font-size: 32rpx;
  color: inherit;
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
