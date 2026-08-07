import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ChannelAccount,
  ChannelAccountQuery,
  ChannelRole,
  ChannelRoleQuery,
  ChannelPermission,
  ChannelInfo,
  ChannelInfoQuery
} from '@/types/channel'

/**
 * 渠道端系统管理接口封装（账号 + 角色 + 权限 + 渠道架构）。
 *
 * 从 admin 端 src/api/channel-sub.ts 改造而来：
 * - URL 前缀统一由 /admin-api/ 改为 /channel-api/（渠道端 Channel Controller）；
 * - 剔除 admin 版的 open-platforms / channel-configs 相关接口（增量2不用）；
 * - 新增角色权限授权 / 账号角色分配 / 权限树 / 渠道架构 CRUD（增量2页面所需）。
 *
 * 对应后端 Controller（增量2新增的渠道端系统管理）：
 * - /channel-api/channel-accounts      （ChannelAccountController）
 * - /channel-api/channel-roles         （ChannelRoleController）
 * - /channel-api/channel-account-roles （ChannelAccountRoleController，角色分配）
 * - /channel-api/channel-permissions   （ChannelPermissionController）
 * - /channel-api/channel-infos         （ChannelInfoController，渠道架构树）
 *
 * 主键约定：
 * - ChannelAccount：业务键 accountCode（String，CA 前缀），路径参数用 accountCode。
 * - ChannelRole：业务键 roleCode（String，RL 前缀），路径参数用 roleCode。
 * - ChannelPermission：业务键 permissionCode（String）；授权时传 permissionCodes 数组。
 * - ChannelInfo：业务键 channelCode（String），路径参数用 channelCode。
 */

// ==================== 渠道账号（channel-accounts）====================

/** 账号分页：GET /channel-api/channel-accounts */
export function pageChannelAccounts(
  query: ChannelAccountQuery
): Promise<PageResult<ChannelAccount>> {
  return request<PageResult<ChannelAccount>>({
    url: '/channel-api/channel-accounts',
    method: 'get',
    params: query
  })
}

/** 账号详情：GET /channel-api/channel-accounts/{accountCode} */
export function getChannelAccount(accountCode: string): Promise<ChannelAccount> {
  return request<ChannelAccount>({
    url: `/channel-api/channel-accounts/${accountCode}`,
    method: 'get'
  })
}

/** 新增账号：POST /channel-api/channel-accounts（返回 accountCode） */
export function createChannelAccount(data: Partial<ChannelAccount>): Promise<string> {
  return request<string>({
    url: '/channel-api/channel-accounts',
    method: 'post',
    data
  })
}

/** 修改账号：PUT /channel-api/channel-accounts/{accountCode}（accountCode/username 不可改） */
export function updateChannelAccount(
  accountCode: string,
  data: Partial<ChannelAccount>
): Promise<void> {
  return request<void>({
    url: `/channel-api/channel-accounts/${accountCode}`,
    method: 'put',
    data
  })
}

/** 重置账号密码：PUT /channel-api/channel-accounts/{accountCode}/reset-password（无 body） */
export function resetChannelAccountPassword(accountCode: string): Promise<void> {
  return request<void>({
    url: `/channel-api/channel-accounts/${accountCode}/reset-password`,
    method: 'put'
  })
}

/** 删除账号：DELETE /channel-api/channel-accounts/{accountCode} */
export function deleteChannelAccount(accountCode: string): Promise<void> {
  return request<void>({
    url: `/channel-api/channel-accounts/${accountCode}`,
    method: 'delete'
  })
}

// ==================== 渠道角色（channel-roles）====================

/** 角色分页：GET /channel-api/channel-roles */
export function pageChannelRoles(query: ChannelRoleQuery): Promise<PageResult<ChannelRole>> {
  return request<PageResult<ChannelRole>>({
    url: '/channel-api/channel-roles',
    method: 'get',
    params: query
  })
}

/** 角色详情：GET /channel-api/channel-roles/{roleCode} */
export function getChannelRole(roleCode: string): Promise<ChannelRole> {
  return request<ChannelRole>({
    url: `/channel-api/channel-roles/${roleCode}`,
    method: 'get'
  })
}

/** 新增角色：POST /channel-api/channel-roles（返回 roleCode） */
export function createChannelRole(data: Partial<ChannelRole>): Promise<string> {
  return request<string>({
    url: '/channel-api/channel-roles',
    method: 'post',
    data
  })
}

/** 修改角色：PUT /channel-api/channel-roles/{roleCode}（roleCode 不可改） */
export function updateChannelRole(roleCode: string, data: Partial<ChannelRole>): Promise<void> {
  return request<void>({
    url: `/channel-api/channel-roles/${roleCode}`,
    method: 'put',
    data
  })
}

/** 删除角色：DELETE /channel-api/channel-roles/{roleCode} */
export function deleteChannelRole(roleCode: string): Promise<void> {
  return request<void>({
    url: `/channel-api/channel-roles/${roleCode}`,
    method: 'delete'
  })
}

// ==================== 角色-权限（channel-roles/{roleCode}/permissions）====================

/**
 * 查询角色已分配的权限编码列表：GET /channel-api/channel-roles/{roleCode}/permissions
 *
 * 用于角色编辑页 el-tree 的回显（勾选已有权限节点）。
 */
export function getChannelRolePermissions(roleCode: string): Promise<string[]> {
  return request<string[]>({
    url: `/channel-api/channel-roles/${roleCode}/permissions`,
    method: 'get'
  })
}

/**
 * 给角色分配权限（全量覆盖）：PUT /channel-api/channel-roles/{roleCode}/permissions
 *
 * 后端按 permissionCodes 整体覆盖（先删后增），前端传当前 el-tree 勾选的完整集合。
 */
export function assignChannelRolePermissions(
  roleCode: string,
  permissionCodes: string[]
): Promise<void> {
  return request<void>({
    url: `/channel-api/channel-roles/${roleCode}/permissions`,
    method: 'put',
    data: permissionCodes
  })
}

// ==================== 账号-角色（channel-account-roles/{accountCode}/roles）====================

/**
 * 查询账号已分配的角色编码列表：GET /channel-api/channel-account-roles/{accountCode}/roles
 *
 * 用于账号编辑页角色多选回显。
 */
export function getChannelAccountRoles(accountCode: string): Promise<string[]> {
  return request<string[]>({
    url: `/channel-api/channel-account-roles/${accountCode}/roles`,
    method: 'get'
  })
}

/**
 * 给账号分配角色（全量覆盖）：PUT /channel-api/channel-account-roles/{accountCode}/roles
 *
 * 后端按 roleCodes 整体覆盖，前端传当前勾选的完整集合。
 */
export function assignChannelAccountRoles(
  accountCode: string,
  roleCodes: string[]
): Promise<void> {
  return request<void>({
    url: `/channel-api/channel-account-roles/${accountCode}/roles`,
    method: 'put',
    data: roleCodes
  })
}

// ==================== 渠道权限（channel-permissions）====================

/**
 * 全量权限列表（平铺）：GET /channel-api/channel-permissions/all
 *
 * 用于按需在前端组树，或下拉过滤。优先用 /tree 接口直接渲染。
 */
export function listAllChannelPermissions(): Promise<ChannelPermission[]> {
  return request<ChannelPermission[]>({
    url: '/channel-api/channel-permissions/all',
    method: 'get'
  })
}

/**
 * 权限授权树：GET /channel-api/channel-permissions/tree
 *
 * 后端已按 parentCode/sortOrder 组装为树形结构，前端直接喂给 el-tree 渲染。
 */
export function getChannelPermissionTree(): Promise<ChannelPermission[]> {
  return request<ChannelPermission[]>({
    url: '/channel-api/channel-permissions/tree',
    method: 'get'
  })
}

// ==================== 渠道架构（channel-infos）====================

/**
 * 渠道架构树：GET /channel-api/channel-infos/tree
 *
 * 后端按当前登录账号所属渠道下钻组装（仅返回本渠道及其子渠道），前端直接渲染树形表格。
 */
export function getChannelInfoTree(query?: ChannelInfoQuery): Promise<ChannelInfo[]> {
  return request<ChannelInfo[]>({
    url: '/channel-api/channel-infos/tree',
    method: 'get',
    params: query
  })
}

/**
 * 当前登录账号所属渠道：GET /channel-api/channel-infos/current
 *
 * 渠道架构页面默认选中节点 / 表单默认 channelCode 填充。
 */
export function getChannelInfoCurrent(): Promise<ChannelInfo> {
  return request<ChannelInfo>({
    url: '/channel-api/channel-infos/current',
    method: 'get'
  })
}

/** 新增渠道：POST /channel-api/channel-infos（返回 channelCode） */
export function createChannelInfo(data: Partial<ChannelInfo>): Promise<string> {
  return request<string>({
    url: '/channel-api/channel-infos',
    method: 'post',
    data
  })
}

/** 修改渠道：PUT /channel-api/channel-infos/{channelCode}（channelCode 不可改） */
export function updateChannelInfo(channelCode: string, data: Partial<ChannelInfo>): Promise<void> {
  return request<void>({
    url: `/channel-api/channel-infos/${channelCode}`,
    method: 'put',
    data
  })
}

/** 删除渠道：DELETE /channel-api/channel-infos/{channelCode}（存在子渠道时后端拒绝） */
export function deleteChannelInfo(channelCode: string): Promise<void> {
  return request<void>({
    url: `/channel-api/channel-infos/${channelCode}`,
    method: 'delete'
  })
}
