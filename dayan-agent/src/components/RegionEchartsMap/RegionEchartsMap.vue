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

/**
 * 加载 GeoJSON。
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
 * 从 GeoJSON features 提取每个区域的中心点坐标。
 * DataV GeoJSON 用 properties.centroid，全国 china.json 用 properties.center。
 */
function buildCentroidMap(geoJson: any): Map<string, number[]> {
  const m = new Map<string, number[]>();
  geoJson.features.forEach((f: any) => {
    const adcode = String(f.properties.adcode);
    const cp = f.properties.centroid || f.properties.center || f.properties.cp;
    if (adcode && cp && cp.length >= 2) {
      m.set(adcode, [cp[0], cp[1]]);
    }
  });
  return m;
}

async function renderChart() {
  const el = document.getElementById(mapDivId.value);
  if (!el) return;

  try {
    const geoJson = await loadGeoJson();
    const mapName = props.level === 'province' ? 'china' : `province-${props.provinceCode}`;
    echarts.registerMap(mapName, geoJson);

    // 重建 adcode → RegionItem 查找表 + adcode → 中心点映射
    const codeMap = new Map(props.items.map(i => [i.code, i]));
    const centroidMap = buildCentroidMap(geoJson);

    if (chart) {
      chart.dispose();
    }
    chart = echarts.init(el);

    // 构建 effectScatter 散点数据：只有 count>0 且有中心点坐标的区域才显示
    const scatterData = props.items
      .filter(item => item.count > 0 && centroidMap.has(item.code))
      .map(item => {
        const cp = centroidMap.get(item.code)!;
        return {
          name: item.name,
          value: [cp[0], cp[1], item.count],
          code: item.code,
        };
      });

    const option: echarts.EChartsCoreOption = {
      geo: {
        map: mapName,
        roam: false,
        zoom: 1.2,
        top: 10,
        bottom: 10,
        label: {
          show: props.level === 'city',
          fontSize: 9,
          color: 'rgba(0,0,0,0.6)',
        },
        itemStyle: {
          areaColor: '#eef4fc',
          borderColor: '#c6d4e8',
          borderWidth: 0.5,
        },
        emphasis: {
          itemStyle: { areaColor: '#d6e6fc' },
          label: { show: true },
        },
      },
      tooltip: {
        trigger: 'item',
        formatter: (p: any) => {
          if (p.data?.value?.[2] != null) {
            return `${p.data.name}<br/>机构：${p.data.value[2]} 家`;
          }
          return p.name;
        },
      },
      series: [
        {
          type: 'effectScatter',
          coordinateSystem: 'geo',
          showEffectOn: 'render',
          rippleEffect: { period: 4, scale: 3, brushType: 'stroke' },
          symbolSize: 10,
          itemStyle: {
            color: '#409eff',
            shadowBlur: 6,
            shadowColor: 'rgba(64,158,255,0.4)',
          },
          label: {
            show: true,
            formatter: (p: any) => p.data.value[2],
            position: 'right',
            fontSize: 10,
            color: '#409eff',
            fontWeight: 'bold',
          },
          data: scatterData,
        },
      ],
    };

    chart.setOption(option);

    // 点击散点 → 下钻（参考项目标准模式：setOption 之后绑定）
    chart.on('click', (params: any) => {
      if (params.data?.code) {
        const item = codeMap.get(params.data.code);
        if (item) {
          emit('select', item);
        }
      }
    });

    // hover 时关闭区域高亮（参考项目模式）
    chart.on('mouseover', () => {
      chart?.dispatchAction({ type: 'downplay' });
    });

    window.addEventListener('resize', handleResize);
  } catch (e) {
    console.error('[RegionEchartsMap] 渲染失败:', e);
  }
}

function handleResize() {
  if (chart) chart.resize();
}

// items / level / provinceCode 变化时重渲染
watch(
  () => [props.items, props.level, props.provinceCode],
  () => {
    nextTick(() => {
      setTimeout(() => renderChart(), 300);
    });
  },
  { deep: true },
);

onMounted(() => {
  // 参考项目模式：nextTick + setTimeout(300) 确保容器有尺寸
  nextTick(() => {
    setTimeout(() => renderChart(), 300);
  });
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
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
  min-height: 300px;
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
