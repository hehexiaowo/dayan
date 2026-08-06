<template>
  <view class="home">
    <!-- 顶部欢迎 + banner -->
    <view class="header">
      <view class="welcome">
        <text class="greeting">{{ greeting }}</text>
        <text class="name">{{ displayName }}</text>
      </view>
      <view class="banner">
        <swiper v-if="banners.length" class="banner-swiper" circular autoplay :interval="4000" :duration="500">
          <swiper-item v-for="b in banners" :key="b.bannerId" @click="onBanner(b)">
            <image class="banner-img" :src="b.imageUrl" mode="aspectFill" />
          </swiper-item>
        </swiper>
        <view v-else class="banner banner-placeholder">
          <view class="banner-logo">大雁</view>
          <text class="banner-text">大雁养老 · 安心托付</text>
        </view>
      </view>
    </view>

    <!-- 功能宫格 -->
    <view class="grid">
      <view class="grid-item" @click="goGrid('park')">
        <view class="grid-icon icon-park">构</view>
        <text class="grid-label">找机构</text>
      </view>
      <view class="grid-item" @click="goGrid('service')">
        <view class="grid-icon icon-service">务</view>
        <text class="grid-label">服务</text>
      </view>
      <view class="grid-item" @click="goGrid('equity')">
        <view class="grid-icon icon-equity">益</view>
        <text class="grid-label">我的权益</text>
      </view>
      <view class="grid-item" @click="goGrid('order')">
        <view class="grid-icon icon-order">单</view>
        <text class="grid-label">我的订单</text>
      </view>
    </view>

    <!-- 推荐机构 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">为你推荐</text>
        <text class="section-more" @click="goGrid('park')">更多 ></text>
      </view>

      <view v-if="recommend.length" class="rec-list">
        <view class="rec-card" v-for="p in recommend" :key="p.parkCode" @click="goParkDetail(p.parkCode)">
          <image v-if="p.coverImage" class="rec-cover" :src="p.coverImage" mode="aspectFill" />
          <view v-else class="rec-cover rec-cover-ph">
            <text class="rec-cover-text">{{ p.parkName ? p.parkName.charAt(0) : '机构' }}</text>
          </view>
          <view class="rec-info">
            <text class="rec-name">{{ p.parkName }}</text>
            <view class="rec-meta">
              <text v-if="p.ratingScore" class="rec-score">★ {{ p.ratingScore.toFixed(1) }}</text>
              <text v-if="p.startPrice" class="rec-price">¥{{ p.startPrice }}/月起</text>
            </view>
            <text v-if="p.address" class="rec-addr">{{ p.address }}</text>
            <view v-if="p.tags && p.tags.length" class="rec-tags">
              <text class="tag" v-for="t in p.tags" :key="t">{{ t }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty">
        <text class="empty-text">{{ loading ? '加载中...' : '暂无推荐，接口待后端提供' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/stores/user';
import { getBanners, getRecommend } from '@/api/home';
import type { Banner, Park } from '@/types';

const userStore = useUserStore();
const banners = ref<Banner[]>([]);
const recommend = ref<Park[]>([]);
const loading = ref(false);

const displayName = computed(() => userStore.userInfo.realName || '尊贵客户');
const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 6) return '夜深了，';
  if (h < 12) return '早上好，';
  if (h < 14) return '中午好，';
  if (h < 18) return '下午好，';
  return '晚上好，';
});

async function loadData() {
  loading.value = true;
  // 后端业务接口未实现，降级为空状态
  try {
    banners.value = await getBanners();
  } catch (e) {
    banners.value = [];
  }
  try {
    const list = await getRecommend();
    recommend.value = Array.isArray(list) ? list : [];
  } catch (e) {
    recommend.value = [];
  } finally {
    loading.value = false;
  }
}

function onBanner(b: Banner) {
  if (b.linkUrl) {
    uni.navigateTo({ url: b.linkUrl }).catch(() => {});
  }
}

function goGrid(type: string) {
  if (type === 'park') {
    uni.switchTab({ url: '/pages/park/index' });
  } else if (type === 'service') {
    uni.switchTab({ url: '/pages/service/index' });
  } else if (type === 'equity') {
    uni.showToast({ title: '我的权益（待开发）', icon: 'none' });
  } else if (type === 'order') {
    uni.showToast({ title: '我的订单（待开发）', icon: 'none' });
  }
}

function goParkDetail(code: string) {
  uni.navigateTo({
    url: `/pages/park/detail?id=${code}`,
    fail: () => uni.showToast({ title: '机构详情页待开放', icon: 'none' }),
  });
}

onMounted(loadData);
onShow(() => {
  // Tab 页每次展示时如未加载则加载
  if (banners.value.length === 0 && recommend.value.length === 0) {
    loadData();
  }
});
</script>

<style lang="scss" scoped>
.home {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 30rpx;
}

/* 顶部 */
.header {
  background: linear-gradient(135deg, #67C23A 0%, #4eaf2a 100%);
  padding: 40rpx 30rpx 50rpx;
}
.welcome {
  margin-bottom: 30rpx;
  .greeting,
  .name {
    color: #fff;
    font-size: 36rpx;
  }
  .name {
    font-weight: bold;
    margin-left: 8rpx;
  }
}
.banner {
  height: 260rpx;
  border-radius: 16rpx;
  overflow: hidden;
  background: #fff;
}
.banner-swiper,
.banner-img {
  width: 100%;
  height: 260rpx;
}
.banner-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  .banner-logo {
    width: 100rpx;
    height: 100rpx;
    border-radius: 50%;
    background: rgba(103, 194, 58, 0.15);
    color: #67C23A;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 30rpx;
    font-weight: bold;
  }
  .banner-text {
    margin-top: 16rpx;
    color: #67C23A;
    font-size: 26rpx;
    font-weight: bold;
  }
}

/* 宫格 */
.grid {
  display: flex;
  flex-wrap: wrap;
  background: #fff;
  margin: -30rpx 24rpx 0;
  border-radius: 16rpx;
  padding: 30rpx 0;
  position: relative;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}
.grid-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.grid-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 14rpx;
}
.icon-park {
  background: #67C23A;
}
.icon-service {
  background: #e6a23c;
}
.icon-equity {
  background: #409eff;
}
.icon-order {
  background: #f56c6c;
}
.grid-label {
  font-size: 26rpx;
  color: #303133;
}

/* 推荐区 */
.section {
  margin: 30rpx 24rpx 0;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
}
.section-more {
  font-size: 26rpx;
  color: #67C23A;
}
.rec-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.rec-card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  display: flex;
  padding: 20rpx;
}
.rec-cover {
  width: 200rpx;
  height: 160rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  background: #f0f0f0;
}
.rec-cover-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #95d475, #67C23A);
}
.rec-cover-text {
  color: #fff;
  font-size: 56rpx;
  font-weight: bold;
}
.rec-info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}
.rec-name {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}
.rec-meta {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
}
.rec-score {
  color: #ff9900;
  font-size: 26rpx;
  margin-right: 20rpx;
}
.rec-price {
  color: #f56c6c;
  font-size: 26rpx;
  font-weight: bold;
}
.rec-addr {
  font-size: 24rpx;
  color: #909399;
  margin-top: 8rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.rec-tags {
  margin-top: 8rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}
.tag {
  font-size: 20rpx;
  color: #67C23A;
  border: 1px solid #67C23A;
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
}

/* 空状态 */
.empty {
  background: #fff;
  border-radius: 16rpx;
  padding: 80rpx 0;
  text-align: center;
}
.empty-text {
  color: #909399;
  font-size: 26rpx;
}
</style>
