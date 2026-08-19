import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { ApiResult, CODE_SUCCESS, CODE_TOKEN_INVALID, CODE_UNAUTHORIZED, TOKEN_HEADER } from '@/types/global'

/** Token 持久化的 localStorage key（需与 stores/user.ts 保持一致） */
export const TOKEN_STORAGE_KEY = 'dayan_channel_token'

/**
 * Axios 实例。
 *
 * - baseURL 取自 import.meta.env.VITE_API_BASE_URL（开发期留空走 Vite 代理）；
 * - 请求拦截器注入 Channel-Token；
 * - 响应拦截器对统一封装 R<T> 做拆包：code===0 返回 data，否则报错；
 *   10100 / 10101 视为 Token 失效，清 token 并跳登录页。
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// ---------------- 请求拦截器 ----------------
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY)
    if (token && config.headers) {
      config.headers[TOKEN_HEADER] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------------- 响应拦截器 ----------------
/**
 * 清除登录态并跳转到登录页。
 *
 * 此处直接操作 localStorage + window.location，避免引入 router / pinia store
 * 造成循环依赖（request.ts 被 store 与 api 层依赖）。
 */
function handleUnauthorized() {
  localStorage.removeItem(TOKEN_STORAGE_KEY)
  // 避免在登录页重复跳转造成死循环
  if (!window.location.pathname.startsWith('/login')) {
    ElMessage.error('登录状态已失效，请重新登录')
    window.location.href = '/login'
  }
}

/**
 * 判断本次请求是否要求静默（不弹全局错误 toast）。
 *
 * 调用方可在 config 上挂 silent: true，用于"接口未实现/已知降级"等场景——
 * 此时错误仍会 reject 给业务层 catch 自行处理，但拦截器不再重复弹 toast，
 * 避免与页面的业务提示叠加（例如上传多文件时按文件逐个提示失败原因）。
 */
function isSilent(config: unknown): boolean {
  return Boolean((config as { silent?: boolean } | undefined)?.silent)
}

service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult
    const silent = isSilent(response.config)

    // 后端统一封装 R<T>：code===0 视为成功，返回 data
    if (res && typeof res.code === 'number') {
      if (res.code === CODE_SUCCESS) {
        return res.data
      }

      // Token 失效：未登录 / Token 过期（静默选项不抑制登录失效跳转）
      if (res.code === CODE_UNAUTHORIZED || res.code === CODE_TOKEN_INVALID) {
        handleUnauthorized()
        return Promise.reject(new Error(res.message || '登录状态已失效'))
      }

      // 其它业务错误：默认弹 toast，silent 时只 reject 不弹
      if (!silent) {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }

    // 非标准封装（如文件流等）：原样返回
    return response.data
  },
  (error) => {
    // HTTP 层错误
    const status = error?.response?.status
    if (status === 401) {
      handleUnauthorized()
    } else if (!isSilent(error?.config)) {
      const msg = error?.response?.data?.message || error.message || '网络异常，请稍后重试'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

/** 通用请求方法，返回后端 R<T> 中的 data */
export function request<T = unknown>(config: AxiosRequestConfig & { silent?: boolean }): Promise<T> {
  // 响应拦截器已对 R<T> 拆包并返回 data，此处用 unknown 中转后断言为 T
  // （axios 类型按完整 response 推导，与拦截器拆包语义不一致，故需断言）
  return service.request(config) as Promise<T>
}

export default service
