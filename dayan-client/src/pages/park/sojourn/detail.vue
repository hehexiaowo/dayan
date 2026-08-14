<template>
  <view class="detail-page">
    <!-- 骨架屏 -->
    <view v-if="loading" class="skeleton-wrap">
      <DySkeleton :rows="1" avatar card />
      <DySkeleton :rows="4" card />
      <DySkeleton :rows="3" card />
    </view>

    <!-- 错误重试 -->
    <DyEmpty
      v-else-if="loadError"
      text="加载失败，请重试"
      icon="!"
      color="gray"
      action-text="重新加载"
      @action="retryLoad"
    />

    <!-- 正常内容 -->
    <view v-else-if="detail" class="detail-content">
      <!-- Banner 轮播 -->
      <view v-if="bannerImages.length" class="banner-section">
        <swiper
          class="banner-swiper"
          :autoplay="true"
          :interval="3000"
          :circular="true"
          :indicator-dots="false"
          @change="onSwiperChange"
        >
          <swiper-item v-for="(img, i) in bannerImages" :key="i" @click="previewBanner(Number(i))">
            <image :src="img" class="banner-img" mode="aspectFill" />
          </swiper-item>
        </swiper>
        <view class="banner-indicator">
          <text>{{ swiperCurrent + 1 }}/{{ bannerImages.length }}</text>
        </view>
      </view>

      <!-- 信息卡片 -->
      <view class="info-card">
        <text class="park-name">{{ park.fullName }}</text>
        <view class="park-tags">
          <text v-for="tag in networkTagItems" :key="tag.label" class="dy-tag" :class="'dy-tag-' + tag.color">{{ tag.label }}</text>
          <text v-if="park.natureTypeDescription" class="dy-tag dy-tag-green">{{ park.natureTypeDescription }}</text>
          <text v-if="park.isHot === 1" class="dy-tag dy-tag-red">热门</text>
        </view>

        <view class="info-row">
          <text class="info-icon">📍</text>
          <text class="info-value">{{ formatAddress(park) }}</text>
          <text class="copy-btn" @click="onCopy(formatAddress(park))">复制</text>
        </view>
        <view v-if="park.serviceHotline" class="info-row" @click="onCall">
          <text class="info-icon">📞</text>
          <text class="info-value phone-link">{{ park.serviceHotline }}</text>
          <text class="copy-btn">拨打</text>
        </view>
        <view v-if="park.totalBeds != null" class="info-row">
          <text class="info-icon">🛏</text>
          <text class="info-value">{{ park.totalBeds }} 张床位</text>
          <text v-if="park.availableBeds != null" class="beds-tag">余位 {{ park.availableBeds }}</text>
        </view>
        <view v-if="park.minPriceDisplay" class="info-row">
          <text class="info-icon">💰</text>
          <text class="price-highlight">¥{{ park.minPriceDisplay }}{{ park.maxPriceDisplay ? '-' + park.maxPriceDisplay : '' }}</text>
          <text class="price-unit">/{{ park.priceUnit || '月' }}</text>
        </view>
      </view>

      <!-- Sticky Tab 栏 -->
      <view class="tabs-bar" :class="{ sticky: tabsSticky }">
        <scroll-view scroll-x class="tabs-scroll" :show-scrollbar="false">
          <view class="tabs-inner">
            <text
              v-for="(tab, i) in visibleTabs"
              :key="tab.key"
              class="tab-item"
              :class="{ active: activeTab === i }"
              @click="activeTab = i"
            >{{ tab.label }}</text>
          </view>
        </scroll-view>
      </view>

      <!-- Tab 内容区 -->
      <view class="tab-content">
        <!-- 基础信息 -->
        <view v-if="currentTabKey === 'base'" class="tab-panel">
          <view class="stat-grid">
            <view v-if="park.totalBeds != null" class="stat-item">
              <text class="stat-value green">{{ park.totalBeds }}</text>
              <text class="stat-label">总床位</text>
            </view>
            <view v-if="park.availableBeds != null" class="stat-item">
              <text class="stat-value green">{{ park.availableBeds }}</text>
              <text class="stat-label">可用床位</text>
            </view>
            <view v-if="park.staffCount != null" class="stat-item">
              <text class="stat-value orange">{{ park.staffCount }}</text>
              <text class="stat-label">员工数</text>
            </view>
            <view v-if="park.nurseCount != null" class="stat-item">
              <text class="stat-value green">{{ park.nurseCount }}</text>
              <text class="stat-label">护理员</text>
            </view>
          </view>
          <view v-if="park.nursePatientRatio" class="kv-row">
            <text class="kv-label">护患比</text>
            <text class="kv-value">{{ park.nursePatientRatio }}</text>
          </view>
          <view v-if="park.checkInAgeMin || park.checkInAgeMax" class="kv-row">
            <text class="kv-label">入住年龄</text>
            <text class="kv-value">{{ park.checkInAgeMin || '-' }} ~ {{ park.checkInAgeMax || '-' }} 岁</text>
          </view>
          <view v-if="park.openingTime" class="kv-row">
            <text class="kv-label">开业时间</text>
            <text class="kv-value">{{ formatDate(park.openingTime) }}</text>
          </view>
          <view v-if="park.baseDescription" class="desc-block">
            <text class="desc-title">机构简介</text>
            <rich-text :nodes="park.baseDescription" class="desc-text" />
          </view>
          <view v-if="park.specialtyDescription" class="desc-block">
            <text class="desc-title">机构特色</text>
            <rich-text :nodes="park.specialtyDescription" class="desc-text" />
          </view>
        </view>

        <!-- 房型介绍 -->
        <view v-if="currentTabKey === 'room'" class="tab-panel">
          <view
            v-for="room in detail.roomTypes"
            :key="room.id"
            class="room-card"
            @click="previewRoomImages(room)"
          >
            <image
              v-if="formatFileUrl(room.coverImage)"
              :src="formatFileUrl(room.coverImage)"
              class="room-img"
              mode="aspectFill"
            />
            <view class="room-info">
              <text class="room-name">{{ room.roomTypeName }}</text>
              <view class="room-meta">
                <text v-if="room.area" class="room-tag">{{ room.area }}㎡</text>
                <text v-if="room.bedCount" class="room-tag">{{ room.bedCount }}人</text>
                <text v-if="room.orientation" class="room-tag">{{ room.orientation }}</text>
                <text v-if="room.totalRooms" class="room-tag">{{ room.totalRooms }}间</text>
              </view>
              <view v-if="room.facilities || hasAnyFacility(room)" class="room-facilities">
                <text v-if="room.hasBathroom === 1" class="facility-icon">卫</text>
                <text v-if="room.hasAircon === 1" class="facility-icon">空</text>
                <text v-if="room.hasTv === 1" class="facility-icon">视</text>
                <text v-if="room.hasWifi === 1" class="facility-icon">网</text>
                <text v-if="room.hasFridge === 1" class="facility-icon">冰</text>
                <text v-if="room.hasBalcony === 1" class="facility-icon">台</text>
                <text v-if="room.hasKitchen === 1" class="facility-icon">厨</text>
                <text v-if="room.hasWasher === 1" class="facility-icon">洗</text>
              </view>
              <text v-if="room.description" class="room-desc">{{ room.description }}</text>
            </view>
          </view>
        </view>

        <!-- 收费方案 -->
        <view v-if="currentTabKey === 'fee'" class="tab-panel">
          <view class="fee-tip">
            <text>当前价格仅供参考，具体费用以机构实际报价为准。</text>
          </view>
          <view v-for="fee in detail.pricingList" :key="fee.id" class="fee-card">
            <view class="fee-header">
              <text class="fee-name">{{ fee.planName }}</text>
              <text v-if="fee.isCurrent === 1" class="fee-current">现行</text>
              <text v-if="fee.isPromotion === 1" class="fee-promo">促销</text>
            </view>
            <view class="fee-row">
              <text v-if="fee.salePrice" class="fee-price">¥{{ fee.salePrice }}</text>
              <text v-if="fee.priceUnit" class="fee-unit">/{{ fee.priceUnit }}</text>
              <text v-if="fee.originalPrice && fee.originalPrice !== fee.salePrice" class="fee-original">¥{{ fee.originalPrice }}</text>
            </view>
            <view v-if="fee.refName" class="kv-row">
              <text class="kv-label">关联项目</text>
              <text class="kv-value">{{ fee.refName }}</text>
            </view>
            <view v-if="fee.includesItems" class="kv-row">
              <text class="kv-label">包含项目</text>
              <text class="kv-value">{{ fee.includesItems }}</text>
            </view>
            <view v-if="fee.promotionDescription" class="kv-row">
              <text class="kv-label">促销说明</text>
              <text class="kv-value">{{ fee.promotionDescription }}</text>
            </view>
          </view>
        </view>

        <!-- 照护服务 -->
        <view v-if="currentTabKey === 'care'" class="tab-panel">
          <view v-for="care in detail.careTypes" :key="care.id" class="care-card">
            <view class="care-header">
              <text class="care-name">{{ care.careTypeName }}</text>
              <text v-if="care.careLevel" class="care-level">Lv.{{ care.careLevel }}</text>
            </view>
            <view v-if="care.careTarget" class="kv-row">
              <text class="kv-label">服务对象</text>
              <text class="kv-value">{{ care.careTarget }}</text>
            </view>
            <view v-if="care.careItems" class="kv-row">
              <text class="kv-label">照护项目</text>
              <text class="kv-value">{{ care.careItems }}</text>
            </view>
            <view v-if="care.careFrequency" class="kv-row">
              <text class="kv-label">服务频率</text>
              <text class="kv-value">{{ care.careFrequency }}</text>
            </view>
            <view v-if="care.nursePatientRatio" class="kv-row">
              <text class="kv-label">护患比</text>
              <text class="kv-value">{{ care.nursePatientRatio }}</text>
            </view>
            <text v-if="care.description" class="care-desc">{{ care.description }}</text>
          </view>
        </view>

        <!-- 餐饮设施 -->
        <view v-if="currentTabKey === 'catering'" class="tab-panel">
          <!-- 餐饮 -->
          <view v-if="detail.foodTypes?.length" class="sub-section">
            <text class="sub-title">🍽 餐饮服务</text>
            <view v-for="food in detail.foodTypes" :key="food.id" class="item-card">
              <text class="item-name">{{ food.foodTypeName }}</text>
              <text v-if="food.dietFeatures" class="item-desc">{{ food.dietFeatures }}</text>
              <text v-if="food.sampleMenu" class="item-desc">样例菜单：{{ food.sampleMenu }}</text>
            </view>
          </view>
          <!-- 设施 -->
          <view v-if="detail.facilityTypes?.length" class="sub-section">
            <text class="sub-title">🏗 配套设施</text>
            <view v-for="fac in detail.facilityTypes" :key="fac.id" class="item-card">
              <text class="item-name">{{ fac.facilityTypeName }}</text>
              <text v-if="fac.openTime" class="item-desc">开放时间：{{ fac.openTime }}</text>
              <text v-if="fac.facilityTypeDescription" class="item-desc">{{ fac.facilityTypeDescription }}</text>
            </view>
          </view>
          <!-- 服务 -->
          <view v-if="detail.serviceTypes?.length" class="sub-section">
            <text class="sub-title">⛑ 服务项目</text>
            <view v-for="svc in detail.serviceTypes" :key="svc.id" class="item-card">
              <text class="item-name">{{ svc.serviceTypeName }}</text>
              <text v-if="svc.serviceTypeDescription" class="item-desc">{{ svc.serviceTypeDescription }}</text>
            </view>
          </view>
        </view>

        <!-- 交通周边 -->
        <view v-if="currentTabKey === 'traffic'" class="tab-panel">
          <!-- 地图 -->
          <view v-if="park.latitude && park.longitude" class="map-block">
            <!-- #ifdef H5 -->
            <div id="detail-map" class="detail-map-container"></div>
            <!-- #endif -->
          </view>
          <view class="address-block">
            <text class="info-icon">📍</text>
            <text class="address-text">{{ formatAddress(park) }}</text>
          </view>
          <!-- 周边配套 -->
          <view v-if="peripheryGroups.length" class="rim-section">
            <view class="rim-tabs">
              <text
                v-for="(g, i) in peripheryGroups"
                :key="g.type"
                class="rim-tab"
                :class="{ active: activeRim === i }"
                @click="activeRim = i"
              >{{ g.label }}</text>
            </view>
            <view v-if="currentRimList.length" class="rim-list">
              <view v-for="(place, i) in currentRimList" :key="i" class="rim-item">
                <text class="rim-name">{{ place.placeName }}</text>
                <text v-if="place.distance" class="rim-distance">{{ place.distance }}</text>
                <text v-if="place.placeAddress" class="rim-addr">{{ place.placeAddress }}</text>
              </view>
            </view>
            <DyEmpty v-else text="暂无数据" icon="空" color="gray" />
          </view>
        </view>
      </view>

      <!-- 图文展示板块 -->
      <view v-if="detail.displayBlocks?.length" class="display-section">
        <view v-for="block in detail.displayBlocks" :key="block.id" class="display-block">
          <text v-if="block.blockTitle" class="block-title">{{ block.blockTitle }}</text>
          <rich-text v-if="block.content" :nodes="block.content" class="block-content" />
          <view v-if="parseImageList(block.images).length" class="block-images">
            <image
              v-for="(img, i) in parseImageList(block.images)"
              :key="i"
              :src="img"
              class="block-img"
              mode="aspectFill"
              @click="previewImageList(parseImageList(block.images), i)"
            />
          </view>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <DyEmpty
      v-else
      text="机构信息不存在"
      icon="空"
      color="gray"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted, nextTick } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getParkFullDetail } from '@/api/park';
import type { ParkFullDetail, ParkDetail, ParkRoomType, ParkPeriphery } from '@/types/park';
import { NETWORK_TAG_LABELS } from '@/types/park';
import { formatFileUrl, parseImageList } from '@/utils/file';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const detail = ref<ParkFullDetail | null>(null);
const loading = ref(true);
const loadError = ref(false);
const swiperCurrent = ref(0);
const activeTab = ref(0);
const activeRim = ref(0);
const tabsSticky = ref(false);
let parkCode = '';

// #ifdef H5
let detailMap: any = null;
// #endif

const park = computed<ParkDetail>(() => detail.value?.parkInfo || ({} as ParkDetail));

/** 网络归属标签（直接从 networkTags 字段渲染） */
const networkTagItems = computed(() =>
  (park.value.networkTags || [])
    .filter((t) => NETWORK_TAG_LABELS[t])
    .map((t) => NETWORK_TAG_LABELS[t]),
);

/** Banner 图片：优先从 sojournConfig.banners 取，fallback 到全部 type=1 图 */
const NETWORK = 'sojourn' as const;
const CONFIG_FIELD: Record<string, 'vitalConfig' | 'careConfig' | 'sojournConfig'> = { vital: 'vitalConfig', care: 'careConfig', sojourn: 'sojournConfig' };
const bannerImages = computed(() => {
  const configRaw = park.value[CONFIG_FIELD[NETWORK]];
  if (configRaw) {
    try {
      const parsed = JSON.parse(configRaw);
      if (Array.isArray(parsed.banners) && parsed.banners.length) {
        return parsed.banners.map((k: string) => formatFileUrl(k)).filter(Boolean);
      }
    } catch {}
  }
  const assets = detail.value?.assets || [];
  return assets
    .filter((a) => a.assetType === 1)
    .sort((a, b) => (b.isCover || 0) - (a.isCover || 0))
    .map((a) => formatFileUrl(a.assetUrl))
    .filter(Boolean);
});

/** Tab 定义 + 可见性判断 */
const allTabs = [
  { key: 'base', label: '基础信息' },
  { key: 'room', label: '房型介绍' },
  { key: 'fee', label: '收费方案' },
  { key: 'care', label: '照护服务' },
  { key: 'catering', label: '餐饮设施' },
  { key: 'traffic', label: '交通周边' },
] as const;

const visibleTabs = computed(() => {
  const d = detail.value;
  if (!d) return [{ key: 'base' as const, label: '基础信息' }];
  return allTabs.filter((t) => {
    if (t.key === 'room') return (d.roomTypes?.length || 0) > 0;
    if (t.key === 'fee') return (d.pricingList?.length || 0) > 0;
    if (t.key === 'care') return (d.careTypes?.length || 0) > 0;
    if (t.key === 'catering')
      return (d.foodTypes?.length || 0) + (d.facilityTypes?.length || 0) + (d.serviceTypes?.length || 0) > 0;
    if (t.key === 'traffic')
      return !!(park.value.latitude && park.value.longitude) || (d.peripheries?.length || 0) > 0;
    return true; // base 永远显示
  });
});

const currentTabKey = computed(() => visibleTabs.value[activeTab.value]?.key || 'base');

/** 周边配套分组（按 peripheryType 分：交通/景点/医疗/购物） */
const peripheryTypeMap: Record<number, string> = {
  1: '交通', 2: '景点', 3: '医疗', 4: '购物',
};

const peripheryGroups = computed(() => {
  const peripheries = detail.value?.peripheries || [];
  const groups: { type: number; label: string; items: ParkPeriphery[] }[] = [];
  const typeSet = new Set(peripheries.map((p) => p.peripheryType || 0));
  typeSet.forEach((t) => {
    groups.push({
      type: t,
      label: peripheryTypeMap[t] || '其他',
      items: peripheries.filter((p) => (p.peripheryType || 0) === t),
    });
  });
  return groups;
});

const currentRimList = computed(() => peripheryGroups.value[activeRim.value]?.items || []);

// ===== Tab 切换 → 懒加载地图 =====
watch(activeTab, async (idx) => {
  const tab = visibleTabs.value[idx];
  if (tab?.key !== 'traffic') return;
  const p = park.value;
  if (!p.latitude || !p.longitude) return;

  // #ifdef H5
  await nextTick();
  setTimeout(() => {
    if (!detailMap) {
      import('@/utils/map').then(({ initDetailMap }) => {
        detailMap = initDetailMap(
          'detail-map',
          Number(p.latitude),
          Number(p.longitude),
          p.shortName || p.fullName || '',
          '#19be6b',
        );
      });
    }
  }, 300);
  // #endif
});

onUnmounted(() => {
  // #ifdef H5
  if (detailMap) {
    import('@/utils/map').then(({ destroyMap }) => destroyMap(detailMap));
    detailMap = null;
  }
  // #endif
});

// ===== 数据加载 =====
async function loadDetail(code: string) {
  loading.value = true;
  loadError.value = false;
  try {
    detail.value = await getParkFullDetail(code, NETWORK);
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

function retryLoad() {
  if (parkCode) loadDetail(parkCode);
}

// ===== 工具方法 =====
function formatAddress(p: ParkDetail): string {
  return [p.province, p.city, p.district, p.address].filter(Boolean).join(' ');
}

function formatDate(t?: string): string {
  if (!t) return '-';
  return t.substring(0, 10);
}

function hasAnyFacility(room: ParkRoomType): boolean {
  return [
    room.hasBathroom, room.hasAircon, room.hasTv, room.hasWifi,
    room.hasFridge, room.hasBalcony, room.hasKitchen, room.hasWasher,
  ].some((v) => v === 1);
}

function onSwiperChange(e: any) {
  swiperCurrent.value = e.detail.current;
}

function previewBanner(i: number) {
  uni.previewImage({ urls: bannerImages.value, current: i });
}

function previewImageList(urls: string[], i: number) {
  uni.previewImage({ urls, current: i });
}

function previewRoomImages(room: ParkRoomType) {
  const images = parseImageList(room.images);
  if (images.length) {
    uni.previewImage({ urls: images });
  } else if (room.coverImage) {
    uni.previewImage({ urls: [formatFileUrl(room.coverImage)] });
  }
}

function onCopy(text: string) {
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: '已复制', icon: 'none' }),
  });
}

function onCall() {
  if (!park.value.serviceHotline) return;
  uni.makePhoneCall({ phoneNumber: park.value.serviceHotline });
}

onLoad(async (options: any) => {
  parkCode = options?.parkCode;
  if (!parkCode) {
    loading.value = false;
    return;
  }
  await loadDetail(parkCode);
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.detail-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 40rpx;
}

.skeleton-wrap {
  padding: $spacing-md;
}

/* Banner */
.banner-section {
  position: relative;
  width: 100%;
  height: 400rpx;
}
.banner-swiper {
  width: 100%;
  height: 400rpx;
}
.banner-img {
  width: 100%;
  height: 400rpx;
}
.banner-indicator {
  position: absolute;
  bottom: $spacing-sm;
  right: $spacing-md;
  background: rgba(0, 0, 0, 0.5);
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  color: #fff;
}

/* 信息卡片 */
.info-card {
  background: $bg-card;
  margin: $spacing-sm $spacing-md;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
}
.park-name {
  display: block;
  font-size: 38rpx;
  font-weight: bold;
  color: $text-primary;
  margin-bottom: $spacing-sm;
}
.park-tags {
  display: flex;
  gap: $spacing-sm;
  flex-wrap: wrap;
  margin-bottom: $spacing-sm;
}
.info-row {
  display: flex;
  align-items: center;
  padding: 12rpx 0;
}
.info-icon {
  font-size: 28rpx;
  margin-right: $spacing-sm;
  flex-shrink: 0;
}
.info-value {
  flex: 1;
  font-size: 26rpx;
  color: $text-primary;
}
.copy-btn {
  font-size: 22rpx;
  color: $brand-success;
  padding: 4rpx 16rpx;
  background: $brand-success-light;
  border-radius: 16rpx;
  flex-shrink: 0;
}
.phone-link {
  color: $brand-success;
}
.beds-tag {
  font-size: 22rpx;
  color: $brand-success;
  background: $brand-success-light;
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}
.price-highlight {
  font-size: 36rpx;
  font-weight: bold;
  color: $brand-warning;
}
.price-unit {
  font-size: 24rpx;
  color: $text-secondary;
  margin-left: 4rpx;
}

/* Tab 栏 */
.tabs-bar {
  background: $bg-card;
  margin: 0 $spacing-md;
  border-radius: $radius-md $radius-md 0 0;
  border-bottom: 1rpx solid $border-light;
}
.tabs-bar.sticky {
  position: sticky;
  top: 0;
  z-index: 100;
  margin: 0;
  border-radius: 0;
}
.tabs-scroll {
  white-space: nowrap;
}
.tabs-inner {
  display: inline-flex;
  padding: 0 $spacing-sm;
}
.tab-item {
  padding: 24rpx 28rpx;
  font-size: 28rpx;
  color: $text-secondary;
  position: relative;
  flex-shrink: 0;
}
.tab-item.active {
  color: $brand-success;
  font-weight: bold;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 4rpx;
  background: $brand-success;
  border-radius: 2rpx;
}

/* Tab 内容 */
.tab-content {
  background: $bg-card;
  margin: 0 $spacing-md;
  border-radius: 0 0 $radius-md $radius-md;
  padding: $spacing-md;
  min-height: 300rpx;
}
.tab-panel {
  animation: fadeIn 200ms ease;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 统计宫格 */
.stat-grid {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: $spacing-sm;
}
.stat-item {
  width: 25%;
  text-align: center;
  padding: $spacing-sm 0;
}
.stat-value {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  &.green { color: $brand-success; }
  &.orange { color: $brand-warning; }
}
.stat-label {
  display: block;
  font-size: 22rpx;
  color: $text-secondary;
  margin-top: $spacing-xs;
}

/* 键值行 */
.kv-row {
  display: flex;
  padding: 10rpx 0;
}
.kv-label {
  width: 140rpx;
  font-size: 26rpx;
  color: $text-secondary;
  flex-shrink: 0;
}
.kv-value {
  flex: 1;
  font-size: 26rpx;
  color: $text-primary;
}

/* 描述块 */
.desc-block {
  margin-top: $spacing-md;
}
.desc-title {
  display: block;
  font-size: 28rpx;
  font-weight: bold;
  color: $text-primary;
  margin-bottom: $spacing-sm;
}
.desc-text {
  font-size: 28rpx;
  color: $text-regular;
  line-height: 1.8;
}

/* 房型 */
.room-card {
  display: flex;
  background: $bg-page;
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  overflow: hidden;
}
.room-img {
  width: 200rpx;
  height: 200rpx;
  flex-shrink: 0;
}
.room-info {
  flex: 1;
  padding: $spacing-md;
}
.room-name {
  display: block;
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}
.room-meta {
  display: flex;
  gap: $spacing-xs;
  flex-wrap: wrap;
  margin-top: $spacing-xs;
}
.room-tag {
  font-size: 22rpx;
  color: $text-secondary;
  background: $border-light;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}
.room-facilities {
  display: flex;
  gap: $spacing-xs;
  flex-wrap: wrap;
  margin-top: $spacing-sm;
}
.facility-icon {
  font-size: 20rpx;
  width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  background: $brand-success-light;
  color: $brand-success;
  border-radius: 50%;
}
.room-desc {
  display: block;
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: $spacing-xs;
}

/* 收费 */
.fee-tip {
  background: $brand-warning-light;
  border-radius: $radius-sm;
  padding: $spacing-sm $spacing-md;
  font-size: 24rpx;
  color: $brand-warning;
  margin-bottom: $spacing-md;
}
.fee-card {
  background: $bg-page;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
}
.fee-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}
.fee-name {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}
.fee-current {
  font-size: 20rpx;
  color: $brand-success;
  background: $brand-success-light;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}
.fee-promo {
  font-size: 20rpx;
  color: #fff;
  background: $brand-error;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}
.fee-row {
  display: flex;
  align-items: baseline;
  margin-bottom: $spacing-sm;
}
.fee-price {
  font-size: 40rpx;
  font-weight: bold;
  color: $brand-warning;
}
.fee-unit {
  font-size: 24rpx;
  color: $text-secondary;
  margin-left: 4rpx;
}
.fee-original {
  font-size: 24rpx;
  color: $text-placeholder;
  text-decoration: line-through;
  margin-left: $spacing-sm;
}

/* 照护 */
.care-card {
  background: $bg-page;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
}
.care-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}
.care-name {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}
.care-level {
  font-size: 22rpx;
  color: $brand-success;
  background: $brand-success-light;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}
.care-desc {
  display: block;
  font-size: 26rpx;
  color: $text-regular;
  line-height: 1.6;
  margin-top: $spacing-sm;
}

/* 餐饮设施 */
.sub-section {
  margin-bottom: $spacing-lg;
}
.sub-title {
  display: block;
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
  margin-bottom: $spacing-sm;
}
.item-card {
  background: $bg-page;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
}
.item-name {
  display: block;
  font-size: 28rpx;
  font-weight: 500;
  color: $text-primary;
}
.item-desc {
  display: block;
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: $spacing-xs;
  line-height: 1.5;
}

/* 交通周边 */
.map-block {
  margin-bottom: $spacing-md;
}
.detail-map-container {
  width: 100%;
  height: 200px;
  border-radius: $radius-md;
  overflow: hidden;
}
.address-block {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;
  margin-bottom: $spacing-sm;
}
.address-text {
  flex: 1;
  font-size: 26rpx;
  color: $text-primary;
}
.rim-section {
  margin-top: $spacing-sm;
}
.rim-tabs {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-sm;
}
.rim-tab {
  font-size: 26rpx;
  color: $text-secondary;
  padding: 8rpx 24rpx;
  border-radius: 20rpx;
  background: $bg-page;
}
.rim-tab.active {
  color: #fff;
  background: $brand-success;
}
.rim-list {
  display: flex;
  flex-direction: column;
}
.rim-item {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  padding: 16rpx 0;
  border-bottom: 1rpx solid $border-light;
}
.rim-name {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}
.rim-distance {
  font-size: 24rpx;
  color: $brand-success;
  margin-left: $spacing-sm;
}
.rim-addr {
  width: 100%;
  font-size: 22rpx;
  color: $text-secondary;
  margin-top: 4rpx;
}

/* 图文展示板块 */
.display-section {
  margin: $spacing-md;
}
.display-block {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}
.block-title {
  display: block;
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
  margin-bottom: $spacing-sm;
}
.block-content {
  font-size: 28rpx;
  color: $text-regular;
  line-height: 1.8;
}
.block-images {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
}
.block-img {
  width: 200rpx;
  height: 200rpx;
  border-radius: $radius-sm;
}
</style>
