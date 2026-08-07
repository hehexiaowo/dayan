import type { RouteRecordRaw } from 'vue-router'
import type { Menu } from '@/types/menu'
import { MenuType } from '@/types/menu'

/**
 * 预加载 src/views 下所有页面组件（Vite 编译期静态分析）。
 *
 * 从 dayan-admin 原样复制。import.meta.glob 的 key 保留 pattern 前缀形式
 * '../views/**'（相对本文件 src/router/dynamic.ts），在 channel 工程下自动解析
 * channel 的 views 目录，无需改动。
 *
 * 注意：resolveComponent 的 key 拼接必须与此前缀一致。
 */
const modules = import.meta.glob('../views/**/*.vue')

/**
 * 根据菜单的 component 字段（如 'dashboard/index'）解析为异步组件工厂。
 *
 * 解析规则：component 值拼接为 `../views/{component}.vue` 在 modules 中查找。
 * 找不到时返回 null（对应页面尚未实现），调用方跳过该路由。
 *
 * @param component 菜单 component 字段（相对 src/views 的路径，无 .vue 后缀）
 */
function resolveComponent(component: string): (() => Promise<unknown>) | null {
  const normalized = component.replace(/^\/+/, '').replace(/\.vue$/, '')
  const key = `../views/${normalized}.vue`
  if (key in modules) {
    return modules[key] as () => Promise<unknown>
  }
  return null
}

/**
 * 将后端菜单树转换为 Vue Router 动态路由记录。
 *
 * 规则：
 * - menuType=1（目录）：侧边栏分组展示用，递归进入 children；
 * - menuType=2（菜单）：作为叶子路由，component 由 resolveComponent 解析；解析失败则跳过；
 * - menuType=3（按钮）：不生成路由；
 * - status=0 或 isVisible=0 的菜单跳过。
 *
 * 所有叶子菜单扁平挂到布局 / 下（菜单 path 已是绝对路径）。
 *
 * @param menus 后端菜单树（已组装 children）
 * @returns 可直接 addRoute 到布局 / 下的子路由数组
 */
export function buildAsyncRoutes(menus: Menu[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  collectLeafRoutes(menus, routes)
  return routes
}

/**
 * 递归收集所有叶子菜单为扁平路由。
 * - 目录：跳过自身，递归进入 children；
 * - 菜单：解析组件，成功则 push 一条顶层路由；
 * - 按钮：忽略。
 */
function collectLeafRoutes(menus: Menu[], routes: RouteRecordRaw[]) {
  for (const menu of menus) {
    if (menu.status === 0 || menu.isVisible === 0) continue
    if (menu.menuType === MenuType.BUTTON) continue

    if (menu.menuType === MenuType.DIRECTORY) {
      if (menu.children && menu.children.length > 0) {
        collectLeafRoutes(menu.children, routes)
      }
      continue
    }

    // 叶子菜单
    if (!menu.component) continue
    const comp = resolveComponent(menu.component)
    if (!comp) continue
    routes.push({
      path: menu.path ?? menu.menuCode,
      name: menu.menuCode,
      component: comp,
      meta: {
        title: menu.menuName,
        icon: menu.icon ?? undefined,
        permissionCode: menu.permissionCode ?? undefined,
        menuCode: menu.menuCode
      }
    })
  }
}
