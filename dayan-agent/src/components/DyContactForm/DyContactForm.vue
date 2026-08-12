<template>
  <view v-if="!submitted" class="contact-form">
    <view class="form-header dy-clickable" @click="expanded = !expanded">
      <text class="form-title">💌 留下联系方式，顾问为您服务</text>
      <text class="form-arrow" :class="{ rotated: expanded }">›</text>
    </view>

    <view v-if="expanded" class="form-body">
      <view class="form-row">
        <input
          v-model="phone"
          type="number"
          maxlength="11"
          placeholder="请输入手机号"
          class="form-input"
        />
      </view>
      <view class="form-row">
        <input
          v-model="name"
          type="text"
          placeholder="称呼（选填）"
          class="form-input"
        />
      </view>
      <view class="form-btn dy-clickable" :class="{ disabled: !phone || phone.length < 11 }" @click="onSubmit">
        <text class="form-btn-text">提交</text>
      </view>
    </view>
  </view>

  <view v-else class="contact-done">
    <text class="done-icon">✓</text>
    <text class="done-text">感谢您的留言，顾问会尽快与您联系</text>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { leaveContact } from '@/api/share';

const phone = ref('');
const name = ref('');
const expanded = ref(false);
const submitted = ref(false);

async function onSubmit() {
  if (!phone.value || phone.value.length < 11) return;
  const ok = await leaveContact({
    visitorToken: '',
    phone: phone.value,
    name: name.value || undefined,
  });
  if (ok) {
    submitted.value = true;
  } else {
    uni.showToast({ title: '提交失败，请重试', icon: 'none' });
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.contact-form {
  margin: $spacing-md;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  overflow: hidden;
}

.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
}

.form-title {
  font-size: 28rpx;
  font-weight: 500;
  color: $text-primary;
}

.form-arrow {
  font-size: 36rpx;
  color: $text-secondary;
  transition: transform 0.2s ease;
  transform: rotate(90deg);
}
.form-arrow.rotated {
  transform: rotate(-90deg);
}

.form-body {
  padding: 0 $spacing-lg $spacing-lg;
}

.form-row {
  margin-bottom: $spacing-sm;
}

.form-input {
  width: 100%;
  height: $control-height;
  background: $bg-page;
  border-radius: $radius-sm;
  padding: 0 $spacing-md;
  font-size: 28rpx;
  color: $text-primary;
  box-sizing: border-box;
}

.form-btn {
  height: $control-height;
  background: $gradient-blue;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: $spacing-sm;
}
.form-btn.disabled {
  opacity: 0.5;
}

.form-btn-text {
  color: #fff;
  font-size: 30rpx;
  font-weight: 500;
}

.contact-done {
  margin: $spacing-md;
  padding: $spacing-lg;
  background: $brand-success-light;
  border-radius: $radius-lg;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
}

.done-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: $brand-success;
  color: #fff;
  font-size: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.done-text {
  font-size: 26rpx;
  color: $text-regular;
  text-align: center;
}
</style>
