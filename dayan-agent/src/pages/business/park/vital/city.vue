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
import * as echarts from 'echarts';
import { getRegions } from '@/api/park';
import type { RegionItem, ParkCard } from '@/types/park';
import { PROVINCE_CENTERS } from '@/utils/region';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const provinceCode = ref('');
const cityCode = ref('');
const cityName = ref('');
const districts = ref<RegionItem[]>([]);
const loading = ref(true);
let myChart: echarts.ECharts | null = null;

const totalParks = computed(() => districts.value.reduce((s, i) => s + i.count, 0));

async function fetchData() {
  loading.value = true;
  try {
    // 并行：区县列表 + 机构坐标（地图散点用）
    const [districtResult, parkResult] = await Promise.all([
      getRegions({
        category: 'vital',
        level: 'district',
        provinceCode: provinceCode.value,
        cityCode: cityCode.value,
      }),
      getRegions({
        category: 'vital',
        level: 'park',
        provinceCode: provinceCode.value,
        cityCode: cityCode.value,
      }).catch(() => ({ parkList: [] as ParkCard[] })),
    ]);
    districts.value = districtResult.items || [];
    await nextTick();
    setTimeout(() => initChart(parkResult.parkList || []), 300);
  } catch {
    districts.value = [];
  } finally {
    loading.value = false;
  }
}

async function initChart(parks: ParkCard[]) {
  // #ifdef H5
  const container = document.getElementById('vital-city-map');
  if (!container) return;

  let provinceGeo: any = null;
  try {
    const resp = await fetch(`/static/geo/provinces/${provinceCode.value}.json`);
    provinceGeo = await resp.json();
  } catch {
    console.warn('[vital/city] 省份 GeoJSON 加载失败:', provinceCode.value);
    return;
  }

  // 从省份 GeoJSON 中找到当前城市的 centroid
  let cityCenter: [number, number] | null = null;
  provinceGeo.features.forEach((f: any) => {
    const cp = f.properties?.centroid || f.properties?.center || f.properties?.cp;
    if (String(f.properties?.adcode) === cityCode.value) {
      cityName.value = f.properties?.name || '';
      if (cp) cityCenter = [cp[0], cp[1]];
    }
  });

  // 兜底：用省份中心
  if (!cityCenter) {
    const fallback = PROVINCE_CENTERS[provinceCode.value];
    if (fallback) cityCenter = [fallback.lng, fallback.lat];
  }

  const mapName = 'vital-city-' + provinceCode.value;
  echarts.registerMap(mapName, provinceGeo);
  myChart = echarts.init(container);

  // 机构真实坐标散点
  const scatterData = parks
    .filter((p) => p.latitude && p.longitude)
    .map((p) => ({
      name: p.shortName || p.fullName,
      value: [p.longitude!, p.latitude!, 1],
      code: p.parkCode,
    }));

  const geoConfig: any = {
    map: mapName,
    roam: true,
    scaleLimit: { min: 1, max: 15 },
    zoom: 5,
    top: 20,
    aspectScale: 0.85,
    label: { show: false },
    itemStyle: {
      areaColor: '#eef4ff',
      borderColor: '#dcdfe6',
      borderWidth: 0.5,
    },
    emphasis: {
      itemStyle: { areaColor: '#d6e4ff', borderColor: '#409eff' },
      label: { show: true, fontSize: 9 },
    },
  };
  if (cityCenter) {
    geoConfig.center = cityCenter;
  }

  myChart.setOption({
    geo: geoConfig,
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => (p.data ? p.data.name : p.name),
    },
    series: [
      {
        type: 'effectScatter',
        coordinateSystem: 'geo',
        rippleEffect: { period: 4, scale: 3, brushType: 'stroke' },
        symbolSize: 10,
        itemStyle: {
          color: '#409eff',
          shadowBlur: 6,
          shadowColor: 'rgba(64,158,255,0.4)',
        },
        data: scatterData,
      },
    ],
  });

  myChart.on('click', (params: any) => {
    if (params.data?.code) {
      uni.navigateTo({
        url: `/pages/business/park/vital/detail?parkCode=${params.data.code}`,
      });
    }
  });

  window.addEventListener('resize', handleResize);
  // #endif
}

function handleResize() {
  myChart?.resize();
}

function onDistrictClick(item: RegionItem) {
  uni.navigateTo({
    url: `/pages/business/park/vital/district?category=vital&provinceCode=${provinceCode.value}&cityCode=${cityCode.value}&districtCode=${item.code}`,
  });
}

onLoad((options: any) => {
  if (options?.provinceCode) provinceCode.value = options.provinceCode;
  if (options?.cityCode) cityCode.value = options.cityCode;
});

onMounted(fetchData);

onUnmounted(() => {
  // #ifdef H5
  window.removeEventListener('resize', handleResize);
  myChart?.dispose();
  myChart = null;
  // #endif
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

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
