<template>
  <view class="region-page">
    <!-- 地图区域（固定顶部） -->
    <view class="map-section">
      <TiandituMap :markers="mapMarkers" :center="mapCenter" :zoom="mapZoom" />
      <view class="map-label">📍 {{ pageTitle }}</view>
      <view class="map-count">{{ totalCount }} {{ level === 'park' ? '家机构' : '个区域' }}</view>
    </view>

    <!-- 面包屑 -->
    <view class="breadcrumb">
      <text>{{ breadcrumb || '加载中...' }}</text>
    </view>

    <!-- 列表区域 -->
    <view class="list-section">
      <!-- 下钻层：显示区域列表 -->
      <template v-if="level !== 'park'">
        <view class="list-header">
          选择{{ levelName }}
          <text v-if="regionItems.length" class="list-count">({{ regionItems.length }})</text>
        </view>
        <view
          v-for="item in regionItems"
          :key="item.code"
          class="region-item"
          @click="onRegionClick(item)"
        >
          <text class="region-name">{{ item.name }}</text>
          <text class="region-count">{{ item.count }} 家</text>
          <text class="region-arrow">›</text>
        </view>
        <view v-if="!loading && !regionItems.length" class="empty-state">
          <text>该区域暂无机构</text>
        </view>
      </template>

      <!-- 末层：显示机构卡片清单 -->
      <template v-else>
        <view class="list-header">机构清单({{ parkList.length }})</view>
        <view
          v-for="park in parkList"
          :key="park.parkCode"
          class="park-card"
          @click="onParkClick(park.parkCode)"
        >
          <view class="park-img">🏥</view>
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
        <view v-if="!loading && !parkList.length" class="empty-state">
          <text>该区县暂无机构</text>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import TiandituMap from '@/components/TiandituMap/TiandituMap.vue';
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
      // 直辖市直接到 district
      const cityCodeForMunicipality = item.code.substring(0, 4) + '00';
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

<style scoped>
.region-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}
.map-section {
  height: 500rpx; /* 约 35vh */
  position: relative;
  flex-shrink: 0;
}
.map-label {
  position: absolute;
  top: 16rpx;
  left: 20rpx;
  background: rgba(255, 255, 255, 0.92);
  padding: 8rpx 20rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  color: #409eff;
  z-index: 10;
}
.map-count {
  position: absolute;
  bottom: 16rpx;
  right: 20rpx;
  background: rgba(0, 0, 0, 0.5);
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  color: #fff;
  z-index: 10;
}
.breadcrumb {
  padding: 16rpx 24rpx;
  background: #fff;
  font-size: 24rpx;
  color: #909399;
  border-bottom: 1rpx solid #f0f0f0;
}
.list-section {
  flex: 1;
  background: #fff;
}
.list-header {
  padding: 24rpx;
  font-size: 26rpx;
  color: #909399;
}
.list-count {
  color: #c0c4cc;
  margin-left: 8rpx;
}
.region-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f5f7fa;
}
.region-name {
  flex: 1;
  font-size: 30rpx;
  color: #303133;
}
.region-count {
  font-size: 24rpx;
  color: #c0c4cc;
  margin-right: 16rpx;
}
.region-arrow {
  color: #c0c4cc;
  font-size: 28rpx;
}
.park-card {
  display: flex;
  align-items: center;
  padding: 24rpx;
  margin: 16rpx;
  background: #fff;
  border-radius: 16rpx;
  border: 1rpx solid #ebeef5;
}
.park-img {
  width: 100rpx;
  height: 100rpx;
  border-radius: 16rpx;
  background: #e8f0fe;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  flex-shrink: 0;
}
.park-info {
  margin-left: 20rpx;
  flex: 1;
}
.park-name {
  display: block;
  font-size: 28rpx;
  font-weight: bold;
  color: #303133;
}
.park-addr {
  display: block;
  font-size: 22rpx;
  color: #909399;
  margin-top: 8rpx;
}
.park-tags {
  margin-top: 10rpx;
  display: flex;
  gap: 12rpx;
}
.tag {
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 12rpx;
}
.tag-price {
  background: #fdf6ec;
  color: #e6a23c;
}
.tag-bed {
  background: #f0f9eb;
  color: #67c23a;
}
.park-arrow {
  color: #c0c4cc;
  font-size: 28rpx;
  flex-shrink: 0;
}
.empty-state {
  padding: 80rpx 0;
  text-align: center;
  color: #c0c4cc;
  font-size: 26rpx;
}
</style>
