import { request } from '@/utils/request'
import type { ParkScore } from '@/types/park'

/**
 * 机构评分（ParkScore）接口封装。
 *
 * 对应后端 /admin-api/park/score/*。
 *
 * 评分从 park_info 拆出独立表，避免高频写评分影响机构主表编辑。
 * 评分与机构一对一（upsert 语义），不使用 id 而以 parkCode 为键。
 */

/** 评分详情：GET /admin-api/park/score/{parkCode} */
export function getScore(parkCode: string): Promise<ParkScore> {
  return request<ParkScore>({
    url: `/admin-api/park/score/${parkCode}`,
    method: 'get'
  })
}

/** 保存评分（upsert）：PUT /admin-api/park/score/{parkCode} */
export function updateScore(parkCode: string, data: Partial<ParkScore>): Promise<void> {
  return request<void>({
    url: `/admin-api/park/score/${parkCode}`,
    method: 'put',
    data
  })
}
