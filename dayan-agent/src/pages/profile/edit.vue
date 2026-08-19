<template>
  <view class="page">
    <view class="form-card">
      <!-- 头像行：圆形头像(点击换) + "点击更换头像"提示 -->
      <view class="avatar-row" @click="chooseAvatar">
        <image v-if="avatarUrl" class="avatar-lg" :src="avatarUrl" mode="aspectFill" />
        <view v-else class="avatar-lg avatar-fallback">
          <text>{{ avatarChar }}</text>
        </view>
        <text class="avatar-tip">点击更换头像</text>
      </view>

      <view class="form-row">
        <text class="form-label">姓名</text>
        <input
          class="form-input"
          v-model="form.fullName"
          :disabled="certifiedLocked"
          :placeholder="certifiedLocked ? '已认证，联系管理员修改' : '请输入姓名'"
          maxlength="20"
        />
      </view>
      <view class="form-row">
        <text class="form-label">性别</text>
        <view class="gender-group">
          <text
            v-for="g in genderOptions"
            :key="g.value"
            class="gender-item"
            :class="{ active: form.gender === g.value }"
            @click="form.gender = g.value"
          >
            {{ g.label }}
          </text>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">手机号</text>
        <view class="phone-row">
          <text class="form-value">{{ maskPhone(form.phone) }}</text>
          <text class="phone-edit" @click="phonePopupVisible = true">修改</text>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">邮箱</text>
        <input class="form-input" v-model="form.email" placeholder="选填" maxlength="100" />
      </view>
      <view class="form-row">
        <text class="form-label">所在地区</text>
        <picker
          class="form-picker"
          mode="multiSelector"
          :range="regionRange"
          :value="regionIndex"
          @columnchange="onRegionColumnChange"
          @change="onRegionChange"
        >
          <view class="form-value" :class="{ placeholder: !regionText }">
            {{ regionText || '请选择' }}
          </view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-label">详细地址</text>
        <input class="form-input" v-model="form.address" placeholder="选填" maxlength="200" />
      </view>
      <view class="form-row column">
        <text class="form-label">服务简介</text>
        <textarea
          class="form-textarea"
          v-model="form.serviceIntro"
          maxlength="200"
          placeholder="向客户介绍你的服务专长（200 字内）"
        />
        <text class="textarea-count">{{ (form.serviceIntro || '').length }}/200</text>
      </view>
    </view>

    <button class="save-btn dy-clickable" :disabled="saving" @click="onSave">
      {{ saving ? '保存中…' : '保存' }}
    </button>

    <!-- 手机号换绑弹层 -->
    <view v-if="phonePopupVisible" class="popup-mask" @click="closePhonePopup">
      <view class="popup" @click.stop>
        <view class="popup-title">换绑手机号</view>
        <view class="popup-tip">验证码将发送至新手机号，换绑后请使用新手机号登录</view>
        <input
          class="popup-input"
          v-model="phoneForm.mobile"
          type="number"
          maxlength="11"
          placeholder="请输入新手机号"
        />
        <view class="code-row">
          <input
            class="popup-input code-input"
            v-model="phoneForm.code"
            type="number"
            maxlength="6"
            placeholder="验证码"
          />
          <button class="code-btn" :disabled="codeCountdown > 0" @click="onSendCode">
            {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
          </button>
        </view>
        <view class="popup-actions">
          <button class="popup-btn cancel" @click="closePhonePopup">取消</button>
          <button class="popup-btn confirm" :disabled="phoneSubmitting" @click="onChangePhone">
            {{ phoneSubmitting ? '提交中…' : '确认换绑' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { getProfile, updateProfile, sendPhoneChangeCode, changePhone } from '@/api/agent';
import { uploadFile } from '@/api/file';
import { listProvinces, listRegionChildren } from '@/api/region';
import type { Region } from '@/api/region';
import { formatFileUrl } from '@/utils/file';

const form = reactive({
  fullName: '',
  gender: 0 as number,
  phone: '',
  email: '',
  avatar: '',
  provinceCode: '',
  cityCode: '',
  districtCode: '',
  address: '',
  serviceIntro: '',
});
const isCertified = ref(0);
const saving = ref(false);
/** 手机号换绑弹层可见性 */
const phonePopupVisible = ref(false);
/** 换绑提交中（防重复点击） */
const phoneSubmitting = ref(false);
/** 发码倒计时秒数，>0 时按钮禁用并显示秒数 */
const codeCountdown = ref(0);
/** 换绑弹层表单（失败时保留已输入内容） */
const phoneForm = reactive({ mobile: '', code: '' });
let countdownTimer: ReturnType<typeof setInterval> | null = null;
/** 已认证锁定姓名（不可自助修改） */
const certifiedLocked = computed(() => isCertified.value === 1);

const genderOptions = [
  { value: 1, label: '男' },
  { value: 2, label: '女' },
  { value: 0, label: '保密' },
];

/** 头像 URL（OSS key 转可访问地址） */
const avatarUrl = computed(() => formatFileUrl(form.avatar));
/** 头像首字（无图片时显示） */
const avatarChar = computed(() => (form.fullName ? form.fullName.charAt(0) : '代'));

/** 手机号脱敏：11 位手机号中间四位打码，其余原样（与主页同款，页面私有） */
function maskPhone(p?: string): string {
  if (!p) return '-';
  return p.length === 11 ? `${p.slice(0, 3)}****${p.slice(7)}` : p;
}

/* ===== 地区三级联动 ===== */
const provinces = ref<Region[]>([]);
const cities = ref<Region[]>([]);
const districts = ref<Region[]>([]);
const regionIndex = ref<[number, number, number]>([0, 0, 0]);
const regionRange = computed(() => [
  provinces.value.map((r) => r.regionName),
  cities.value.map((r) => r.regionName),
  districts.value.map((r) => r.regionName),
]);
const regionText = computed(() =>
  [
    provinces.value[regionIndex.value[0]]?.regionName,
    cities.value[regionIndex.value[1]]?.regionName,
    districts.value[regionIndex.value[2]]?.regionName,
  ]
    .filter(Boolean)
    .join(' '),
);

async function initRegion() {
  provinces.value = await listProvinces();
  if (form.provinceCode) {
    const pi = provinces.value.findIndex((r) => r.regionCode === form.provinceCode);
    if (pi >= 0) {
      cities.value = await listRegionChildren(form.provinceCode);
      const ci = cities.value.findIndex((r) => r.regionCode === form.cityCode);
      if (form.cityCode && ci >= 0) {
        districts.value = await listRegionChildren(form.cityCode);
        const di = districts.value.findIndex((r) => r.regionCode === form.districtCode);
        regionIndex.value = [pi, ci, Math.max(di, 0)];
        return;
      }
      regionIndex.value = [pi, 0, 0];
      return;
    }
  }
  // 无初始值：默认加载第一省/市下级，避免空列
  if (provinces.value.length) {
    cities.value = await listRegionChildren(provinces.value[0].regionCode);
    if (cities.value.length) {
      districts.value = await listRegionChildren(cities.value[0].regionCode);
    }
  }
}

async function onRegionColumnChange(e: { detail: { column: number; value: number } }) {
  const { column, value } = e.detail;
  regionIndex.value[column] = value;
  if (column === 0) {
    cities.value = await listRegionChildren(provinces.value[value].regionCode);
    districts.value = cities.value.length
      ? await listRegionChildren(cities.value[0].regionCode)
      : [];
    regionIndex.value = [value, 0, 0];
  } else if (column === 1) {
    districts.value = await listRegionChildren(cities.value[value].regionCode);
    regionIndex.value = [regionIndex.value[0], value, 0];
  }
}

function onRegionChange() {
  form.provinceCode = provinces.value[regionIndex.value[0]]?.regionCode || '';
  form.cityCode = cities.value[regionIndex.value[1]]?.regionCode || '';
  form.districtCode = districts.value[regionIndex.value[2]]?.regionCode || '';
}

/* ===== 头像上传（上传后立即预览，保存时才随 PUT 提交） ===== */
function chooseAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      const path = res.tempFilePaths[0];
      if (!path) return;
      uni.showLoading({ title: '上传中', mask: true });
      try {
        const result = await uploadFile(path, 'avatar');
        form.avatar = result.key;
      } catch {
        // uploadFile 内部已 toast，失败保留原头像
      } finally {
        uni.hideLoading();
      }
    },
  });
}

/* ===== 加载：先回显资料，再按 provinceCode/cityCode 初始化地区列 ===== */
onLoad(async () => {
  try {
    const data = await getProfile();
    if (data) {
      form.fullName = data.fullName || '';
      form.gender = typeof data.gender === 'number' ? data.gender : 0;
      form.phone = data.phone || '';
      form.email = data.email || '';
      form.avatar = data.avatar || '';
      form.provinceCode = data.provinceCode || '';
      form.cityCode = data.cityCode || '';
      form.districtCode = data.districtCode || '';
      form.address = data.address || '';
      form.serviceIntro = data.serviceIntro || '';
      isCertified.value = typeof data.isCertified === 'number' ? data.isCertified : 0;
    }
  } catch {
    // request 拦截器已 toast，表单保留空值
  }
  try {
    await initRegion();
  } catch {
    // 地区接口异常时保留空列，request 已 toast
  }
});

/* ===== 保存 ===== */
async function onSave() {
  if (!certifiedLocked.value) {
    const name = form.fullName.trim();
    if (name.length < 2 || name.length > 20) {
      uni.showToast({ title: '姓名长度须为 2-20 字', icon: 'none' });
      return;
    }
  }
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    uni.showToast({ title: '邮箱格式不正确', icon: 'none' });
    return;
  }
  saving.value = true;
  try {
    await updateProfile({
      fullName: certifiedLocked.value ? undefined : form.fullName.trim(),
      gender: form.gender,
      email: form.email.trim(),
      avatar: form.avatar,
      provinceCode: form.provinceCode,
      cityCode: form.cityCode,
      districtCode: form.districtCode,
      address: form.address.trim(),
      serviceIntro: form.serviceIntro.trim(),
    });
    uni.showToast({ title: '已保存', icon: 'success' });
    setTimeout(() => uni.navigateBack(), 600);
  } catch {
    // request 拦截器已 toast
  } finally {
    saving.value = false;
  }
}

/* ===== 手机号换绑弹层 ===== */
async function onSendCode() {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.mobile)) {
    uni.showToast({ title: '请输入正确的新手机号', icon: 'none' });
    return;
  }
  try {
    const res = await sendPhoneChangeCode(phoneForm.mobile);
    codeCountdown.value = 60;
    countdownTimer = setInterval(() => {
      codeCountdown.value -= 1;
      if (codeCountdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }, 1000);
    if (res.devCode) {
      uni.showToast({ title: `开发态验证码：${res.devCode}`, icon: 'none', duration: 3000 });
    } else {
      uni.showToast({ title: '验证码已发送', icon: 'none' });
    }
  } catch {
    // 拦截器已 toast（冷却/占用等）
  }
}

async function onChangePhone() {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.mobile)) {
    uni.showToast({ title: '请输入正确的新手机号', icon: 'none' });
    return;
  }
  if (!/^\d{6}$/.test(phoneForm.code)) {
    uni.showToast({ title: '请输入 6 位验证码', icon: 'none' });
    return;
  }
  phoneSubmitting.value = true;
  try {
    await changePhone(phoneForm.mobile, phoneForm.code);
    form.phone = phoneForm.mobile;
    closePhonePopup();
    uni.showToast({ title: '换绑成功，下次登录请用新手机号', icon: 'none', duration: 2500 });
  } catch {
    // 失败保留已输入内容（不清空），拦截器已 toast
  } finally {
    phoneSubmitting.value = false;
  }
}

function closePhonePopup() {
  phonePopupVisible.value = false;
  phoneForm.mobile = '';
  phoneForm.code = '';
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
  codeCountdown.value = 0;
}
</script>

<style lang="scss" scoped>

.page {
  padding: $spacing-md $spacing-md 60rpx;
  min-height: 100vh;
  background: $bg-page;
}

.form-card {
  background: $bg-card;
  border-radius: $radius-md;
  padding: $spacing-md 28rpx;
  box-shadow: $shadow-card;
}

/* 头像行 */
.avatar-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-md 0 $spacing-lg;
}
.avatar-lg {
  width: 144rpx;
  height: 144rpx;
  border-radius: 50%;
  background: $brand-primary-light;
}
.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: $gradient-blue;
  color: #fff;
  font-size: 56rpx;
  font-weight: bold;
}
.avatar-tip {
  margin-top: $spacing-sm;
  font-size: 24rpx;
  color: $text-secondary;
}

/* 表单行 */
.form-row {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $border-light;

  &:last-child {
    border-bottom: none;
  }

  &.column {
    flex-direction: column;
    align-items: stretch;
  }
}
.form-label {
  width: 150rpx;
  flex-shrink: 0;
  font-size: 28rpx;
  color: $text-primary;
}
/* uni-app H5 编译 input 为 uni-input 自定义元素，需 display:block + height 才可见可交互 */
.form-input {
  display: block;
  flex: 1;
  min-width: 0;
  height: 64rpx;
  font-size: 28rpx;
  color: $text-primary;
  text-align: right;
}
.form-value {
  flex: 1;
  font-size: 28rpx;
  color: $text-primary;
  text-align: right;
  word-break: break-all;

  &.placeholder {
    color: $text-placeholder;
  }
}
/* picker 编译后为 uni-picker，类名会透传到根元素 */
.form-picker {
  flex: 1;
  min-width: 0;

  .form-value {
    width: 100%;
  }
}

/* 性别 */
.gender-group {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}
.gender-item {
  padding: 8rpx 32rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: $text-secondary;
  background: $bg-page;
  border-radius: 999rpx;

  & + .gender-item {
    margin-left: $spacing-sm;
  }

  &.active {
    color: #fff;
    background: $gradient-blue;
  }
}

/* 手机号行 */
.phone-row {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.phone-edit {
  margin-left: $spacing-md;
  flex-shrink: 0;
  font-size: 26rpx;
  color: $brand-primary;
}

/* 服务简介 */
.form-textarea {
  display: block;
  width: 100%;
  min-height: 160rpx;
  margin-top: $spacing-sm;
  padding: $spacing-sm;
  font-size: 28rpx;
  color: $text-primary;
  background: $bg-page;
  border-radius: $radius-sm;
  box-sizing: border-box;
}
.textarea-count {
  align-self: flex-end;
  margin-top: $spacing-xs;
  font-size: 22rpx;
  color: $text-placeholder;
}

/* 保存按钮 */
.save-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: $control-height;
  margin-top: $spacing-xl;
  border-radius: $radius-md;
  font-size: 30rpx;
  font-weight: 500;
  color: #fff;
  background: $gradient-blue;
  box-shadow: 0 8rpx 20rpx rgba(64, 158, 255, 0.3);

  &[disabled] {
    opacity: 0.6;
  }
}

/* ===== 手机号换绑弹层 ===== */
.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-mask;
}
.popup {
  display: flex;
  flex-direction: column;
  width: 600rpx;
  padding: $spacing-lg $spacing-md;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-hover;
  box-sizing: border-box;
}
.popup-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $text-primary;
  text-align: center;
}
.popup-tip {
  margin-top: $spacing-sm;
  font-size: 24rpx;
  color: $text-secondary;
  text-align: center;
  line-height: 1.5;
}
/* uni-app H5 编译 input 为 uni-input，需 display:block + height 才可见可交互 */
.popup-input {
  display: block;
  width: 100%;
  height: $control-height;
  margin-top: $spacing-md;
  padding: 0 $spacing-sm;
  font-size: 28rpx;
  color: $text-primary;
  background: $bg-page;
  border-radius: $radius-sm;
  box-sizing: border-box;
}
/* 验证码行：flex 并排（勿用 absolute 叠 input，会拦截点击） */
.code-row {
  display: flex;
  align-items: center;

  .code-input {
    flex: 1;
    min-width: 0;
  }
}
.code-btn {
  flex-shrink: 0;
  margin-left: $spacing-sm;
  margin-top: $spacing-md;
  height: $control-height;
  line-height: $control-height;
  padding: 0 $spacing-sm;
  font-size: 26rpx;
  color: $brand-primary;
  background: $brand-primary-light;
  border-radius: $radius-sm;

  &[disabled] {
    color: $text-placeholder;
    background: $brand-info-light;
  }

  &::after {
    border: none;
  }
}
.popup-actions {
  display: flex;
  margin-top: $spacing-lg;
}
.popup-btn {
  flex: 1;
  height: $control-height;
  line-height: $control-height;
  font-size: 30rpx;
  border-radius: $radius-md;

  & + .popup-btn {
    margin-left: $spacing-md;
  }

  &::after {
    border: none;
  }

  &.cancel {
    color: $text-regular;
    background: $brand-info-light;
  }

  &.confirm {
    color: #fff;
    background: $gradient-blue;

    &[disabled] {
      opacity: 0.6;
    }
  }
}
</style>
