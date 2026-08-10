<template>
  <view class="detail-page">
    <!-- 骨架屏 -->
    <view v-if="loading" class="skeleton-wrap">
      <DySkeleton :rows="1" avatar card />
      <DySkeleton :rows="4" card />
      <DySkeleton :rows="3" card />
    </view>

    <view v-else-if="park" class="detail-content">
      <!-- 机构名称区 -->
      <view class="header-section">
        <text class="park-title">{{ park.fullName }}</text>
        <view class="park-tags">
          <text v-if="park.abilityTypeDescription" class="dy-tag dy-tag-blue">{{ park.abilityTypeDescription }}</text>
          <text v-if="park.natureTypeDescription" class="dy-tag dy-tag-green">{{ park.natureTypeDescription }}</text>
          <text v-if="park.isHot === 1" class="dy-tag dy-tag-red">热门</text>
        </view>
      </view>

      <!-- 地址+电话 -->
      <view class="info-card">
        <view class="info-row">
          <text class="info-label">地址</text>
          <text class="info-value">{{ formatAddress(park) }}</text>
        </view>
        <view v-if="park.serviceHotline" class="info-row">
          <text class="info-label">电话</text>
          <text class="info-value">{{ park.serviceHotline }}</text>
        </view>
        <view v-if="park.openingTime" class="info-row">
          <text class="info-label">开业</text>
          <text class="info-value">{{ park.openingTime }}</text>
        </view>
      </view>

      <!-- 基本信息 -->
      <view class="section-title">基本信息</view>
      <view class="info-card">
        <view class="stat-grid">
          <view v-if="park.totalBeds != null" class="stat-item">
            <text class="stat-value blue">{{ park.totalBeds }}</text>
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
            <text class="stat-value blue">{{ park.nurseCount }}</text>
            <text class="stat-label">护理员</text>
          </view>
        </view>
        <view v-if="park.nursePatientRatio" class="info-row">
          <text class="info-label">护患比</text>
          <text class="info-value">{{ park.nursePatientRatio }}</text>
        </view>
        <view v-if="park.checkInAgeMin || park.checkInAgeMax" class="info-row">
          <text class="info-label">入住年龄</text>
          <text class="info-value">{{ park.checkInAgeMin || '-' }} ~ {{ park.checkInAgeMax || '-' }} 岁</text>
        </view>
      </view>

      <!-- 价格信息 -->
      <template v-if="park.minPriceDisplay">
        <view class="section-title">价格信息</view>
        <view class="info-card price-highlight">
          <view class="price-row">
            <text class="price-symbol">¥</text>
            <text class="price-value">{{ park.minPriceDisplay }}{{ park.maxPriceDisplay ? ' - ' + park.maxPriceDisplay : '' }}</text>
            <text class="price-unit">/{{ park.priceUnit || '月' }}</text>
          </view>
          <view v-if="park.depositAmount" class="info-row">
            <text class="info-label">押金</text>
            <text class="info-value">¥{{ park.depositAmount }}</text>
          </view>
          <view v-if="park.contractPeriod" class="info-row">
            <text class="info-label">合同周期</text>
            <text class="info-value">{{ park.contractPeriod }}</text>
          </view>
        </view>
      </template>

      <!-- 机构简介 -->
      <template v-if="park.baseDescription">
        <view class="section-title">机构简介</view>
        <view class="info-card">
          <text class="desc-text">{{ park.baseDescription }}</text>
        </view>
      </template>

      <!-- 特色 -->
      <template v-if="park.specialtyDescription">
        <view class="section-title">机构特色</view>
        <view class="info-card">
          <text class="desc-text">{{ park.specialtyDescription }}</text>
        </view>
      </template>

      <!-- 入住说明 -->
      <template v-if="park.checkInDescription">
        <view class="section-title">入住说明</view>
        <view class="info-card">
          <text class="desc-text">{{ park.checkInDescription }}</text>
        </view>
      </template>
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
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getParkDetail } from '@/api/park';
import type { ParkDetail } from '@/types/park';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const park = ref<ParkDetail | null>(null);
const loading = ref(true);

function formatAddress(p: ParkDetail): string {
  return [p.province, p.city, p.district, p.address].filter(Boolean).join(' ');
}

onLoad(async (options: any) => {
  const parkCode = options?.parkCode;
  if (!parkCode) {
    loading.value = false;
    return;
  }
  try {
    park.value = await getParkDetail(parkCode);
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
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

/* 名称区 */
.header-section {
  background: $gradient-blue;
  padding: $spacing-xl $spacing-lg $spacing-lg;
}
.park-title {
  display: block;
  font-size: 42rpx;
  font-weight: bold;
  color: #fff;
  margin-bottom: $spacing-sm;
}
.park-tags {
  display: flex;
  gap: $spacing-sm;
  flex-wrap: wrap;
}

/* 区块标题 */
.section-title {
  padding: $spacing-lg $spacing-lg $spacing-sm;
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}

/* 信息卡片 */
.info-card {
  background: $bg-card;
  margin: 0 $spacing-lg $spacing-sm;
  border-radius: $radius-md;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
}
.info-row {
  display: flex;
  padding: 12rpx 0;
}
.info-label {
  width: 140rpx;
  font-size: 26rpx;
  color: $text-secondary;
  flex-shrink: 0;
}
.info-value {
  flex: 1;
  font-size: 26rpx;
  color: $text-primary;
}

/* 统计四宫格 */
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
  font-size: 44rpx;
  font-weight: bold;

  &.blue {
    color: $brand-primary;
  }
  &.green {
    color: $brand-success;
  }
  &.orange {
    color: $brand-warning;
  }
}
.stat-label {
  display: block;
  font-size: 22rpx;
  color: $text-secondary;
  margin-top: $spacing-xs;
}

/* 价格高亮区 */
.price-highlight {
  background: linear-gradient(135deg, #fffbeb 0%, $bg-card 100%);
  border: 1rpx solid rgba(255, 153, 0, 0.2);
}
.price-row {
  display: flex;
  align-items: baseline;
  padding: 12rpx 0;
}
.price-symbol {
  font-size: 28rpx;
  font-weight: bold;
  color: $brand-warning;
}
.price-value {
  font-size: 52rpx;
  font-weight: bold;
  color: $brand-warning;
}
.price-unit {
  font-size: 24rpx;
  color: $text-secondary;
  margin-left: $spacing-xs;
}

/* 描述文字 */
.desc-text {
  font-size: 28rpx;
  color: $text-regular;
  line-height: 1.8;
}
</style>
