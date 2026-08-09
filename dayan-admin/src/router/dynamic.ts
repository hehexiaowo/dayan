import type { RouteRecordRaw } from 'vue-router'
import type { Menu } from '@/types/menu'
import { MenuType } from '@/types/menu'

/**
 * 预加载 src/views 下所有页面组件（Vite 编译期静态分析）。
 *
 * 注意：import.meta.glob 的 key 保留了 pattern 的前缀形式，即 '../views/**'（相对
 * 本文件 src/router/dynamic.ts），而非绝对路径 /src/views/...。resolveComponent 的
 * key 拼接必须与此保持一致。
 */
const modules = import.meta.glob('../views/**/*.vue')

/**
 * 根据菜单的 component 字段（如 'basic/account/index'）解析为异步组件工厂。
 *
 * 解析规则：component 值拼接为 `../views/{component}.vue` 在 modules 中查找
 * （与 import.meta.glob 的 key 前缀一致）。找不到时返回 null（对应页面尚未实现），调用方跳过该路由。
 *
 * @param component 菜单 component 字段（相对 src/views 的路径，无 .vue 后缀）
 */
function resolveComponent(component: string): (() => Promise<unknown>) | null {
  // 标准化：去前导斜杠 + 去 .vue 后缀，拼成与 import.meta.glob key 一致的形式
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
 * - menuType=1（目录）：作为布局下的嵌套父路由（含 children），无 component。
 * - menuType=2（菜单）：作为叶子路由，component 由 resolveComponent 解析；解析失败则跳过。
 * - menuType=3（按钮）：不生成路由。
 * - status=0 或 isVisible=0 的菜单跳过。
 * - 外链（isExternal=1）暂按普通路由处理（后续可拓展为外链跳转）。
 *
 * 生成的路由 name 用 menuCode（唯一），meta 携带 title/icon/permissionCode 供布局与守卫使用。
 *
 * @param menus 后端菜单树（已组装 children）
 * @returns 可直接 addRoute 到布局 / 下的子路由数组
 */
export function buildAsyncRoutes(menus: Menu[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  // 扁平化：所有叶子菜单（menuType=2）直接作为顶层路由挂到布局 / 下，
  // 目录（menuType=1）只用于侧边栏分组展示，不生成路由（避免目录 + 绝对路径子路由
  // 的嵌套匹配歧义）。菜单 path 在 seed 里已是绝对路径（如 /resource/content），平铺后
  // Vue Router 直接精确匹配，无嵌套层级问题。
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
