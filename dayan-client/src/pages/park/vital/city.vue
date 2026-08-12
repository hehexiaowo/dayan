<template>
  <view class="city-page">
    <!-- 位置指示器 -->
    <view class="location-bar">
      <text class="location-text">活力长居 · 选择区县</text>
    </view>

    <!-- 加载骨架 -->
    <view v-if="loading" class="district-list">
      <DySkeleton v-for="i in 3" :key="i" :rows="1" card />
    </view>

    <template v-else>
      <!-- 统计 -->
      <view class="stats-bar">
        <text class="stats-text">{{ districts.length }} 个区县 · {{ totalParks }} 家机构</text>
      </view>

      <!-- 区县列表 -->
      <view class="district-list">
        <view
          v-for="item in districts"
          :key="item.code"
          class="district-card dy-clickable"
          @click="onDistrictClick(item)"
        >
          <view class="district-info">
            <text class="district-name">{{ item.name }}</text>
            <text class="district-count">{{ item.count }} 家机构</text>
          </view>
          <view class="district-right">
            <text class="arrow">›</text>
          </view>
        </view>
        <DyEmpty v-if="!districts.length" text="该城市暂无机构" icon="空" color="gray" />
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getRegions } from '@/api/park';
import type { RegionItem } from '@/types/park';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const provinceCode = ref('');
const cityCode = ref('');
const districts = ref<RegionItem[]>([]);
const loading = ref(true);

const totalParks = computed(() => districts.value.reduce((s, i) => s + i.count, 0));

async function fetchData() {
  loading.value = true;
  try {
    const result = await getRegions({
      category: 'vital',
      level: 'district',
      provinceCode: provinceCode.value,
      cityCode: cityCode.value,
    });
    districts.value = result.items || [];
  } catch {
    districts.value = [];
  } finally {
    loading.value = false;
  }
}

function onDistrictClick(item: RegionItem) {
  uni.navigateTo({
    url: `/pages/park/vital/district?category=vital&provinceCode=${provinceCode.value}&cityCode=${cityCode.value}&districtCode=${item.code}`,
  });
}

onLoad((options: any) => {
  if (options?.provinceCode) provinceCode.value = options.provinceCode;
  if (options?.cityCode) cityCode.value = options.cityCode;
});

onMounted(fetchData);
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.city-page {
  min-height: 100vh;
  background: $bg-page;
}

/* 位置指示器 */
.location-bar {
  background: $bg-card;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1rpx solid $border-light;
}
.location-text {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}

/* 统计栏 */
.stats-bar {
  padding: $spacing-md;
}
.stats-text {
  font-size: 26rpx;
  color: $text-secondary;
}

/* 区县列表 */
.district-list {
  padding: 0 $spacing-md;
}
.district-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-lg;
  background: $bg-card;
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  box-shadow: $shadow-card;
}
.district-info {
  flex: 1;
}
.district-name {
  display: block;
  font-size: 32rpx;
  color: $text-primary;
  font-weight: 500;
}
.district-count {
  display: block;
  font-size: 24rpx;
  color: $brand-primary;
  margin-top: $spacing-xs;
}
.district-right {
  flex-shrink: 0;
}
.arrow {
  font-size: 40rpx;
  color: $text-placeholder;
}
</style>
