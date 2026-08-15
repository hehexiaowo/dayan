import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { MessageRecord, MessageRecordQuery, MessageTemplate, MessageTemplateQuery } from '@/types/message'

/**
 * 消息管理接口封装（消息模板 + 发送记录）。
 *
 * 对应后端 SystemMessageTemplateAdminController（/admin-api/message-templates/*）
 * 与 SystemMessageAdminController（/admin-api/messages/*，只读审计）。
 */

/** 消息模板分页：GET /admin-api/message-templates */
export function pageMessageTemplates(query: MessageTemplateQuery): Promise<PageResult<MessageTemplate>> {
  return request<PageResult<MessageTemplate>>({
    url: '/admin-api/message-templates',
    method: 'get',
    params: {
      templateCode: query.templateCode || undefined,
      templateName: query.templateName || undefined,
      bizType: query.bizType || undefined,
      channelType: query.channelType ?? undefined,
      status: query.status ?? undefined,
      current: query.current,
      size: query.size
    }
  })
}

/** 新增消息模板：POST /admin-api/message-templates */
export function createMessageTemplate(data: Partial<MessageTemplate>): Promise<number> {
  return request<number>({ url: '/admin-api/message-templates', method: 'post', data })
}

/** 修改消息模板（模板编码不可改）：PUT /admin-api/message-templates/{id} */
export function updateMessageTemplate(id: number, data: Partial<MessageTemplate>): Promise<void> {
  return request<void>({ url: `/admin-api/message-templates/${id}`, method: 'put', data })
}

/** 删除消息模板：DELETE /admin-api/message-templates/{id} */
export function deleteMessageTemplate(id: number): Promise<void> {
  return request<void>({ url: `/admin-api/message-templates/${id}`, method: 'delete' })
}

/** 发送记录分页：GET /admin-api/messages */
export function pageMessageRecords(query: MessageRecordQuery): Promise<PageResult<MessageRecord>> {
  return request<PageResult<MessageRecord>>({
    url: '/admin-api/messages',
    method: 'get',
    params: {
      bizType: query.bizType || undefined,
      channelType: query.channelType ?? undefined,
      sendStatus: query.sendStatus ?? undefined,
      targetType: query.targetType || undefined,
      templateCode: query.templateCode || undefined,
      startTime: query.startTime || undefined,
      endTime: query.endTime || undefined,
      current: query.current,
      size: query.size
    }
  })
}

/** 发送记录详情：GET /admin-api/messages/{id} */
export function getMessageRecordDetail(id: number): Promise<MessageRecord> {
  return request<MessageRecord>({ url: `/admin-api/messages/${id}`, method: 'get' })
}
