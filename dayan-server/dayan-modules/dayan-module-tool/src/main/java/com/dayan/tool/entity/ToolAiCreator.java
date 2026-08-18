package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 创作项目（tool 域六阶段流水线状态）。
 *
 * <p>各阶段产物以 JSON 字符串存 JSON 列（materials/material_refs/fact_digest/strategy/
 * titles/outline/audit_log/scores/images/warnings），body 为正文 LONGTEXT。
 * 素材由前端聚合提交（快照存 materials，digest 阶段一次性消费），后续阶段基于 fact_digest。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_ai_creator")
public class ToolAiCreator extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 创建代理人编码（登录上下文注入，防越权） */
    private String agentCode;
    /** 渠道编码（租户隔离） */
    private String channelCode;
    /** product=产品宣传/park=机构推荐/science=科普获客 */
    private String purpose;
    /** 1图文 2朋友圈 3视频脚本 4小红书 */
    private Integer contentType;
    /** 写作风格（professional/warm/authoritative/colloquial） */
    private String styleCode;
    /** 目标读者（children/elder/general） */
    private String audience;
    /** 主题/切入话题 */
    private String topic;
    /** JSON：素材引用 {refContentCode,kbFiles[{fileId,fileName}],goods[{code,name}],parks[{code,name}]} */
    private String materialRefs;
    /** JSON：素材快照 [{type,title,text}]（前端供材，digest 阶段一次性消费） */
    private String materials;
    /** 见 ToolAiCreatorPhase 常量 */
    private String status;
    /** JSON：hardFacts/softPoints/missing */
    private String factDigest;
    /** JSON：策略面板（含 coreExecutionPrompt） */
    private String strategy;
    /** JSON：[{title,tag,viralScore,reasoning}] */
    private String titles;
    /** 选定标题 */
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
