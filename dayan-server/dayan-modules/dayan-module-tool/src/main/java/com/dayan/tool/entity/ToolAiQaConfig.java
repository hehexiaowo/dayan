package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 tool_ai_qa_config 对应实体（AI 问答人物配置，平台共享，无 channel_code）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_ai_qa_config")
public class ToolAiQaConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置编码（QAC+5位序列） */
    private String configCode;

    /** 人物名称 */
    private String personaName;

    /** 头像（文字或图标名） */
    private String icon;

    /** 图标色（blue/green/orange/red/gray） */
    private String iconColor;

    /** 人设描述（注入 system prompt） */
    private String systemPrompt;

    /** 开场白/欢迎语 */
    private String welcomeMsg;

    /** 推荐问题数组 JSON */
    private String recommendQuestions;

    /** 绑定知识库 ID 数组 JSON */
    private String repoIds;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=禁用 1=启用） */
    private Integer status;

    /** 备注 */
    private String remark;
}
