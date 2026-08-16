package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 创作项目（六阶段流水线状态）。
 *
 * <p>各阶段产物以 JSON 字符串存 JSON 列（material_refs/fact_digest/strategy/titles/
 * outline/audit_log/scores/images/warnings），body 为正文 LONGTEXT。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_creation_project")
public class AiCreationProject extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String agentCode;
    private String channelCode;
    /** product=产品宣传/park=机构推荐/science=科普获客 */
    private String purpose;
    /** 1图文 2朋友圈 3视频脚本 4小红书 */
    private Integer contentType;
    private String styleCode;
    private String audience;
    private String topic;
    /** JSON：refContentCode/kbFileIds/goodsCodes/parkCodes */
    private String materialRefs;
    /** 见 AiProjectPhase 常量 */
    private String status;
    /** JSON：hardFacts/softPoints/missing */
    private String factDigest;
    /** JSON：策略面板（含 coreExecutionPrompt） */
    private String strategy;
    /** JSON：[{title,tag,viralScore,reasoning}] */
    private String titles;
    private String selectedTitle;
    /** JSON：coverImage + nodes[] */
    private String outline;
    /** 正文（图文=HTML 片段；含 [AI_IMAGE_*] 占位符） */
    private String body;
    /** JSON：[{type,message}] */
    private String auditLog;
    /** JSON：五维打分 */
    private String scores;
    /** JSON：[{placeholder,size,prompt,promptZh,fileKey,url,status,error}] */
    private String images;
    /** JSON：[string] */
    private String warnings;
}
