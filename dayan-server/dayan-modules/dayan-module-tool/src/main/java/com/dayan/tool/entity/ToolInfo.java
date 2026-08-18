package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 tool_info 对应实体（工具实例配置，平台共享表）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_info")
public class ToolInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工具编码（TL+5位序列） */
    private String toolCode;

    /** 工具名称 */
    private String toolName;

    /** 工具类型（pension/gap/ai_creator/ai_qa） */
    private String toolType;

    /** 工具简介 */
    private String toolDesc;

    /** 图标（文字或图标名） */
    private String icon;

    /** 入口路径（端上页面路径） */
    private String entryPath;

    /** 工具配置 JSON（按类型承载提示词/默认值等） */
    private String configJson;

    /** 可见端（逗号分隔：agent/client） */
    private String visibleScope;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=禁用, 1=启用） */
    private Integer status;

    /** 备注 */
    private String remark;
}
