<template>
  <view class="echarts-map">
    <!-- #ifdef H5 -->
    <div :id="mapDivId" class="map-div"></div>
    <!-- #endif -->
    <!-- #ifndef H5 -->
    <view class="map-placeholder">
      <text class="map-placeholder-text">地图组件</text>
    </view>
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';
// #ifdef H5
import * as echarts from 'echarts';
import type { RegionItem } from '@/types/park';
// #endif

const props = defineProps<{
  /** 区域列表（code+name+count） */
  items: RegionItem[];
  /** 当前层级：province=全国省图，city=省份市图 */
  level: 'province' | 'city';
  /** 市级时传当前省 adcode（如 '320000'），用于加载对应省 GeoJSON */
  provinceCode?: string;
}>();

const emit = defineEmits<{ select: [item: RegionItem] }>();

// 唯一 div id（防多实例冲突）
const mapDivId = ref(`echarts-map-${Date.now()}-${Math.floor(Math.random() * 10000)}`);

// #ifdef H5
let chart: echarts.ECharts | null = null;
/** adcode → RegionItem 的查找表（用 code 精确匹配，避免名称不一致） */
let codeMap: Map<string, RegionItem> = new Map();

/**
 * 加载 GeoJSON 并注册到 echarts。
 * 省级：static/geo/china.json（全国省界）
 * 市级：static/geo/provinces/{provinceCode}.json（单省市界）
 */
async function loadGeoJson(): Promise<any> {
  if (props.level === 'province') {
    const res = await fetch('/static/geo/china.json');
    return res.json();
  }
  const res = await fetch(`/static/geo/provinces/${props.provinceCode}.json`);
  return res.json();
}

/**
 * 从 GeoJSON properties 构建 adcode→name 映射。
 * ECharts map data 用 name 匹配区域，所以必须用 GeoJSON 里的标准名称。
 */
function buildDataNameMap(geoJson: any): Map<string, string> {
  const m = new Map<string, string>();
  geoJson.features.forEach((f: any) => {
    const adcode = f.properties.adcode;
    const name = f.properties.name;
    if (adcode && name) m.set(String(adcode), name);
  });
  return m;
}

/**
 * 构建 ECharts series data：
 * 对每个 RegionItem，用 code 查 GeoJSON 的标准 name 作为 data.name，
 * count 作为 data.value。
 */
function buildSeriesData(nameByCode: Map<string, string>) {
  return props.items.map(item => {
    const geoName = nameByCode.get(item.code) || item.name;
    return { name: geoName, value: item.count, code: item.code };
  });
}

async function renderChart() {
  const el = document.getElementById(mapDivId.value);
  if (!el) return;

  try {
    const geoJson = await loadGeoJson();
    const mapName = props.level === 'province' ? 'china' : `province-${props.provinceCode}`;
    echarts.registerMap(mapName, geoJson);

    // 重建查找表
    const nameByCode = buildDataNameMap(geoJson);
    codeMap = new Map(props.items.map(i => [i.code, i]));

    if (!chart) {
      chart = echarts.init(el);
    }

    const seriesData = buildSeriesData(nameByCode);
    const maxCount = Math.max(1, ...props.items.map(i => i.count));

    chart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (p: any) => {
          if (p.data) return `${p.name}<br/>机构：${p.data.value} 家`;
          return p.name;
        },
      },
      visualMap: {
        type: 'piecewise',
        min: 0,
        max: maxCount,
        show: false,
        pieces: [
          { min: 1, max: maxCount * 0.34, color: '#c6e0ff' },
          { min: maxCount * 0.34, max: maxCount * 0.67, color: '#79b8ff' },
          { min: maxCount * 0.67, color: '#409eff' },
          { value: 0, color: '#f0f0f0' },
        ],
      },
      series: [
        {
          type: 'map',
          map: mapName,
          roam: false,
          label: { show: props.level === 'city', fontSize: 10, color: '#333' },
          emphasis: {
            label: { show: true, fontWeight: 'bold' },
            itemStyle: { areaColor: '#409eff', borderColor: '#fff', borderWidth: 1 },
          },
          itemStyle: {
            borderColor: '#fff',
            borderWidth: 0.5,
          },
          data: seriesData,
        },
      ],
    }, true);

    // 点击下钻（只允许 count>0 的区域）
    chart.off('click');
    chart.on('click', (params: any) => {
      if (params.seriesType !== 'map') return;
      const code = params.data?.code;
      if (!code) return;
      const item = codeMap.get(code);
      if (item && item.count > 0) {
        emit('select', item);
      }
    });
  } catch (e) {
    console.error('[RegionEchartsMap] 渲染失败:', e);
  }
}

// items / level / provinceCode 变化时重渲染
watch(
  () => [props.items, props.level, props.provinceCode],
  () => {
    if (chart) renderChart();
  },
  { deep: true },
);

onMounted(() => {
  nextTick(() => {
    setTimeout(renderChart, 100);
  });
});

onUnmounted(() => {
  if (chart) {
    chart.dispose();
    chart = null;
  }
});
// #endif
</script>

<style scoped>
.echarts-map {
  width: 100%;
  height: 100%;
  position: relative;
}
/* #ifdef H5 */
.map-div {
  width: 100%;
  height: 100%;
}
/* #endif */
/* #ifndef H5 */
.map-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f0fe, #d4e4fc);
}
.map-placeholder-text {
  font-size: 32rpx;
  color: #909399;
}
/* #endif */
</style>
