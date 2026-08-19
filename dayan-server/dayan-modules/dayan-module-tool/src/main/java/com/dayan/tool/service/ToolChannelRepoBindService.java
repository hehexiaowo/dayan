package com.dayan.tool.service;

import java.util.List;

/**
 * 渠道问答人物补充知识库绑定服务。
 *
 * <p>合并规则：有效知识库 = admin 全局 repoIds ∪ 渠道补充 repoIds（去重保序）。
 * 无渠道上下文时退化为仅全局。</p>
 */
public interface ToolChannelRepoBindService {

    /** 某渠道对某人物补充的知识库 ID 列表（无则空列表） */
    List<Long> listRepoIds(String toolCode, String channelCode);

    /** 合并有效知识库：全局 + 当前渠道补充（去重保序；无渠道上下文退化为仅全局） */
    List<Long> mergeRepoIds(String toolCode, List<Long> globalRepoIds);

    /**
     * 保存渠道补充（全量替换：删除旧行后插入新集合）。
     * 校验：每个 repo 必须属于该渠道自己或后代渠道名下（复用 SystemKnowledgeRepoService.listChannelScopeRepos），
     * 范围外抛 BusinessException。
     */
    void saveChannelRepos(String toolCode, String channelCode, List<Long> repoIds);
}
