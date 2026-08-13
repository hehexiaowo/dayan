<template>
  <view class="page dy-safe-bottom">
    <template v-if="loading">
      <DySkeleton :rows="8" card />
    </template>

    <template v-else-if="poster">
      <!-- 封面图 -->
      <view v-if="coverUrl" class="cover-section">
        <image :src="coverUrl" mode="widthFix" class="cover-image" />
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

      <!-- 留资卡片 -->
      <DyContactForm />

      <!-- 分享人名片 -->
      <view v-if="card" class="agent-card">
        <view class="agent-card-header">
          <view v-if="card.avatar" class="agent-avatar">
            <image :src="formatFileUrl(card.avatar)" mode="aspectFill" class="avatar-img" />
          </view>
          <view v-else class="agent-avatar">
            <text class="agent-avatar-text">{{ (card.displayName || '顾问').charAt(0) }}</text>
          </view>
          <view class="agent-info">
            <text class="agent-name">{{ card.displayName || '养老顾问' }}</text>
            <text v-if="card.title" class="agent-job">{{ card.title }}</text>
            <text v-if="card.company" class="agent-company">{{ card.company }}</text>
          </view>
        </view>
        <view v-if="card.intro" class="agent-intro">{{ card.intro }}</view>
        <view class="agent-contact">
          <view v-if="card.phone" class="contact-btn contact-phone dy-clickable" @click="onCall(card.phone)">
            <text>📞 {{ card.phone }}</text>
          </view>
          <view v-if="card.wechat" class="contact-btn contact-wechat dy-clickable" @click="onCopyWechat(card.wechat)">
            <text>💬 复制微信</text>
          </view>
        </view>
      </view>

      <!-- 品牌页脚 -->
      <view class="brand-footer">
        <text>大雁养老 · 专业养老服务平台</text>
      </view>
    </template>

    <DyEmpty v-else text="内容不存在或已下线" icon="!" color="gray" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getSharePoster, trackShare } from '@/api/share';
import { formatFileUrl } from '@/utils/file';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';
import DyContactForm from '@/components/DyContactForm/DyContactForm.vue';

const poster = ref<any>(null);
const card = ref<any>(null);
const loading = ref(true);

const coverUrl = computed(() => poster.value?.coverImage ? formatFileUrl(poster.value.coverImage) : '');

async function loadData(code: string, agent: string) {
  loading.value = true;
  try {
    const res = await getSharePoster(code, agent);
    poster.value = res.poster;
    card.value = res.card;
  } catch {
    poster.value = null;
  } finally {
    loading.value = false;
  }
}

function onCall(phone: string) {
  uni.makePhoneCall({ phoneNumber: phone });
}

function onCopyWechat(wechat: string) {
  uni.setClipboardData({ data: wechat, success: () => uni.showToast({ title: '微信号已复制', icon: 'none' }) });
}

onLoad((opts) => {
  const code = opts?.code || '';
  const agent = opts?.agent || '';
  if (code) {
    loadData(code, agent);
    // 追踪访客打开（异步，不阻塞渲染）
    if (agent) {
      trackShare({
        agentCode: agent,
        shareType: 3,
        bizCode: code,
        bizTitle: '海报分享',
      });
    }
  }
});
</script>

<style lang="scss" scoped>

.page {
  min-height: 100vh;
  background: $bg-page;
}

.cover-section { width: 100%; }
.cover-image { width: 100%; }

.content-section {
  background: $bg-card;
  margin: -$spacing-sm $spacing-md 0;
  border-radius: $radius-lg $radius-lg 0 0;
  padding: $spacing-lg $spacing-md;
  position: relative;
  z-index: 1;
}
.badge-row { margin-bottom: $spacing-sm; }
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

/* 名片 */
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
  overflow: hidden;
}
.avatar-img { width: 100%; height: 100%; }
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
.agent-job {
  margin-top: 4rpx;
  font-size: 24rpx;
  color: $brand-primary;
}
.agent-company {
  margin-top: 2rpx;
  font-size: 22rpx;
  color: $text-secondary;
}
.agent-intro {
  margin-top: $spacing-sm;
  font-size: 24rpx;
  color: $text-regular;
  line-height: 1.6;
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
.contact-phone { background: $brand-primary; color: #fff; }
.contact-wechat { background: $brand-success-light; color: $brand-success; }

.brand-footer {
  text-align: center;
  padding: $spacing-md;
  font-size: 24rpx;
  color: $text-placeholder;
}
</style>
