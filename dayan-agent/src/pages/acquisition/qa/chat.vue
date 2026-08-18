<template>
  <view class="chat-page">
    <!-- 顶部人物条 -->
    <view class="chat-header">
      <text class="chat-title">{{ persona?.personaName || 'AI 问答' }}</text>
      <text class="sessions-btn dy-clickable" @click="openSessions">会话</text>
    </view>

    <!-- 消息列表 -->
    <scroll-view scroll-y class="msg-list" :scroll-into-view="scrollInto" :scroll-with-animation="true">
      <!-- 开场白 / 推荐问题（无消息时） -->
      <view v-if="messages.length === 0 && persona">
        <view class="msg assistant">
          <view class="bubble">{{ persona.welcomeMsg || '您好，我是' + persona.personaName + '，有什么可以帮您？' }}</view>
        </view>
        <view v-for="(q, i) in (persona.recommendQuestions || [])" :key="i" class="rec-q dy-clickable" @click="ask(q)">
          <text>{{ q }}</text>
        </view>
      </view>

      <view v-for="(m, i) in messages" :key="m.id" :id="'msg' + m.id" class="msg" :class="m.role">
        <view class="bubble">
          <text class="bubble-text">{{ m.content }}</text>
          <view v-if="m.role === 'assistant' && m.citations?.length" class="cites" @click.stop="toggleCite(i)">
            <text class="cite-toggle">▸ 引用出处（{{ m.citations.length }}条）</text>
            <view v-if="openCite === i" class="cite-list">
              <view v-for="(c, ci) in m.citations" :key="ci" class="cite-item">
                <text class="cite-repo">{{ c.repoName || '知识库' }}</text>
                <text class="cite-text">「{{ c.text.slice(0, 60) }}{{ c.text.length > 60 ? '…' : '' }}」</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 底部输入框 -->
    <view class="input-bar">
      <input
        v-model="question"
        class="msg-input"
        placeholder="输入问题…"
        confirm-type="send"
        @confirm="send"
      />
      <button class="send-btn" :disabled="sending" @click="send">{{ sending ? '…' : '发送' }}</button>
    </view>

    <!-- 会话切换弹层 -->
    <view v-if="showSessions" class="mask" @click="showSessions = false">
      <view class="session-panel" @click.stop>
        <view class="panel-head">
          <text class="panel-title">历史会话</text>
          <text class="new-btn dy-clickable" @click="newSession">＋ 新会话</text>
        </view>
        <scroll-view scroll-y class="session-list">
          <view
            v-for="s in sessions"
            :key="s.sessionCode"
            class="session-item"
            :class="{ active: s.sessionCode === sessionCode }"
            @click="switchSession(s)"
          >
            <text class="s-title dy-ellipsis">{{ s.title || s.personaName + ' 会话' }}</text>
            <text class="s-del dy-clickable" @click.stop="removeSession(s.sessionCode)">删除</text>
          </view>
          <view v-if="sessions.length === 0" class="session-empty">暂无历史会话</view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import {
  getQaConfigs,
  getQaSessions,
  createQaSession,
  deleteQaSession,
  getQaMessages,
  chatQa,
} from '@/api/toolQa';
import type { QaConfig, QaSession, QaMessage } from '@/types';

const configId = ref(0);
const persona = ref<QaConfig | null>(null);
const sessionCode = ref('');
const messages = ref<QaMessage[]>([]);
const sessions = ref<QaSession[]>([]);
const question = ref('');
const scrollInto = ref('');
const openCite = ref(-1);
const showSessions = ref(false);
const loading = ref(false);
const sending = ref(false);

// 用于消息 id（新增临时消息无后端 id 时）
let tmpId = -1;

onLoad(async (opts) => {
  configId.value = Number(opts?.configId || 0);
  if (configId.value) {
    await loadPersona();
    await loadSessions();
    // 若存在上一个会话，默认进入
    if (sessions.value.length > 0) {
      await enterSession(sessions.value[0]);
    }
  }
});

async function loadPersona() {
  try {
    const list = await getQaConfigs();
    persona.value = list.find((c) => c.id === configId.value) || null;
  } catch {
    persona.value = null;
  }
}

async function loadMessagesFromApi() {
  if (!sessionCode.value) {
    messages.value = [];
    return;
  }
  try {
    const list = await getQaMessages(sessionCode.value);
    messages.value = list;
  } catch {
    messages.value = [];
  }
}

async function loadSessions() {
  if (!configId.value) return;
  try {
    sessions.value = await getQaSessions(configId.value);
  } catch {
    sessions.value = [];
  }
}

function openSessions() {
  loadSessions();
  showSessions.value = true;
}

/** 新建会话：仅创建本地空会话（首次发言时才真正建后端会话） */
async function newSession() {
  sessionCode.value = '';
  messages.value = [];
  openCite.value = -1;
  showSessions.value = false;
}

/** 切换到指定会话，拉取其历史消息 */
async function switchSession(s: QaSession) {
  sessionCode.value = s.sessionCode;
  showSessions.value = false;
  openCite.value = -1;
  await loadMessagesFromApi();
}

async function enterSession(s: QaSession) {
  sessionCode.value = s.sessionCode;
  await loadMessagesFromApi();
}

async function removeSession(code: string) {
  uni.showModal({
    title: '删除会话',
    content: '删除后不可恢复，确定删除该会话？',
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await deleteQaSession(code);
        sessions.value = sessions.value.filter((s) => s.sessionCode !== code);
        if (sessionCode.value === code) {
          sessionCode.value = '';
          messages.value = [];
        }
        uni.showToast({ title: '已删除', icon: 'none' });
      } catch {
        // request 层已提示
      }
    },
  });
}

function toggleCite(i: number) {
  openCite.value = openCite.value === i ? -1 : i;
}

/** 推荐问题点击：填入输入框直接发送 */
function ask(q: string) {
  question.value = q;
  send();
}

async function send() {
  const q = question.value.trim();
  if (!q || sending.value || !configId.value) return;
  if (!persona.value) return;

  question.value = '';
  sending.value = true;

  // 立即回显用户消息
  pushLocal('user', q);

  try {
    // 首次发送时新建后端会话
    if (!sessionCode.value) {
      sessionCode.value = await createQaSession(configId.value);
    }
    const res = await chatQa({ configId: configId.value, sessionCode: sessionCode.value, question: q });
    messages.value.push({
      id: tmpId--,
      sessionCode: sessionCode.value,
      role: 'assistant',
      content: res.answer,
      citations: res.citations || [],
    });
    scrollToBottom();
    // 发言后刷新会话列表（标题可能变化）
    loadSessions();
  } catch {
    // request 层已 toast
  } finally {
    sending.value = false;
  }
}

/** 本地临时消息（user 无后端 id） */
function pushLocal(role: 'user' | 'assistant', content: string) {
  messages.value.push({
    id: tmpId--,
    sessionCode: sessionCode.value,
    role,
    content,
    citations: [],
  });
  scrollToBottom();
}

function scrollToBottom() {
  // 滚到最后一条（用最后一条 id 作锚点）
  const last = messages.value[messages.value.length - 1];
  if (last) {
    scrollInto.value = 'msg' + last.id;
  }
}
</script>

<style lang="scss" scoped>

.chat-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: $bg-page;
}

/* 顶部人物条 */
.chat-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: $gradient-blue;
  padding: $spacing-sm $spacing-lg;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chat-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #fff;
}
.sessions-btn {
  font-size: 26rpx;
  color: #fff;
  padding: 6rpx 24rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: $radius-md;
}

/* 消息列表 */
.msg-list {
  flex: 1;
  height: calc(100vh - 88rpx - 120rpx);
  padding: $spacing-md;
  box-sizing: border-box;
}

/* 消息气泡 */
.msg {
  display: flex;
  margin-bottom: $spacing-md;
}
.msg.user {
  justify-content: flex-end;
}
.msg.assistant .bubble {
  background: $bg-card;
  border: 1rpx solid $border-light;
  border-top-left-radius: 4rpx;
}
.msg.user .bubble {
  background: $brand-success;
  color: #fff;
  border-top-right-radius: 4rpx;
}
.bubble {
  max-width: 75%;
  padding: $spacing-md $spacing-lg;
  border-radius: $radius-lg;
  font-size: 28rpx;
  line-height: 1.6;
  word-break: break-word;
}
.bubble-text {
  color: inherit;
  white-space: pre-wrap;
}

/* 推荐问题 */
.rec-q {
  display: inline-block;
  max-width: 80%;
  padding: $spacing-sm $spacing-lg;
  background: $brand-primary-light;
  color: $brand-primary;
  border-radius: $radius-md;
  font-size: 26rpx;
  margin: 0 0 $spacing-sm $spacing-lg;
}

/* 引用折叠 */
.cites {
  margin-top: $spacing-xs;
  padding-top: $spacing-xs;
  border-top: 1rpx solid $border-light;
}
.msg.assistant .cite-toggle {
  color: $brand-primary;
  font-size: 24rpx;
}
.msg.user .cite-toggle {
  color: rgba(255, 255, 255, 0.9);
  font-size: 24rpx;
}
.cite-list {
  margin-top: $spacing-xs;
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
}
.cite-item {
  display: flex;
  flex-direction: column;
  background: $brand-info-light;
  border-radius: $radius-sm;
  padding: $spacing-xs $spacing-sm;
}
.msg.assistant .cite-item {
  background: $brand-info-light;
}
.msg.user .cite-item {
  background: rgba(255, 255, 255, 0.2);
}
.cite-repo {
  font-size: 22rpx;
  color: $brand-primary;
  font-weight: 600;
}
.msg.user .cite-repo {
  color: #fff;
}
.cite-text {
  font-size: 22rpx;
  color: $text-regular;
}
.msg.user .cite-text {
  color: rgba(255, 255, 255, 0.85);
}

/* 输入栏 */
.input-bar {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-lg;
  background: $bg-card;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.msg-input {
  flex: 1;
  height: $control-height-sm;
  padding: 0 $spacing-md;
  border: 2rpx solid $border-base;
  border-radius: $radius-md;
  font-size: 28rpx;
  color: $text-primary;
  background: $bg-page;
  box-sizing: border-box;
}
.send-btn {
  height: $control-height-sm;
  padding: 0 $spacing-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $brand-success;
  color: #fff;
  font-size: 28rpx;
  border-radius: $radius-md;
  border: none;
  line-height: 1;
}
.send-btn[disabled] {
  opacity: 0.6;
}

/* 会话弹层 */
.mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: $bg-mask;
  z-index: 999;
  display: flex;
  justify-content: flex-end;
}
.session-panel {
  width: 60%;
  height: 100%;
  background: $bg-card;
  display: flex;
  flex-direction: column;
  animation: slide-in 200ms ease;
}
@keyframes slide-in {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1rpx solid $border-light;
}
.panel-title {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-primary;
}
.new-btn {
  font-size: 26rpx;
  color: $brand-primary;
}
.session-list {
  flex: 1;
}
.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1rpx solid $border-light;
}
.session-item.active {
  background: $brand-primary-light;
}
.s-title {
  flex: 1;
  font-size: 28rpx;
  color: $text-primary;
  margin-right: $spacing-sm;
}
.session-item.active .s-title {
  color: $brand-primary;
  font-weight: 600;
}
.s-del {
  font-size: 24rpx;
  color: $brand-error;
}
.session-empty {
  padding: $spacing-lg;
  text-align: center;
  font-size: 26rpx;
  color: $text-secondary;
}
</style>