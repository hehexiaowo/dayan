<template>
  <view class="city-page">
    <!-- 位置指示器 -->
    <view class="location-bar">
      <text class="location-text">活力长居 · {{ cityName || '选择区县' }}</text>
    </view>

    <!-- ECharts 地图区 -->
    <view class="map-section">
      <!-- #ifdef H5 -->
      <div id="vital-city-map" class="map-container"></div>
      <!-- #endif -->
      <!-- #ifndef H5 -->
      <view class="map-placeholder">
        <text class="map-placeholder-text">地图组件（仅 H5 支持）</text>
      </view>
      <!-- #endif -->
    </view>

    <!-- 统计卡片 -->
    <view v-if="districts.length" class="stats-card">
      <view class="stat-item">
        <text class="stat-value">{{ districts.length }}</text>
        <text class="stat-label">覆盖区县</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-value">{{ totalParks }}</text>
        <text class="stat-label">机构总数</text>
      </view>
    </view>

    <!-- 加载骨架 -->
    <view v-if="loading" class="district-list">
      <DySkeleton v-for="i in 3" :key="i" :rows="1" card />
    </view>

    <template v-else>
      <!-- 区县列表 -->
      <view class="district-list">
        <view
          v-for="item in districts"
          :key="item.code"
          class="district-card dy-clickable"
          @click="onDistrictClick(item)"
        >
          <view class="district-info">
            <text class="district-name">{{ item.name }}</text>
            <text class="district-count">{{ item.count }} 家机构</text>
          </view>
          <view class="district-right">
            <text class="arrow">›</text>
          </view>
        </view>
        <DyEmpty v-if="!districts.length" text="该城市暂无机构" icon="空" color="gray" />
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getRegions } from '@/api/park';
import type { RegionItem } from '@/types/park';
import { useRegionHeatmap } from '@/composables/useRegionHeatmap';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const provinceCode = ref('');
const cityCode = ref('');
const cityName = ref('');
const districts = ref<RegionItem[]>([]);
const loading = ref(true);

const totalParks = computed(() => districts.value.reduce((s, i) => s + i.count, 0));

// 热力图：城市区县边界（DataV {cityCode}_full），显示区县名，按机构数暖色填色
const heatmap = useRegionHeatmap({
  containerId: 'vital-city-map',
  get geoCode() {
    return cityCode.value;
  },
  get items() {
    return districts.value;
  },
  showLabels: true,
  onRegionClick: (r) => navigateToDistrict(r.code),
});

async function fetchData() {
  loading.value = true;
  try {
    const result = await getRegions({
      category: 'vital',
      level: 'district',
      provinceCode: provinceCode.value,
      cityCode: cityCode.value,
    });
    districts.value = result.items || [];
    await nextTick();
    heatmap.init();
  } catch {
    districts.value = [];
  } finally {
    loading.value = false;
  }
}

function navigateToDistrict(districtCode: string) {
  uni.navigateTo({
    url: `/pages/business/park/vital/district?category=vital&provinceCode=${provinceCode.value}&cityCode=${cityCode.value}&districtCode=${districtCode}`,
  });
}

function onDistrictClick(item: RegionItem) {
  navigateToDistrict(item.code);
}

onLoad((options: { provinceCode?: string; cityCode?: string; cityName?: string }) => {
  if (options?.provinceCode) provinceCode.value = options.provinceCode;
  if (options?.cityCode) cityCode.value = options.cityCode;
  if (options?.cityName) cityName.value = decodeURIComponent(options.cityName);
});

onMounted(fetchData);

onUnmounted(() => {
  heatmap.dispose();
});
</script>

<style lang="scss" scoped>

.city-page {
  min-height: 100vh;
  background: $bg-page;
}

/* 位置指示器 */
.location-bar {
  background: $bg-card;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1rpx solid $border-light;
}
.location-text {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}

/* 地图区 */
.map-section {
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

/* 区县列表 */
.district-list {
  padding: $spacing-sm $spacing-md;
}
.district-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-lg;
  background: $bg-card;
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}
.district-info {
  flex: 1;
}
.district-name {
  display: block;
  font-size: 32rpx;
  color: $text-primary;
  font-weight: 500;
}
.district-count {
  display: block;
  font-size: 24rpx;
  color: $brand-primary;
  margin-top: $spacing-xs;
}
.district-right {
  flex-shrink: 0;
}
.arrow {
  font-size: 40rpx;
  color: $text-placeholder;
}
</style>
