import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SystemConfig, ConfigQuery } from '@/types/config'

/**
 * 系统配置接口封装。
 *
 * 对应后端 SystemConfigAdminController（/admin-api/configs/*），完整 CRUD。
 */

/** 分页查询配置：GET /admin-api/configs?configGroup&configKey&current&size */
export function pageConfigs(query: ConfigQuery): Promise<PageResult<SystemConfig>> {
  return request<PageResult<SystemConfig>>({
    url: '/admin-api/configs',
    method: 'get',
    params: {
      configGroup: query.configGroup || undefined,
      configKey: query.configKey || undefined,
      current: query.current,
      size: query.size
    }
  })
}

/** 按分组查询配置列表：GET /admin-api/configs/group/{configGroup} */
export function listConfigsByGroup(configGroup: string): Promise<SystemConfig[]> {
  return request<SystemConfig[]>({
    url: `/admin-api/configs/group/${configGroup}`,
    method: 'get'
  })
}

/** 新增配置：POST /admin-api/configs */
export function createConfig(data: Partial<SystemConfig>): Promise<string> {
  return request<string>({
    url: '/admin-api/configs',
    method: 'post',
    data
  })
}

/** 修改配置：PUT /admin-api/configs/{configKey} */
export function updateConfig(configKey: string, data: Partial<SystemConfig>): Promise<void> {
  return request<void>({
    url: `/admin-api/configs/${configKey}`,
    method: 'put',
    data
  })
}

/** 删除配置：DELETE /admin-api/configs/{configKey} */
export function deleteConfig(configKey: string): Promise<void> {
  return request<void>({
    url: `/admin-api/configs/${configKey}`,
    method: 'delete'
  })
}
