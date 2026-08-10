<template>
  <view class="tianditu-map">
    <!-- #ifdef H5 -->
    <div :id="mapDivId" class="map-div"></div>
    <!-- #endif -->
    <!-- #ifndef H5 -->
    <view class="map-placeholder">
      <text class="map-placeholder-text">🗺️ 地图组件</text>
    </view>
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';

interface MapMarker {
  lng: number;
  lat: number;
  name?: string;
  color?: string;
}

const props = withDefaults(defineProps<{
  markers?: MapMarker[];
  center?: { lng: number; lat: number };
  zoom?: number;
}>(), {
  markers: () => [],
  center: () => ({ lng: 116.4, lat: 39.9 }), // 默认北京
  zoom: 5,
});

// 唯一 div id（防止多实例冲突）
const mapDivId = ref(`tianditu-map-${Date.now()}-${Math.floor(Math.random() * 10000)}`);
let mapInstance: any = null;
let overlayList: any[] = [];

// #ifdef H5
/**
 * 初始化天地图（H5 端）。
 * 天地图 JS API 通过 window.T 暴露。
 */
function initMap() {
  const T = (window as any).T;
  if (!T) {
    console.warn('[TiandituMap] 天地图 JS API 未加载，请检查 index.html script 注入');
    return;
  }

  const el = document.getElementById(mapDivId.value);
  if (!el) {
    console.warn('[TiandituMap] 地图容器不存在:', mapDivId.value);
    return;
  }

  try {
    mapInstance = new T.Map(el, {
      projection: 'EPSG:4326',
    });
    mapInstance.centerAndZoom(
      new T.LngLat(props.center.lng, props.center.lat),
      props.zoom
    );
    addMarkers();
  } catch (e) {
    console.error('[TiandituMap] 初始化失败:', e);
  }
}

/**
 * 添加标记点。
 */
function addMarkers() {
  if (!mapInstance) return;
  const T = (window as any).T;

  // 先清除旧标记
  overlayList.forEach(o => mapInstance.removeOverLay(o));
  overlayList = [];

  props.markers.forEach(m => {
    if (!m.lng || !m.lat) return; // 跳过无坐标的

    const lngLat = new T.LngLat(m.lng, m.lat);

    // 标记点
    const iconUrl = getMarkerIcon(m.color || '#409eff');
    const icon = new T.Icon({
      iconUrl,
      iconSize: new T.Point(20, 20),
      iconAnchor: new T.Point(10, 10),
    });
    const marker = new T.Marker(lngLat, { icon });
    mapInstance.addOverLay(marker);
    overlayList.push(marker);

    // 名称标签
    if (m.name) {
      const label = new T.Label({
        text: m.name,
        position: lngLat,
        offset: new T.Point(12, -8),
      });
      label.setFontSize(11);
      label.setFontColor(m.color || '#409eff');
      mapInstance.addOverLay(label);
      overlayList.push(label);
    }
  });
}

/**
 * 生成圆形 SVG marker（用 data URI 避免外部图片依赖）。
 */
function getMarkerIcon(color: string): string {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20"><circle cx="10" cy="10" r="7" fill="${color}" opacity="0.6"/><circle cx="10" cy="10" r="3" fill="#fff"/></svg>`;
  return 'data:image/svg+xml;base64,' + btoa(svg);
}

// markers 变化时重新打点
watch(() => props.markers, () => {
  if (mapInstance) addMarkers();
}, { deep: true });

// center 变化时移动地图
watch(() => props.center, (newCenter) => {
  if (mapInstance && newCenter) {
    const T = (window as any).T;
    mapInstance.panTo(new T.LngLat(newCenter.lng, newCenter.lat));
  }
}, { deep: true });

onMounted(() => {
  // 等待 DOM 渲染完成
  nextTick(() => {
    setTimeout(initMap, 100); // 延迟确保 div 有尺寸
  });
});

onUnmounted(() => {
  if (mapInstance) {
    mapInstance = null;
    overlayList = [];
  }
});
// #endif
</script>

<style scoped>
.tianditu-map {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
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
