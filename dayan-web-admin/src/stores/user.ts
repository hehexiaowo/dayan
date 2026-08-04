import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, getInfo as getInfoApi } from '@/api/auth'
import { TOKEN_STORAGE_KEY } from '@/utils/request'
import type { LoginParams, UserInfo } from '@/types/auth'

/**
 * 用户登录态 Store。
 *
 * token 持久化到 localStorage，刷新页面后由 request.ts 请求拦截器读取并注入 Admin-Token。
 */
export const useUserStore = defineStore('user', () => {
  /** Token 值（Sa-Token 签发） */
  const token = ref<string>(localStorage.getItem(TOKEN_STORAGE_KEY) || '')
  /** 当前登录人信息 */
  const userInfo = ref<UserInfo | null>(null)

  /** 是否已登录 */
  function isLoggedIn(): boolean {
    return !!token.value
  }

  /** 持久化 token 到 localStorage */
  function setToken(value: string) {
    token.value = value
    if (value) {
      localStorage.setItem(TOKEN_STORAGE_KEY, value)
    } else {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
    }
  }

  /**
   * 登录：调用接口，成功后存 token 并缓存 userInfo。
   *
   * @returns 完整登录响应（含 token / tokenName / accountCode 等）
   */
  async function login(params: LoginParams): Promise<UserInfo> {
    const data = await loginApi(params)
    setToken(data.token)
    userInfo.value = data
    return data
  }

  /** 拉取当前登录人信息并缓存 */
  async function getInfo(): Promise<UserInfo> {
    const data = await getInfoApi()
    userInfo.value = data
    return data
  }

  /** 登出：通知后端失效 token（忽略失败），再清本地态 */
  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 后端登出失败不阻塞前端清理
    } finally {
      reset()
    }
  }

  /** 清空本地登录态 */
  function reset() {
    setToken('')
    userInfo.value = null
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    login,
    getInfo,
    logout,
    reset
  }
})
