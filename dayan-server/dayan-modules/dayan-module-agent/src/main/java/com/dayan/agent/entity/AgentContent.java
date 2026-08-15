package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代理人 AI 生成个人内容（agent_content）。
 *
 * <p>生成结果快照：素材引用（范文/知识库文档/商品）以 JSON 冗余，
 * 素材侧变更不影响历史内容展示。channel_code 租户自动隔离。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_content")
public class AgentContent extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 代理人编码 */
    private String agentCode;

    /** 渠道编码 */
    private String channelCode;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 封面（OSS key） */
    private String coverImage;

    /** 形态（1=图文 2=朋友圈 3=视频脚本） */
    private Integer contentType;

    /** 正文（图文=HTML；朋友圈=纯文本；脚本=结构化文本） */
    private String contentBody;

    /** 风格档位（professional/warm/authoritative/colloquial） */
    private String styleCode;

    /** 参考范文 contentCode */
    private String refContentCode;

    /** 勾选知识库文档 JSON（[{fileId,fileName}]） */
    private String refKbFiles;

    /** 勾选商品 codes JSON（["GDxxx"]） */
    private String refGoodsCodes;

    /** 目标读者（children=子女决策者/elder=老人本人/general=通用） */
    private String audience;

    /** 状态（1=正常） */
    private Integer status;
}
