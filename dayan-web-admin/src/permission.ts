import router from '@/router'
import { useUserStore } from '@/stores/user'

/** 登录页白名单 */
const WHITE_LIST = ['/login']

/**
 * 全局前置路由守卫。
 *
 * - 白名单路由直接放行；
 * - 无 token 访问受保护路由时跳转 /login，并带上 redirect 参数；
 * - 已登录访问 /login 时重定向到首页。
 */
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const hasToken = userStore.isLoggedIn()

  if (hasToken) {
    if (to.path === '/login') {
      next({ path: '/' })
    } else {
      next()
    }
    return
  }

  if (WHITE_LIST.includes(to.path)) {
    next()
    return
  }

  // 无 token 访问受保护路由 -> 登录页
  next({
    path: '/login',
    query: { redirect: to.fullPath }
  })
})
