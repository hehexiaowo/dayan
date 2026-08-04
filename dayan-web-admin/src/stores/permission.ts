import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { getMenuTree } from '@/api/menu'
import type { Menu } from '@/types/menu'
import { buildAsyncRoutes } from '@/router/dynamic'

/**
 * 权限 / 菜单 Store。
 *
 * 负责从后端拉取当前端菜单树，并生成动态路由供 router 守卫挂载。
 * 菜单树同时驱动 layout 侧边栏渲染。
 */
export const usePermissionStore = defineStore('permission', () => {
  /** 当前端菜单树（已组装 children） */
  const menus = ref<Menu[]>([])
  /** 由菜单生成的动态路由（挂到布局 / 下） */
  const dynamicRoutes = ref<RouteRecordRaw[]>([])
  /** 是否已完成首次加载 */
  const loaded = ref(false)

  /**
   * 拉取菜单树并生成动态路由。
   *
   * 仅加载一次（loaded 标记），登出时调用 reset 后可重新加载。
   *
   * @param domainType 端类型，Admin 端传 'admin'
   */
  async function loadMenus(domainType: 'admin' | 'channel' | 'agent' | 'client' = 'admin') {
    if (loaded.value) return dynamicRoutes.value
    const tree = await getMenuTree(domainType)
    menus.value = tree
    dynamicRoutes.value = buildAsyncRoutes(tree)
    loaded.value = true
    return dynamicRoutes.value
  }

  /** 重置（登出时调用） */
  function reset() {
    menus.value = []
    dynamicRoutes.value = []
    loaded.value = false
  }

  return {
    menus,
    dynamicRoutes,
    loaded,
    loadMenus,
    reset
  }
})
