<template>
  <view class="form-page">
    <!-- 骨架屏（编辑模式加载中） -->
    <view v-if="loading" class="skeleton-wrap">
      <DySkeleton :rows="4" card />
      <DySkeleton :rows="3" card />
    </view>

    <view v-else class="form-content">
      <!-- 基本信息 -->
      <view class="section-title">{{ isEdit ? '编辑线索' : '新增线索' }}</view>
      <view class="form-card">
        <!-- 姓名 -->
        <view class="form-item">
          <text class="form-label">姓名 <text class="required">*</text></text>
          <input
            v-model="form.name"
            class="form-input"
            placeholder="请输入客户姓名"
            placeholder-class="input-placeholder"
            maxlength="100"
          />
        </view>

        <!-- 手机号 -->
        <view class="form-item">
          <text class="form-label">手机号</text>
          <input
            v-model="form.phone"
            class="form-input"
            placeholder="请输入11位手机号"
            placeholder-class="input-placeholder"
            type="number"
            maxlength="11"
          />
        </view>

        <!-- 性别 -->
        <view class="form-item">
          <text class="form-label">性别</text>
          <view class="radio-group">
            <view
              v-for="opt in genderOptions"
              :key="opt.value"
              class="radio-item dy-clickable"
              :class="{ active: form.gender === opt.value }"
              @click="form.gender = opt.value"
            >
              <text class="radio-text">{{ opt.label }}</text>
            </view>
          </view>
        </view>

        <!-- 年龄 -->
        <view class="form-item">
          <text class="form-label">年龄</text>
          <input
            v-model="form.age"
            class="form-input"
            placeholder="请输入年龄"
            placeholder-class="input-placeholder"
            type="number"
            maxlength="3"
          />
        </view>

        <!-- 意向等级 -->
        <view class="form-item">
          <text class="form-label">意向等级</text>
          <view class="radio-group">
            <view
              v-for="opt in intentionOptions"
              :key="opt.value"
              class="radio-item dy-clickable"
              :class="{ active: form.intentionLevel === opt.value }"
              @click="form.intentionLevel = opt.value"
            >
              <text class="radio-text">{{ opt.label }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 养老需求 -->
      <view class="section-title">养老需求</view>
      <view class="form-card">
        <!-- 关注类型 -->
        <view class="form-item">
          <text class="form-label">关注类型</text>
          <view class="tag-group">
            <view
              v-for="tag in interestTypeOptions"
              :key="tag"
              class="tag-item dy-clickable"
              :class="{ active: selectedInterests.includes(tag) }"
              @click="toggleInterest(tag)"
            >
              <text class="tag-text">{{ tag }}</text>
            </view>
          </view>
        </view>

        <!-- 关注区域 -->
        <view class="form-item">
          <text class="form-label">关注区域</text>
          <input
            v-model="form.region"
            class="form-input"
            placeholder="如：北京朝阳区"
            placeholder-class="input-placeholder"
            maxlength="200"
          />
        </view>

        <!-- 备注 -->
        <view class="form-item">
          <text class="form-label">备注</text>
          <textarea
            v-model="form.remark"
            class="form-textarea"
            placeholder="补充线索的备注信息"
            placeholder-class="input-placeholder"
            maxlength="500"
            :auto-height="true"
          />
        </view>
      </view>

      <!-- 提交按钮 -->
      <view class="btn-area">
        <button class="btn btn-primary" :class="{ 'is-disabled': submitting }" :disabled="submitting" @click="onSubmit">
          {{ submitting ? '提交中...' : isEdit ? '保存修改' : '新增线索' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { createLead, updateLead, getLeadDetail } from '@/api/lead';
import DySkeleton from '@/components/DySkeleton/DySkeleton.vue';

const isEdit = ref(false);
const loading = ref(false);
const submitting = ref(false);
const leadId = ref<string | null>(null);

const form = ref({
  name: '',
  phone: '',
  gender: 0 as number,
  age: undefined as number | undefined,
  intentionLevel: undefined as number | undefined,
  region: '',
  remark: '',
});

const selectedInterests = ref<string[]>([]);

const genderOptions = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
];

const intentionOptions = [
  { label: '低', value: 1 },
  { label: '中', value: 2 },
  { label: '高', value: 3 },
];

const interestTypeOptions = ['旅居', '活力长居', '照护'];

function toggleInterest(tag: string) {
  const idx = selectedInterests.value.indexOf(tag);
  if (idx >= 0) {
    selectedInterests.value.splice(idx, 1);
  } else {
    selectedInterests.value.push(tag);
  }
}

function validate(): string | null {
  if (!form.value.name?.trim()) {
    return '请输入客户姓名';
  }
  if (form.value.phone && !/^1\d{10}$/.test(form.value.phone.trim())) {
    return '手机号格式不正确';
  }
  if (form.value.age !== undefined && form.value.age !== null) {
    if (form.value.age < 0 || form.value.age > 150) {
      return '年龄请填入合理数值';
    }
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

  // 组装提交数据
  const interestType = selectedInterests.value.length > 0 ? selectedInterests.value.join(', ') : undefined;
  const payload: any = {
    name: form.value.name.trim(),
    phone: form.value.phone?.trim() || undefined,
    gender: form.value.gender,
    age: form.value.age || undefined,
    intentionLevel: form.value.intentionLevel || undefined,
    interestType,
    region: form.value.region?.trim() || undefined,
    remark: form.value.remark?.trim() || undefined,
  };

  try {
    if (isEdit.value && leadId.value) {
      await updateLead(leadId.value, payload);
      uni.showToast({ title: '保存成功', icon: 'success' });
    } else {
      // 新增时固定来源为手工录入
      payload.sourceType = 1;
      await createLead(payload);
      uni.showToast({ title: '添加成功', icon: 'success' });
    }
    setTimeout(() => uni.navigateBack(), 500);
  } catch {
    // 错误已由 request 拦截器提示
  } finally {
    submitting.value = false;
  }
}

onLoad(async (options: any) => {
  if (options?.id) {
    leadId.value = String(options.id);
    isEdit.value = true;
    uni.setNavigationBarTitle({ title: '编辑线索' });
    loading.value = true;
    try {
      const detail = await getLeadDetail(leadId.value);
      form.value.name = detail.name || '';
      form.value.phone = detail.phone || '';
      form.value.gender = detail.gender ?? 0;
      form.value.age = detail.age;
      form.value.intentionLevel = detail.intentionLevel;
      form.value.region = detail.region || '';
      form.value.remark = detail.remark || '';
      if (detail.interestType) {
        selectedInterests.value = detail.interestType.split(/[，,]/).map((s) => s.trim()).filter(Boolean);
      }
    } catch {
      uni.showToast({ title: '加载线索失败', icon: 'none' });
    } finally {
      loading.value = false;
    }
  } else {
    uni.setNavigationBarTitle({ title: '新增线索' });
  }
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.form-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 60rpx;
}

.skeleton-wrap {
  padding: $spacing-md;
}

/* 区块标题 */
.section-title {
  padding: $spacing-lg $spacing-lg $spacing-sm;
  font-size: 32rpx;
  font-weight: bold;
  color: $text-primary;
}

/* 表单卡片 */
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

.form-input {
  width: 100%;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  padding: 24rpx 28rpx;
  font-size: 30rpx;
  color: $text-primary;
  transition: border-color $transition-base;
  background: $bg-card;

  &:focus {
    border-color: $brand-primary;
  }
}

.form-textarea {
  width: 100%;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  padding: 24rpx 28rpx;
  font-size: 30rpx;
  color: $text-primary;
  min-height: 120rpx;
  transition: border-color $transition-base;

  &:focus {
    border-color: $brand-primary;
  }
}

.input-placeholder {
  color: $text-placeholder;
  font-size: 28rpx;
}

/* 单选按钮组 */
.radio-group {
  display: flex;
  gap: $spacing-sm;
  flex-wrap: wrap;
}
.radio-item {
  padding: 16rpx 36rpx;
  border-radius: $radius-md;
  border: 2rpx solid $border-base;
  background: $bg-card;
  transition: all $transition-base;
}
.radio-item.active {
  border-color: $brand-primary;
  background: $brand-primary-light;
}
.radio-text {
  font-size: 28rpx;
  color: $text-regular;
}
.radio-item.active .radio-text {
  color: $brand-primary;
  font-weight: 500;
}

/* 多选标签组 */
.tag-group {
  display: flex;
  gap: $spacing-sm;
  flex-wrap: wrap;
}
.tag-item {
  padding: 16rpx 36rpx;
  border-radius: $radius-md;
  border: 2rpx solid $border-base;
  background: $bg-card;
  transition: all $transition-base;
}
.tag-item.active {
  border-color: $brand-primary;
  background: $brand-primary-light;
}
.tag-text {
  font-size: 28rpx;
  color: $text-regular;
}
.tag-item.active .tag-text {
  color: $brand-primary;
  font-weight: 500;
}

/* 提交按钮区 */
.btn-area {
  padding: $spacing-lg;
}
.btn {
  width: 100%;
  border-radius: $radius-md;
  font-size: 32rpx;
  font-weight: 500;
  padding: 24rpx 0;
  transition: all $transition-base;

  &:active {
    transform: scale(0.98);
  }

  &.is-disabled {
    opacity: 0.6;
  }
}
.btn-primary {
  background: $gradient-blue;
  color: #fff;
  box-shadow: 0 8rpx 20rpx rgba(64, 158, 255, 0.3);
}
</style>
