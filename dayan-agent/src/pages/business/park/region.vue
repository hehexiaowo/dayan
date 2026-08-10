<template>
  <view class="region-page">
    <!-- 地图区域（固定顶部） -->
    <view class="map-section">
      <TiandituMap :markers="mapMarkers" :center="mapCenter" :zoom="mapZoom" />
      <view class="map-label">
        <text class="map-label-text">{{ pageTitle }}</text>
      </view>
      <view class="map-count">
        <text class="map-count-num">{{ totalCount }}</text>
        <text class="map-count-unit">{{ level === 'park' ? '家机构' : '个区域' }}</text>
      </view>
    </view>

    <!-- 面包屑 -->
    <view class="breadcrumb">
      <text class="breadcrumb-text">{{ breadcrumb || '加载中...' }}</text>
    </view>

    <!-- 列表区域 -->
    <view class="list-section">
      <!-- 加载骨架屏 -->
      <template v-if="loading">
        <DySkeleton v-for="i in 3" :key="i" :rows="1" avatar card />
      </template>

      <!-- 下钻层：显示区域列表 -->
      <template v-else-if="level !== 'park'">
        <view class="list-header">
          <text>选择{{ levelName }}</text>
          <text v-if="regionItems.length" class="list-count">{{ regionItems.length }} 个</text>
        </view>
        <view
          v-for="item in regionItems"
          :key="item.code"
          class="region-item dy-clickable"
          @click="onRegionClick(item)"
        >
          <text class="region-name">{{ item.name }}</text>
          <view class="region-right">
            <text class="region-count">{{ item.count }} 家</text>
            <text class="region-arrow">›</text>
          </view>
        </view>
        <DyEmpty
          v-if="!loading && !regionItems.length"
          text="该区域暂无机构"
          icon="空"
          color="gray"
        />
      </template>

      <!-- 末层：显示机构卡片清单 -->
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
              <text v-if="park.availableBeds != null" class="tag tag-bed">
                余位 {{ park.availableBeds }}
              </text>
            </view>
          </view>
          <text class="park-arrow">›</text>
        </view>
        <DyEmpty
          v-if="!loading && !parkList.length"
          text="该区县暂无机构"
          icon="空"
          color="gray"
        />
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import TiandituMap from '@/components/TiandituMap/TiandituMap.vue';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';
import { getRegions } from '@/api/park';
import type { RegionItem, ParkCard, RegionQuery, ParkCategory, DrillLevel } from '@/types/park';
import { MUNICIPALITIES } from '@/types/park';

// 路由参数状态
const category = ref<ParkCategory>('vital');
const level = ref<DrillLevel>('province');
const provinceCode = ref('');
const cityCode = ref('');
const districtCode = ref('');

// 数据
const regionItems = ref<RegionItem[]>([]);
const parkList = ref<ParkCard[]>([]);
const breadcrumb = ref('');
const loading = ref(false);

// 分类颜色（地图 pin 用）
const categoryColor: Record<ParkCategory, string> = {
  vital: '#409eff',
  care: '#e6a23c',
  sojourn: '#67c23a',
};

const levelName = computed(() => {
  return { province: '省份', city: '城市', district: '区县' }[level.value] || '';
});

const totalCount = computed(() => {
  if (level.value === 'park') return parkList.value.length;
  return regionItems.value.reduce((sum, i) => sum + i.count, 0);
});

const pageTitle = computed(() => {
  const catName = { vital: '活力长居', care: '照护长居', sojourn: '旅居养老' }[category.value];
  return `${catName} · ${levelName.value || '机构'}分布`;
});

// 地图标记
const mapMarkers = computed(() => {
  if (level.value === 'park') {
    // 末层：从机构列表提取坐标
    return parkList.value
      .filter(p => p.longitude && p.latitude)
      .map(p => ({
        lng: p.longitude!,
        lat: p.latitude!,
        name: p.shortName || p.fullName,
        color: categoryColor[category.value],
      }));
  }
  // 非末层：区域列表无坐标，显示空地图（中心可放全国/省中心）
  return [];
});

const mapCenter = computed(() => {
  // 简化：全国用北京中心，后续可按 provinceCode 精确定位
  return { lng: 116.4, lat: 39.9 };
});

const mapZoom = computed(() => {
  return { province: 4, city: 7, district: 10, park: 12 }[level.value] || 5;
});

async function loadData() {
  loading.value = true;
  try {
    const params: RegionQuery = {
      category: category.value,
      level: level.value,
    };
    if (level.value !== 'province') params.provinceCode = provinceCode.value;
    if (level.value === 'district' || level.value === 'park') params.cityCode = cityCode.value;
    if (level.value === 'park') params.districtCode = districtCode.value;

    const result = await getRegions(params);
    breadcrumb.value = result.breadcrumb || '';
    regionItems.value = result.items || [];
    parkList.value = result.parkList || [];
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

// 区域名点击下钻
function onRegionClick(item: RegionItem) {
  if (level.value === 'province') {
    // 判断直辖市：跳过 city 层
    if (MUNICIPALITIES.includes(item.code)) {
      // 直辖市市级码规律：省码前2位 + 0100（如 110000 → 110100）
      const cityCodeForMunicipality = item.code.substring(0, 2) + '0100';
      uni.navigateTo({
        url: `/pages/business/park/region?category=${category.value}&level=district&provinceCode=${item.code}&cityCode=${cityCodeForMunicipality}`,
      });
    } else {
      uni.navigateTo({
        url: `/pages/business/park/region?category=${category.value}&level=city&provinceCode=${item.code}`,
      });
    }
  } else if (level.value === 'city') {
    uni.navigateTo({
      url: `/pages/business/park/region?category=${category.value}&level=district&provinceCode=${provinceCode.value}&cityCode=${item.code}`,
    });
  } else if (level.value === 'district') {
    uni.navigateTo({
      url: `/pages/business/park/region?category=${category.value}&level=park&provinceCode=${provinceCode.value}&cityCode=${cityCode.value}&districtCode=${item.code}`,
    });
  }
}

function onParkClick(parkCode: string) {
  uni.navigateTo({
    url: `/pages/business/park/detail?parkCode=${parkCode}`,
  });
}

function formatAddress(park: ParkCard): string {
  const parts = [park.province, park.city, park.district, park.address].filter(Boolean);
  return parts.join(' ');
}

// uni-app 页面生命周期：接收路由参数
onLoad((options: any) => {
  if (options.category) category.value = options.category;
  if (options.level) level.value = options.level;
  if (options.provinceCode) provinceCode.value = options.provinceCode;
  if (options.cityCode) cityCode.value = options.cityCode;
  if (options.districtCode) districtCode.value = options.districtCode;
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
.breadcrumb-text {
  font-size: 24rpx;
  color: $text-secondary;
}

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
