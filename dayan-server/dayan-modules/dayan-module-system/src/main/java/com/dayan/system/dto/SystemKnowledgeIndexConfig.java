package com.dayan.system.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识仓库索引配置（存 system_knowledge_repo.config_json）。
 *
 * <p>切分/向量/重排/改写参数仅在创建（懒建库 initIndex）时应用；已建库仅
 * denseTopK/sparseTopK/rerankMinScore 可更新（UpdateIndex 支持面）。
 */
@Data
public class SystemKnowledgeIndexConfig {

    /** 切分方式：null=智能切分；"regex"=自定义（分隔符切分） */
    private String chunkMode;

    /** 正则分隔符（仅 chunkMode=regex 生效） */
    private String separator;

    /** 切块长度 1-6000（默认 500） */
    private Integer chunkSize;

    /** 重叠 0-1024（默认 100，必须 < chunkSize） */
    private Integer overlapSize;

    /** 向量模型：text-embedding-v3 / text-embedding-v4（空=服务端默认） */
    private String embeddingModel;

    /** 重排模型：qwen3-rerank / qwen3-rerank-hybrid（空=服务端默认） */
    private String rerankModel;

    /** 重排模式：qa / similar / custom（默认 qa） */
    private String rerankMode;

    /** 相似度阈值 0.01-1.00（默认 0.01） */
    private Double rerankMinScore;

    /** 多轮改写（默认 true） */
    private Boolean enableRewrite;

    /** 稠密召回数（仅已建库 UpdateIndex 用） */
    private Integer denseTopK;

    /** 稀疏召回数（仅已建库 UpdateIndex 用） */
    private Integer sparseTopK;

    /** 校验配置合法性；非法抛 IllegalArgumentException */
    public void validate() {
        if (chunkSize != null && (chunkSize < 1 || chunkSize > 6000)) {
            throw new IllegalArgumentException("切块长度需在 1-6000 之间");
        }
        if (overlapSize != null && (overlapSize < 0 || overlapSize > 1024)) {
            throw new IllegalArgumentException("重叠长度需在 0-1024 之间");
        }
        if (chunkSize != null && overlapSize != null && overlapSize >= chunkSize) {
            throw new IllegalArgumentException("重叠长度必须小于切块长度");
        }
        if (rerankMinScore != null && (rerankMinScore < 0.01 || rerankMinScore > 1.0)) {
            throw new IllegalArgumentException("相似度阈值需在 0.01-1.00 之间");
        }
        if (chunkMode != null && !"regex".equals(chunkMode)) {
            throw new IllegalArgumentException("切分方式仅支持智能切分（不传）或 regex（自定义）");
        }
        if ("regex".equals(chunkMode) && (separator == null || separator.isBlank())) {
            throw new IllegalArgumentException("自定义切分必须填写分隔符");
        }
    }

    /** 转 CreateIndex 扩展参数（仅已设置且建库相关的字段；TopK 不在此列） */
    public Map<String, String> toQueryMap() {
        Map<String, String> map = new HashMap<>();
        if (chunkMode != null) map.put("chunkMode", chunkMode);
        if (separator != null && !separator.isBlank()) map.put("Separator", separator);
        if (chunkSize != null) map.put("ChunkSize", String.valueOf(chunkSize));
        if (overlapSize != null) map.put("OverlapSize", String.valueOf(overlapSize));
        if (embeddingModel != null && !embeddingModel.isBlank()) map.put("EmbeddingModelName", embeddingModel);
        if (rerankModel != null && !rerankModel.isBlank()) map.put("RerankModelName", rerankModel);
        if (rerankMode != null && !rerankMode.isBlank()) map.put("RerankMode", rerankMode);
        if (rerankMinScore != null) map.put("RerankMinScore", String.valueOf(rerankMinScore));
        if (enableRewrite != null) map.put("EnableRewrite", String.valueOf(enableRewrite));
        return map;
    }
}
