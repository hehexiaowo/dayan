<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import type { LoginParams } from '@/types/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive<LoginParams>({
  username: '',
  password: ''
})

// 记住我：用户名存 localStorage，下次访问自动回填
const REMEMBER_KEY = 'dayan_channel_remember_user'
const rememberMe = ref(false)
try {
  const saved = localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    loginForm.username = saved
    rememberMe.value = true
  }
} catch {
  /* localStorage 不可用时忽略 */
}

const loginRules: FormRules<LoginParams> = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 100, message: '登录账号长度不能超过 100', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }
  ]
}

// 忘记密码：B 端内部系统走管理员重置流程
function handleForgotPassword() {
  ElMessageBox.alert(
    '请联系平台管理员为您重置密码，重置后可使用新密码登录。',
    '忘记密码',
    { confirmButtonText: '我知道了', type: 'info' }
  )
}

async function handleLogin() {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.login({ ...loginForm })
    // 登录成功后按勾选状态持久化用户名
    try {
      if (rememberMe.value) {
        localStorage.setItem(REMEMBER_KEY, loginForm.username)
      } else {
        localStorage.removeItem(REMEMBER_KEY)
      }
    } catch {
      /* 忽略 */
    }
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (err) {
    void err
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- ===== 左侧品牌展示区 ===== -->
    <div class="brand-panel">
      <!-- 装饰性背景图形 -->
      <div class="brand-deco brand-deco--1" />
      <div class="brand-deco brand-deco--2" />
      <div class="brand-deco brand-deco--3" />

      <div class="brand-content">
        <!-- Logo 区（真实品牌 logo） -->
        <div class="brand-logo">
          <img src="/dayan_logo.png" alt="渠道核心" class="brand-logo__img" />
        </div>

        <!-- 标语 -->
        <div class="brand-slogan">
          <h1 class="brand-slogan__title">渠道核心</h1>
          <p class="brand-slogan__desc">
            渠道分销 · 代理人管理 · 客户跟进 · 订单采购<br />
            合作伙伴专属工作台
          </p>
        </div>

        <!-- 特性列表 -->
        <ul class="brand-features">
          <li>
            <span class="brand-features__dot" />
            <span>渠道 / 子渠道层级管理，权限精细管控</span>
          </li>
          <li>
            <span class="brand-features__dot" />
            <span>商品浏览与权益采购，在线下单即时入库</span>
          </li>
          <li>
            <span class="brand-features__dot" />
            <span>客户档案与代理人业绩，数据一目了然</span>
          </li>
        </ul>
      </div>

      <div class="brand-footer">
        © {{ new Date().getFullYear() }} 渠道核心 · All Rights Reserved
      </div>
    </div>

    <!-- ===== 右侧登录卡 ===== -->
    <div class="form-panel">
      <div class="form-card">
        <div class="form-header">
          <h2 class="form-title">渠道管理后台</h2>
          <p class="form-subtitle">欢迎回来，请输入您的账号信息</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username" label="登录账号">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名 / 手机号 / 邮箱"
              clearable
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="password" label="登录密码">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <el-link type="primary" :underline="false" @click="handleForgotPassword">忘记密码?</el-link>
          </div>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <p class="form-hint">使用渠道分配的账号登录，如需帮助请联系平台管理员</p>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  height: 100%;
  display: flex;
}

/* ===== 左侧品牌展示区 ===== */
.brand-panel {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 64px;
  overflow: hidden;
  background: linear-gradient(160deg, #0d4a4a 0%, #0f766e 55%, #14b8a6 100%);
  color: #fff;

  /* 装饰性浮动圆形 */
  .brand-deco {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;

    &--1 {
      width: 420px;
      height: 420px;
      top: -120px;
      right: -100px;
      background: rgba(255, 255, 255, 0.05);
    }
    &--2 {
      width: 280px;
      height: 280px;
      bottom: 40px;
      right: -60px;
      background: rgba(255, 255, 255, 0.04);
    }
    &--3 {
      width: 160px;
      height: 160px;
      top: 45%;
      left: 55%;
      background: rgba(255, 255, 255, 0.03);
    }
  }
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 460px;
}

.brand-logo {
  margin-bottom: 56px;

  &__img {
    width: 200px;
    height: auto;
    /* 真实 logo 为红色图标 + 白色文字，直接贴深色渐变面板即可清晰可见 */
  }
}

.brand-slogan {
  margin-bottom: 48px;

  &__title {
    margin: 0 0 16px;
    font-size: 34px;
    font-weight: 700;
    line-height: 1.3;
  }

  &__desc {
    margin: 0;
    font-size: 15px;
    line-height: 1.9;
    color: rgba(255, 255, 255, 0.7);
  }
}

.brand-features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;

  li {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.85);
  }
}

.brand-features__dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #5eead4;
  box-shadow: 0 0 0 4px rgba(94, 234, 212, 0.2);
}

.brand-footer {
  position: absolute;
  bottom: 32px;
  left: 64px;
  right: 64px;
  z-index: 1;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

/* ===== 右侧登录卡 ===== */
.form-panel {
  width: 480px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
}

.form-card {
  width: 360px;
  max-width: calc(100% - 48px);
}

.form-header {
  margin-bottom: 36px;

  .form-title {
    margin: 0 0 8px;
    font-size: 26px;
    font-weight: 700;
    color: #1a1a2e;
  }

  .form-subtitle {
    margin: 0;
    font-size: 14px;
    color: #8c8fa6;
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  border: none;
  background: linear-gradient(135deg, #0f766e, #14b8a6);
  box-shadow: 0 4px 16px rgba(20, 184, 166, 0.25);
  transition: transform 0.15s, box-shadow 0.15s;

  &:hover {
    background: linear-gradient(135deg, #0c5d56, #0f9e90);
    box-shadow: 0 6px 20px rgba(20, 184, 166, 0.35);
  }

  &:active {
    transform: translateY(1px);
    box-shadow: 0 2px 8px rgba(20, 184, 166, 0.3);
  }
}

/* 记住我 / 忘记密码 行 */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
}

/* 输入框聚焦态：主题色边框 + 柔和外阴影（覆盖 Element Plus 默认偏淡样式） */
.form-card {
  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #14b8a6 inset, 0 0 0 3px rgba(20, 184, 166, 0.12) !important;
  }
}

.form-hint {
  margin: 8px 0 0;
  text-align: center;
  font-size: 12px;
  color: #b0b3c6;
}

/* ===== 响应式：小屏隐藏左侧品牌区 ===== */
@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
  .form-panel {
    width: 100%;
    background: linear-gradient(160deg, #0d4a4a 0%, #0f766e 55%, #14b8a6 100%);
  }
  .form-card {
    padding: 48px 36px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.18);
  }
}
</style>
