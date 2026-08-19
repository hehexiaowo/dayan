package com.dayan.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识仓库创建 DTO。
 *
 * <p>mode=create：调用百炼 CreateIndex + SubmitIndexJob 新建远端索引；
 * mode=bind：绑定百炼控制台已建索引（手填 IndexId），不做远端创建。
 */
@Data
public class SystemKnowledgeRepoCreateDTO {

    /** 仓库名称（如：大雁养老平台知识库 / xx渠道知识库） */
    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 100, message = "仓库名称最长 100 字")
    private String repoName;

    /** 归属类型（1=平台大雁养老 2=渠道） */
    @NotNull(message = "仓库归属类型不能为空")
    private Integer repoType;

    /** 渠道编码（repoType=2 时必填） */
    private String channelCode;

    /** 创建方式：create=新建远端索引（默认） bind=绑定已有索引 */
    private String mode;

    /** 绑定的百炼远端索引 ID（mode=bind 时必填） */
    private String indexId;

    /** 仓库描述 */
    @Size(max = 255, message = "描述最长 255 字")
    private String description;

    /** 索引配置（切分方式/检索参数；mode=bind 时忽略） */
    private SystemKnowledgeIndexConfig indexConfig;

    /** 排序号 */
    private Integer sortOrder;
}
