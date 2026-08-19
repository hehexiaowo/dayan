/**
 * 区域热力图 composable —— list / province / city 三级共用。
 *
 * 替代三页各自重复 ~80 行的 ECharts 内联代码。统一为：
 *   1. getRegionGeoJSON(code) 按 adcode 拉边界
 *   2. 用 adcode→GeoJSON权威名称 映射，解决「北京 vs 北京市」名称不一致
 *   3. series type:'map' + visualMap 真正的区域填色热力图（暖色渐变）
 *   4. 区域点击 → 下钻
 *
 * 用法：
 *   const heatmap = useRegionHeatmap({
 *     containerId: 'vital-list-map', geoCode: '100000',
 *     items: regions, showLabels: false,
 *     onRegionClick: (r) => navigateToProvince(r.code),
 *   });
 *   onMounted(async () => { await fetchData(); await nextTick(); heatmap.init(); });
 *   onUnmounted(() => heatmap.dispose());
 */
import type * as ECharts from 'echarts';
import { getRegionGeoJSON } from '@/utils/geo';
import type { RegionItem } from '@/types/park';

/** 通用暖色热力渐变（黄→橙→红），与参考项目一致，跨网络统一 */
const WARM_GRADIENT = ['#FDE4AF', '#F9BD6F', '#F7A049', '#FE8817', '#EE6516'];

/** 无机构区域的底色（与暖色系协调的浅米色） */
const BASE_AREA_COLOR = '#fef6e6';
const BORDER_COLOR = '#e8d9c0';

export interface HeatmapConfig {
  /** 地图容器 DOM id */
  containerId: string;
  /** 行政区划码：'100000'(全国) | 省码 | 市码 */
  geoCode: string;
  /** 下级区域列表（code/name/count），驱动区域填色 */
  items: RegionItem[];
  /** 是否显示区域名称标签（list 不显示省名；province/city 显示） */
  showLabels?: boolean;
  /** 区域点击回调（下钻） */
  onRegionClick?: (item: { code: string; name: string }) => void;
  /**
   * 地图布局中心（容器百分比，如 ['50%','58%']）。传入后改用 layoutCenter/layoutSize 布局，
   * 用于把全国地图往下挪（南海撑大 bbox 导致大陆偏上）。不传则用默认 top+zoom 布局。
   */
  layoutCenter?: [string, string];
  /** 配合 layoutCenter 的地图尺寸（容器百分比，如 '115%'） */
  layoutSize?: string;
}

export function useRegionHeatmap(cfg: HeatmapConfig) {
  let chart: ECharts.ECharts | null = null;
  const resize = () => chart?.resize();

  async function init() {
    const container = document.getElementById(cfg.containerId);
    if (!container) return; // 非 H5（无 DOM）自动跳过

    const echarts = await import('echarts');

    let geo: GeoJSON.FeatureCollection;
    try {
      geo = await getRegionGeoJSON(cfg.geoCode);
    } catch (e) {
      console.warn(`[useRegionHeatmap] GeoJSON 加载失败: ${cfg.geoCode}`, e);
      return;
    }

    // adcode → GeoJSON 权威名称（解决「北京 vs 北京市」名称不一致）
    const adcodeToName: Record<string, string> = {};
    geo.features.forEach((f: GeoJSON.Feature) => {
      adcodeToName[String(f.properties?.adcode)] = f.properties?.name;
    });

    const maxValue = Math.max(1, ...cfg.items.map((i) => i.count));
    // 唯一 mapName，避免不同页面/级别 registerMap 互相覆盖
    const mapName = `heat-${cfg.geoCode}-${Date.now()}`;
    echarts.registerMap(mapName, geo);
    chart = echarts.init(container);

    chart.setOption({
      visualMap: {
        min: 0,
        max: maxValue,
        calculable: true,
        show: maxValue > 0,
        right: '2%',
        bottom: 8,
        itemWidth: 8,
        itemHeight: 70,
        textStyle: { fontSize: 9, color: '#888' },
        inRange: { color: WARM_GRADIENT },
      },
      geo: {
        map: mapName,
        roam: true,
        scaleLimit: { min: 1, max: 12 },
        aspectScale: 0.85,
        // layoutCenter 模式（全国地图下移）vs 默认 top+zoom 模式
        ...(cfg.layoutCenter
          ? { layoutCenter: cfg.layoutCenter, layoutSize: cfg.layoutSize || '110%' }
          : { zoom: 1.2, top: 16 }),
        label: {
          show: !!cfg.showLabels,
          fontSize: 9,
          color: 'rgba(0,0,0,0.6)',
        },
        itemStyle: {
          areaColor: BASE_AREA_COLOR,
          borderColor: BORDER_COLOR,
          borderWidth: 0.6,
        },
        emphasis: {
          label: { show: true },
        },
      },
      tooltip: {
        trigger: 'item',
        formatter: (p: { data?: { name: string; value: number }; name?: string }) =>
          p.data ? `${p.data.name}：${p.data.value} 家机构` : p.name,
      },
      series: [
        {
          type: 'map',
          geoIndex: 0,
          data: cfg.items.map((i) => ({
            name: adcodeToName[String(i.code)] || i.name,
            value: i.count,
            code: i.code,
          })),
        },
      ],
    });

    chart.on('click', (params: { data?: { code: string; name: string } }) => {
      if (params.data?.code) {
        cfg.onRegionClick?.({ code: params.data.code, name: params.data.name });
      }
    });
    // 取消鼠标移入高亮（与参考项目一致，避免视觉嘈杂）
    chart.on('mouseover', () => chart?.dispatchAction({ type: 'downplay' }));

    window.addEventListener('resize', resize);
  }

  function dispose() {
    window.removeEventListener('resize', resize);
    chart?.dispose();
    chart = null;
  }

  return { init, dispose, resize };
}
