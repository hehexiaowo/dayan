/**
 * Leaflet + 天地图瓦片地图工具（参照参考项目 utils/map.js 的 TS 版）。
 *
 * 用 Leaflet 替代天地图原生 JS API（window.T）。
 * 天地图仅作为瓦片图层来源（vec 底图 + cva 标注），无需引入天地图 JS SDK。
 */
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

/** 天地图 API Key */
const TIANDITU_KEY = '1ea38bada071978da6b6cfd68c464450';
const SUBDOMAINS = ['0', '1', '2', '3', '4', '5', '6', '7'];

/** Marker 数据项（与后端 parkList 字段对齐） */
export interface MapMarkerItem {
  latitude: number;
  longitude: number;
  name?: string;
  code?: string;
  color?: string;
}

/** 天地图矢量底图（vec_w） */
function createVecLayer(): L.TileLayer {
  return L.tileLayer(
    `https://t{s}.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILECOL={x}&TILEROW={y}&TILEMATRIX={z}&tk=${TIANDITU_KEY}`,
    { subdomains: SUBDOMAINS },
  );
}

/** 天地图矢量标注（cva_w — 道路/地名文字叠加层） */
function createCvaLayer(): L.TileLayer {
  return L.tileLayer(
    `https://t{s}.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILECOL={x}&TILEROW={y}&TILEMATRIX={z}&tk=${TIANDITU_KEY}`,
    { subdomains: SUBDOMAINS },
  );
}

/**
 * 初始化列表页地图（中心北京，zoom=5）。
 * @returns L.Map 实例，或 null（容器不存在）
 */
export function initMap(containerId: string): L.Map | null {
  const container = document.getElementById(containerId);
  if (!container) return null;

  const map = L.map(containerId, {
    center: [39.9042, 116.4074],
    zoom: 5,
    scrollWheelZoom: true,
    zoomControl: true,
    attributionControl: false,
  });
  createVecLayer().addTo(map);
  createCvaLayer().addTo(map);
  return map;
}

/**
 * 批量添加 markers，auto-fit 到所有标记的范围。
 * 用 circleMarker（SVG 圆）避免 Leaflet 默认 marker icon 在 Vite 打包后 404 的问题。
 *
 * @returns L.LayerGroup（可用于后续清除）
 */
export function addMarkers(
  map: L.Map,
  items: MapMarkerItem[],
  onClick?: (item: MapMarkerItem) => void,
): L.LayerGroup {
  const group = L.featureGroup();

  items.forEach((item) => {
    if (!item.latitude || !item.longitude) return;

    const color = item.color || '#409eff';
    const marker = L.circleMarker([item.latitude, item.longitude], {
      radius: 8,
      fillColor: color,
      color: '#fff',
      weight: 2,
      opacity: 1,
      fillOpacity: 0.8,
    }).addTo(group);

    if (item.name) {
      marker.bindTooltip(item.name, {
        permanent: false,
        direction: 'top',
        offset: [0, -8],
      });
    }
    if (onClick && item.code) {
      marker.on('click', () => onClick(item));
    }
  });

  group.addTo(map);

  // 自动适配视野（有 markers 时缩放到全部可见）
  if (group.getLayers().length > 0) {
    map.fitBounds(group.getBounds().pad(0.1));
  }

  return group;
}

/**
 * 按地名搜索（天地图 geocoder 兜底：无坐标 markers 时定位到城市/区名）。
 */
export async function searchByName(map: L.Map, name: string): Promise<void> {
  if (!name) return;
  try {
    const resp = await fetch(
      `https://api.tianditu.gov.cn/geocoder?ds=${encodeURIComponent(JSON.stringify({ keyWord: name }))}&tk=${TIANDITU_KEY}`,
    );
    const data = await resp.json();
    if (data.status === '0' && data.result?.location) {
      const { lat, lon } = data.result.location;
      map.setView([lat, lon], 13);
    }
  } catch {
    // 静默失败
  }
}

/**
 * 初始化详情页地图（单 marker + popup，zoom=15）。
 * @param color marker 填充色（各网络主题色：vital=#409eff / care=#ff9900 / sojourn=#19be6b）
 */
export function initDetailMap(
  containerId: string,
  latitude: number,
  longitude: number,
  name?: string,
  color?: string,
): L.Map | null {
  const container = document.getElementById(containerId);
  if (!container) return null;

  const fillColor = color || '#409eff';

  const map = L.map(containerId, {
    center: [latitude, longitude],
    zoom: 15,
    scrollWheelZoom: true,
    zoomControl: true,
    attributionControl: false,
  });
  createVecLayer().addTo(map);
  createCvaLayer().addTo(map);

  const marker = L.circleMarker([latitude, longitude], {
    radius: 10,
    fillColor: fillColor,
    color: '#fff',
    weight: 2,
    opacity: 1,
    fillOpacity: 0.9,
  }).addTo(map);

  if (name) {
    marker.bindTooltip(name, {
      permanent: true,
      direction: 'top',
      offset: [0, -12],
    }).openTooltip();
  }

  return map;
}

/** 销毁地图（清理 DOM 和事件监听） */
export function destroyMap(map: L.Map | null): void {
  if (map) {
    map.remove();
  }
}
