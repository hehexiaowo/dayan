<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import SidebarItem from '@/components/SidebarItem.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const isCollapse = ref(false)

const userName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.accountCode || '渠道用户')
const avatarUrl = computed(() => userStore.userInfo?.avatar || '')

// 顶部/侧边标题
const systemTitle = '大雁养老渠道后台'

/** 动态菜单树（来自后端，permissionStore 加载后填充） */
const menuTree = computed(() => permissionStore.menus)

function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出登录',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await userStore.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="layout-root">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <span v-if="!isCollapse" class="logo-text">{{ systemTitle }}</span>
        <span v-else class="logo-text">大雁</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        unique-opened
        router
        background-color="#0c2d57"
        text-color="#c9d1d9"
        active-text-color="#ffffff"
      >
        <sidebar-item
          v-for="menu in menuTree"
          :key="menu.menuCode"
          :item="menu"
        />
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <span class="page-title">{{ route.meta.title }}</span>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="32" :src="avatarUrl">
                {{ userName.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ userName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.layout-root {
  height: 100%;
}

.layout-aside {
  background-color: #0c2d57;
  transition: width 0.28s ease;
  overflow: hidden;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ffffff;
    font-weight: 600;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);

    .logo-text {
      white-space: nowrap;
      letter-spacing: 1px;
    }
  }

  :deep(.el-menu) {
    border-right: none;
  }
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border-bottom: 1px solid #e6e8eb;
  padding: 0 20px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: #595959;

      &:hover {
        color: #409eff;
      }
    }

    .page-title {
      font-size: 16px;
      font-weight: 500;
      color: #1f2329;
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      outline: none;

      .user-name {
        font-size: 14px;
        color: #1f2329;
      }
    }
  }
}

.layout-main {
  background-color: #f0f2f5;
  padding: 16px;
}
</style>
