<template>
  <view class="list-page">
    <!-- ECharts 地图区 -->
    <view class="map-section">
      <!-- #ifdef H5 -->
      <div id="vital-list-map" class="map-container"></div>
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
        <text class="stat-label">养老机构总数</text>
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
import { onLoad } from '@dcloudio/uni-app';
import * as echarts from 'echarts';
import { getRegions } from '@/api/park';
import type { RegionItem } from '@/types/park';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const regions = ref<RegionItem[]>([]);
const loading = ref(true);
let myChart: echarts.ECharts | null = null;

const totalParks = computed(() => regions.value.reduce((s, i) => s + i.count, 0));

// GeoJSON 名称规范化（API 返回 "河北省" → GeoJSON "河北"）
function normalizeName(name: string, code: string): string {
  let n = name.replace(/省$/, '');
  const autonomousMap: Record<string, string> = {
    '640000': '宁夏',
    '650000': '新疆',
    '450000': '广西',
    '150000': '内蒙古',
    '540000': '西藏',
  };
  if (autonomousMap[code]) n = autonomousMap[code];
  n = n.replace(/市$/, '');
  return n;
}

async function fetchData() {
  loading.value = true;
  try {
    const result = await getRegions({
      category: 'vital',
      level: 'province',
    });
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
  const container = document.getElementById('vital-list-map');
  if (!container) return;

  let chinaGeo: any = null;
  try {
    const resp = await fetch('/static/geo/china.json');
    chinaGeo = await resp.json();
  } catch {
    console.warn('[vital/list] china.json 加载失败');
    return;
  }

  const centroidMap: Record<string, [number, number]> = {};
  chinaGeo.features.forEach((f: any) => {
    const cp = f.properties?.centroid || f.properties?.center || f.properties?.cp;
    if (cp) centroidMap[f.properties.name] = [cp[0], cp[1]];
  });

  echarts.registerMap('china', chinaGeo);
  myChart = echarts.init(container);

  const scatterData = regions.value
    .filter((r) => r.count > 0)
    .map((r) => {
      const normalName = normalizeName(r.name, r.code);
      const coord = centroidMap[normalName];
      return {
        name: r.name,
        value: coord ? [...coord, r.count] : [],
        code: r.code,
      };
    })
    .filter((d) => d.value.length > 0);

  myChart.setOption({
    geo: {
      map: 'china',
      roam: true,
      scaleLimit: { min: 1, max: 10 },
      zoom: 1.2,
      top: 20,
      aspectScale: 0.85,
      label: {
        show: true,
        fontSize: 8,
        color: 'rgba(0,0,0,0.5)',
      },
      itemStyle: {
        areaColor: '#eef4ff',
        borderColor: '#dcdfe6',
        borderWidth: 0.5,
      },
      emphasis: {
        itemStyle: { areaColor: '#d6e4ff', borderColor: '#409eff' },
        label: { show: true },
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
      navigateToProvince(params.data.code);
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

function onProvinceClick(item: RegionItem) {
  navigateToProvince(item.code);
}

function navigateToProvince(provinceCode: string) {
  uni.navigateTo({
    url: `/pages/business/park/vital/province?provinceCode=${provinceCode}`,
  });
}

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
