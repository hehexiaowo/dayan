<template>
  <view class="form-page">
    <!-- 骨架屏（编辑模式加载中） -->
    <view v-if="loading" class="skeleton-wrap">
      <DySkeleton :rows="4" card />
      <DySkeleton :rows="3" card />
    </view>

    <view v-else class="form-content">
      <!-- 名片信息 -->
      <view class="section-title">名片信息</view>
      <view class="form-card">
        <view class="form-item">
          <text class="form-label">名片名称 <text class="required">*</text></text>
          <input
            v-model="form.cardName"
            class="dy-input"
            placeholder="如：旅游短居专员名片"
            placeholder-class="input-placeholder"
            maxlength="100"
          />
        </view>

        <view class="form-item">
          <text class="form-label">显示姓名 <text class="required">*</text></text>
          <input
            v-model="form.displayName"
            class="dy-input"
            placeholder="客户看到的姓名"
            placeholder-class="input-placeholder"
            maxlength="50"
          />
        </view>

        <view class="form-item">
          <text class="form-label">职务 / 头衔</text>
          <input
            v-model="form.title"
            class="dy-input"
            placeholder="如：高级养老规划师"
            placeholder-class="input-placeholder"
            maxlength="100"
          />
        </view>
      </view>

      <!-- 联系方式 -->
      <view class="section-title">联系方式</view>
      <view class="form-card">
        <view class="form-item">
          <text class="form-label">手机号 <text class="required">*</text></text>
          <input
            v-model="form.phone"
            class="dy-input"
            placeholder="请输入11位手机号"
            placeholder-class="input-placeholder"
            inputmode="numeric"
            maxlength="11"
          />
        </view>

        <view class="form-item">
          <text class="form-label">微信号</text>
          <input
            v-model="form.wechat"
            class="dy-input"
            placeholder="微信号（选填）"
            placeholder-class="input-placeholder"
            maxlength="50"
          />
        </view>

        <view class="form-item">
          <text class="form-label">邮箱</text>
          <input
            v-model="form.email"
            class="dy-input"
            placeholder="电子邮箱（选填）"
            placeholder-class="input-placeholder"
            inputmode="email"
            maxlength="100"
          />
        </view>
      </view>

      <!-- 工作信息 -->
      <view class="section-title">工作信息</view>
      <view class="form-card">
        <view class="form-item">
          <text class="form-label">公司名称</text>
          <input
            v-model="form.company"
            class="dy-input"
            placeholder="如：泰康养老"
            placeholder-class="input-placeholder"
            maxlength="200"
          />
        </view>

        <view class="form-item">
          <text class="form-label">办公地址</text>
          <input
            v-model="form.address"
            class="dy-input"
            placeholder="如：北京市朝阳区xx大厦"
            placeholder-class="input-placeholder"
            maxlength="300"
          />
        </view>
      </view>

      <!-- 个人简介 -->
      <view class="section-title">个人简介</view>
      <view class="form-card">
        <view class="form-item">
          <text class="form-label">专长标签</text>
          <input
            v-model="form.tags"
            class="dy-input"
            placeholder="逗号分隔，如：旅游短居,长照咨询"
            placeholder-class="input-placeholder"
            maxlength="500"
          />
        </view>

        <view class="form-item">
          <text class="form-label">个人简介</text>
          <textarea
            v-model="form.intro"
            class="dy-textarea"
            placeholder="介绍自己的从业经验、专长领域等"
            placeholder-class="input-placeholder"
            maxlength="2000"
            :auto-height="true"
          />
        </view>
      </view>

      <!-- 提交按钮 -->
      <view class="btn-area">
        <button
          class="dy-btn dy-btn-primary"
          :class="{ 'dy-btn-disabled': submitting }"
          :disabled="submitting"
          @click="onSubmit"
        >
          {{ submitting ? '提交中...' : isEdit ? '保存修改' : '创建名片' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { createCard, updateCard, getCardDetail } from '@/api/card';
import type { CardCreateData } from '@/api/card';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';

const isEdit = ref(false);
const loading = ref(false);
const submitting = ref(false);
const cardId = ref<string | null>(null);

const form = ref({
  cardName: '',
  displayName: '',
  title: '',
  phone: '',
  wechat: '',
  email: '',
  company: '',
  address: '',
  intro: '',
  tags: '',
});

function validate(): string | null {
  if (!form.value.cardName?.trim()) {
    return '请输入名片名称';
  }
  if (!form.value.displayName?.trim()) {
    return '请输入显示姓名';
  }
  if (!form.value.phone?.trim()) {
    return '请输入手机号';
  }
  if (!/^1\d{10}$/.test(form.value.phone.trim())) {
    return '手机号格式不正确';
  }
  return null;
}

async function onSubmit() {
  const err = validate();
  if (err) {
    uni.showToast({ title: err, icon: 'none' });
    return;
  }
  submitting.value = true;

  const payload: CardCreateData = {
    cardName: form.value.cardName.trim(),
    displayName: form.value.displayName.trim(),
    title: form.value.title?.trim() || undefined,
    phone: form.value.phone.trim(),
    wechat: form.value.wechat?.trim() || undefined,
    email: form.value.email?.trim() || undefined,
    company: form.value.company?.trim() || undefined,
    address: form.value.address?.trim() || undefined,
    intro: form.value.intro?.trim() || undefined,
    tags: form.value.tags?.trim() || undefined,
  };

  try {
    if (isEdit.value && cardId.value) {
      await updateCard(cardId.value, payload);
      uni.showToast({ title: '保存成功', icon: 'success' });
    } else {
      await createCard(payload);
      uni.showToast({ title: '创建成功', icon: 'success' });
    }
    setTimeout(() => uni.navigateBack(), 500);
  } catch {
    // 错误已由 request 拦截器提示
  } finally {
    submitting.value = false;
  }
}

interface CardEditRouteParams {
  id?: string;
}

onLoad(async (options: CardEditRouteParams) => {
  if (options?.id) {
    cardId.value = String(options.id);
    isEdit.value = true;
    uni.setNavigationBarTitle({ title: '编辑名片' });
    loading.value = true;
    try {
      const detail = await getCardDetail(cardId.value);
      form.value.cardName = detail.cardName || '';
      form.value.displayName = detail.displayName || '';
      form.value.title = detail.title || '';
      form.value.phone = detail.phone || '';
      form.value.wechat = detail.wechat || '';
      form.value.email = detail.email || '';
      form.value.company = detail.company || '';
      form.value.address = detail.address || '';
      form.value.intro = detail.intro || '';
      form.value.tags = detail.tags || '';
    } catch {
      uni.showToast({ title: '加载名片失败', icon: 'none' });
    } finally {
      loading.value = false;
    }
  } else {
    uni.setNavigationBarTitle({ title: '新建名片' });
  }
});
</script>

<style lang="scss" scoped>

.form-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 60rpx;
}

.skeleton-wrap {
  padding: $spacing-md;
}

.section-title {
  padding: $spacing-lg $spacing-lg $spacing-sm;
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}

.form-card {
  background: $bg-card;
  margin: 0 $spacing-lg $spacing-sm;
  border-radius: $radius-md;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
}

.form-item {
  margin-bottom: $spacing-lg;

  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  display: block;
  font-size: 26rpx;
  color: $text-regular;
  margin-bottom: $spacing-sm;
  font-weight: 500;
}

.required {
  color: $brand-error;
}

.input-placeholder {
  color: $text-placeholder;
  font-size: 28rpx;
}

.btn-area {
  padding: $spacing-lg;
}
</style>
