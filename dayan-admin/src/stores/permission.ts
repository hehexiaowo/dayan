import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { getMyMenuTree, getMyPermissions } from '@/api/menu'
import type { Menu } from '@/types/menu'
import { buildAsyncRoutes } from '@/router/dynamic'

/**
 * 权限 / 菜单 Store。
 *
 * 负责从后端拉取当前端菜单树，并生成动态路由供 router 守卫挂载。
 * 菜单树同时驱动 layout 侧边栏渲染；permCodes 驱动 v-permission 按钮级指令。
 */
export const usePermissionStore = defineStore('permission', () => {
  /** 当前端菜单树（已组装 children） */
  const menus = ref<Menu[]>([])
  /** 由菜单生成的动态路由（挂到布局 / 下） */
  const dynamicRoutes = ref<RouteRecordRaw[]>([])
  /** 当前账号按钮级权限码集合（超管为 ['*'] 通配） */
  const permCodes = ref<string[]>([])
  /** 是否已完成首次加载 */
  const loaded = ref(false)

  /**
   * 拉取当前账号可见菜单树（RBAC 数据权限）并生成动态路由，
   * 同时拉取按钮级权限码供 v-permission 使用。
   *
   * 仅加载一次（loaded 标记），登出时调用 reset 后可重新加载。
   * 走 /menus/mine：超管返回全部，非超管按角色 organ_role_menu_rel 过滤。
   *
   * @param domainType 端类型，Admin 端传 'admin'
   */
  async function loadMenus(domainType: 'admin' | 'channel' | 'agent' | 'client' = 'admin') {
    if (loaded.value) return dynamicRoutes.value
    const [tree, perms] = await Promise.all([getMyMenuTree(domainType), getMyPermissions()])
    menus.value = tree
    permCodes.value = perms
    dynamicRoutes.value = buildAsyncRoutes(tree)
    loaded.value = true
    return dynamicRoutes.value
  }

  /**
   * 是否拥有指定权限码（v-permission 指令的唯一判定入口）。
   * 超管 '*' 通配放行；支持传入数组（任一命中即放行）。
   */
  function hasPerm(code: string | string[]): boolean {
    const codes = Array.isArray(code) ? code : [code]
    if (permCodes.value.includes('*')) return true
    return codes.some((c) => permCodes.value.includes(c))
  }

  /** 重置（登出时调用） */
  function reset() {
    menus.value = []
    dynamicRoutes.value = []
    permCodes.value = []
    loaded.value = false
  }

  return {
    menus,
    dynamicRoutes,
    permCodes,
    loaded,
    loadMenus,
    hasPerm,
    reset
  }
})
