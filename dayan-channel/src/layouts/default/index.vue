<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { getChannelInfoCurrent } from '@/api/channel-sub'
import { formatFileUrl } from '@/utils/file'
import SidebarItem from '@/components/SidebarItem.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const isCollapse = ref(false)

const userName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.accountCode || '渠道用户')
const avatarUrl = computed(() => formatFileUrl(userStore.userInfo?.avatar))

/**
 * 侧边栏顶部标题：显示本渠道简称。
 * 后端 /channel-infos/current 返回 shortName；取不到时回退固定文案。
 * layout 常驻只挂载一次，此处取一次即可，无需进 store。
 */
const channelTitle = ref('渠道核心')
onMounted(async () => {
  try {
    const current = await getChannelInfoCurrent()
    channelTitle.value = current?.shortName ? `${current.shortName}渠道核心` : '渠道核心'
  } catch (err) {
    // 接口未实现或鉴权失败时降级：保持默认文案
    console.warn('[layout] 获取当前渠道简称失败:', err)
  }
})

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
  // 清空动态菜单与路由标记，确保换号登录后守卫重新拉取菜单（loaded 重置）。
  permissionStore.reset()
  router.replace('/login')
}
</script>

<template>
  <el-container class="layout-root">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '200px'" class="layout-aside">
      <div class="logo">
        <span v-if="!isCollapse" class="logo-text">{{ channelTitle }}</span>
        <span v-else class="logo-text">渠道</span>
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
