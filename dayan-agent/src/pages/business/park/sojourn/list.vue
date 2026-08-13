<template>
  <view class="list-page">
    <!-- 头部统计 -->
    <view class="header-section">
      <text class="header-title">旅居养老</text>
      <text class="header-subtitle">精选旅居机构 · 随心住 · 灵活周期</text>
      <view v-if="!loading" class="header-count">
        <text class="count-num">{{ parks.length }}</text>
        <text class="count-label">家机构</text>
      </view>
    </view>

    <!-- 机构卡片列表 -->
    <view class="park-list">
      <view
        v-for="park in parks"
        :key="park.parkCode"
        class="park-card dy-clickable"
        @click="onParkClick(park)"
      >
        <image v-if="park.thumbnailUrl" class="park-thumb" :src="formatFileUrl(park.thumbnailUrl)" mode="aspectFill" />
        <DyIconBlock
          v-else
          :text="park.shortName?.charAt(0) || '机'"
          color="green"
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
      <DyEmpty v-if="!loading && !parks.length" text="暂无旅居机构" icon="空" color="gray" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getRegions } from '@/api/park';
import type { ParkCard } from '@/types/park';
import { parseNetworkTags } from '@/types/park';
import { formatFileUrl } from '@/utils/file';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const parks = ref<ParkCard[]>([]);
const loading = ref(true);

async function fetchData() {
  loading.value = true;
  try {
    const result = await getRegions({
      category: 'sojourn',
      level: 'park',
    });
    parks.value = result.parkList || [];
  } catch {
    parks.value = [];
  } finally {
    loading.value = false;
  }
}

function onParkClick(park: ParkCard) {
  uni.navigateTo({
    url: `/pages/business/park/sojourn/detail?parkCode=${park.parkCode}`,
  });
}

function formatAddress(park: ParkCard): string {
  return [park.province, park.city, park.district, park.address].filter(Boolean).join(' ');
}

onMounted(fetchData);
</script>

<style lang="scss" scoped>

.list-page {
  min-height: 100vh;
  background: $bg-page;
}

.header-section {
  background: linear-gradient(135deg, $brand-success 0%, $brand-success-dark 100%);
  padding: $spacing-xl $spacing-md $spacing-lg;
  text-align: center;
  border-bottom-left-radius: $radius-lg;
  border-bottom-right-radius: $radius-lg;
  box-shadow: $shadow-card;
}
.header-title {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
}
.header-subtitle {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: $spacing-xs;
}
.header-count {
  margin-top: $spacing-sm;
  display: flex;
  align-items: baseline;
  justify-content: center;
}
.count-num {
  font-size: 48rpx;
  font-weight: bold;
  color: #fff;
}
.count-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-left: 8rpx;
}

.park-list {
  padding: $spacing-md;
}

.park-card {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}
.park-thumb {
  width: 96rpx;
  height: 96rpx;
  border-radius: $radius-sm;
  flex-shrink: 0;
}

.park-info {
  flex: 1;
  margin-left: $spacing-md;
  overflow: hidden;
}
.park-name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.park-addr {
  display: block;
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: 6rpx;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.park-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 10rpx;
}
.tag {
  font-size: 22rpx;
  padding: 2rpx 14rpx;
  border-radius: $radius-sm;
}
.tag-price {
  background: $brand-success-light;
  color: $brand-success;
}
.tag-bed {
  background: $brand-info-light;
  color: $brand-info;
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
  font-size: 36rpx;
  color: $text-placeholder;
  margin-left: $spacing-sm;
}
</style>
