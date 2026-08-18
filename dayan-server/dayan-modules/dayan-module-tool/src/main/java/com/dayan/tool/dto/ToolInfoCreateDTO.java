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

    /** 工具类型（pension/gap/ai_creator/ai_qa） */
    @NotBlank(message = "工具类型不能为空")
    @Size(max = 32)
    private String toolType;

    @Size(max = 500)
    private String toolDesc;

    @Size(max = 100)
    private String icon;

    /** 入口路径（端上页面路径） */
    @NotBlank(message = "入口路径不能为空")
    @Size(max = 200)
    private String entryPath;

    /** 工具配置 JSON */
    private String configJson;

    /** 可见端（逗号分隔：agent/client），默认 agent */
    @Size(max = 50)
    private String visibleScope;

    private Integer sortOrder;

    /** 状态：0=禁用 / 1=启用，默认 1 */
    private Integer status;

    @Size(max = 500)
    private String remark;
}
