import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

/**
 * 路由表（Channel 端）。
 *
 * - /login   登录页（白名单）
 * - /        后台主框架（DefaultLayout 嵌套，需登录），redirect 到 /dashboard
 *   - 业务路由（dashboard/agent/client/equity/order）由 permission 守卫从后端菜单动态 addRoute 挂载
 * - /:pathMatch(.*)*  404
 *
 * 动态路由 name 统一用 menuCode，作为 Layout 路由的 children 挂载（见 permission.ts 守卫）。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/default/index.vue'),
    redirect: '/dashboard',
    // children 运行时由 addDynamicRoutes 动态注入（permission 守卫登录后调用）。
    // 以下为静态详情路由（与动态菜单路由平级共存，列表页"详情"跳转进入）。
    children: [
      {
        path: '/agent/detail/:agentCode',
        name: 'AgentDetail',
        component: () => import('@/views/agent/detail/index.vue'),
        meta: { title: '代理人详情' }
      },
      {
        path: '/client/detail/:clientCode',
        name: 'ClientDetail',
        component: () => import('@/views/client/detail/index.vue'),
        meta: { title: '客户详情' }
      },
      {
        path: '/scene/detail/:sceneCode',
        name: 'SceneDetail',
        component: () => import('@/views/scene/detail/index.vue'),
        meta: { title: '场景详情' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 将动态生成的业务路由挂载到布局 / 下。
 *
 * Vue Router 的 addRoute(name, route) 形式可向具名路由追加 children，
 * 这里向 Layout 根路由（name='Layout'）追加。由 permission 守卫在登录后调用一次。
 *
 * @param dynamicRoutes 由 buildAsyncRoutes 生成的业务路由
 */
export function addDynamicRoutes(dynamicRoutes: RouteRecordRaw[]) {
  for (const route of dynamicRoutes) {
    router.addRoute('Layout', route)
  }
}

export default router
