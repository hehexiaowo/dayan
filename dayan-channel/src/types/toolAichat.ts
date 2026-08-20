/** 渠道端问答人物（对齐后端 ToolChannelPersonaVO） */
export interface ToolChannelPersona {
  /** 工具实例编码（TL 前缀） */
  toolCode: string
  /** 人物名称 */
  personaName: string
  /** 工具简介 */
  toolDesc?: string
  /** admin 全局绑定的知识库 ID（只读） */
  globalRepoIds: number[]
  /** 本渠道补充的知识库 ID（可编辑） */
  channelRepoIds: number[]
}

/** 可补充知识库选项（对齐后端 SystemKnowledgeRepoVO 子集） */
export interface ToolChannelRepoOption {
  id: number
  repoName: string
  channelName?: string
  channelShortName?: string
}
