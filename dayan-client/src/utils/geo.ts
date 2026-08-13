/**
 * 行政区划 GeoJSON 动态拉取工具。
 *
 * 数据源：阿里云 DataV.GeoAtlas（https://datav.aliyun.com/portal/school/atlas/area_selector）
 *   - 免费、无需 Key、CORS 全开（Access-Control-Allow-Origin: *）
 *   - 边界为国家统计局标准（GB/T 2260），与 dayan 的 province_code/city_code/district_code 完全一致
 *   - {code}_full.json 返回该行政区的下一级子区域（如 330000_full → 浙江省 11 个地级市）
 *
 * 替代项目内静态 china.json + provinces/*.json（原方案 4.4MB，且无城市/区县级 GeoJSON）。
 * 采用 内存 Map + sessionStorage 双层缓存：同会话内同一区域只拉一次。
 */

/** DataV 行政区划边界 API 基址 */
const GEO_BASE = 'https://geo.datav.aliyun.com/areas_v3/bound/';

/** 内存缓存（同会话内、跨页面共享） */
const memCache = new Map<string, any>();

/** sessionStorage key 前缀 */
const SS_PREFIX = 'dayan-geo-';

/**
 * 按 6 位行政区划码拉取「自身 + 下一级子区域」GeoJSON。
 *
 * @param code 行政区划码：'100000'(全国→省) | 省码(→市) | 市码(→区县)
 * @returns GeoJSON FeatureCollection（features 为下一级子区域）
 */
export async function getRegionGeoJSON(code: string): Promise<any> {
  if (memCache.has(code)) return memCache.get(code);

  // sessionStorage 持久缓存（刷新页面后仍命中）
  const ssKey = SS_PREFIX + code;
  try {
    const cached = sessionStorage.getItem(ssKey);
    if (cached) {
      const json = JSON.parse(cached);
      memCache.set(code, json);
      return json;
    }
  } catch {
    // sessionStorage 不可用（隐私模式等）→ 走内存缓存即可
  }

  const resp = await fetch(`${GEO_BASE}${code}_full.json`);
  if (!resp.ok) {
    throw new Error(`GeoJSON ${code} 加载失败: HTTP ${resp.status}`);
  }
  const json = await resp.json();
  memCache.set(code, json);
  try {
    sessionStorage.setItem(ssKey, JSON.stringify(json));
  } catch {
    // 存储满或不可用 → 忽略，内存缓存仍生效
  }
  return json;
}
