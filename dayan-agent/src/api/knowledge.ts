import request from '@/utils/request'
import type { KnowledgeDocOption, KnowledgeRepoOption } from '@/types/aiContent'

/** 当前渠道可见知识仓库（GET /agent-api/knowledge/repos） */
export function getMyKnowledgeRepos(): Promise<KnowledgeRepoOption[]> {
  return request<KnowledgeRepoOption[]>({ url: '/knowledge/repos', method: 'GET' })
}

/** 可见仓库文档合并列表（GET /agent-api/knowledge/docs?keyword=） */
export function getKnowledgeDocs(keyword?: string): Promise<KnowledgeDocOption[]> {
  return request<KnowledgeDocOption[]>({
    url: '/knowledge/docs',
    method: 'GET',
    data: keyword ? { keyword } : {}
  })
}
