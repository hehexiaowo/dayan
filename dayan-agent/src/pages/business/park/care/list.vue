<template>
  <view class="list-page">
    <!-- ECharts 地图区 -->
    <view class="map-section">
      <!-- #ifdef H5 -->
      <div id="care-list-map" class="map-container"></div>
      <!-- #endif -->
      <!-- #ifndef H5 -->
      <view class="map-placeholder">
        <text class="map-placeholder-text">地图组件（仅 H5 支持）</text>
      </view>
      <!-- #endif -->
    </view>

    <!-- 统计卡片 -->
    <view v-if="regions.length" class="stats-card">
      <view class="stat-item">
        <text class="stat-value">{{ regions.length }}</text>
        <text class="stat-label">覆盖省份</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-value">{{ totalParks }}</text>
        <text class="stat-label">护理机构总数</text>
      </view>
    </view>

    <!-- 省份表格 -->
    <view class="table-section">
      <view class="table-header">
        <text class="col-name">省份</text>
        <text class="col-count">机构数</text>
      </view>
      <view
        v-for="item in regions"
        :key="item.code"
        class="table-row dy-clickable"
        @click="onProvinceClick(item)"
      >
        <text class="col-name">{{ item.name }}</text>
        <view class="col-count-wrap">
          <text class="col-count">{{ item.count }}</text>
          <text class="arrow">›</text>
        </view>
      </view>
      <DyEmpty v-if="!loading && !regions.length" text="暂无机构数据" icon="空" color="gray" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { getRegions } from '@/api/park';
import type { RegionItem } from '@/types/park';
import { useRegionHeatmap } from '@/composables/useRegionHeatmap';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const regions = ref<RegionItem[]>([]);
const loading = ref(true);

const totalParks = computed(() => regions.value.reduce((s, i) => s + i.count, 0));

// 热力图：全国省级边界（不显示省名），区域按机构数暖色填色
// layoutCenter 下移：南海撑大 bbox 致大陆偏上，中心点下移到 60% 让大陆居中
const heatmap = useRegionHeatmap({
  containerId: 'care-list-map',
  geoCode: '100000',
  get items() {
    return regions.value;
  },
  showLabels: false,
  layoutCenter: ['50%', '60%'],
  layoutSize: '118%',
  onRegionClick: (r) => navigateToProvince(r.code),
});

async function fetchData() {
  loading.value = true;
  try {
    const result = await getRegions({
      category: 'care',
      level: 'province',
    });
    regions.value = result.items || [];
    await nextTick();
    heatmap.init();
  } catch {
    regions.value = [];
  } finally {
    loading.value = false;
  }
}

function onProvinceClick(item: RegionItem) {
  navigateToProvince(item.code);
}

function navigateToProvince(provinceCode: string) {
  uni.navigateTo({
    url: `/pages/business/park/care/province?provinceCode=${provinceCode}`,
  });
}

onMounted(fetchData);

onUnmounted(() => {
  heatmap.dispose();
});
</script>

<style lang="scss" scoped>

.list-page {
  min-height: 100vh;
  background: $bg-page;
}

/* 地图区 */
.map-section {
  position: relative;
  background: $bg-card;
  border-bottom-left-radius: $radius-lg;
  border-bottom-right-radius: $radius-lg;
  box-shadow: $shadow-card;
  overflow: hidden;
}
.map-container {
  width: 100%;
  height: 300px;
}
.map-placeholder {
  width: 100%;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f0fe, #d4e4fc);
}
.map-placeholder-text {
  font-size: 28rpx;
  color: $text-placeholder;
}

/* 统计卡片 */
.stats-card {
  margin: $spacing-sm $spacing-md 0;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-sm $spacing-md;
  display: flex;
  align-items: center;
  box-shadow: $shadow-card;
}
.stat-item {
  flex: 1;
  text-align: center;
}
.stat-value {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: $brand-primary;
}
.stat-label {
  display: block;
  font-size: 22rpx;
  color: $text-secondary;
  margin-top: 4rpx;
}
.stat-divider {
  width: 1rpx;
  height: 48rpx;
  background: $border-base;
}

/* 省份表格 */
.table-section {
  padding: $spacing-md;
}
.table-header {
  display: flex;
  justify-content: space-between;
  padding: $spacing-sm $spacing-md;
  font-size: 24rpx;
  color: $text-secondary;
  border-bottom: 1rpx solid $border-base;
}
.table-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx $spacing-md;
  background: $bg-card;
  border-radius: $radius-md;
  margin-top: $spacing-sm;
  box-shadow: $shadow-card;
}
.col-name {
  flex: 1;
  font-size: 30rpx;
  color: $text-primary;
  font-weight: 500;
}
.table-header .col-name {
  font-weight: normal;
}
.col-count-wrap {
  display: flex;
  align-items: center;
}
.col-count {
  font-size: 28rpx;
  color: $brand-primary;
  font-weight: bold;
}
.arrow {
  font-size: 36rpx;
  color: $text-placeholder;
  margin-left: $spacing-sm;
}
</style>
