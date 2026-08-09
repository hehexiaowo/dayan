<template>
  <view class="park">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input">
        <text class="search-icon">🔍</text>
        <input
          v-model="keyword"
          class="input"
          placeholder="搜索机构名称 / 区域"
          confirm-type="search"
          @confirm="onSearch"
        />
      </view>
      <view class="region-btn" @click="toggleRegion">
        <text class="region-text">{{ regionLabel }}</text>
        <text class="region-arrow">{{ regionOpen ? '▲' : '▼' }}</text>
      </view>
    </view>

    <!-- 区域筛选下拉 -->
    <view v-if="regionOpen" class="region-panel">
      <view
        class="region-item"
        :class="{ active: !query.region }"
        @click="selectRegion('')"
      >全部</view>
      <view
        v-for="r in regionOptions"
        :key="r"
        class="region-item"
        :class="{ active: query.region === r }"
        @click="selectRegion(r)"
      >{{ r }}</view>
    </view>

    <!-- 机构列表 -->
    <scroll-view scroll-y class="list" :refresher-enabled="true" :refresher-triggered="refreshing" @refresherrefresh="onRefresh">
      <view v-if="list.length" class="cards">
        <view class="card" v-for="p in list" :key="p.parkCode" @click="goDetail(p.parkCode)">
          <image v-if="p.coverImage" class="cover" :src="p.coverImage" mode="aspectFill" />
          <view v-else class="cover cover-ph">
            <text class="cover-text">{{ p.parkName ? p.parkName.charAt(0) : '机构' }}</text>
          </view>
          <view class="info">
            <text class="name">{{ p.parkName }}</text>
            <view class="meta">
              <text v-if="p.ratingScore" class="score">★ {{ p.ratingScore.toFixed(1) }}</text>
              <text v-if="p.startPrice" class="price">¥{{ p.startPrice }}/月起</text>
            </view>
            <view v-if="p.tags && p.tags.length" class="tags">
              <text class="tag" v-for="t in p.tags" :key="t">{{ t }}</text>
            </view>
            <text v-if="p.address" class="addr">{{ p.address }}</text>
          </view>
        </view>
      </view>

      <view v-else class="empty">
        <text class="empty-text">{{ loading ? '加载中...' : '暂无机构，接口待后端提供' }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { onPullDownRefresh } from '@dcloudio/uni-app';
import { getParks, type ParkQuery } from '@/api/park';
import type { Park } from '@/types';

const keyword = ref('');
const query = ref<ParkQuery>({ current: 1, size: 20 });
const list = ref<Park[]>([]);
const loading = ref(false);
const refreshing = ref(false);

const regionOpen = ref(false);
const regionOptions = ['北京', '上海', '广州', '深圳', '杭州', '成都'];
const regionLabel = computed(() => (query.value.region ? query.value.region : '区域'));

function toggleRegion() {
  regionOpen.value = !regionOpen.value;
}
function selectRegion(r: string) {
  query.value.region = r || undefined;
  regionOpen.value = false;
  loadData();
}

function onSearch() {
  query.value.keyword = keyword.value || undefined;
  loadData();
}

async function loadData() {
  loading.value = true;
  try {
    const res = await getParks(query.value);
    list.value = res?.records || [];
  } catch (e) {
    // 后端业务接口未实现，降级为空列表
    list.value = [];
  } finally {
    loading.value = false;
    refreshing.value = false;
  }
}

function onRefresh() {
  refreshing.value = true;
  loadData();
}

// uni-app 原生下拉刷新（pages.json 可启用）
onPullDownRefresh(() => {
  loadData().finally(() => uni.stopPullDownRefresh());
});

function goDetail(code: string) {
  uni.navigateTo({
    url: `/pages/park/detail?id=${code}`,
    fail: () => uni.showToast({ title: '机构详情页待开放', icon: 'none' }),
  });
}

onMounted(loadData);
</script>

<style lang="scss" scoped>
.park {
  min-height: 100vh;
  background: #f5f6f8;
  display: flex;
  flex-direction: column;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: #fff;
}
.search-input {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f5f6f8;
  border-radius: 32rpx;
  padding: 0 24rpx;
  height: 64rpx;
}
.search-icon {
  font-size: 26rpx;
  margin-right: 12rpx;
  opacity: 0.6;
}
.input {
  flex: 1;
  font-size: 26rpx;
}
.region-btn {
  margin-left: 20rpx;
  display: flex;
  align-items: center;
  color: #67C23A;
}
.region-text {
  font-size: 26rpx;
}
.region-arrow {
  font-size: 20rpx;
  margin-left: 6rpx;
}

/* 区域面板 */
.region-panel {
  display: flex;
  flex-wrap: wrap;
  padding: 20rpx 24rpx;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  gap: 16rpx;
}
.region-item {
  padding: 10rpx 28rpx;
  border-radius: 28rpx;
  background: #f5f6f8;
  font-size: 24rpx;
  color: #606266;
  &.active {
    background: #67C23A;
    color: #fff;
  }
}

/* 列表 */
.list {
  flex: 1;
  padding: 20rpx 24rpx;
}
.cards {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  display: flex;
}
.cover {
  width: 200rpx;
  height: 160rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
  background: #f0f0f0;
}
.cover-ph {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #95d475, #67C23A);
}
.cover-text {
  color: #fff;
  font-size: 56rpx;
  font-weight: bold;
}
.info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.name {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}
.meta {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
}
.score {
  color: #ff9900;
  font-size: 26rpx;
  margin-right: 20rpx;
}
.price {
  color: #f56c6c;
  font-size: 26rpx;
  font-weight: bold;
}
.tags {
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
.addr {
  font-size: 24rpx;
  color: #909399;
  margin-top: 8rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 空状态 */
.empty {
  background: #fff;
  border-radius: 16rpx;
  padding: 100rpx 0;
  text-align: center;
}
.empty-text {
  color: #909399;
  font-size: 26rpx;
}
</style>
