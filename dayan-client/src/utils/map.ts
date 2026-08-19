/**
 * Leaflet + 天地图瓦片地图工具。
 *
 * 用 Leaflet 替代天地图原生 JS API（window.T）。
 * 天地图仅作为瓦片图层来源（vec 底图 + cva 标注），无需引入天地图 JS SDK。
 *
 * 天地图 Key 运行时经 /client-api/v1/config/map-key 从后端系统配置拉取。
 */
import type L from 'leaflet';

let leafletModule: typeof L | null = null;

async function getLeaflet(): Promise<typeof L> {
  if (!leafletModule) {
    const mod = await import('leaflet');
    leafletModule = mod.default;
    await import('leaflet/dist/leaflet.css');
  }
  return leafletModule;
}

const SUBDOMAINS = ['0', '1', '2', '3', '4', '5', '6', '7'];

/** 天地图 Key 单例 Promise（同页多次建图只请求一次） */
let tiandituKeyPromise: Promise<string> | null = null;

/** 运行时获取天地图 Key（后端系统配置下发，失败抛出异常） */
export function getTiandituKey(): Promise<string> {
  if (!tiandituKeyPromise) {
    tiandituKeyPromise = fetch('/client-api/v1/config/map-key')
      .then((r) => r.json())
      .then((body) => {
        if (!body?.data?.tiandituKey) {
          throw new Error('天地图 Key 未配置，请在系统配置中设置 map.tianditu-key');
        }
        return body.data.tiandituKey as string;
      });
  }
  return tiandituKeyPromise;
}

/** 网络类型（用于地图标记图标区分） */
export type NetworkType = 'vital' | 'care' | 'sojourn';

/** 网络类型 → 主题色 + 符号文字 */
const NETWORK_MARKER_CONFIG: Record<NetworkType, { color: string; symbol: string }> = {
  vital: { color: '#409eff', symbol: '活' },
  care: { color: '#ff9900', symbol: '照' },
  sojourn: { color: '#19be6b', symbol: '旅' },
};

/** Marker 数据项（与后端 parkList 字段对齐） */
export interface MapMarkerItem {
  latitude: number;
  longitude: number;
  name?: string;
  code?: string;
  color?: string;
  networkType?: NetworkType;
}

/** 天地图矢量底图（vec_w） */
async function createVecLayer(): Promise<L.TileLayer> {
  const L = await getLeaflet();
  const key = await getTiandituKey();
  return L.tileLayer(
    `https://t{s}.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILECOL={x}&TILEROW={y}&TILEMATRIX={z}&tk=${key}`,
    { subdomains: SUBDOMAINS },
  );
}

/** 天地图矢量标注（cva_w — 道路/地名文字叠加层） */
async function createCvaLayer(): Promise<L.TileLayer> {
  const L = await getLeaflet();
  const key = await getTiandituKey();
  return L.tileLayer(
    `https://t{s}.tianditu.gov.cn/cva_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=cva&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILECOL={x}&TILEROW={y}&TILEMATRIX={z}&tk=${key}`,
    { subdomains: SUBDOMAINS },
  );
}

/**
 * 初始化列表页地图（中心北京，zoom=5）。异步：先取天地图 Key 再建图层。
 * @returns L.Map 实例，或 null（容器不存在）
 */
export async function initMap(containerId: string): Promise<L.Map | null> {
  const L = await getLeaflet();
  const container = document.getElementById(containerId);
  if (!container) return null;

  const map = L.map(containerId, {
    center: [39.9042, 116.4074],
    zoom: 5,
    scrollWheelZoom: true,
    zoomControl: true,
    attributionControl: false,
  });
  const [vec, cva] = await Promise.all([createVecLayer(), createCvaLayer()]);
  vec.addTo(map);
  cva.addTo(map);
  return map;
}

/**
 * 批量添加 markers，auto-fit 到所有标记的范围。
 * 用 circleMarker（SVG 圆）避免 Leaflet 默认 marker icon 在 Vite 打包后 404 的问题。
 *
 * @returns L.LayerGroup（可用于后续清除）
 */
export async function addMarkers(
  map: L.Map,
  items: MapMarkerItem[],
  onClick?: (item: MapMarkerItem) => void,
): Promise<L.LayerGroup> {
  const L = await getLeaflet();
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
 * 批量添加图标 markers（水滴 pin + 网络符号），auto-fit 到所有标记的范围。
 * 用 divIcon 自定义 HTML 替代 circleMarker，按网络类型显示不同颜色+符号。
 * fitBounds 带 maxZoom 防止过度放大，pad(0.25) 保持更宽的全局视野。
 *
 * @returns L.LayerGroup（可用于后续清除）
 */
export async function addIconMarkers(
  map: L.Map,
  items: MapMarkerItem[],
  onClick?: (item: MapMarkerItem) => void,
): Promise<L.LayerGroup> {
  const L = await getLeaflet();
  const group = L.featureGroup();

  items.forEach((item) => {
    if (!item.latitude || !item.longitude) return;

    const networkType = item.networkType || 'vital';
    const config = NETWORK_MARKER_CONFIG[networkType];
    const color = item.color || config.color;

    const icon = L.divIcon({
      className: 'dy-map-pin-wrapper',
      html: `<div style="
        width:28px;height:28px;border-radius:50% 50% 50% 0;
        transform:rotate(-45deg);
        background:${color};border:2px solid #fff;
        box-shadow:0 2px 6px rgba(0,0,0,0.35);
        display:flex;align-items:center;justify-content:center;
      "><span style="transform:rotate(45deg);color:#fff;font-size:12px;font-weight:bold;line-height:1;">${config.symbol}</span></div>`,
      iconSize: [32, 32],
      iconAnchor: [16, 28],
      tooltipAnchor: [0, -28],
    });

    const marker = L.marker([item.latitude, item.longitude], { icon }).addTo(group);

    if (item.name) {
      marker.bindTooltip(item.name, {
        permanent: false,
        direction: 'top',
        offset: [0, -4],
      });
    }
    if (onClick && item.code) {
      marker.on('click', () => onClick(item));
    }
  });

  group.addTo(map);

  // 自动适配视野，maxZoom 防止过度放大，pad 保持全局视野
  if (group.getLayers().length > 0) {
    map.fitBounds(group.getBounds().pad(0.25), { maxZoom: 12 });
  }

  return group;
}

/**
 * 按地名搜索（天地图 geocoder 兜底：无坐标 markers 时定位到城市/区名）。
 */
export async function searchByName(map: L.Map, name: string): Promise<void> {
  if (!name) return;
  try {
    const key = await getTiandituKey();
    const resp = await fetch(
      `https://api.tianditu.gov.cn/geocoder?ds=${encodeURIComponent(JSON.stringify({ keyWord: name }))}&tk=${key}`,
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
 * 初始化详情页地图（单 marker + popup，zoom=15）。异步：先取天地图 Key 再建图层。
 * @param color marker 填充色（各网络主题色：vital=#409eff / care=#ff9900 / sojourn=#19be3b）
 */
export async function initDetailMap(
  containerId: string,
  latitude: number,
  longitude: number,
  name?: string,
  color?: string,
): Promise<L.Map | null> {
  const L = await getLeaflet();
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
  const [vec, cva] = await Promise.all([createVecLayer(), createCvaLayer()]);
  vec.addTo(map);
  cva.addTo(map);

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
