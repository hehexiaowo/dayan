<template>
  <view class="edit-page dy-safe-bottom">
    <!-- 复用预填入口（仅新增模式） -->
    <view v-if="!isEdit" class="suggest-bar dy-clickable" @click="openSuggest">
      <DyIconBlock text="荐" color="orange" size="sm" shape="circle" />
      <text class="suggest-text">选择常用权益人（一键填充）</text>
      <text class="suggest-arrow">›</text>
    </view>

    <!-- 表单 -->
    <view class="form-card">
      <view class="form-item">
        <text class="label"><text class="req">*</text>姓名</text>
        <input v-model="form.usePersonName" class="input" placeholder="请输入权益人姓名" placeholder-class="ph" />
      </view>

      <view class="form-item">
        <text class="label">性别</text>
        <picker :range="genderLabels" :value="genderIndex" @change="onGenderChange">
          <view class="picker-display">
            <text :class="{ 'ph-text': genderIndex < 0 }">{{ genderIndex >= 0 ? genderLabels[genderIndex] : '请选择' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">与持卡人关系</text>
        <picker :range="relationLabels" :value="relationIndex" @change="onRelationChange">
          <view class="picker-display">
            <text :class="{ 'ph-text': relationIndex < 0 }">{{ relationIndex >= 0 ? relationLabels[relationIndex] : '请选择（按权益构成校验席位）' }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">联系电话</text>
        <input v-model="form.usePersonPhone" class="input" type="number" :maxlength="11" placeholder="请输入手机号" placeholder-class="ph" />
      </view>

      <view class="form-item">
        <text class="label">身份证号</text>
        <input v-model="form.usePersonIdCard" class="input" placeholder="请输入身份证号（选填）" placeholder-class="ph" />
      </view>

      <view v-if="!isEdit" class="form-item switch-item">
        <text class="label">设为默认权益人</text>
        <switch :checked="form.isDefaultHolder === 1" color="#67C23A" @change="onDefaultChange" />
      </view>
    </view>

    <view class="tips">
      <text class="tip-text">权益人为实际享受养老服务的老人，信息仅用于服务安排，我们将严格保护隐私。</text>
    </view>

    <view class="bottom-bar">
      <button class="dy-btn dy-btn-primary" :disabled="submitting" @click="handleSubmit">
        {{ submitting ? '保存中...' : isEdit ? '保存修改' : '添加权益人' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import {
  createUsePerson,
  updateUsePerson,
  getUsePersons,
  suggestUsePersons,
} from '@/api/equity';
import type { EquityUsePerson, EquityUsePersonCreate } from '@/types';
import { RELATION_OPTIONS, relationLabel } from '@/types';
import DyIconBlock from '@/components/DyIconBlock/DyIconBlock.vue';

const equityCode = ref('');
const personId = ref('');
const isEdit = computed(() => !!personId.value);
const submitting = ref(false);

const genderLabels = ['男', '女'];
const genderIndex = ref(-1);

const relationLabels = RELATION_OPTIONS.map((o) => o.label);
const relationIndex = ref(-1);

function onRelationChange(e: { detail: { value: number } }) {
  relationIndex.value = Number(e.detail.value);
  form.relationWithHolder = RELATION_OPTIONS[relationIndex.value].value;
}

/** 按字典code回填关系选择器（存量自由文本不在字典内时不预选） */
function setRelationByCode(code?: string) {
  const i = RELATION_OPTIONS.findIndex((o) => o.value === code);
  relationIndex.value = i;
  form.relationWithHolder = i >= 0 ? RELATION_OPTIONS[i].value : '';
}

const form = reactive({
  usePersonName: '',
  usePersonGender: undefined as number | undefined,
  relationWithHolder: '',
  usePersonPhone: '',
  usePersonIdCard: '',
  isDefaultHolder: 0,
});

function onGenderChange(e: { detail: { value: number } }) {
  genderIndex.value = e.detail.value;
  form.usePersonGender = Number(e.detail.value) + 1; // 1=男 2=女
}
function onDefaultChange(e: { detail: { value: boolean } }) {
  form.isDefaultHolder = e.detail.value ? 1 : 0;
}

async function loadExisting() {
  if (!personId.value) return;
  try {
    const list = await getUsePersons(equityCode.value);
    const p = list.find((x) => x.id === personId.value);
    if (p) {
      form.usePersonName = p.usePersonName || '';
      form.usePersonGender = p.usePersonGender;
      setRelationByCode(p.relationWithHolder);
      form.usePersonPhone = p.usePersonPhone || '';
      form.isDefaultHolder = p.isDefaultHolder ?? 0;
      if (p.usePersonGender) genderIndex.value = p.usePersonGender - 1;
      // 身份证在 VO 中已解密返回（getDetail 解密），列表接口可能不含；此处尝试填入
      form.usePersonIdCard = p.usePersonIdCard || '';
    }
  } catch (e) {
    /* 拦截器已提示 */
  }
}

/** 常用权益人复用 */
async function openSuggest() {
  try {
    const list = await suggestUsePersons();
    if (!list || list.length === 0) {
      uni.showToast({ title: '暂无常用权益人可复用', icon: 'none' });
      return;
    }
    const names = list.map((p) => {
      const rel = p.relationWithHolder ? `（${relationLabel(p.relationWithHolder)}）` : '';
      return p.usePersonName + rel;
    });
    uni.showActionSheet({
      itemList: names,
      success: (res) => {
        const p = list[res.tapIndex];
        form.usePersonName = p.usePersonName || '';
        form.usePersonGender = p.usePersonGender;
        setRelationByCode(p.relationWithHolder);
        form.usePersonPhone = p.usePersonPhone || '';
        if (p.usePersonGender) genderIndex.value = p.usePersonGender - 1;
      },
    });
  } catch (e) {
    /* 拦截器已提示 */
  }
}

async function handleSubmit() {
  if (!form.usePersonName.trim()) {
    uni.showToast({ title: '请输入姓名', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    if (isEdit.value) {
      await updateUsePerson(personId.value, {
        usePersonName: form.usePersonName,
        usePersonGender: form.usePersonGender,
        relationWithHolder: form.relationWithHolder,
        usePersonPhone: form.usePersonPhone,
        usePersonIdCard: form.usePersonIdCard,
      });
    } else {
      const dto: EquityUsePersonCreate = {
        equityCode: equityCode.value,
        usePersonName: form.usePersonName,
        usePersonGender: form.usePersonGender,
        relationWithHolder: form.relationWithHolder,
        usePersonPhone: form.usePersonPhone,
        usePersonIdCard: form.usePersonIdCard,
        isDefaultHolder: form.isDefaultHolder,
      };
      await createUsePerson(dto);
    }
    uni.showToast({ title: isEdit.value ? '已保存' : '已添加', icon: 'success' });
    setTimeout(() => uni.navigateBack(), 800);
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false;
  }
}

onLoad((q) => {
  equityCode.value = q?.equityCode || '';
  personId.value = q?.id || '';
  if (isEdit.value) loadExisting();
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.edit-page {
  min-height: 100vh;
  background: $bg-page;
}

/* 复用入口 */
.suggest-bar {
  display: flex;
  align-items: center;
  background: $bg-card;
  margin: $spacing-sm $spacing-md 0;
  border-radius: $radius-lg;
  padding: $spacing-md;
  box-shadow: $shadow-card;
  .suggest-text {
    flex: 1;
    font-size: 28rpx;
    color: $brand-primary;
    font-weight: 500;
    margin-left: $spacing-md;
  }
  .suggest-arrow {
    color: $brand-primary;
    font-size: 32rpx;
  }
}

/* 表单卡 */
.form-card {
  background: $bg-card;
  margin: $spacing-sm $spacing-md 0;
  border-radius: $radius-lg;
  padding: 10rpx $spacing-md;
  box-shadow: $shadow-card;
}
.form-item {
  padding: $spacing-md 0;
  border-bottom: 1px solid $border-light;
  &:last-child {
    border-bottom: none;
  }
}
.label {
  display: block;
  font-size: 26rpx;
  color: $text-secondary;
  margin-bottom: 12rpx;
  .req {
    color: $brand-error;
    margin-right: 4rpx;
  }
}
.input {
  height: 60rpx;
  font-size: 30rpx;
  color: $text-primary;
}
.ph {
  color: $text-placeholder;
}
.picker-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60rpx;
  font-size: 30rpx;
  color: $text-primary;
  .ph-text {
    color: $text-placeholder;
  }
  .picker-arrow {
    color: $text-placeholder;
  }
}
.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  .label {
    margin-bottom: 0;
    color: $text-primary;
    font-size: 30rpx;
  }
}

.tips {
  margin: $spacing-sm $spacing-md 0;
  .tip-text {
    font-size: 22rpx;
    color: $text-placeholder;
    line-height: 1.6;
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: $spacing-sm $spacing-md;
  background: $bg-card;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  .dy-btn {
    font-size: 32rpx;
    height: 84rpx;
    &[disabled] {
      background: lighten($brand-primary, 15%);
      box-shadow: none;
    }
  }
}
</style>
