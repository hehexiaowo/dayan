package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工具创建入参。
 *
 * <p>{@code toolCode} 由系统生成（TL + 5 位序列）。
 */
@Data
public class ToolInfoCreateDTO {

    @NotBlank(message = "工具名称不能为空")
    @Size(max = 100)
    private String toolName;

    /** 工具类型（1=计算器, 2=测评, 3=表单, 4=其他） */
    private Integer toolType;

    @Size(max = 500)
    private String toolDesc;

    @Size(max = 100)
    private String icon;

    /** 入口路径（端上页面路径） */
    @NotBlank(message = "入口路径不能为空")
    @Size(max = 200)
    private String entryPath;

    /** 工具配置（JSON 字符串） */
    private String config;

    /** 可见端（逗号分隔：agent/client），默认 agent */
    @Size(max = 50)
    private String visibleScope;

    private Integer sortOrder;

    /** 状态：0=禁用 / 1=启用，默认 1 */
    private Integer status;

    @Size(max = 500)
    private String remark;
}
