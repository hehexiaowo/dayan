<template>
  <view class="region-page">
    <!-- 地图区：省=方格图 市=方格流 区/机构=真实地图 -->
    <view class="map-section" :class="{ schematic: !showRealMap }">
      <template v-if="!showRealMap">
        <ChinaTileMap
          :items="regionItems"
          :mode="level === 'province' ? 'grid' : 'flow'"
          @select="drill"
        />
      </template>
      <TiandituMap v-else :markers="mapMarkers" :center="mapCenter" :zoom="mapZoom" />
      <view class="map-label">
        <text class="map-label-text">{{ pageTitle }}</text>
      </view>
      <view class="map-count">
        <text class="map-count-num">{{ totalCount }}</text>
        <text class="map-count-unit">{{ level === 'park' ? '家机构' : '个区域' }}</text>
      </view>
    </view>

    <!-- 可点面包屑 -->
    <view class="breadcrumb">
      <template v-for="(c, i) in crumbs" :key="i">
        <text
          class="breadcrumb-item"
          :class="{ link: c.clickable, current: i === crumbs.length - 1 }"
          @click="onCrumb(i)"
        >{{ c.label }}</text>
        <text v-if="i < crumbs.length - 1" class="breadcrumb-sep">/</text>
      </template>
    </view>

    <!-- 列表区 -->
    <view class="list-section">
      <template v-if="loading">
        <DySkeleton v-for="i in 3" :key="i" :rows="1" avatar card />
      </template>

      <DyEmpty
        v-else-if="loadError"
        text="加载失败，请检查网络后重试"
        icon="!"
        color="gray"
        action-text="重新加载"
        @action="loadData"
      />

      <template v-else-if="level !== 'park'">
        <view class="list-header">
          <text>选择{{ levelName }}</text>
          <text v-if="regionItems.length" class="list-count">{{ regionItems.length }} 个</text>
        </view>
        <view
          v-for="item in regionItems"
          :key="item.code"
          class="region-item dy-clickable"
          @click="drill(item)"
        >
          <text class="region-name">{{ item.name }}</text>
          <view class="region-right">
            <text class="region-count">{{ item.count }} 家</text>
            <text class="region-arrow">›</text>
          </view>
        </view>
        <DyEmpty v-if="!regionItems.length" text="该区域暂无机构" icon="空" color="gray" />
      </template>

      <template v-else>
        <view class="list-header">
          <text>机构清单</text>
          <text class="list-count">{{ parkList.length }} 家</text>
        </view>
        <view
          v-for="park in parkList"
          :key="park.parkCode"
          class="park-card dy-clickable"
          @click="onParkClick(park.parkCode)"
        >
          <DyIconBlock :text="park.shortName?.charAt(0) || '机'" color="blue" size="md" />
          <view class="park-info">
            <text class="park-name">{{ park.fullName }}</text>
            <text class="park-addr">{{ formatAddress(park) }}</text>
            <view class="park-tags">
              <text v-if="park.minPriceDisplay" class="tag tag-price">
                ¥{{ park.minPriceDisplay }}{{ park.maxPriceDisplay ? '-' + park.maxPriceDisplay : '' }}/{{ park.priceUnit || '月' }}
              </text>
              <text v-if="park.availableBeds != null" class="tag tag-bed">余位 {{ park.availableBeds }}</text>
            </view>
          </view>
          <text class="park-arrow">›</text>
        </view>
        <DyEmpty v-if="!parkList.length" text="该区县暂无机构" icon="空" color="gray" />
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import TiandituMap from '@/components/TiandituMap/TiandituMap.vue';
import ChinaTileMap from '@/components/ChinaTileMap/ChinaTileMap.vue';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';
import { getRegions } from '@/api/park';
import { PROVINCE_CENTERS } from '@/utils/region';
import type { RegionItem, ParkCard, RegionQuery, ParkCategory, DrillLevel } from '@/types/park';
import { MUNICIPALITIES } from '@/types/park';

const category = ref<ParkCategory>('vital');
const level = ref<DrillLevel>('province');
const provinceCode = ref('');
const cityCode = ref('');
const districtCode = ref('');

const regionItems = ref<RegionItem[]>([]);
const parkList = ref<ParkCard[]>([]);
const crumbLabels = ref<string[]>([]); // 后端 breadcrumb 拆分
const center = ref<{ lng: number; lat: number } | null>(null);
const loading = ref(false);
const loadError = ref(false);

const LEVELS: DrillLevel[] = ['province', 'city', 'district', 'park'];

const categoryColor: Record<ParkCategory, string> = {
  vital: '#409eff', care: '#e6a23c', sojourn: '#67c23a',
};
const levelName = computed(
  () =>
    ({ province: '省份', city: '城市', district: '区县' } as Partial<Record<DrillLevel, string>>)[
      level.value
    ] || '',
);
const totalCount = computed(() => {
  if (level.value === 'park') return parkList.value.length;
  return regionItems.value.reduce((s, i) => s + i.count, 0);
});
const pageTitle = computed(() => {
  const cat = { vital: '活力长居', care: '照护长居', sojourn: '旅居养老' }[category.value];
  return `${cat} · ${levelName.value || '机构'}分布`;
});

/** 可点面包屑：labels[i] 对应 LEVELS[i]；当前层不可点 */
const crumbs = computed(() =>
  crumbLabels.value.map((label, i) => ({
    label,
    level: LEVELS[i] as DrillLevel | undefined,
    clickable:
      i < LEVELS.indexOf(level.value) &&
      !(LEVELS[i] === 'city' && MUNICIPALITIES.includes(provinceCode.value)),
  })),
);

function onCrumb(idx: number) {
  const target = crumbs.value[idx];
  if (!target?.clickable || !target.level) return;
  jumpTo(target.level);
}

/** 跳到指定层：保留该层所需 code，清空更深层 */
function jumpTo(target: DrillLevel) {
  level.value = target;
  if (target === 'province') { provinceCode.value = ''; cityCode.value = ''; districtCode.value = ''; }
  else if (target === 'city') { cityCode.value = ''; districtCode.value = ''; }
  else if (target === 'district') { districtCode.value = ''; }
  loadData();
}

function drill(item: RegionItem) {
  if (level.value === 'province') {
    provinceCode.value = item.code;
    if (MUNICIPALITIES.includes(item.code)) {
      cityCode.value = item.code.substring(0, 2) + '0100';
      level.value = 'district';
    } else {
      level.value = 'city';
    }
  } else if (level.value === 'city') {
    cityCode.value = item.code;
    level.value = 'district';
  } else if (level.value === 'district') {
    districtCode.value = item.code;
    level.value = 'park';
  }
  loadData();
}

async function loadData() {
  loading.value = true;
  center.value = null; // 加载期间不显示旧区域中心
  loadError.value = false;
  try {
    const params: RegionQuery = { category: category.value, level: level.value };
    if (level.value !== 'province') params.provinceCode = provinceCode.value;
    if (level.value === 'district' || level.value === 'park') params.cityCode = cityCode.value;
    if (level.value === 'park') params.districtCode = districtCode.value;

    const result = await getRegions(params);
    crumbLabels.value = (result.breadcrumb || '').split(' / ').filter(Boolean);
    regionItems.value = result.items || [];
    parkList.value = result.parkList || [];
    center.value =
      result.centerLng && result.centerLat
        ? { lng: result.centerLng, lat: result.centerLat }
        : null;
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

// ===== 地图（仅区级/机构层用真实地图） =====
const showRealMap = computed(() => level.value === 'district' || level.value === 'park');

const mapMarkers = computed(() => {
  if (level.value !== 'park') return [];
  return parkList.value
    .filter((p) => p.longitude && p.latitude)
    .map((p) => ({
      lng: p.longitude!, lat: p.latitude!,
      name: p.shortName || p.fullName,
      color: categoryColor[category.value],
    }));
});

const mapCenter = computed(() => {
  if (center.value) return center.value;
  // 兜底：省级中心点表 → 北京
  if (provinceCode.value && PROVINCE_CENTERS[provinceCode.value]) {
    return PROVINCE_CENTERS[provinceCode.value];
  }
  return { lng: 116.4, lat: 39.9 };
});

const mapZoom = computed(
  () => ({ province: 4, city: 7, district: 10, park: 12 })[level.value] || 5,
);

function onParkClick(parkCode: string) {
  uni.navigateTo({ url: `/pages/business/park/detail?parkCode=${parkCode}` });
}

function formatAddress(park: ParkCard): string {
  return [park.province, park.city, park.district, park.address].filter(Boolean).join(' ');
}

onLoad((options: any) => {
  if (options?.category) category.value = options.category;
  if (options?.level) level.value = options.level;
  if (options?.provinceCode) provinceCode.value = options.provinceCode;
  if (options?.cityCode) cityCode.value = options.cityCode;
  if (options?.districtCode) districtCode.value = options.districtCode;
  loadData();
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.region-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

/* 地图区域 */
.map-section {
  height: 500rpx;
  position: relative;
  flex-shrink: 0;
  border-bottom-left-radius: $radius-lg;
  border-bottom-right-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-card;
}
.map-section.schematic {
  height: auto;
  min-height: 420rpx;
  padding: 56rpx $spacing-md $spacing-lg;
  background: $bg-card;
}
.map-label {
  position: absolute;
  top: $spacing-sm;
  left: $spacing-md;
  background: rgba(255, 255, 255, 0.92);
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
  z-index: 10;
}
.map-label-text {
  font-size: 24rpx;
  color: $brand-primary;
  font-weight: 500;
}
.map-count {
  position: absolute;
  bottom: $spacing-sm;
  right: $spacing-md;
  background: rgba(0, 0, 0, 0.55);
  padding: 8rpx 24rpx;
  border-radius: 20rpx;
  z-index: 10;
}
.map-count-num {
  font-size: 28rpx;
  color: #fff;
  font-weight: bold;
}
.map-count-unit {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-left: 4rpx;
}

/* 面包屑 */
.breadcrumb {
  padding: $spacing-md $spacing-lg;
  background: $bg-card;
  border-bottom: 1rpx solid $border-light;
}
.breadcrumb-item { font-size: 24rpx; color: $text-secondary; padding: 8rpx 4rpx; }
.breadcrumb-item.link { color: $brand-primary; }
.breadcrumb-item.current { color: $text-primary; font-weight: 500; }
.breadcrumb-sep { font-size: 24rpx; color: $text-placeholder; margin: 0 6rpx; }

/* 列表区域 */
.list-section {
  flex: 1;
  background: $bg-page;
  padding: $spacing-sm $spacing-md;
}
.list-header {
  padding: $spacing-md;
  font-size: 26rpx;
  color: $text-secondary;
  display: flex;
  align-items: center;
}
.list-count {
  color: $text-placeholder;
  margin-left: $spacing-xs;
}

/* 区域列表项 */
.region-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx $spacing-lg;
  background: $bg-card;
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}
.region-name {
  flex: 1;
  font-size: 32rpx;
  color: $text-primary;
  font-weight: 500;
}
.region-right {
  display: flex;
  align-items: center;
}
.region-count {
  font-size: 26rpx;
  color: $brand-primary;
  margin-right: $spacing-sm;
}
.region-arrow {
  color: $text-placeholder;
  font-size: 36rpx;
}

/* 机构卡片 */
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
.park-arrow {
  color: $text-placeholder;
  font-size: 36rpx;
  flex-shrink: 0;
}
</style>
