import type { RouteRecordRaw } from 'vue-router'
import type { Menu } from '@/types/menu'
import { MenuType } from '@/types/menu'

/**
 * 预加载 src/views 下所有页面组件（Vite 编译期静态分析）。
 *
 * key 形如 '/src/views/system/user/index.vue'，用于动态路由的组件解析。
 * eager:false → 懒加载（按需 chunk）；此处用懒加载以匹配 `() => import()` 语义。
 */
const modules = import.meta.glob('../views/**/*.vue')

/**
 * 根据菜单的 component 字段（如 'system/user/index'）解析为异步组件工厂。
 *
 * 解析规则：component 值拼接为 `/src/views/{component}.vue` 在 modules 中查找。
 * 找不到时返回 null（对应页面尚未实现），调用方跳过该路由。
 *
 * @param component 菜单 component 字段（相对 src/views 的路径，无 .vue 后缀）
 */
function resolveComponent(component: string): (() => Promise<unknown>) | null {
  // 标准化：去前导斜杠，拼 .vue
  const normalized = component.replace(/^\/+/, '')
  const key = `/src/views/${normalized}.vue`
  if (key in modules) {
    return modules[key] as () => Promise<unknown>
  }
  // 兜底：尝试 component 已含 .vue
  const key2 = `/src/views/${normalized.replace(/\.vue$/, '')}.vue`
  if (key2 in modules) {
    return modules[key2] as () => Promise<unknown>
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
  for (const menu of menus) {
    // 禁用或隐藏的菜单不生成路由
    if (menu.status === 0 || menu.isVisible === 0) continue
    // 按钮不生成路由
    if (menu.menuType === MenuType.BUTTON) continue

    const meta = {
      title: menu.menuName,
      icon: menu.icon ?? undefined,
      permissionCode: menu.permissionCode ?? undefined,
      menuCode: menu.menuCode
    }

    if (menu.menuType === MenuType.DIRECTORY) {
      // 目录：递归处理子菜单
      const children = menu.children ? buildAsyncRoutes(menu.children) : []
      if (children.length === 0) continue
      routes.push({
        path: menu.path ?? menu.menuCode,
        name: menu.menuCode,
        redirect: children[0].path,
        meta,
        children
      })
    } else {
      // 菜单：解析组件，失败则跳过（页面未实现）
      if (!menu.component) continue
      const comp = resolveComponent(menu.component)
      if (!comp) continue
      routes.push({
        path: menu.path ?? menu.menuCode,
        name: menu.menuCode,
        component: comp,
        meta
      })
    }
  }
  return routes
}
