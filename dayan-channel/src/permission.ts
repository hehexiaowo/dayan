import router from '@/router'
import { addDynamicRoutes } from '@/router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

/** 登录页白名单 */
const WHITE_LIST = ['/login']

/**
 * 全局前置路由守卫。
 *
 * - 白名单路由直接放行；
 * - 无 token 访问受保护路由时跳转 /login，并带上 redirect 参数；
 * - 已登录访问 /login 时重定向到首页；
 * - 首次进入受保护路由时，拉取用户信息 + 菜单树，挂载动态路由后重放当前跳转（确保动态路由已注册）。
 */
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const hasToken = userStore.isLoggedIn()

  if (!hasToken) {
    if (WHITE_LIST.includes(to.path)) {
      next()
      return
    }
    // 无 token 访问受保护路由 -> 登录页
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录访问 /login -> 首页
  if (to.path === '/login') {
    next({ path: '/' })
    return
  }

  // 首次进入：加载用户信息与动态路由
  if (!permissionStore.loaded) {
    try {
      if (!userStore.userInfo) {
        await userStore.getInfo()
      }
      const dynamicRoutes = await permissionStore.loadMenus()
      addDynamicRoutes(dynamicRoutes)
      // 动态路由已注册，重放本次跳转以命中新路由。
      // 只传 path（含 query），不 spread 整个 to 对象——后者会携带旧的 matched/params
      // 等内部字段，导致 router 复用未更新前的匹配结果，深链接刷新仍命中 404。
      next({ path: to.fullPath, replace: true })
      return
    } catch {
      // 菜单拉取失败（如后端未启动 / token 失效）：清登录态并跳登录
      userStore.reset()
      permissionStore.reset()
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  next()
})
