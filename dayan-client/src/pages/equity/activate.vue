<template>
  <view class="activate-page">
    <!-- 顶部引导 -->
    <view class="hero">
      <view class="hero-icon">★</view>
      <text class="hero-title">激活养老权益</text>
      <text class="hero-desc">输入权益卡面的激活码，开启您的专属养老服务</text>
    </view>

    <!-- 激活码输入卡 -->
    <view class="form-card">
      <text class="field-label">权益激活码</text>
      <view class="input-wrap">
        <text class="input-prefix">DY</text>
        <input
          v-model="codeTail"
          class="code-input"
          type="text"
          :maxlength="8"
          placeholder="请输入8位激活码"
          placeholder-class="ph"
          @input="onCodeInput"
        />
      </view>
      <text class="field-hint">激活码位于权益卡背面，格式如 DY12345678</text>

      <button class="btn-submit" :disabled="submitting" @click="handleSubmit">
        {{ submitting ? '激活中...' : '立即激活' }}
      </button>
    </view>

    <!-- 说明 -->
    <view class="tips">
      <text class="tips-title">激活说明</text>
      <view class="tip-item"><text class="dot">·</text><text>激活后需完善权益人（老人）信息</text></view>
      <view class="tip-item"><text class="dot">·</text><text>每张权益卡仅可激活一次</text></view>
      <view class="tip-item"><text class="dot">·</text><text>如有疑问请联系您的服务管家</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { activateEquity } from '@/api/equity';

const codeTail = ref(''); // DY 之后的 8 位
const submitting = ref(false);

/** 自动转大写 + 仅保留字母数字 */
function onCodeInput(e: any) {
  const raw = (e.detail.value || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
  codeTail.value = raw.slice(0, 8);
}

const fullCode = computed(() => 'DY' + codeTail.value);
const isValid = computed(() => /^[A-Z0-9]{8}$/.test(codeTail.value));

async function handleSubmit() {
  if (!isValid.value) {
    uni.showToast({ title: '请输入完整的8位激活码', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    const equity = await activateEquity(fullCode.value);
    uni.showToast({ title: '权益激活成功', icon: 'success' });
    // 激活成功 → 引导完善权益人信息
    setTimeout(() => {
      uni.redirectTo({
        url: `/pages/equity/use-persons/index?equityCode=${equity.equityCode}`,
      });
    }, 1000);
  } catch (e: any) {
    // request 拦截器已弹错误提示
  } finally {
    submitting.value = false;
  }
}
</script>

<style lang="scss" scoped>
.activate-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 60rpx;
}

/* 顶部引导 */
.hero {
  background: linear-gradient(135deg, #67C23A 0%, #4eaf2a 100%);
  padding: 60rpx 40rpx 80rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hero-icon {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}
.hero-title {
  color: #fff;
  font-size: 38rpx;
  font-weight: bold;
}
.hero-desc {
  color: rgba(255, 255, 255, 0.85);
  font-size: 26rpx;
  margin-top: 12rpx;
  text-align: center;
}

/* 表单卡 */
.form-card {
  background: #fff;
  margin: -40rpx 24rpx 0;
  border-radius: 16rpx;
  padding: 40rpx 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  position: relative;
}
.field-label {
  font-size: 28rpx;
  color: #303133;
  font-weight: 500;
  display: block;
  margin-bottom: 16rpx;
}
.input-wrap {
  display: flex;
  align-items: center;
  border: 2rpx solid #dcdfe6;
  border-radius: 12rpx;
  overflow: hidden;
}
.input-prefix {
  background: #f5f7fa;
  color: #67C23A;
  font-size: 36rpx;
  font-weight: bold;
  padding: 0 24rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-right: 2rpx solid #dcdfe6;
}
.code-input {
  flex: 1;
  height: 88rpx;
  font-size: 36rpx;
  letter-spacing: 8rpx;
  padding: 0 24rpx;
  color: #303133;
}
.ph {
  color: #c0c4cc;
  font-size: 28rpx;
  letter-spacing: 0;
}
.field-hint {
  font-size: 24rpx;
  color: #909399;
  margin-top: 12rpx;
  display: block;
}
.btn-submit {
  margin-top: 40rpx;
  background: #67C23A;
  color: #fff;
  font-size: 32rpx;
  border-radius: 12rpx;
  height: 88rpx;
  line-height: 88rpx;
  &[disabled] {
    background: #a4da89;
    color: rgba(255, 255, 255, 0.7);
  }
}

/* 说明 */
.tips {
  margin: 30rpx 24rpx 0;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
}
.tips-title {
  font-size: 28rpx;
  color: #303133;
  font-weight: 500;
  display: block;
  margin-bottom: 16rpx;
}
.tip-item {
  display: flex;
  align-items: flex-start;
  font-size: 24rpx;
  color: #909399;
  line-height: 1.8;
  .dot {
    margin-right: 8rpx;
  }
}
</style>
