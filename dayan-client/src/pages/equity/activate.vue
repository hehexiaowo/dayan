<template>
  <view class="activate-page">
    <!-- 顶部引导 -->
    <view class="hero">
      <DyIconBlock text="活" color="green" size="lg" shape="circle" class="hero-icon-block" />
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

      <button class="dy-btn dy-btn-primary btn-submit" :disabled="submitting" @click="handleSubmit">
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
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';

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
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.activate-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 60rpx;
}

/* 顶部引导 */
.hero {
  background: $gradient-brand;
  padding: 60rpx $spacing-lg 80rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.hero-icon-block {
  margin-bottom: $spacing-md;
  border: 4rpx solid rgba(255, 255, 255, 0.4);
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
  background: $bg-card;
  margin: -40rpx $spacing-md 0;
  border-radius: $radius-lg;
  padding: 40rpx $spacing-md;
  box-shadow: $shadow-card;
  position: relative;
}
.field-label {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  display: block;
  margin-bottom: $spacing-sm;
}
.input-wrap {
  display: flex;
  align-items: center;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  overflow: hidden;
}
.input-prefix {
  background: $bg-page;
  color: $brand-primary;
  font-size: 36rpx;
  font-weight: bold;
  padding: 0 $spacing-md;
  height: $control-height;
  line-height: $control-height;
  border-right: 2rpx solid $border-base;
}
.code-input {
  flex: 1;
  height: $control-height;
  font-size: 36rpx;
  letter-spacing: 8rpx;
  padding: 0 $spacing-md;
  color: $text-primary;
}
.ph {
  color: $text-placeholder;
  font-size: 28rpx;
  letter-spacing: 0;
}
.field-hint {
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: 12rpx;
  display: block;
}
.btn-submit {
  margin-top: 40rpx;
  font-size: 32rpx;
  &[disabled] {
    background: lighten($brand-primary, 15%);
    color: rgba(255, 255, 255, 0.7);
    box-shadow: none;
  }
}

/* 说明 */
.tips {
  margin: $spacing-md $spacing-md 0;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-md;
  box-shadow: $shadow-card;
}
.tips-title {
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
  display: block;
  margin-bottom: $spacing-sm;
}
.tip-item {
  display: flex;
  align-items: flex-start;
  font-size: 24rpx;
  color: $text-secondary;
  line-height: 1.8;
  .dot {
    margin-right: $spacing-xs;
  }
}
</style>
