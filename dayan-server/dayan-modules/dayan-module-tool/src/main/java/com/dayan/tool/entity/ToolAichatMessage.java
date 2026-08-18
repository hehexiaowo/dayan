package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 tool_aichat_message 对应实体（AI 问答消息，含引用溯源）。
 */
@Data
@EqualsAndHashCode
@TableName("tool_aichat_message")
public class ToolAichatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话编码 */
    private String sessionCode;

    /** 角色：user/assistant */
    private String role;

    /** 消息正文 */
    private String content;

    /** 引用 JSON（assistant 有） */
    private String citations;
}
