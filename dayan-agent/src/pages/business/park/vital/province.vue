<template>
  <view class="province-page">
    <!-- ECharts 地图区 -->
    <view class="map-section">
      <!-- #ifdef H5 -->
      <div id="vital-province-map" class="map-container"></div>
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
        <text class="stat-label">{{ isMuni ? '覆盖区域' : '覆盖城市' }}</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-value">{{ totalParks }}</text>
        <text class="stat-label">养老机构总数</text>
      </view>
    </view>

    <!-- 城市/区表格 -->
    <view class="table-section">
      <view class="table-header">
        <text class="col-name">{{ isMuni ? '区域' : '城市' }}</text>
        <text class="col-count">机构数</text>
      </view>
      <view
        v-for="item in regions"
        :key="item.code"
        class="table-row dy-clickable"
        @click="onItemClick(item)"
      >
        <text class="col-name">{{ item.name }}</text>
        <view class="col-count-wrap">
          <text class="col-count">{{ item.count }}</text>
          <text class="arrow">›</text>
        </view>
      </view>
      <DyEmpty v-if="!loading && !regions.length" text="该省份暂无机构" icon="空" color="gray" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getRegions } from '@/api/park';
import type { RegionItem, DrillLevel } from '@/types/park';
import { MUNICIPALITIES } from '@/types/park';
import { useRegionHeatmap } from '@/composables/useRegionHeatmap';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const provinceCode = ref('');
const regions = ref<RegionItem[]>([]);
const loading = ref(true);

const isMuni = computed(() => MUNICIPALITIES.includes(provinceCode.value));
const totalParks = computed(() => regions.value.reduce((s, i) => s + i.count, 0));

// 热力图：省级边界聚焦，显示市级名称（直辖市直接显示区县），区域按机构数暖色填色
const heatmap = useRegionHeatmap({
  containerId: 'vital-province-map',
  get geoCode() {
    return provinceCode.value;
  },
  get items() {
    return regions.value;
  },
  showLabels: true,
  onRegionClick: (r) => handleNavigate(r.code, r.name),
});

async function fetchData() {
  loading.value = true;
  try {
    const level: DrillLevel = isMuni.value ? 'district' : 'city';
    const params: any = {
      category: 'vital',
      level,
      provinceCode: provinceCode.value,
    };
    if (isMuni.value) {
      params.cityCode = provinceCode.value.substring(0, 2) + '0100';
    }

    const result = await getRegions(params);
    regions.value = result.items || [];
    await nextTick();
    heatmap.init();
  } catch {
    regions.value = [];
  } finally {
    loading.value = false;
  }
}

function onItemClick(item: RegionItem) {
  handleNavigate(item.code, item.name);
}

/** 导航：直辖市→district.vue（跳过 city）；普通省→city.vue */
function handleNavigate(cityOrDistrictCode: string, name?: string) {
  if (isMuni.value) {
    uni.navigateTo({
      url: `/pages/business/park/vital/district?category=vital&provinceCode=${provinceCode.value}&cityCode=${provinceCode.value.substring(0, 2) + '0100'}&districtCode=${cityOrDistrictCode}`,
    });
  } else {
    const nameQuery = name ? `&cityName=${encodeURIComponent(name)}` : '';
    uni.navigateTo({
      url: `/pages/business/park/vital/city?category=vital&provinceCode=${provinceCode.value}&cityCode=${cityOrDistrictCode}${nameQuery}`,
    });
  }
}

onLoad((options: any) => {
  if (options?.provinceCode) provinceCode.value = options.provinceCode;
});

onMounted(fetchData);

onUnmounted(() => {
  heatmap.dispose();
});
</script>

<style lang="scss" scoped>

.province-page {
  min-height: 100vh;
  background: $bg-page;
}

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
