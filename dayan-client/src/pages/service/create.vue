<template>
  <view class="create-page dy-safe-bottom">
    <!-- 步骤条 -->
    <view class="step-bar">
      <view v-for="(s, i) in steps" :key="i" class="step" :class="{ active: step === i, done: step > i }">
        <view class="step-dot">{{ step > i ? '✓' : i + 1 }}</view>
        <text class="step-label">{{ s }}</text>
        <view v-if="i < steps.length - 1" class="step-line" :class="{ filled: step > i }"></view>
      </view>
    </view>

    <!-- Step 0: 选服务项目 -->
    <view v-if="step === 0" class="step-content">
      <text class="step-hint">请选择需要的服务项目</text>
      <view class="item-list">
        <view
          v-for="item in items"
          :key="item.itemCode"
          class="item-card dy-clickable"
          :class="{ selected: selected.itemCode === item.itemCode, disabled: item.remaining <= 0 }"
          @click="item.remaining > 0 && selectItem(item.itemCode)"
        >
          <view class="item-info">
            <text class="item-name">{{ item.itemName }}</text>
            <text class="item-quota">剩余 {{ item.remaining }} / {{ item.quantity }} 次</text>
          </view>
          <view class="item-check">
            <text v-if="item.remaining <= 0" class="used-up">已用完</text>
            <text v-else-if="selected.itemCode === item.itemCode" class="checked">✓</text>
          </view>
        </view>
      </view>
      <view v-if="items.length === 0 && !loading" class="empty"><text>该权益暂无可用服务项目</text></view>
    </view>

    <!-- Step 1: 选使用人 -->
    <view v-if="step === 1" class="step-content">
      <text class="step-hint">请选择服务对象（权益人）</text>
      <view v-if="persons.length === 0" class="no-person">
        <text class="no-person-text">该权益还没有添加权益人</text>
        <button class="btn-link" @click="goAddPerson">去添加权益人</button>
      </view>
      <view v-else class="person-list">
        <view
          v-for="p in persons"
          :key="p.id"
          class="person-card dy-clickable"
          :class="{ selected: selected.usePersonId === p.id, placeholder: isPlaceholder(p) }"
          @click="!isPlaceholder(p) && selectPerson(p.id)"
        >
          <view class="avatar"><text>{{ isPlaceholder(p) ? '?' : (p.usePersonName || '?').charAt(0) }}</text></view>
          <view class="person-info">
            <text class="name">{{ p.usePersonName }}</text>
            <text v-if="p.relationWithHolder" class="rel">{{ p.relationWithHolder }}</text>
          </view>
          <text v-if="isPlaceholder(p)" class="warn">请先补全</text>
          <text v-else-if="selected.usePersonId === p.id" class="checked">✓</text>
        </view>
      </view>
    </view>

    <!-- Step 2: 填需求 -->
    <view v-if="step === 2" class="step-content">
      <text class="step-hint">请描述您的服务需求</text>
      <view class="form-card">
        <text class="summary-label">服务项目：{{ selectedItemName }}</text>
        <text class="summary-label">服务对象：{{ selectedPersonName }}</text>
        <view class="demand-section">
          <text class="demand-label">需求描述（选填）</text>
          <textarea
            v-model="selected.demandDesc"
            class="demand-input"
            placeholder="如：老人需要上门健康评估、希望安排在下周等"
            placeholder-class="ph"
            :maxlength="500"
          />
          <text class="char-count">{{ (selected.demandDesc || '').length }} / 500</text>
        </view>
      </view>
    </view>

    <!-- 底部操作 -->
    <view class="bottom-bar">
      <button v-if="step > 0" class="btn-prev" @click="step--">上一步</button>
      <button v-if="step < 2" class="btn-next" :disabled="!canNext" @click="next">下一步</button>
      <button v-else class="btn-submit" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中...' : '提交服务请求' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { getServiceItems, getUsePersons, createServiceRequest } from '@/api/equity';
import type { ClientServiceItem, EquityUsePerson } from '@/types';

const equityCode = ref('');
const step = ref(0);
const steps = ['选项目', '选对象', '填需求'];
const loading = ref(false);
const submitting = ref(false);

const items = ref<ClientServiceItem[]>([]);
const persons = ref<EquityUsePerson[]>([]);

const selected = reactive({
  itemCode: '',
  usePersonId: '',
  demandDesc: '',
});

const selectedItemName = computed(() => items.value.find((i) => i.itemCode === selected.itemCode)?.itemName || '');
const selectedPersonName = computed(() => persons.value.find((p) => p.id === selected.usePersonId)?.usePersonName || '');

const canNext = computed(() => {
  if (step.value === 0) return !!selected.itemCode;
  if (step.value === 1) return !!selected.usePersonId;
  return true;
});

function isPlaceholder(p: EquityUsePerson) {
  return !p.usePersonName || p.usePersonName.startsWith('待填写');
}

function selectItem(code: string) {
  selected.itemCode = code;
}
function selectPerson(id: string) {
  selected.usePersonId = id;
}

function next() {
  if (!canNext.value) return;
  step.value++;
}

async function loadData() {
  loading.value = true;
  try {
    const [it, ps] = await Promise.all([
      getServiceItems(equityCode.value).catch(() => []),
      getUsePersons(equityCode.value).catch(() => []),
    ]);
    items.value = it;
    persons.value = ps;
  } finally {
    loading.value = false;
  }
}

function goAddPerson() {
  uni.navigateTo({ url: `/pages/equity/use-persons/edit?equityCode=${equityCode.value}` });
}

async function submit() {
  submitting.value = true;
  try {
    const sessionCode = await createServiceRequest({
      equityCode: equityCode.value,
      itemCode: selected.itemCode,
      usePersonId: selected.usePersonId,
      demandDesc: selected.demandDesc,
    });
    uni.showToast({ title: '服务请求已提交', icon: 'success' });
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/service/detail?sessionCode=${sessionCode}` });
    }, 1000);
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false;
  }
}

onLoad((q) => {
  equityCode.value = q?.equityCode || '';
  selected.itemCode = q?.itemCode || '';
  loadData().then(() => {
    // 如果带了 itemCode 且有可用项目，直接进步骤 1
    if (selected.itemCode && items.value.some((i) => i.itemCode === selected.itemCode && i.remaining > 0)) {
      step.value = 1;
    }
  });
});
// 从添加权益人页返回时刷新
onShow(() => {
  if (equityCode.value && step.value === 1) {
    getUsePersons(equityCode.value).then((ps) => { persons.value = ps; }).catch(() => {});
  }
});
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.create-page {
  min-height: 100vh;
  background: $bg-page;
}

/* 步骤条 */
.step-bar {
  display: flex;
  align-items: flex-start;
  background: $bg-card;
  padding: 40rpx $spacing-md $spacing-md;
  box-shadow: $shadow-card;
}
.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}
.step-dot {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: $border-base;
  color: $text-secondary;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}
.step.active .step-dot,
.step.done .step-dot {
  background: $gradient-brand;
  color: #fff;
}
.step-label {
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: $spacing-xs;
}
.step.active .step-label {
  color: $brand-primary;
  font-weight: 500;
}
.step-line {
  position: absolute;
  top: 28rpx;
  left: 50%;
  width: 100%;
  height: 4rpx;
  background: $border-base;
  z-index: 0;
  &.filled {
    background: $brand-primary;
  }
}

/* 步骤内容 */
.step-content {
  padding: $spacing-sm $spacing-md;
}
.step-hint {
  font-size: 28rpx;
  color: $text-regular;
  margin-bottom: $spacing-sm;
  display: block;
}

/* 项目/人员卡片 */
.item-card,
.person-card {
  display: flex;
  align-items: center;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: 28rpx $spacing-md;
  margin-bottom: $spacing-sm;
  border: 2rpx solid transparent;
  box-shadow: $shadow-card;
  &.selected {
    border-color: $brand-primary;
    background: $brand-primary-light;
  }
  &.disabled {
    opacity: 0.55;
  }
}
.item-info { flex: 1; }
.item-name {
  font-size: 30rpx;
  color: $text-primary;
  display: block;
}
.item-quota {
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: $spacing-xs;
  display: block;
}
.item-check { margin-left: $spacing-sm; }
.checked {
  color: $brand-primary;
  font-size: 36rpx;
  font-weight: bold;
}
.used-up {
  font-size: 24rpx;
  color: $text-placeholder;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: $gradient-brand;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: bold;
  flex-shrink: 0;
  .placeholder & {
    background: $gradient-red;
  }
}
.person-info {
  flex: 1;
  margin-left: $spacing-sm;
}
.name {
  font-size: 30rpx;
  color: $text-primary;
  display: block;
}
.rel {
  font-size: 24rpx;
  color: $text-secondary;
  margin-top: 4rpx;
  display: block;
}
.person-card.placeholder {
  border-color: $brand-error;
}
.warn {
  font-size: 24rpx;
  color: $brand-error;
}

.no-person {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: 60rpx 0;
  text-align: center;
  box-shadow: $shadow-card;
  .no-person-text {
    font-size: 28rpx;
    color: $text-secondary;
    display: block;
    margin-bottom: $spacing-md;
  }
}
.btn-link {
  background: $brand-primary;
  color: #fff;
  font-size: 26rpx;
  border-radius: $radius-sm;
  padding: 0 40rpx;
  height: 68rpx;
  line-height: 68rpx;
  display: inline-block;
}

/* 需求表单 */
.form-card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-md $spacing-md;
  box-shadow: $shadow-card;
}
.summary-label {
  font-size: 28rpx;
  color: $text-primary;
  display: block;
  margin-bottom: $spacing-sm;
}
.demand-section {
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px solid $border-light;
}
.demand-label {
  font-size: 26rpx;
  color: $text-secondary;
  display: block;
  margin-bottom: 12rpx;
}
.demand-input {
  width: 100%;
  min-height: 200rpx;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  padding: $spacing-sm;
  font-size: 28rpx;
  color: $text-primary;
  box-sizing: border-box;
}
.ph {
  color: $text-placeholder;
}
.char-count {
  font-size: 22rpx;
  color: $text-placeholder;
  text-align: right;
  display: block;
  margin-top: $spacing-xs;
}

/* 底部 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: $bg-card;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
}
.btn-prev {
  flex: 1;
  background: $bg-card;
  color: $brand-primary;
  border: 2rpx solid $brand-primary;
  font-size: 30rpx;
  border-radius: $radius-md;
  height: 84rpx;
  line-height: 80rpx;
}
.btn-next,
.btn-submit {
  flex: 2;
  background: $gradient-brand;
  color: #fff;
  font-size: 30rpx;
  border-radius: $radius-md;
  height: 84rpx;
  line-height: 84rpx;
  &[disabled] {
    background: lighten($brand-primary, 15%);
  }
}

.empty {
  text-align: center;
  padding: 80rpx 0;
  color: $text-placeholder;
  font-size: 28rpx;
}
</style>
