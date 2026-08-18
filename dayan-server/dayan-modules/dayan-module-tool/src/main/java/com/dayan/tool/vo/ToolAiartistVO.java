package com.dayan.tool.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** AI 创作项目详情（各 JSON 列解析后的聚合视图） */
@Data
public class ToolAiartistVO {
    private Long id;
    private String toolCode;
    private String purpose;
    private Integer contentType;
    private String styleCode;
    private String audience;
    private String topic;
    private ToolAiartistRefsVO materialRefs;
    private String status;
    private AiFactDigestVO factDigest;
    private AiStrategyVO strategy;
    private List<AiTitleVO> titles;
    private String selectedTitle;
    private AiOutlineVO outline;
    private String body;
    private List<AiAuditItemVO> auditLog;
    private AiScoresVO scores;
    private List<AiImageVO> images;
    private List<String> warnings;
    // 素材名回显（恢复草稿时展示）
    private String refContentName;
    private List<String> kbFileNames;
    private List<String> goodsNames;
    private List<String> parkNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
