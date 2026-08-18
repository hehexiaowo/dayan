package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 表 tool_ai_qa_session 对应实体（AI 问答会话，按代理人归属）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_ai_qa_session")
public class ToolAiQaSession extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属工具实例（tool_info.tool_code） */
    private String toolCode;

    /** 会话编码（QAS+5位序列） */
    private String sessionCode;

    /** 所属人物 ID */
    private Long configId;

    /** 人物编码（冗余） */
    private String configCode;

    /** 人物名（冗余） */
    private String personaName;

    /** 归属代理人 */
    private String agentCode;

    /** 渠道编码（租户隔离） */
    private String channelCode;

    /** 会话标题 */
    private String title;

    /** 消息数 */
    private Integer messageCount;

    /** 最近消息时间 */
    private LocalDateTime lastMessageAt;
}
