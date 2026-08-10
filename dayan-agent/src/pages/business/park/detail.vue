<template>
  <view class="detail-page">
    <view v-if="loading" class="loading-state">
      <text>加载中...</text>
    </view>

    <view v-else-if="park" class="detail-content">
      <!-- 机构名称区 -->
      <view class="header-section">
        <text class="park-title">{{ park.fullName }}</text>
        <view class="park-tags">
          <text v-if="park.abilityTypeDescription" class="tag tag-type">{{ park.abilityTypeDescription }}</text>
          <text v-if="park.natureTypeDescription" class="tag tag-nature">{{ park.natureTypeDescription }}</text>
          <text v-if="park.isHot === 1" class="tag tag-hot">🔥 热门</text>
        </view>
      </view>

      <!-- 地址+地图 -->
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
            <text class="stat-value">{{ park.totalBeds }}</text>
            <text class="stat-label">总床位</text>
          </view>
          <view v-if="park.availableBeds != null" class="stat-item">
            <text class="stat-value">{{ park.availableBeds }}</text>
            <text class="stat-label">可用床位</text>
          </view>
          <view v-if="park.staffCount != null" class="stat-item">
            <text class="stat-value">{{ park.staffCount }}</text>
            <text class="stat-label">员工数</text>
          </view>
          <view v-if="park.nurseCount != null" class="stat-item">
            <text class="stat-value">{{ park.nurseCount }}</text>
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
      <view v-if="park.minPriceDisplay" class="section-title">价格信息</view>
      <view v-if="park.minPriceDisplay" class="info-card">
        <view class="price-row">
          <text class="price-value">¥{{ park.minPriceDisplay }}{{ park.maxPriceDisplay ? ' - ' + park.maxPriceDisplay : '' }}</text>
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

      <!-- 机构简介 -->
      <view v-if="park.baseDescription" class="section-title">机构简介</view>
      <view v-if="park.baseDescription" class="info-card">
        <text class="desc-text">{{ park.baseDescription }}</text>
      </view>

      <!-- 特色 -->
      <view v-if="park.specialtyDescription" class="section-title">机构特色</view>
      <view v-if="park.specialtyDescription" class="info-card">
        <text class="desc-text">{{ park.specialtyDescription }}</text>
      </view>

      <!-- 入住说明 -->
      <view v-if="park.checkInDescription" class="section-title">入住说明</view>
      <view v-if="park.checkInDescription" class="info-card">
        <text class="desc-text">{{ park.checkInDescription }}</text>
      </view>
    </view>

    <view v-else class="empty-state">
      <text>机构信息不存在</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getParkDetail } from '@/api/park';
import type { ParkDetail } from '@/types/park';

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

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40rpx;
}
.loading-state, .empty-state {
  padding: 120rpx 0;
  text-align: center;
  color: #909399;
  font-size: 28rpx;
}
.header-section {
  background: #fff;
  padding: 32rpx 24rpx;
  margin-bottom: 16rpx;
}
.park-title {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 16rpx;
}
.park-tags {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
}
.tag {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 12rpx;
}
.tag-type { background: #e8f4ff; color: #409eff; }
.tag-nature { background: #f0f9eb; color: #67c23a; }
.tag-hot { background: #fef0f0; color: #f56c6c; }
.section-title {
  padding: 24rpx 24rpx 12rpx;
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
}
.info-card {
  background: #fff;
  margin: 0 24rpx 16rpx;
  border-radius: 16rpx;
  padding: 24rpx;
}
.info-row {
  display: flex;
  padding: 12rpx 0;
}
.info-label {
  width: 140rpx;
  font-size: 26rpx;
  color: #909399;
  flex-shrink: 0;
}
.info-value {
  flex: 1;
  font-size: 26rpx;
  color: #303133;
}
.stat-grid {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 16rpx;
}
.stat-item {
  width: 25%;
  text-align: center;
  padding: 16rpx 0;
}
.stat-value {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: #409eff;
}
.stat-label {
  display: block;
  font-size: 22rpx;
  color: #909399;
  margin-top: 8rpx;
}
.price-row {
  display: flex;
  align-items: baseline;
  padding: 12rpx 0;
}
.price-value {
  font-size: 48rpx;
  font-weight: bold;
  color: #e6a23c;
}
.price-unit {
  font-size: 24rpx;
  color: #909399;
  margin-left: 8rpx;
}
.desc-text {
  font-size: 26rpx;
  color: #606266;
  line-height: 1.7;
}
</style>
