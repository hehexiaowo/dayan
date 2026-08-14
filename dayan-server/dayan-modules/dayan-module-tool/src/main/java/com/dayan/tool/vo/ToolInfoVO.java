package com.dayan.tool.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工具 VO。
 */
@Data
public class ToolInfoVO {

    private Long id;
    /** 工具编码（TL 前缀） */
    private String toolCode;
    /** 工具名称 */
    private String toolName;
    /** 工具类型（1=计算器, 2=测评, 3=表单, 4=其他） */
    private Integer toolType;
    /** 工具简介 */
    private String toolDesc;
    /** 图标 */
    private String icon;
    /** 入口路径 */
    private String entryPath;
    /** 工具配置（JSON 字符串） */
    private String config;
    /** 可见端（逗号分隔：agent/client） */
    private String visibleScope;
    /** 排序号 */
    private Integer sortOrder;
    /** 状态：0=禁用 / 1=启用 */
    private Integer status;
    /** 备注 */
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
