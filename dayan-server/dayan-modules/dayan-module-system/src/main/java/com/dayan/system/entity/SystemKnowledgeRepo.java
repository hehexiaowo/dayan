package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 百炼知识仓库实体。
 *
 * <p>只存「仓库」本地元数据（名称/归属/百炼远端索引 ID）；文档与解析状态
 * 以百炼远端为准（ListIndexDocuments / DescribeFile 实时代理），本地不建文档表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_knowledge_repo")
public class SystemKnowledgeRepo extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 仓库编码（KB+序号，唯一） */
    private String repoCode;

    /** 仓库名称（如：大雁养老平台知识库 / xx渠道知识库） */
    private String repoName;

    /** 归属类型（1=平台大雁养老 2=渠道） */
    private Integer repoType;

    /** 渠道编码（repo_type=2 时关联 channel_info.channel_code） */
    private String channelCode;

    /** 百炼远端索引 ID（CreateIndex 返回 Data.Id） */
    private String indexId;

    /** 建库索引构建任务 ID（SubmitIndexJob 返回 Data.Id） */
    private String buildJobId;

    /** 仓库描述 */
    private String description;

    /** 文档数（以百炼远端为准，sync 时刷新） */
    private Integer docCount;

    /** 状态（0=未初始化/构建中 1=正常 2=远端异常） */
    private Integer status;

    /** 最近同步时间 */
    private LocalDateTime lastSyncAt;

    /** 排序号 */
    private Integer sortOrder;
}
