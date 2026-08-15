<template>
  <view class="page">
    <!-- 头部 -->
    <view class="hero">
      <text class="hero-title">帮助中心</text>
      <text class="hero-sub">在这里找到您关心的常见问题</text>
    </view>

    <!-- 常见问题 -->
    <view class="section-title">常见问题</view>
    <view class="faq-card">
      <view
        v-for="(item, idx) in faqs"
        :key="idx"
        class="faq-item"
        :class="{ last: idx === faqs.length - 1 }"
      >
        <view class="faq-q" @click="toggle(idx)">
          <text class="q-text">{{ item.q }}</text>
          <text class="q-arrow" :class="{ open: openIdx === idx }">›</text>
        </view>
        <view v-if="openIdx === idx" class="faq-a">
          <text class="a-text">{{ item.a }}</text>
        </view>
      </view>
    </view>

    <!-- 联系客服 -->
    <view class="section-title">联系客服</view>
    <view class="contact-card">
      <view class="contact-row" @click="callPhone">
        <text class="contact-label">客服热线</text>
        <view class="contact-value-wrap">
          <text class="contact-value">{{ hotline }}</text>
          <text class="contact-action">拨打</text>
        </view>
      </view>
      <view class="contact-row">
        <text class="contact-label">服务时间</text>
        <text class="contact-value">周一至周日 8:00 - 20:00</text>
      </view>
      <view class="contact-row last">
        <text class="contact-label">客服邮箱</text>
        <text class="contact-value">service@dayan.com</text>
      </view>
    </view>

    <view class="tip">如以上内容未能解决您的问题，请拨打客服热线，我们将竭诚为您服务。</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const hotline = '400-888-0000';

const openIdx = ref<number>(-1);

const faqs = ref([
  {
    q: '如何预订旅游短居养老？',
    a: '在「养老网络」中选择心仪的网络与机构，进入机构详情页选择房型与入住日期，确认费用后提交订单，支付完成即预订成功。',
  },
  {
    q: '如何使用我的权益？',
    a: '进入「我的权益」查看可用权益，激活后可在发起服务时选择对应权益项目进行抵扣，旅游短居订单支付时也可选用权益抵扣。',
  },
  {
    q: '订单如何取消与退款？',
    a: '待支付订单可直接在订单列表取消；已支付订单如需取消或退款，请联系客服协助处理，退款将按原支付渠道返回。',
  },
  {
    q: '权益人是什么？',
    a: '权益人指实际使用权益、接受服务的长辈。可在「权益人管理」中添加多位权益人，使用权益时选择对应人员即可。',
  },
  {
    q: '忘记登录密码怎么办？',
    a: '可使用「短信验证码登录」快速登录，或联系所属渠道工作人员协助重置密码。',
  },
  {
    q: '如何修改个人资料？',
    a: '进入「我的 - 查看资料」，点击底部「编辑资料」即可修改姓名、性别、邮箱、头像、地区等信息。',
  },
  {
    q: '入住前需要准备什么？',
    a: '请携带入住人身份证件、近期体检报告（部分地区要求），具体入住材料以机构通知为准。',
  },
]);

function toggle(idx: number) {
  openIdx.value = openIdx.value === idx ? -1 : idx;
}

function callPhone() {
  uni.makePhoneCall({ phoneNumber: hotline.replace(/-/g, ''), fail: () => {} });
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 60rpx;
}

/* hero */
.hero {
  background: $gradient-brand;
  padding: $spacing-xl $spacing-lg;
}
.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
}
.hero-sub {
  display: block;
  margin-top: $spacing-xs;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

.section-title {
  padding: $spacing-lg $spacing-lg $spacing-sm;
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}

/* FAQ */
.faq-card {
  background: $bg-card;
  margin: 0 $spacing-lg $spacing-sm;
  border-radius: $radius-md;
  padding: 0 $spacing-lg;
  box-shadow: $shadow-card;
}
.faq-item {
  border-bottom: 1rpx solid $border-light;

  &.last {
    border-bottom: none;
  }
}
.faq-q {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
}
.q-text {
  flex: 1;
  font-size: 28rpx;
  color: $text-primary;
  font-weight: 500;
}
.q-arrow {
  font-size: 32rpx;
  color: $text-placeholder;
  transition: transform $transition-base;
  transform: rotate(90deg);

  &.open {
    transform: rotate(-90deg);
  }
}
.faq-a {
  padding: 0 0 28rpx;
}
.a-text {
  font-size: 26rpx;
  color: $text-regular;
  line-height: 1.6;
}

/* 联系客服 */
.contact-card {
  background: $bg-card;
  margin: 0 $spacing-lg;
  border-radius: $radius-md;
  padding: 0 $spacing-lg;
  box-shadow: $shadow-card;
}
.contact-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 1rpx solid $border-light;

  &.last {
    border-bottom: none;
  }
}
.contact-label {
  font-size: 28rpx;
  color: $text-secondary;
}
.contact-value-wrap {
  display: flex;
  align-items: center;
}
.contact-value {
  font-size: 28rpx;
  color: $text-primary;
}
.contact-action {
  margin-left: $spacing-sm;
  font-size: 24rpx;
  color: $brand-primary;
}

.tip {
  margin: $spacing-lg $spacing-lg 0;
  padding: $spacing-md;
  font-size: 24rpx;
  color: $text-placeholder;
  line-height: 1.6;
  text-align: center;
}
</style>
