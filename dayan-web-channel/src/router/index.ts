import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

/**
 * 路由表（Channel 端静态路由）。
 *
 * - /login   登录页（白名单）
 * - /        后台主框架（DefaultLayout 嵌套，需登录）
 *   - /dashboard 工作台（渠道概览）
 *   - /agent    代理人管理
 *   - /client   客户管理
 *   - /equity   权益查询
 *   - /order    订单查询
 * - /:pathMatch(.*)*  404
 *
 * 注：Channel 菜单数据（domain_type=channel）后端尚未 seed，
 * 故本期采用静态路由（不引入动态菜单）。
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
    component: () => import('@/layouts/default/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'agent',
        name: 'Agent',
        component: () => import('@/views/agent/index.vue'),
        meta: { title: '代理人管理', icon: 'User' }
      },
      {
        path: 'client',
        name: 'Client',
        component: () => import('@/views/client/index.vue'),
        meta: { title: '客户管理', icon: 'UserFilled' }
      },
      {
        path: 'equity',
        name: 'Equity',
        component: () => import('@/views/equity/index.vue'),
        meta: { title: '权益查询', icon: 'Ticket' }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单查询', icon: 'List' }
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

export default router
