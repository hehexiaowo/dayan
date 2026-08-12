<template>
  <view class="persons-page">
    <view v-if="loading && persons.length === 0" class="loading">
      <text>加载中...</text>
    </view>

    <view v-else-if="persons.length === 0" class="empty">
      <text class="empty-icon">👤</text>
      <text class="empty-text">还没有添加权益人</text>
      <text class="empty-sub">权益人即享受养老服务的老人</text>
    </view>

    <view v-else class="person-list">
      <view
        v-for="p in persons"
        :key="p.id"
        class="person-card"
        :class="{ placeholder: isPlaceholder(p) }"
        @click="goEdit(p.id)"
        @longpress="onLongPress(p)"
      >
        <view class="avatar">
          <text class="avatar-text">{{ isPlaceholder(p) ? '?' : (p.usePersonName || '?').charAt(0) }}</text>
        </view>
        <view class="person-info">
          <view class="name-row">
            <text class="name">{{ p.usePersonName }}</text>
            <text v-if="p.isDefaultHolder === 1" class="default-badge">默认</text>
          </view>
          <view class="meta-row">
            <text v-if="p.relationWithHolder" class="meta">{{ p.relationWithHolder }}</text>
            <text v-if="p.usePersonPhone" class="meta">{{ p.usePersonPhone }}</text>
          </view>
        </view>
        <view v-if="isPlaceholder(p)" class="warn-btn" @click.stop="goEdit(p.id)">请补全</view>
        <text v-else class="arrow">></text>
      </view>
    </view>

    <!-- 新增按钮 -->
    <view class="add-bar">
      <button class="btn-add" @click="goAdd">+ 新增使用人</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad, onShow } from '@dcloudio/uni-app';
import { getUsePersons, deleteUsePerson, setDefaultUsePerson } from '@/api/equity';
import type { EquityUsePerson } from '@/types';

const equityCode = ref('');
const persons = ref<EquityUsePerson[]>([]);
const loading = ref(false);

function isPlaceholder(p: EquityUsePerson) {
  return !p.usePersonName || p.usePersonName.startsWith('待填写');
}

async function loadPersons() {
  if (!equityCode.value) return;
  loading.value = true;
  try {
    persons.value = await getUsePersons(equityCode.value);
  } catch (e) {
    persons.value = [];
  } finally {
    loading.value = false;
  }
}

function goAdd() {
  uni.navigateTo({ url: `/pages/equity/use-persons/edit?equityCode=${equityCode.value}` });
}
function goEdit(id: string) {
  uni.navigateTo({ url: `/pages/equity/use-persons/edit?equityCode=${equityCode.value}&id=${id}` });
}

function onLongPress(p: EquityUsePerson) {
  const actions = ['编辑'];
  if (p.isDefaultHolder !== 1) actions.push('设为默认');
  actions.push('删除');
  uni.showActionSheet({
    itemList: actions,
    success: (res) => {
      const action = actions[res.tapIndex];
      if (action === '编辑') {
        goEdit(p.id);
      } else if (action === '设为默认') {
        doSetDefault(p);
      } else if (action === '删除') {
        doDelete(p);
      }
    },
  });
}

async function doSetDefault(p: EquityUsePerson) {
  try {
    await setDefaultUsePerson(p.id);
    uni.showToast({ title: '已设为默认', icon: 'success' });
    loadPersons();
  } catch (e) {
    /* 拦截器已提示 */
  }
}

function doDelete(p: EquityUsePerson) {
  uni.showModal({
    title: '确认删除',
    content: `确定删除权益人「${p.usePersonName}」吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteUsePerson(p.id);
          uni.showToast({ title: '已删除', icon: 'success' });
          loadPersons();
        } catch (e) {
          /* 拦截器已提示 */
        }
      }
    },
  });
}

onLoad((q) => {
  equityCode.value = q?.equityCode || '';
});
onShow(loadPersons);
</script>

<style lang="scss" scoped>
.persons-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 140rpx;
}

.loading,
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
  color: #909399;
}
.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}
.empty-text {
  font-size: 30rpx;
  color: #606266;
}
.empty-sub {
  font-size: 24rpx;
  color: #c0c4cc;
  margin-top: 8rpx;
}

.person-list {
  padding: 20rpx 24rpx;
}
.person-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
  &.placeholder {
    border: 2rpx solid #f56c6c;
    background: #fef0f0;
  }
}
.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: #67C23A;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  .placeholder & {
    background: #f56c6c;
  }
}
.avatar-text {
  font-size: 34rpx;
  font-weight: bold;
}
.person-info {
  flex: 1;
  margin-left: 20rpx;
  overflow: hidden;
}
.name-row {
  display: flex;
  align-items: center;
}
.name {
  font-size: 32rpx;
  font-weight: 500;
  color: #303133;
}
.default-badge {
  font-size: 20rpx;
  color: #67C23A;
  border: 1px solid #67C23A;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  margin-left: 12rpx;
}
.meta-row {
  display: flex;
  margin-top: 8rpx;
}
.meta {
  font-size: 24rpx;
  color: #909399;
  margin-right: 20rpx;
}
.arrow {
  color: #c0c4cc;
  font-size: 32rpx;
}
.warn-btn {
  font-size: 24rpx;
  color: #fff;
  background: #f56c6c;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
}

.add-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 24rpx;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
}
.btn-add {
  background: #67C23A;
  color: #fff;
  font-size: 30rpx;
  border-radius: 12rpx;
  height: 80rpx;
  line-height: 80rpx;
}
</style>
