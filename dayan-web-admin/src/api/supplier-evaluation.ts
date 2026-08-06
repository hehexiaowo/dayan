import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SupplierEvaluation, SupplierEvaluationQuery } from '@/types/supplier'

/**
 * 供应商评价接口封装。
 *
 * 对应后端 SupplierEvaluationController（/admin-api/supplier/evaluation/*）。
 *
 * 注意：
 * - 主键 Long id（自增）。
 * - update 的 id 走 query string（@RequestParam），不是 path variable。
 * - totalScore（综合分）/ scoreLevel（等级 A/B/C/D）由后端按三科评分和投诉率自动计算
 *   （为空时算）；前端表单不含这两字段，只读展示。
 * - status create 默认 1（已提交）。
 */

/** 评价分页：GET /admin-api/supplier/evaluation/page */
export function pageEvaluations(
  query: SupplierEvaluationQuery
): Promise<PageResult<SupplierEvaluation>> {
  return request<PageResult<SupplierEvaluation>>({
    url: '/admin-api/supplier/evaluation/page',
    method: 'get',
    params: query
  })
}

/** 评价详情：GET /admin-api/supplier/evaluation/{id} */
export function getEvaluation(id: number): Promise<SupplierEvaluation> {
  return request<SupplierEvaluation>({
    url: `/admin-api/supplier/evaluation/${id}`,
    method: 'get'
  })
}

/** 新增评价：POST /admin-api/supplier/evaluation（返回 id） */
export function createEvaluation(data: Partial<SupplierEvaluation>): Promise<number> {
  return request<number>({
    url: '/admin-api/supplier/evaluation',
    method: 'post',
    data
  })
}

/**
 * 修改评价：PUT /admin-api/supplier/evaluation?id=
 *
 * id 走 query string（@RequestParam），非 path。body 为修改字段。
 */
export function updateEvaluation(
  id: number,
  data: Partial<SupplierEvaluation>
): Promise<void> {
  return request<void>({
    url: '/admin-api/supplier/evaluation',
    method: 'put',
    params: { id },
    data
  })
}

/** 删除评价：DELETE /admin-api/supplier/evaluation/{id} */
export function deleteEvaluation(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/supplier/evaluation/${id}`,
    method: 'delete'
  })
}
