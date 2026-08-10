<template>
  <view class="district-page">
    <!-- Leaflet 地图区 -->
    <view class="map-section">
      <!-- #ifdef H5 -->
      <div id="care-district-map" class="map-container"></div>
      <!-- #endif -->
      <!-- #ifndef H5 -->
      <view class="map-placeholder">
        <text class="map-placeholder-text">地图组件（仅 H5 支持）</text>
      </view>
      <!-- #endif -->

      <!-- 统计卡片 -->
      <view v-if="parks.length" class="stats-card">
        <text class="stat-value">{{ parks.length }}</text>
        <text class="stat-label">家机构</text>
      </view>
    </view>

    <!-- 机构列表 -->
    <view class="park-list">
      <view
        v-for="park in parks"
        :key="park.parkCode"
        class="park-card dy-clickable"
        @click="onParkClick(park)"
      >
        <DyIconBlock
          :text="park.shortName?.charAt(0) || '机'"
          :color="iconColor"
          size="md"
        />
        <view class="park-info">
          <text class="park-name">{{ park.fullName }}</text>
          <text class="park-addr">{{ formatAddress(park) }}</text>
          <view class="park-tags">
            <text v-if="park.minPriceDisplay" class="tag tag-price">
              ¥{{ park.minPriceDisplay }}{{ park.maxPriceDisplay ? '-' + park.maxPriceDisplay : '' }}/{{ park.priceUnit || '月' }}
            </text>
            <text v-if="park.availableBeds != null" class="tag tag-bed">
              余位 {{ park.availableBeds }}
            </text>
            <text v-for="tag in parseNetworkTags(park.networkTags)" :key="tag.label" class="tag" :class="'tag-' + tag.color">
              {{ tag.label }}
            </text>
          </view>
        </view>
        <text class="park-arrow">›</text>
      </view>
      <DyEmpty v-if="!loading && !parks.length" text="该区县暂无机构" icon="空" color="gray" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
// #ifdef H5
import { initMap, addMarkers, searchByName, destroyMap, type MapMarkerItem } from '@/utils/map';
import type L from 'leaflet';
// #endif
import { getRegions } from '@/api/park';
import { PROVINCE_CENTERS } from '@/utils/region';
import type { ParkCard } from '@/types/park';
import { parseNetworkTags } from '@/types/park';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const provinceCode = ref('');
const cityCode = ref('');
const districtCode = ref('');
const parks = ref<ParkCard[]>([]);
const loading = ref(true);

// #ifdef H5
let map: L.Map | null = null;
let markerGroup: L.LayerGroup | null = null;
// #endif

const iconColor = 'orange' as const;
const categoryColorHex = '#ff9900' as const;

async function fetchData() {
  loading.value = true;
  try {
    const result = await getRegions({
      category: 'care',
      level: 'park',
      provinceCode: provinceCode.value,
      cityCode: cityCode.value,
      districtCode: districtCode.value,
    });
    parks.value = result.parkList || [];
    await nextTick();
    // #ifdef H5
    setTimeout(initMapView, 300);
    // #endif
  } catch {
    parks.value = [];
  } finally {
    loading.value = false;
  }
}

// #ifdef H5
function initMapView() {
  map = initMap('care-district-map');
  if (!map) return;

  // 先设一个初始中心点（省中心或北京）
  const fallback = PROVINCE_CENTERS[provinceCode.value] || { lng: 116.4, lat: 39.9 };
  map.setView([fallback.lat, fallback.lng], 10);

  const itemsWithCoords: MapMarkerItem[] = parks.value
    .filter((p) => p.latitude && p.longitude)
    .map((p) => ({
      latitude: p.latitude!,
      longitude: p.longitude!,
      name: p.shortName || p.fullName,
      code: p.parkCode,
      color: categoryColorHex,
    }));

  if (itemsWithCoords.length > 0) {
    markerGroup = addMarkers(map, itemsWithCoords, (item) => {
      uni.navigateTo({
        url: `/pages/business/park/care/detail?parkCode=${item.code}`,
      });
    });
  } else {
    // 无坐标机构 → 按区县名搜索定位
    const districtName = parks.value[0]?.district;
    if (districtName) {
      searchByName(map, districtName);
    }
  }
}
// #endif

function onParkClick(park: ParkCard) {
  uni.navigateTo({
    url: `/pages/business/park/care/detail?parkCode=${park.parkCode}`,
  });
}

function formatAddress(park: ParkCard): string {
  return [park.province, park.city, park.district, park.address].filter(Boolean).join(' ');
}

onLoad((options: any) => {
  if (options?.provinceCode) provinceCode.value = options.provinceCode;
  if (options?.cityCode) cityCode.value = options.cityCode;
  if (options?.districtCode) districtCode.value = options.districtCode;
});

onMounted(fetchData);

onUnmounted(() => {
  // #ifdef H5
  markerGroup = null;
  destroyMap(map);
  map = null;
  // #endif
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.district-page {
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
  height: 280px;
}
.map-placeholder {
  width: 100%;
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff7e6, #ffe8cc);
}
.map-placeholder-text {
  font-size: 28rpx;
  color: $text-placeholder;
}

/* 统计卡片 */
.stats-card {
  position: absolute;
  bottom: $spacing-sm;
  right: $spacing-md;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 20rpx;
  padding: 8rpx 24rpx;
  display: flex;
  align-items: baseline;
}
.stat-value {
  font-size: 28rpx;
  font-weight: bold;
  color: #fff;
}
.stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-left: 4rpx;
}

/* 机构列表 */
.park-list {
  padding: $spacing-sm $spacing-md;
}
.park-card {
  display: flex;
  align-items: center;
  padding: $spacing-lg;
  margin-bottom: $spacing-sm;
  background: $bg-card;
  border-radius: $radius-md;
  box-shadow: $shadow-card;
}
.park-info {
  margin-left: $spacing-md;
  flex: 1;
}
.park-name {
  display: block;
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}
.park-addr {
  display: block;
  font-size: 22rpx;
  color: $text-secondary;
  margin-top: $spacing-xs;
}
.park-tags {
  margin-top: $spacing-sm;
  display: flex;
  gap: $spacing-sm;
  flex-wrap: wrap;
}
.tag {
  font-size: 20rpx;
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
}
.tag-price {
  background: $brand-warning-light;
  color: $brand-warning;
}
.tag-bed {
  background: $brand-success-light;
  color: $brand-success;
}
.tag-type {
  background: $brand-warning-light;
  color: $brand-warning;
}
.tag-blue {
  background: $brand-primary-light;
  color: $brand-primary;
}
.tag-orange {
  background: $brand-warning-light;
  color: $brand-warning;
}
.tag-green {
  background: $brand-success-light;
  color: $brand-success;
}
.park-arrow {
  color: $text-placeholder;
  font-size: 36rpx;
  flex-shrink: 0;
}
</style>
