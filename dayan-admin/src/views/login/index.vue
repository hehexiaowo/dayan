<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
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
        <!-- Logo 区 -->
        <div class="brand-logo">
          <svg class="brand-logo__icon" viewBox="0 0 48 48" fill="none">
            <path
              d="M24 4L8 14v12c0 9.5 6.8 17.8 16 20 9.2-2.2 16-10.5 16-20V14L24 4z"
              stroke="white"
              stroke-width="2.5"
              stroke-linejoin="round"
              fill="rgba(255,255,255,0.08)"
            />
            <path
              d="M18 22l4 4 8-8"
              stroke="white"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <span class="brand-logo__text">大雁养老</span>
        </div>

        <!-- 标语 -->
        <div class="brand-slogan">
          <h1 class="brand-slogan__title">智慧养老运营平台</h1>
          <p class="brand-slogan__desc">
            机构管理 · 权益运营 · 管家服务 · 渠道分销<br />
            全链路数字化解决方案
          </p>
        </div>

        <!-- 特性列表 -->
        <ul class="brand-features">
          <li>
            <span class="brand-features__dot" />
            <span>全机构资源统一管理，20+ 机构数据实时掌控</span>
          </li>
          <li>
            <span class="brand-features__dot" />
            <span>权益模板 / 批次 / 仓库三级运营，状态机驱动</span>
          </li>
          <li>
            <span class="brand-features__dot" />
            <span>管家服务全程跟踪，需求 → 方案 → 安排 → 回访闭环</span>
          </li>
        </ul>
      </div>

      <div class="brand-footer">
        © {{ new Date().getFullYear() }} 大雁养老 · All Rights Reserved
      </div>
    </div>

    <!-- ===== 右侧登录卡 ===== -->
    <div class="form-panel">
      <div class="form-card">
        <div class="form-header">
          <h2 class="form-title">运营管理后台</h2>
          <p class="form-subtitle">欢迎回来，请输入您的账号信息</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名 / 手机号 / 邮箱"
              clearable
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

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

        <p class="form-hint">使用平台分配的账号登录，如需帮助请联系系统管理员</p>
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
  background: linear-gradient(160deg, #143a6b 0%, #1e50a2 55%, #2563eb 100%);
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
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 56px;

  &__icon {
    width: 44px;
    height: 44px;
  }

  &__text {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 1px;
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
  background: #60a5fa;
  box-shadow: 0 0 0 4px rgba(96, 165, 250, 0.2);
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
  background: linear-gradient(135deg, #1e50a2, #2563eb);
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.25);

  &:hover {
    background: linear-gradient(135deg, #1a4480, #1d4ed8);
    box-shadow: 0 6px 20px rgba(37, 99, 235, 0.35);
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
    background: linear-gradient(160deg, #143a6b 0%, #1e50a2 55%, #2563eb 100%);
  }
  .form-card {
    padding: 48px 36px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.18);
  }
}
</style>
