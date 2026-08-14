import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  ButlerInfo,
  ButlerInfoQuery,
  ButlerAccount,
  ButlerAccountQuery,
  ButlerSkill,
  ButlerSkillQuery,
  ButlerClientRel,
  ButlerClientRelQuery,
  ButlerServiceRecord,
  ButlerServiceRecordQuery,
  ButlerRating,
  ButlerRatingQuery,
  ServiceSession,
  ServiceSessionQuery
} from '@/types/service'

/**
 * 服务域接口封装（管家信息 + 服务会话）。
 *
 * 对应后端：
 * - ButlerInfoAdminController（/admin-api/butler/info/*）：标准 CRUD
 * - ServiceSessionAdminController（/admin-api/service/session/*）：CRUD + 状态机动作
 */

// ==================== 管家信息 ====================

/** 管家分页：GET /admin-api/butler/info/page */
export function pageButlers(query: ButlerInfoQuery): Promise<PageResult<ButlerInfo>> {
  return request<PageResult<ButlerInfo>>({
    url: '/admin-api/butler/info/page',
    method: 'get',
    params: query
  })
}

/** 管家列表（全量）：GET /admin-api/butler/info/list */
export function listButlers(query?: Partial<ButlerInfoQuery>): Promise<ButlerInfo[]> {
  return request<ButlerInfo[]>({
    url: '/admin-api/butler/info/list',
    method: 'get',
    params: query
  })
}

/** 管家详情：GET /admin-api/butler/info/{butlerCode} */
export function getButler(butlerCode: string): Promise<ButlerInfo> {
  return request<ButlerInfo>({
    url: `/admin-api/butler/info/${butlerCode}`,
    method: 'get'
  })
}

/**
 * 新增管家：POST /admin-api/butler/info（返回 butlerCode）。
 * username/password 为创建时可选的后台账号开通字段（填 username 即同步开通 organ 账号）。
 */
export function createButler(
  data: Partial<ButlerInfo> & { username?: string; password?: string }
): Promise<string> {
  return request<string>({
    url: '/admin-api/butler/info',
    method: 'post',
    data
  })
}

/** 修改管家：PUT /admin-api/butler/info/{butlerCode} */
export function updateButler(butlerCode: string, data: Partial<ButlerInfo>): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/info/${butlerCode}`,
    method: 'put',
    data
  })
}

/** 删除管家：DELETE /admin-api/butler/info/{butlerCode} */
export function deleteButler(butlerCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/info/${butlerCode}`,
    method: 'delete'
  })
}

/**
 * 开通管家后台账号：POST /admin-api/butler/info/{butlerCode}/account。
 *
 * 为管家创建 organ_account（可登录 admin）+ 员工档案（养老管家部门）+ 普通管家角色，
 * 返回 accountCode；重复开通后端报错。密码留空使用系统默认密码。
 */
export function openButlerAccount(
  butlerCode: string,
  data: { username: string; password?: string }
): Promise<string> {
  return request<string>({
    url: `/admin-api/butler/info/${butlerCode}/account`,
    method: 'post',
    data
  })
}

// ==================== 管家子表（5 张，详情页 tab 用） ====================
//
// 主键规则：
// - Account/Skill：自增 number id（标准 CRUD）。
// - ClientRel/ServiceRecord/Rating：雪花 id（前端按 string 处理，防 JS Number 精度溢出）。
// 列表统一用 GET /list?butlerCode=xxx 返回 List（非分页）；同时提供 page 供分页场景使用。

// ---------------- 1. 管家独立账号（butler/account，预留未来管家端登录）----------------

/** 账号分页：GET /admin-api/butler/account/page */
export function pageButlerAccounts(query: ButlerAccountQuery): Promise<PageResult<ButlerAccount>> {
  return request<PageResult<ButlerAccount>>({
    url: '/admin-api/butler/account/page',
    method: 'get',
    params: query
  })
}

/** 账号列表（按 butlerCode 过滤）：GET /admin-api/butler/account/list?butlerCode=xxx */
export function listButlerAccounts(butlerCode: string): Promise<ButlerAccount[]> {
  return request<ButlerAccount[]>({
    url: '/admin-api/butler/account/list',
    method: 'get',
    params: { butlerCode }
  })
}

/** 账号详情：GET /admin-api/butler/account/{id} */
export function getButlerAccount(id: number): Promise<ButlerAccount> {
  return request<ButlerAccount>({
    url: `/admin-api/butler/account/${id}`,
    method: 'get'
  })
}

/**
 * 新增账号：POST /admin-api/butler/account。
 * password 可填（留空服务端用默认值 dayan@123）；VO 无 password 字段，故单独扩展。
 */
export function createButlerAccount(data: Partial<ButlerAccount> & { password?: string }): Promise<void> {
  return request<void>({
    url: '/admin-api/butler/account',
    method: 'post',
    data
  })
}

/** 修改账号：PUT /admin-api/butler/account/{id}（不含 username/password，username 创建后不可改） */
export function updateButlerAccount(id: number, data: Partial<ButlerAccount>): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/account/${id}`,
    method: 'put',
    data
  })
}

/** 重置账号密码：PUT /admin-api/butler/account/{id}/reset-password（无 body，重置 dayan@123） */
export function resetButlerAccountPassword(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/account/${id}/reset-password`,
    method: 'put'
  })
}

/** 删除账号：DELETE /admin-api/butler/account/{id} */
export function deleteButlerAccount(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/account/${id}`,
    method: 'delete'
  })
}

// ---------------- 2. 管家技能（butler/skill）----------------

/** 技能分页：GET /admin-api/butler/skill/page */
export function pageButlerSkills(query: ButlerSkillQuery): Promise<PageResult<ButlerSkill>> {
  return request<PageResult<ButlerSkill>>({
    url: '/admin-api/butler/skill/page',
    method: 'get',
    params: query
  })
}

/** 技能列表（按 butlerCode 过滤）：GET /admin-api/butler/skill/list?butlerCode=xxx */
export function listButlerSkills(butlerCode: string): Promise<ButlerSkill[]> {
  return request<ButlerSkill[]>({
    url: '/admin-api/butler/skill/list',
    method: 'get',
    params: { butlerCode }
  })
}

/** 技能详情：GET /admin-api/butler/skill/{id} */
export function getButlerSkill(id: number): Promise<ButlerSkill> {
  return request<ButlerSkill>({
    url: `/admin-api/butler/skill/${id}`,
    method: 'get'
  })
}

/** 新增技能：POST /admin-api/butler/skill */
export function createButlerSkill(data: Partial<ButlerSkill>): Promise<void> {
  return request<void>({
    url: '/admin-api/butler/skill',
    method: 'post',
    data
  })
}

/**
 * 修改技能：PUT /admin-api/butler/skill/{id}。
 * 仅可改 skillName/proficiency/isCertified/certificateNo/obtainDate/sortOrder，skillCode 不可改。
 */
export function updateButlerSkill(id: number, data: Partial<ButlerSkill>): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/skill/${id}`,
    method: 'put',
    data
  })
}

/** 删除技能：DELETE /admin-api/butler/skill/{id} */
export function deleteButlerSkill(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/skill/${id}`,
    method: 'delete'
  })
}

// ---------------- 3. 管家-客户绑定（butler/client-rel，非标准 CRUD） ----------------

/** 绑定关系分页：GET /admin-api/butler/client-rel/page */
export function pageButlerClientRels(query: ButlerClientRelQuery): Promise<PageResult<ButlerClientRel>> {
  return request<PageResult<ButlerClientRel>>({
    url: '/admin-api/butler/client-rel/page',
    method: 'get',
    params: query
  })
}

/** 绑定关系列表（按 butlerCode 过滤）：GET /admin-api/butler/client-rel/list?butlerCode=xxx */
export function listButlerClientRels(butlerCode: string): Promise<ButlerClientRel[]> {
  return request<ButlerClientRel[]>({
    url: '/admin-api/butler/client-rel/list',
    method: 'get',
    params: { butlerCode }
  })
}

/** 绑定关系详情：GET /admin-api/butler/client-rel/{id} */
export function getButlerClientRel(id: string): Promise<ButlerClientRel> {
  return request<ButlerClientRel>({
    url: `/admin-api/butler/client-rel/${id}`,
    method: 'get'
  })
}

/**
 * 绑定客户（语义 bind）：POST /admin-api/butler/client-rel。
 * body 仅 butlerCode + clientCode；bindTime 服务端设 now()，status 服务端置 1。
 * 后端校验"一客户一管家"（同 clientCode status=1 仅允许一条）。
 */
export function bindButlerClient(data: { butlerCode: string; clientCode: string }): Promise<void> {
  return request<void>({
    url: '/admin-api/butler/client-rel',
    method: 'post',
    data
  })
}

/**
 * 解绑客户：PUT /admin-api/butler/client-rel/{id}/unbind（无 body，status 置 0）。
 */
export function unbindButlerClient(id: string): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/client-rel/${id}/unbind`,
    method: 'put'
  })
}

/** 删除绑定关系：DELETE /admin-api/butler/client-rel/{id} */
export function deleteButlerClientRel(id: string): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/client-rel/${id}`,
    method: 'delete'
  })
}

// ---------------- 4. 管家服务记录（butler/service-record） ----------------

/** 服务记录分页：GET /admin-api/butler/service-record/page */
export function pageButlerServiceRecords(
  query: ButlerServiceRecordQuery
): Promise<PageResult<ButlerServiceRecord>> {
  return request<PageResult<ButlerServiceRecord>>({
    url: '/admin-api/butler/service-record/page',
    method: 'get',
    params: query
  })
}

/** 服务记录列表（按 butlerCode 过滤）：GET /admin-api/butler/service-record/list?butlerCode=xxx */
export function listButlerServiceRecords(butlerCode: string): Promise<ButlerServiceRecord[]> {
  return request<ButlerServiceRecord[]>({
    url: '/admin-api/butler/service-record/list',
    method: 'get',
    params: { butlerCode }
  })
}

/** 服务记录详情：GET /admin-api/butler/service-record/{id} */
export function getButlerServiceRecord(id: string): Promise<ButlerServiceRecord> {
  return request<ButlerServiceRecord>({
    url: `/admin-api/butler/service-record/${id}`,
    method: 'get'
  })
}

/** 新增服务记录：POST /admin-api/butler/service-record（前端显式传 status，不依赖后端默认） */
export function createButlerServiceRecord(data: Partial<ButlerServiceRecord>): Promise<void> {
  return request<void>({
    url: '/admin-api/butler/service-record',
    method: 'post',
    data
  })
}

/** 修改服务记录：PUT /admin-api/butler/service-record/{id} */
export function updateButlerServiceRecord(
  id: string,
  data: Partial<ButlerServiceRecord>
): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/service-record/${id}`,
    method: 'put',
    data
  })
}

/** 删除服务记录：DELETE /admin-api/butler/service-record/{id} */
export function deleteButlerServiceRecord(id: string): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/service-record/${id}`,
    method: 'delete'
  })
}

// ---------------- 5. 管家评价（butler/rating） ----------------

/** 评价分页：GET /admin-api/butler/rating/page */
export function pageButlerRatings(query: ButlerRatingQuery): Promise<PageResult<ButlerRating>> {
  return request<PageResult<ButlerRating>>({
    url: '/admin-api/butler/rating/page',
    method: 'get',
    params: query
  })
}

/** 评价列表（按 butlerCode 过滤）：GET /admin-api/butler/rating/list?butlerCode=xxx */
export function listButlerRatings(butlerCode: string): Promise<ButlerRating[]> {
  return request<ButlerRating[]>({
    url: '/admin-api/butler/rating/list',
    method: 'get',
    params: { butlerCode }
  })
}

/** 评价详情：GET /admin-api/butler/rating/{id} */
export function getButlerRating(id: string): Promise<ButlerRating> {
  return request<ButlerRating>({
    url: `/admin-api/butler/rating/${id}`,
    method: 'get'
  })
}

/** 新增评价：POST /admin-api/butler/rating */
export function createButlerRating(data: Partial<ButlerRating>): Promise<void> {
  return request<void>({
    url: '/admin-api/butler/rating',
    method: 'post',
    data
  })
}

/** 修改评价：PUT /admin-api/butler/rating/{id}（仅改 rating/content/status，clientCode/serviceRecordCode 不可改） */
export function updateButlerRating(id: string, data: Partial<ButlerRating>): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/rating/${id}`,
    method: 'put',
    data
  })
}

/** 删除评价：DELETE /admin-api/butler/rating/{id} */
export function deleteButlerRating(id: string): Promise<void> {
  return request<void>({
    url: `/admin-api/butler/rating/${id}`,
    method: 'delete'
  })
}

// ==================== 服务会话 ====================

/** 会话分页：GET /admin-api/service/session/page */
export function pageSessions(query: ServiceSessionQuery): Promise<PageResult<ServiceSession>> {
  return request<PageResult<ServiceSession>>({
    url: '/admin-api/service/session/page',
    method: 'get',
    params: query
  })
}

/** 会话列表（按条件）：GET /admin-api/service/session/list */
export function listSessions(query?: Partial<ServiceSessionQuery>): Promise<ServiceSession[]> {
  return request<ServiceSession[]>({
    url: '/admin-api/service/session/list',
    method: 'get',
    params: query
  })
}

/** 会话详情：GET /admin-api/service/session/{sessionCode} */
export function getSession(sessionCode: string): Promise<ServiceSession> {
  return request<ServiceSession>({
    url: `/admin-api/service/session/${sessionCode}`,
    method: 'get'
  })
}

/**
 * 修改会话（普通字段）：PUT /admin-api/service/session/{sessionCode}
 *
 * 用于编辑非状态机字段（serviceTitle / serviceDescription / priority / remark 等）。
 */
export function updateSession(sessionCode: string, data: Partial<ServiceSession>): Promise<void> {
  return request<void>({
    url: `/admin-api/service/session/${sessionCode}`,
    method: 'put',
    data
  })
}

/** 删除会话：DELETE /admin-api/service/session/{sessionCode} */
export function deleteSession(sessionCode: string): Promise<void> {
  return request<void>({
    url: `/admin-api/service/session/${sessionCode}`,
    method: 'delete'
  })
}

/**
 * 通用状态机流转：POST /admin-api/service/session/transition
 *
 * 入参对齐后端 TransitionDTO（@RequestBody）。
 * event 取值见 com.dayan.service.enums.ServiceSessionEvent：
 * assign_butler / submit_demand / confirm_solution / reject_solution /
 * start_service / finish / cancel。
 * 业务专用接口（/assign-butler 等）已封装对应事件，本接口供前端统一调用。
 * 返回流转后的新 sessionStatus 状态码。
 */
export function transitionSession(sessionCode: string, event: string): Promise<number> {
  return request<number>({
    url: '/admin-api/service/session/transition',
    method: 'post',
    data: { sessionCode, event }
  })
}

/**
 * 分配管家：POST /admin-api/service/session/assign-butler
 *
 * 入参对齐后端 AssignButlerDTO（@RequestBody）。
 * 触发 assign_butler 事件，写 butlerCode/butlerFullName(快照)/acceptTime=now。
 */
export function assignButler(sessionCode: string, butlerCode: string): Promise<void> {
  return request<void>({
    url: '/admin-api/service/session/assign-butler',
    method: 'post',
    data: { sessionCode, butlerCode }
  })
}
