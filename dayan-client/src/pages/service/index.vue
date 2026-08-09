<template>
  <view class="service">
    <!-- 顶部发起服务入口 -->
    <view class="action-bar">
      <view class="action-info">
        <text class="action-title">需要帮助？</text>
        <text class="action-desc">选择权益发起专属服务</text>
      </view>
      <button class="start-btn" @click="onStartService">发起服务</button>
    </view>

    <!-- 状态筛选 -->
    <scroll-view scroll-x class="filter-bar" :show-scrollbar="false">
      <view
        v-for="f in filters"
        :key="String(f.value)"
        class="filter-item"
        :class="{ active: query.sessionStatus === f.value }"
        @click="selectFilter(f.value)"
      >{{ f.label }}</view>
    </scroll-view>

    <!-- 服务会话列表 -->
    <view class="list">
      <view v-if="list.length" class="cards">
        <view class="card" v-for="s in list" :key="s.sessionCode" @click="onTapSession(s)">
          <view class="card-head">
            <text class="title">{{ s.serviceTitle || s.title || '服务会话' }}</text>
            <text class="status" :class="statusClass(s.sessionStatus)">{{ statusText(s.sessionStatus) }}</text>
          </view>
          <view class="card-row">
            <text class="label">管家</text>
            <text class="value">{{ s.butlerFullName || s.butlerName || '未分配' }}</text>
          </view>
          <view v-if="s.subStatus" class="card-row">
            <text class="label">进度</text>
            <text class="value">{{ subStatusText(s.subStatus) }}</text>
          </view>
          <view class="card-foot">
            <text class="time">{{ formatTime(s.createdAt) }}</text>
            <text class="code">单号 {{ s.sessionCode }}</text>
          </view>
        </view>
      </view>

      <view v-else class="empty">
        <text class="empty-text">{{ loading ? '加载中...' : '暂无服务会话' }}</text>
      </view>
    </view>

    <!-- 发起服务弹窗 -->
    <view v-if="showForm" class="modal-mask" @click="showForm = false">
      <view class="modal" @click.stop>
        <view class="modal-title">发起服务请求</view>

        <!-- Step 1: 选权益 -->
        <view class="form-group">
          <text class="form-label">选择权益</text>
          <view class="picker" @click="showEquityPicker = true">
            <text :class="{ 'picker-placeholder': !selectedEquity }">
              {{ selectedEquity ? formatEquityLabel(selectedEquity) : '请选择权益' }}
            </text>
          </view>
        </view>

        <!-- Step 2: 选服务项目（选权益后加载） -->
        <view v-if="serviceItems.length" class="form-group">
          <text class="form-label">服务项目</text>
          <view class="item-list">
            <view
              v-for="item in serviceItems"
              :key="item.itemCode"
              class="item-card"
              :class="{ selected: selectedItemId === item.itemCode, disabled: item.remaining <= 0 }"
              @click="selectItem(item)"
            >
              <view class="item-name">{{ item.itemName }}</view>
              <view class="item-quota">
                <text class="quota-text">
                  剩余 {{ item.remaining }}/{{ item.quantity }}{{ item.quotaType === 2 ? '次/年' : '次' }}
                </text>
              </view>
              <view v-if="item.remaining <= 0" class="item-badge">已用尽</view>
            </view>
          </view>
        </view>

        <!-- Step 3: 选权益人（选权益后加载） -->
        <view v-if="usePersons.length" class="form-group">
          <text class="form-label">权益人</text>
          <view class="item-list">
            <view
              v-for="person in usePersons"
              :key="person.id"
              class="person-card"
              :class="{ selected: selectedPersonId === String(person.id) }"
              @click="selectedPersonId = String(person.id)"
            >
              <text class="person-name">{{ person.usePersonName }}</text>
              <text class="person-relation">{{ person.relationWithHolder || '—' }}</text>
            </view>
          </view>
        </view>

        <!-- Step 4: 需求描述 -->
        <view class="form-group">
          <text class="form-label">需求描述（选填）</text>
          <textarea
            v-model="demandDesc"
            class="textarea"
            placeholder="简单描述您的需求，如期望时间、特殊照护要求等"
            :maxlength="500"
          />
        </view>

        <view class="modal-actions">
          <button class="btn-cancel" @click="showForm = false">取消</button>
          <button class="btn-submit" :disabled="submitting" @click="submitRequest">
            {{ submitting ? '提交中...' : '提交请求' }}
          </button>
        </view>
      </view>
    </view>

    <!-- 权益选择弹窗 -->
    <view v-if="showEquityPicker" class="modal-mask" @click="showEquityPicker = false">
      <view class="modal modal-sm" @click.stop>
        <view class="modal-title">选择权益</view>
        <scroll-view scroll-y class="equity-scroll">
          <view v-if="equities.length === 0" class="empty">暂无可用权益</view>
          <view
            v-for="eq in equities"
            :key="eq.equityCode"
            class="equity-option"
            @click="selectEquity(eq)"
          >
            <view class="equity-name">{{ eq.goodsName || eq.equityName || eq.equityCode }}</view>
            <view class="equity-meta">
              <text class="equity-status" :class="equityStatusClass(eq.equityStatus)">
                {{ equityStatusText(eq.equityStatus) }}
              </text>
              <text class="equity-code">{{ eq.equityCode }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { onPullDownRefresh } from '@dcloudio/uni-app';
import { getServices, type ServiceQuery } from '@/api/service';
import { getEquities, getUsePersons, getServiceItems, createServiceRequest } from '@/api/equity';
import type { ServiceSession, ServiceSessionStatus, Equity, EquityUsePerson, ClientServiceItem } from '@/types';

// ===== 列表 =====
const query = ref<ServiceQuery>({ page: 1, size: 20 });
const list = ref<ServiceSession[]>([]);
const loading = ref(false);

const filters = [
  { label: '全部', value: undefined as number | undefined },
  { label: '待分配', value: 1 },
  { label: '方案中', value: 3 },
  { label: '安排中', value: 4 },
  { label: '服务中', value: 5 },
  { label: '已完成', value: 6 },
  { label: '已取消', value: 7 },
];

function selectFilter(v?: number) {
  query.value.sessionStatus = v;
  loadData();
}

async function loadData() {
  loading.value = true;
  try {
    const res = await getServices(query.value);
    list.value = res?.list || [];
  } catch {
    list.value = [];
  } finally {
    loading.value = false;
  }
}

function onTapSession(s: ServiceSession) {
  uni.showToast({ title: `会话 ${s.sessionCode}（详情待开放）`, icon: 'none' });
}

// ===== 发起服务表单 =====
const showForm = ref(false);
const showEquityPicker = ref(false);
const equities = ref<Equity[]>([]);
const selectedEquity = ref<Equity | null>(null);
const serviceItems = ref<ClientServiceItem[]>([]);
const usePersons = ref<EquityUsePerson[]>([]);
const selectedItemId = ref('');
const selectedPersonId = ref('');
const demandDesc = ref('');
const submitting = ref(false);

async function onStartService() {
  showForm.value = true;
  selectedEquity.value = null;
  serviceItems.value = [];
  usePersons.value = [];
  selectedItemId.value = '';
  selectedPersonId.value = '';
  demandDesc.value = '';
  // 加载已激活/使用中的权益列表
  try {
    const res = await getEquities({ page: 1, size: 50 });
    // 过滤出可用权益（已激活=2 或 使用中=3）
    equities.value = (res?.list || []).filter(
      e => e.equityStatus === 2 || e.equityStatus === 3
    );
  } catch {
    equities.value = [];
  }
}

async function selectEquity(eq: Equity) {
  selectedEquity.value = eq;
  showEquityPicker.value = false;
  selectedItemId.value = '';
  selectedPersonId.value = '';
  // 并行加载服务项目 + 使用人
  try {
    const [items, persons] = await Promise.all([
      getServiceItems(eq.equityCode),
      getUsePersons(eq.equityCode),
    ]);
    serviceItems.value = items || [];
    usePersons.value = persons || [];
    // 默认选第一个可用项目和人
    const firstAvailable = serviceItems.value.find(i => i.remaining > 0);
    if (firstAvailable) selectedItemId.value = firstAvailable.itemCode;
    if (usePersons.value.length) selectedPersonId.value = String(usePersons.value[0].id);
  } catch {
    serviceItems.value = [];
    usePersons.value = [];
  }
}

function selectItem(item: ClientServiceItem) {
  if (item.remaining <= 0) {
    uni.showToast({ title: '该服务项目配额已用尽', icon: 'none' });
    return;
  }
  selectedItemId.value = item.itemCode;
}

async function submitRequest() {
  if (!selectedEquity.value) {
    uni.showToast({ title: '请选择权益', icon: 'none' });
    return;
  }
  if (!selectedItemId.value) {
    uni.showToast({ title: '请选择服务项目', icon: 'none' });
    return;
  }
  if (!selectedPersonId.value) {
    uni.showToast({ title: '请选择权益人', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    const sessionCode = await createServiceRequest({
      equityCode: selectedEquity.value.equityCode,
      itemCode: selectedItemId.value,
      usePersonId: selectedPersonId.value,
      demandDesc: demandDesc.value || undefined,
    });
    uni.showToast({ title: '服务请求已提交', icon: 'success' });
    showForm.value = false;
    await loadData(); // 刷新列表
    // 短暂延迟后提示
    setTimeout(() => {
      uni.showToast({ title: `已创建服务单 ${sessionCode}`, icon: 'none' });
    }, 1500);
  } catch {
    // request 封装已处理错误提示
  } finally {
    submitting.value = false;
  }
}

// ===== 工具函数 =====
function statusText(st: ServiceSessionStatus): string {
  const map: Record<number, string> = {
    1: '待分配', 2: '待收集', 3: '方案中', 4: '安排中',
    5: '服务中', 6: '已完成', 7: '已取消',
  };
  return map[st] || '未知';
}

function statusClass(st: ServiceSessionStatus): string {
  if (st === 5) return 'st-urgent';   // 服务中
  if (st === 6) return 'st-done';     // 完成
  if (st === 7) return 'st-cancel';   // 取消
  return 'st-normal';
}

function subStatusText(sub: string): string {
  const map: Record<string, string> = {
    normal: '正常', hold: '暂停', urgent: '紧急',
    reassign: '改派中', refund_review: '退款审核中',
    refund_done: '退款完成', interrupted: '已中断',
  };
  return map[sub] || sub;
}

function formatTime(t?: number | string): string {
  if (!t) return '';
  const d = new Date(t);
  if (isNaN(d.getTime())) return String(t);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function formatEquityLabel(eq: Equity): string {
  return eq.goodsName || eq.equityName || eq.equityCode;
}

function equityStatusText(st: number): string {
  const map: Record<number, string> = {
    2: '已激活', 3: '使用中', 4: '已完成', 5: '已过期',
  };
  return map[st] || '未知';
}

function equityStatusClass(st: number): string {
  if (st === 3) return 'eq-active';
  if (st === 4) return 'eq-done';
  return 'eq-normal';
}

onPullDownRefresh(() => {
  loadData().finally(() => uni.stopPullDownRefresh());
});

onMounted(loadData);
</script>

<style lang="scss" scoped>
.service {
  min-height: 100vh;
  background: #f5f6f8;
}

/* 发起服务入口 */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #67C23A 0%, #4eaf2a 100%);
  padding: 30rpx;
}
.action-info { display: flex; flex-direction: column; }
.action-title { color: #fff; font-size: 32rpx; font-weight: bold; }
.action-desc { color: rgba(255,255,255,0.85); font-size: 24rpx; margin-top: 6rpx; }
.start-btn {
  background: #fff; color: #67C23A; font-size: 26rpx; font-weight: bold;
  border-radius: 32rpx; padding: 0 36rpx; height: 64rpx; line-height: 64rpx; margin: 0;
}

/* 筛选条 */
.filter-bar { white-space: nowrap; background: #fff; padding: 20rpx 24rpx; border-bottom: 1px solid #f0f0f0; }
.filter-item {
  display: inline-block; padding: 10rpx 28rpx; margin-right: 16rpx;
  border-radius: 28rpx; background: #f5f6f8; font-size: 24rpx; color: #606266;
  &.active { background: #67C23A; color: #fff; }
}

/* 列表 */
.list { padding: 20rpx 24rpx; }
.cards { display: flex; flex-direction: column; gap: 20rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 24rpx; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 30rpx; font-weight: bold; color: #303133; flex: 1; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; margin-left: 16rpx; flex-shrink: 0; }
.st-normal { color: #e6a23c; background: #fdf6ec; }
.st-urgent { color: #67C23A; background: #f0f9eb; }
.st-done { color: #909399; background: #f4f4f5; }
.st-cancel { color: #f56c6c; background: #fef0f0; }
.card-row { display: flex; margin-top: 14rpx; }
.label { font-size: 26rpx; color: #909399; width: 100rpx; }
.value { font-size: 26rpx; color: #303133; flex: 1; }
.card-foot { display: flex; justify-content: space-between; margin-top: 16rpx; padding-top: 16rpx; border-top: 1px solid #f5f5f5; }
.time, .code { font-size: 22rpx; color: #c0c4cc; }

.empty { background: #fff; border-radius: 16rpx; padding: 100rpx 0; text-align: center; }
.empty-text { color: #909399; font-size: 26rpx; }

/* 弹窗 */
.modal-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); z-index: 999;
  display: flex; align-items: center; justify-content: center;
}
.modal {
  background: #fff; border-radius: 20rpx; width: 90%; max-height: 80vh;
  overflow-y: auto; padding: 30rpx;
}
.modal-sm { width: 80%; max-height: 60vh; }
.modal-title { font-size: 32rpx; font-weight: bold; color: #303133; margin-bottom: 24rpx; text-align: center; }

.form-group { margin-bottom: 24rpx; }
.form-label { font-size: 26rpx; color: #606266; display: block; margin-bottom: 10rpx; font-weight: 600; }
.picker {
  background: #f5f6f8; border-radius: 12rpx; padding: 20rpx; font-size: 28rpx;
}
.picker-placeholder { color: #c0c4cc; }

.item-list { display: flex; flex-direction: column; gap: 12rpx; }
.item-card {
  background: #f5f6f8; border-radius: 12rpx; padding: 20rpx;
  border: 2rpx solid transparent;
  &.selected { border-color: #67C23A; background: #f0f9eb; }
  &.disabled { opacity: 0.5; }
}
.item-name { font-size: 28rpx; color: #303133; font-weight: 600; }
.item-quota { margin-top: 6rpx; }
.quota-text { font-size: 24rpx; color: #909399; }
.item-badge { display: inline-block; font-size: 22rpx; color: #f56c6c; margin-top: 6rpx; }

.person-card {
  display: flex; align-items: center; justify-content: space-between;
  background: #f5f6f8; border-radius: 12rpx; padding: 20rpx;
  border: 2rpx solid transparent;
  &.selected { border-color: #67C23A; background: #f0f9eb; }
}
.person-name { font-size: 28rpx; color: #303133; }
.person-relation { font-size: 24rpx; color: #909399; }

.textarea {
  width: 100%; background: #f5f6f8; border-radius: 12rpx; padding: 20rpx;
  font-size: 26rpx; min-height: 120rpx; box-sizing: border-box;
}

.modal-actions { display: flex; gap: 20rpx; margin-top: 30rpx; }
.btn-cancel { flex: 1; background: #f5f6f8; color: #606266; font-size: 28rpx; border-radius: 12rpx; margin: 0; }
.btn-submit { flex: 1; background: #67C23A; color: #fff; font-size: 28rpx; border-radius: 12rpx; margin: 0; }
.btn-submit[disabled] { opacity: 0.6; }

/* 权益选择列表 */
.equity-scroll { max-height: 600rpx; }
.equity-option {
  padding: 24rpx; border-bottom: 1px solid #f5f5f5;
  &:active { background: #f9f9f9; }
}
.equity-name { font-size: 28rpx; color: #303133; font-weight: 600; }
.equity-meta { display: flex; align-items: center; gap: 16rpx; margin-top: 8rpx; }
.equity-status { font-size: 22rpx; padding: 2rpx 12rpx; border-radius: 16rpx; }
.eq-active { color: #67C23A; background: #f0f9eb; }
.eq-done { color: #909399; background: #f4f4f5; }
.eq-normal { color: #e6a23c; background: #fdf6ec; }
.equity-code { font-size: 22rpx; color: #c0c4cc; }
</style>
