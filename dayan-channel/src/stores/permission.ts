import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { getMyMenuTree } from '@/api/menu'
import type { Menu } from '@/types/menu'
import { buildAsyncRoutes } from '@/router/dynamic'

/**
 * 权限 / 菜单 Store。
 *
 * 负责从后端拉取当前端菜单树，并生成动态路由供 router 守卫挂载。
 * 菜单树同时驱动 layout 侧边栏渲染。
 *
 * 从 dayan-admin 复制（P8 已建立），默认 domainType 改为 'channel'。
 */

/**
 * 递归过滤菜单树：剔除 status=0 或 isVisible=0 的节点（含子节点）。
 *
 * 与 router/dynamic.ts 的 collectLeafRoutes 过滤规则一致，确保侧边栏
 * 与路由注册使用同一套可见性规则（隐藏菜单既不注册路由也不显示在导航中）。
 */
function filterVisibleMenus(menus: Menu[]): Menu[] {
  return menus
    .filter((m) => m.status !== 0 && m.isVisible !== 0)
    .map((m) => (m.children && m.children.length > 0 ? { ...m, children: filterVisibleMenus(m.children) } : m))
}

export const usePermissionStore = defineStore('permission', () => {
  /** 当前端菜单树（已组装 children，已过滤隐藏项） */
  const menus = ref<Menu[]>([])
  /** 由菜单生成的动态路由（挂到布局 / 下） */
  const dynamicRoutes = ref<RouteRecordRaw[]>([])
  /** 是否已完成首次加载 */
  const loaded = ref(false)

  /**
   * 拉取当前端菜单树并生成动态路由。
   *
   * 仅加载一次（loaded 标记），登出时调用 reset 后可重新加载。
   * 后端 /channel-api/menus/mine 写死 domainType=channel，无需前端传参。
   * 侧边栏 menus 在赋值前过滤 status=0 / isVisible=0（隐藏菜单不显示）。
   */
  async function loadMenus() {
    if (loaded.value) return dynamicRoutes.value
    const tree = await getMyMenuTree()
    menus.value = filterVisibleMenus(tree)
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
