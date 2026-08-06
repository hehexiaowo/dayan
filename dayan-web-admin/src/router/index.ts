import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

/**
 * 路由表。
 *
 * - /login   登录页（白名单）
 * - /        后台主框架（DefaultLayout 嵌套，需登录）
 *   - /dashboard 首页看板（静态注册，始终可用）
 *   - 其余业务路由由 permission 守卫从后端菜单动态 addRoute 挂载（见 stores/permission + router/dynamic）
 * - /:pathMatch(.*)*  404
 *
 * 动态路由 name 统一用 menuCode；布局 / 下通过 addRoute({ path: '/', name: 'Layout', ... }) 的
 * children 方式挂载（见 permission.ts 守卫实现）。
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
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页看板', icon: 'Odometer' }
      },
      // ---------- 详情页（静态注册，不进侧边栏——菜单树来自后端，静态路由天然不显示） ----------
      // 主从详情页：从主列表页"详情/管理"按钮跳转进入，带主实体 code 路由参数。
      // 各详情页内部用 el-tabs 组织子表，tab 内联 CRUD。
      {
        path: 'resource/park/detail/:parkCode',
        name: 'ParkDetail',
        component: () => import('@/views/resource/park/detail/index.vue'),
        // hidden 标记：虽然侧边栏只读后端菜单树不会显示它，但保留 meta.hidden 供守卫/面包屑识别
        meta: { title: '机构详情', hidden: true }
      },
      {
        path: 'resource/supplier/detail/:supplierCode',
        name: 'SupplierDetail',
        component: () => import('@/views/resource/supplier/detail/index.vue'),
        meta: { title: '供应商详情', hidden: true }
      },
      {
        path: 'resource/supplier/contract',
        name: 'SupplierContract',
        component: () => import('@/views/resource/supplier/contract/index.vue'),
        meta: { title: '供应商合同', hidden: true }
      },
      {
        path: 'channel/client/detail/:clientCode',
        name: 'ClientDetail',
        component: () => import('@/views/channel/client/detail/index.vue'),
        meta: { title: '客户详情', hidden: true }
      },
      {
        path: 'service/butler/detail/:butlerCode',
        name: 'ButlerDetail',
        component: () => import('@/views/service/butler/detail/index.vue'),
        meta: { title: '管家详情', hidden: true }
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
 * 这里向 Layout 根路由（name 缺省时按 path='/' 匹配）追加。
 * 由 permission 守卫在登录后调用一次。
 *
 * @param dynamicRoutes 由 buildAsyncRoutes 生成的业务路由
 */
export function addDynamicRoutes(dynamicRoutes: RouteRecordRaw[]) {
  for (const route of dynamicRoutes) {
    // addRoute(parentName, route)：第一个参数是父路由的 name（非 path）。
    // 根布局路由 name 为 'Layout'（见上方路由表），动态业务路由作为它的 children 挂载，
    // 从而共享 DefaultLayout 的 <router-view> 容器。
    router.addRoute('Layout', route)
  }
}

export default router
