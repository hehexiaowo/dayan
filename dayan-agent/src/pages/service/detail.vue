<template>
  <view class="page dy-safe-bottom">
    <!-- 加载中 -->
    <template v-if="loading">
      <DySkeleton :rows="3" card />
      <DySkeleton :rows="2" card />
      <DySkeleton :rows="2" card />
    </template>

    <!-- 加载失败 -->
    <DyEmpty
      v-else-if="loadError"
      text="加载失败，请检查网络后重试"
      icon="!"
      color="gray"
      action-text="重新加载"
      @action="loadDetail"
    />

    <!-- 不存在或无权 -->
    <DyEmpty
      v-else-if="!detail"
      text="权益不存在或无权查看"
      icon="卡"
      color="gray"
    />

    <template v-else>
      <!-- ===== Hero ===== -->
      <view class="hero">
        <view class="hero-top">
          <text class="hero-no">{{ detail.equityNo || detail.equityCode }}</text>
          <view class="hero-badges">
            <view class="badge carrier" :class="carrierCls(detail.carrierType)">
              {{ carrierText(detail.carrierType) }}
            </view>
            <view class="badge status" :class="statusCls(detail.equityStatus)">
              {{ statusText(detail.equityStatus) }}
            </view>
          </view>
        </view>
        <text v-if="detail.goodsName" class="hero-goods">{{ detail.goodsName }}</text>
        <view class="hero-code" @click="copyText(detail.equityCode, '权益编码')">
          <text class="code-label">编码</text>
          <text class="code-val">{{ detail.equityCode }}</text>
          <text class="code-copy">复制</text>
        </view>
      </view>

      <!-- ===== 激活/绑定码（库存 / 出库时高亮展示，供代理人交给客户）===== -->
      <view v-if="shouldShowCode" class="code-card">
        <view class="code-card-title">
          <text>客户激活凭证</text>
          <text class="code-card-hint">将以下凭证发给客户激活</text>
        </view>
        <view v-if="detail.activateCode" class="credential-row" @click="copyText(detail.activateCode, '激活码')">
          <text class="cred-label">激活码</text>
          <text class="cred-value">{{ detail.activateCode }}</text>
          <text class="cred-copy">复制</text>
        </view>
        <view v-if="detail.bindCode" class="credential-row" @click="copyText(detail.bindCode, '绑定码')">
          <text class="cred-label">绑定码</text>
          <text class="cred-value">{{ detail.bindCode }}</text>
          <text class="cred-copy">复制</text>
        </view>
      </view>

      <!-- ===== 客户信息（已激活后展示）===== -->
      <view v-if="detail.clientName || detail.clientPhone" class="info-card">
        <text class="card-title">客户信息</text>
        <view v-if="detail.clientName" class="info-row">
          <text class="info-label">客户姓名</text>
          <text class="info-value">{{ detail.clientName }}</text>
        </view>
        <view v-if="detail.clientPhone" class="info-row dy-clickable" @click="callPhone(detail.clientPhone!)">
          <text class="info-label">手机号</text>
          <text class="info-value">{{ detail.clientPhone }}</text>
          <text class="info-action">拨打</text>
        </view>
      </view>

      <!-- ===== 商品信息 ===== -->
      <view class="info-card">
        <text class="card-title">商品信息</text>
        <view v-if="detail.goodsName" class="info-row">
          <text class="info-label">商品名称</text>
          <text class="info-value">{{ detail.goodsName }}</text>
        </view>
        <view v-if="detail.skuName" class="info-row">
          <text class="info-label">规格</text>
          <text class="info-value">{{ detail.skuName }}</text>
        </view>
        <view v-if="detail.personCount" class="info-row">
          <text class="info-label">使用人数</text>
          <text class="info-value">{{ detail.personCount }} 人</text>
        </view>
        <view v-if="detail.validDays" class="info-row">
          <text class="info-label">有效天数</text>
          <text class="info-value">{{ detail.validDays }} 天</text>
        </view>
      </view>

      <!-- ===== 状态时间线 ===== -->
      <view class="info-card">
        <text class="card-title">状态时间</text>
        <view v-if="detail.produceTime" class="info-row">
          <text class="info-label">生产时间</text>
          <text class="info-value">{{ formatDateTime(detail.produceTime) }}</text>
        </view>
        <view v-if="detail.outboundTime" class="info-row">
          <text class="info-label">出库时间</text>
          <text class="info-value">{{ formatDateTime(detail.outboundTime) }}</text>
        </view>
        <view v-if="detail.activateTime" class="info-row">
          <text class="info-label">激活时间</text>
          <text class="info-value">{{ formatDateTime(detail.activateTime) }}</text>
        </view>
        <view v-if="detail.firstUseTime" class="info-row">
          <text class="info-label">首次使用</text>
          <text class="info-value">{{ formatDateTime(detail.firstUseTime) }}</text>
        </view>
        <view v-if="detail.lastUseTime" class="info-row">
          <text class="info-label">最近使用</text>
          <text class="info-value">{{ formatDateTime(detail.lastUseTime) }}</text>
        </view>
        <view v-if="detail.expireTime" class="info-row">
          <text class="info-label">有效期至</text>
          <text class="info-value highlight">{{ formatDateTime(detail.expireTime) }}</text>
        </view>
      </view>

      <!-- ===== 批次与来源 ===== -->
      <view v-if="detail.batchCode || detail.orderCode || detail.logisticsNo" class="info-card">
        <text class="card-title">批次与来源</text>
        <view v-if="detail.batchCode" class="info-row">
          <text class="info-label">批次编码</text>
          <text class="info-value mono">{{ detail.batchCode }}</text>
        </view>
        <view v-if="detail.orderCode" class="info-row">
          <text class="info-label">订单编码</text>
          <text class="info-value mono">{{ detail.orderCode }}</text>
        </view>
        <view v-if="detail.logisticsNo" class="info-row">
          <text class="info-label">物流单号</text>
          <text class="info-value mono">{{ detail.logisticsNo }}</text>
        </view>
      </view>

      <!-- ===== 备注 / 作废 ===== -->
      <view v-if="detail.voidReason || detail.remark" class="info-card">
        <text class="card-title">其他</text>
        <view v-if="detail.voidReason" class="info-row">
          <text class="info-label">作废原因</text>
          <text class="info-value danger">{{ detail.voidReason }}</text>
        </view>
        <view v-if="detail.remark" class="info-row">
          <text class="info-label">备注</text>
          <text class="info-value">{{ detail.remark }}</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getEquityDetail } from '@/api/equity';
import type { EquityDetail } from '@/types';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';
import DyEmpty from '@/components/DyEmpty/DyEmpty.vue';

const equityCode = ref('');
const detail = ref<EquityDetail | null>(null);
const loading = ref(true);
const loadError = ref(false);

/** 库存 / 出库状态时展示激活凭证区 */
const shouldShowCode = computed(() => {
  const s = detail.value?.equityStatus as number | undefined;
  return (s === 0 || s === 1) && !!(detail.value?.activateCode || detail.value?.bindCode);
});

async function loadDetail() {
  if (!equityCode.value) return;
  loading.value = true;
  loadError.value = false;
  try {
    detail.value = await getEquityDetail(equityCode.value);
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

// ===== 交互 =====

function copyText(text: string, label: string) {
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: `${label}已复制`, icon: 'none' }),
  });
}

function callPhone(phone: string) {
  uni.makePhoneCall({ phoneNumber: phone }).catch(() => {});
}

// ===== 显示辅助（与 service/index.vue 对齐）=====

function carrierText(t?: number): string {
  return t === 2 ? '函' : '卡';
}
function carrierCls(t?: number): string {
  return t === 2 ? 'carrier-letter' : 'carrier-card';
}
function statusText(s?: number): string {
  const map: Record<number, string> = {
    0: '库存中', 1: '已出库', 2: '已激活', 3: '使用中',
    4: '已完成', 5: '已过期', 6: '已作废', 7: '更换中',
  };
  return map[s ?? -1] || '未知';
}
function statusCls(s?: number): string {
  const map: Record<number, string> = {
    0: 'st-stock', 1: 'st-outbound', 2: 'st-activated', 3: 'st-inuse',
    4: 'st-completed', 5: 'st-expired', 6: 'st-void', 7: 'st-changing',
  };
  return map[s ?? -1] || 'st-stock';
}

function formatDateTime(dt?: string): string {
  if (!dt) return '-';
  // 后端返回 "2026-08-12T10:30:00" 或 "2026-08-12 10:30:00"
  return dt.length >= 16 ? dt.substring(0, 16).replace('T', ' ') : dt;
}

onLoad((q) => {
  equityCode.value = q?.equityCode || '';
  loadDetail();
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  padding: 0 0 140rpx;
  min-height: 100vh;
  background: $bg-page;
}

/* ===== Hero ===== */
.hero {
  background: $gradient-blue;
  padding: $spacing-lg $spacing-lg $spacing-md;
}
.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hero-no {
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
  flex: 1;
}
.hero-badges {
  display: flex;
  gap: 8rpx;
  flex-shrink: 0;
}
.badge {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 20rpx;
}
.hero-goods {
  display: block;
  margin-top: 10rpx;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
}
.hero-code {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 20rpx;
  background: rgba(255, 255, 255, 0.18);
  border-radius: $radius-sm;
  padding: 14rpx 20rpx;
}
.hero-code .code-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  flex-shrink: 0;
}
.hero-code .code-val {
  flex: 1;
  font-size: 24rpx;
  color: #fff;
  font-family: monospace;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.hero-code .code-copy {
  font-size: 22rpx;
  color: #fff;
  background: rgba(255, 255, 255, 0.25);
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

/* 载体 + 状态徽章配色（hero 上用半透明白底） */
.carrier-card { background: rgba(255, 255, 255, 0.25); color: #fff; }
.carrier-letter { background: rgba(255, 255, 255, 0.25); color: #fff; }
.st-stock { background: rgba(255, 255, 255, 0.25); color: #fff; }
.st-outbound { background: rgba(255, 255, 255, 0.25); color: #fff; }
.st-activated { background: rgba(255, 255, 255, 0.3); color: #fff; }
.st-inuse { background: rgba(255, 255, 255, 0.25); color: #fff; }
.st-completed { background: rgba(255, 255, 255, 0.25); color: #fff; }
.st-expired { background: rgba(250, 53, 52, 0.5); color: #fff; }
.st-void { background: rgba(250, 53, 52, 0.5); color: #fff; }
.st-changing { background: rgba(255, 255, 255, 0.25); color: #fff; }

/* ===== 激活凭证卡 ===== */
.code-card {
  margin: $spacing-md $spacing-md 0;
  background: linear-gradient(135deg, #e8f3ff, #f0f7ff);
  border: 2rpx solid #b3d8ff;
  border-radius: $radius-md;
  padding: $spacing-md;
}
.code-card-title {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  margin-bottom: $spacing-sm;
  text {
    font-size: 28rpx;
    font-weight: bold;
    color: $brand-primary;
  }
  .code-card-hint {
    font-size: 22rpx;
    font-weight: normal;
    color: $text-secondary;
  }
}
.credential-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: #fff;
  border-radius: $radius-sm;
  padding: 20rpx $spacing-md;
  margin-top: 12rpx;
}
.cred-label {
  font-size: 26rpx;
  color: $text-secondary;
  flex-shrink: 0;
  width: 80rpx;
}
.cred-value {
  flex: 1;
  font-size: 32rpx;
  font-weight: bold;
  color: $brand-primary;
  font-family: monospace;
  letter-spacing: 2rpx;
}
.cred-copy {
  font-size: 24rpx;
  color: #fff;
  background: $gradient-blue;
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
}

/* ===== 通用信息卡 ===== */
.info-card {
  margin: $spacing-sm $spacing-md 0;
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md;
  box-shadow: $shadow-card;
}
.card-title {
  font-size: 28rpx;
  font-weight: bold;
  color: $text-primary;
  display: block;
  margin-bottom: $spacing-sm;
  padding-bottom: $spacing-xs;
  border-bottom: 1rpx solid $border-light;
}
.info-row {
  display: flex;
  align-items: center;
  padding: 14rpx 0;
}
.info-label {
  font-size: 26rpx;
  color: $text-secondary;
  width: 150rpx;
  flex-shrink: 0;
}
.info-value {
  flex: 1;
  font-size: 28rpx;
  color: $text-primary;
  word-break: break-all;
}
.info-value.mono {
  font-family: monospace;
  font-size: 26rpx;
}
.info-value.highlight {
  color: $brand-warning;
  font-weight: 500;
}
.info-value.danger {
  color: $brand-error;
}
.info-action {
  font-size: 24rpx;
  color: $brand-primary;
  padding: 6rpx 20rpx;
  border: 2rpx solid $brand-primary;
  border-radius: 20rpx;
  flex-shrink: 0;
  margin-left: 12rpx;
}
</style>
