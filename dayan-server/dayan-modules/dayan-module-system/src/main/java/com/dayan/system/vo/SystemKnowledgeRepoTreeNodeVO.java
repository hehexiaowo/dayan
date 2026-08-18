package com.dayan.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识仓库树节点 VO（渠道树 + 每节点知识库归属）。
 *
 * <p>渠道可看到「自己 + 全部后代」的树形知识库：
 * <ul>
 *   <li>{@code repo}：本渠道独立配置的知识仓库（无则为 null）；</li>
 *   <li>{@code effectiveRepo}：本渠道实际可用的仓库 = 独立库，否则沿祖先链最近建有仓库的渠道的库（继承）；</li>
 *   <li>{@code inheritedFrom/inheritedFromName}：继承来源渠道（独立配置时为 null）。</li>
 * </ul>
 */
@Data
public class SystemKnowledgeRepoTreeNodeVO {

    /** 渠道编码 */
    private String channelCode;

    /** 渠道全称 */
    private String fullName;

    /** 渠道简称 */
    private String shortName;

    /** 层级（1=一级） */
    private Integer level;

    /** 本渠道独立配置的知识仓库（无则为 null） */
    private SystemKnowledgeRepoVO repo;

    /** 实际可用仓库（独立库，或沿祖先链继承的最近仓库；无则为 null） */
    private SystemKnowledgeRepoVO effectiveRepo;

    /** 继承来源渠道编码（独立配置或未继承时为 null） */
    private String inheritedFrom;

    /** 继承来源渠道简称（独立配置或未继承时为 null） */
    private String inheritedFromName;

    /** 子节点（后代渠道） */
    private List<SystemKnowledgeRepoTreeNodeVO> children = new ArrayList<>();
}
