import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ChannelAccount,
  ChannelAccountQuery,
  ChannelRole,
  ChannelRoleQuery,
  ChannelOpenPlatform,
  ChannelOpenPlatformQuery,
  ChannelConfigContent,
  ChannelConfigScene,
  ChannelConfigGoods
} from '@/types/channel'

/**
 * 渠道子表接口封装（账户 + 角色 + 开放平台 + 配置三类）。
 *
 * 对应后端 4 个 admin Controller + 1 个配置 Controller：
 * - /admin-api/channel-accounts   （ChannelAccountAdminController）
 * - /admin-api/channel-roles      （ChannelRoleAdminController）
 * - /admin-api/open-platforms     （ChannelOpenPlatformAdminController）
 * - /admin-api/channel-configs    （ChannelConfigAdminController，list+save 全量覆盖模式）
 *
 * 三套主键约定（差异最大，务必注意）：
 * - ChannelAccount：业务键 accountCode（String，服务端生成 CA 前缀），路径参数用 accountCode。
 * - ChannelRole：业务键 roleCode（String，服务端生成 RL 前缀），路径参数用 roleCode。
 * - ChannelOpenPlatform：自增 id（Long），路径参数用 id（与其他子表不同）。
 * - ChannelConfig*：不带主键路径，按 channelCode 整体 list/save（先删后增全量覆盖）。
 *
 * 权限分配端点（role 绑权限、account 绑角色）本次不实现，留待后续。
 */

// ==================== 渠道账户（channel-accounts）====================

/** 账户分页：GET /admin-api/channel-accounts */
export function pageChannelAccounts(query: ChannelAccountQuery): Promise<PageResult<ChannelAccount>> {
  return request<PageResult<ChannelAccount>>({
    url: '/admin-api/channel-accounts',
    method: 'get',
    params: query
  })
}

/** 账户详情：GET /admin-api/channel-accounts/{accountCode} */
export function getChannelAccount(accountCode: string): Promise<ChannelAccount> {
  return request<ChannelAccount>({
    url: `/admin-api/channel-accounts/${accountCode}`,
    method: 'get'
  })
}

/** 新增账户：POST /admin-api/channel-accounts（返回 accountCode） */
export function createChannelAccount(data: Partial<ChannelAccount>): Promise<string> {
  return request<string>({
    url: '/admin-api/channel-accounts',
    method: 'post',
    data
  })
}

/** 修改账户：PUT /admin-api/channel-accounts/{accountCode}（accountCode/username 不可改） */
export function updateChannelAccount(accountCode: string, data: Partial<ChannelAccount>): Promise<void> {
  return request<void>({
    url: `/admin-api/channel-accounts/${accountCode}`,
    method: 'put',
    data
  })
}

/** 重置账户密码：PUT /admin-api/channel-accounts/{accountCode}/reset-password（无 body） */
export function resetChannelAccountPassword(accountCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/channel-accounts/${accountCode}/reset-password`,
    method: 'put'
  })
}

/** 删除账户：DELETE /admin-api/channel-accounts/{accountCode} */
export function deleteChannelAccount(accountCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/channel-accounts/${accountCode}`,
    method: 'delete'
  })
}

// ==================== 渠道角色（channel-roles）====================

/** 角色分页：GET /admin-api/channel-roles */
export function pageChannelRoles(query: ChannelRoleQuery): Promise<PageResult<ChannelRole>> {
  return request<PageResult<ChannelRole>>({
    url: '/admin-api/channel-roles',
    method: 'get',
    params: query
  })
}

/** 角色详情：GET /admin-api/channel-roles/{roleCode} */
export function getChannelRole(roleCode: string): Promise<ChannelRole> {
  return request<ChannelRole>({
    url: `/admin-api/channel-roles/${roleCode}`,
    method: 'get'
  })
}

/** 新增角色：POST /admin-api/channel-roles（返回 roleCode） */
export function createChannelRole(data: Partial<ChannelRole>): Promise<string> {
  return request<string>({
    url: '/admin-api/channel-roles',
    method: 'post',
    data
  })
}

/** 修改角色：PUT /admin-api/channel-roles/{roleCode}（roleCode 不可改） */
export function updateChannelRole(roleCode: string, data: Partial<ChannelRole>): Promise<void> {
  return request<void>({
    url: `/admin-api/channel-roles/${roleCode}`,
    method: 'put',
    data
  })
}

/** 删除角色：DELETE /admin-api/channel-roles/{roleCode} */
export function deleteChannelRole(roleCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/channel-roles/${roleCode}`,
    method: 'delete'
  })
}

// ==================== 开放平台对接（open-platforms）====================

/** 开放平台分页：GET /admin-api/open-platforms */
export function pageOpenPlatforms(query: ChannelOpenPlatformQuery): Promise<PageResult<ChannelOpenPlatform>> {
  return request<PageResult<ChannelOpenPlatform>>({
    url: '/admin-api/open-platforms',
    method: 'get',
    params: query
  })
}

/** 开放平台详情：GET /admin-api/open-platforms/{id}（注意用 id，非编码） */
export function getOpenPlatform(id: number): Promise<ChannelOpenPlatform> {
  return request<ChannelOpenPlatform>({
    url: `/admin-api/open-platforms/${id}`,
    method: 'get'
  })
}

/** 新增开放平台：POST /admin-api/open-platforms（返回 id） */
export function createOpenPlatform(data: Partial<ChannelOpenPlatform>): Promise<number> {
  return request<number>({
    url: '/admin-api/open-platforms',
    method: 'post',
    data
  })
}

/** 修改开放平台：PUT /admin-api/open-platforms/{id}（appSecret 留空不改，填值轮换） */
export function updateOpenPlatform(id: number, data: Partial<ChannelOpenPlatform>): Promise<void> {
  return request<void>({
    url: `/admin-api/open-platforms/${id}`,
    method: 'put',
    data
  })
}

/** 删除开放平台：DELETE /admin-api/open-platforms/{id} */
export function deleteOpenPlatform(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/open-platforms/${id}`,
    method: 'delete'
  })
}

// ==================== 渠道配置（channel-configs，list+save 全量覆盖模式）====================
//
// 与其他子表不同：无 page/get/create/update/delete，只有每类各 2 个端点：
//   GET /{channelCode}/{type}  → 返回整张配置表（List）
//   PUT /{channelCode}/{type}  → 全量覆盖（先删后增，后端 setId(null) 忽略入参 id）
// 前端 UI 范式：可编辑表格 + 整体「保存配置」按钮，而非逐条 CRUD。

// ---------- 内容配置（content）----------

/** 内容配置列表：GET /admin-api/channel-configs/{channelCode}/content */
export function listContentConfigs(channelCode: string): Promise<ChannelConfigContent[]> {
  return request<ChannelConfigContent[]>({
    url: `/admin-api/channel-configs/${channelCode}/content`,
    method: 'get'
  })
}

/** 保存内容配置（全量覆盖）：PUT /admin-api/channel-configs/{channelCode}/content */
export function saveContentConfigs(channelCode: string, data: ChannelConfigContent[]): Promise<void> {
  return request<void>({
    url: `/admin-api/channel-configs/${channelCode}/content`,
    method: 'put',
    data
  })
}

// ---------- 场景配置（scene）----------

/** 场景配置列表：GET /admin-api/channel-configs/{channelCode}/scene */
export function listSceneConfigs(channelCode: string): Promise<ChannelConfigScene[]> {
  return request<ChannelConfigScene[]>({
    url: `/admin-api/channel-configs/${channelCode}/scene`,
    method: 'get'
  })
}

/** 保存场景配置（全量覆盖）：PUT /admin-api/channel-configs/{channelCode}/scene */
export function saveSceneConfigs(channelCode: string, data: ChannelConfigScene[]): Promise<void> {
  return request<void>({
    url: `/admin-api/channel-configs/${channelCode}/scene`,
    method: 'put',
    data
  })
}

// ---------- 商品配置（goods）----------

/** 商品配置列表：GET /admin-api/channel-configs/{channelCode}/goods */
export function listGoodsConfigs(channelCode: string): Promise<ChannelConfigGoods[]> {
  return request<ChannelConfigGoods[]>({
    url: `/admin-api/channel-configs/${channelCode}/goods`,
    method: 'get'
  })
}

/** 保存商品配置（全量覆盖）：PUT /admin-api/channel-configs/{channelCode}/goods */
export function saveGoodsConfigs(channelCode: string, data: ChannelConfigGoods[]): Promise<void> {
  return request<void>({
    url: `/admin-api/channel-configs/${channelCode}/goods`,
    method: 'put',
    data
  })
}
