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
import * as echarts from 'echarts';
import { getRegions } from '@/api/park';
import type { RegionItem, DrillLevel } from '@/types/park';
import { MUNICIPALITIES } from '@/types/park';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const provinceCode = ref('');
const regions = ref<RegionItem[]>([]);
const loading = ref(true);
let myChart: echarts.ECharts | null = null;

const isMuni = computed(() => MUNICIPALITIES.includes(provinceCode.value));
const totalParks = computed(() => regions.value.reduce((s, i) => s + i.count, 0));

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
    setTimeout(initChart, 300);
  } catch {
    regions.value = [];
  } finally {
    loading.value = false;
  }
}

async function initChart() {
  // #ifdef H5
  const container = document.getElementById('vital-province-map');
  if (!container) return;

  let provinceGeo: any = null;
  try {
    const resp = await fetch(`/static/geo/provinces/${provinceCode.value}.json`);
    provinceGeo = await resp.json();
  } catch {
    console.warn('[vital/province] 省份 GeoJSON 加载失败:', provinceCode.value);
    return;
  }

  const centroidMap: Record<string, [number, number]> = {};
  provinceGeo.features.forEach((f: any) => {
    const cp = f.properties?.centroid || f.properties?.center || f.properties?.cp;
    if (cp) centroidMap[f.properties.name] = [cp[0], cp[1]];
  });

  const mapName = 'vital-province-' + provinceCode.value;
  echarts.registerMap(mapName, provinceGeo);
  myChart = echarts.init(container);

  const scatterData = regions.value
    .filter((r) => r.count > 0)
    .map((r) => {
      const coord = centroidMap[r.name];
      return {
        name: r.name,
        value: coord ? [...coord, r.count] : [],
        code: r.code,
      };
    })
    .filter((d) => d.value.length > 0);

  myChart.setOption({
    geo: {
      map: mapName,
      roam: true,
      scaleLimit: { min: 1, max: 10 },
      zoom: 1,
      top: 20,
      label: {
        show: true,
        fontSize: 9,
        color: 'rgba(0,0,0,0.6)',
      },
      itemStyle: {
        areaColor: '#f0f6ff',
        borderColor: 'rgba(0, 0, 0, 0.2)',
      },
      emphasis: {
        itemStyle: { areaColor: '#f0f6ff' },
        label: { borderWidth: 0 },
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: (p: any) =>
        p.data ? `${p.data.name}<br/>机构：${p.data.value?.[2] || 0} 家` : p.name,
    },
    series: [
      {
        type: 'effectScatter',
        coordinateSystem: 'geo',
        rippleEffect: { period: 4, scale: 4, brushType: 'fill' },
        symbolSize: 8,
        itemStyle: {
          color: '#409eff',
          shadowBlur: 8,
          shadowColor: 'rgba(64,158,255,0.5)',
        },
        data: scatterData,
      },
    ],
  });

  myChart.on('click', (params: any) => {
    if (params.data?.code) {
      handleNavigate(params.data.code);
    }
  });

  myChart.on('mouseover', () => {
    myChart?.dispatchAction({ type: 'downplay' });
  });

  window.addEventListener('resize', handleResize);
  // #endif
}

function handleResize() {
  myChart?.resize();
}

function onItemClick(item: RegionItem) {
  handleNavigate(item.code);
}

/** 导航：直辖市→district.vue（跳过 city）；普通省→city.vue */
function handleNavigate(cityOrDistrictCode: string) {
  if (isMuni.value) {
    uni.navigateTo({
      url: `/pages/business/park/vital/district?category=vital&provinceCode=${provinceCode.value}&cityCode=${provinceCode.value.substring(0, 2) + '0100'}&districtCode=${cityOrDistrictCode}`,
    });
  } else {
    uni.navigateTo({
      url: `/pages/business/park/vital/city?category=vital&provinceCode=${provinceCode.value}&cityCode=${cityOrDistrictCode}`,
    });
  }
}

onLoad((options: any) => {
  if (options?.provinceCode) provinceCode.value = options.provinceCode;
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
  position: absolute;
  bottom: $spacing-sm;
  left: $spacing-md;
  right: $spacing-md;
  background: rgba(255, 255, 255, 0.95);
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
