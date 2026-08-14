package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 tool_info 对应实体（获客工具定义，平台共享表）。
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

    /** 工具类型（1=计算器, 2=测评, 3=表单, 4=其他） */
    private Integer toolType;

    /** 工具简介 */
    private String toolDesc;

    /** 图标（文字或图标名） */
    private String icon;

    /** 入口路径（端上页面路径） */
    private String entryPath;

    /** 工具配置（JSON 字符串，按工具自定义） */
    private String config;

    /** 可见端（逗号分隔：agent/client） */
    private String visibleScope;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=禁用, 1=启用） */
    private Integer status;

    /** 备注 */
    private String remark;
}
