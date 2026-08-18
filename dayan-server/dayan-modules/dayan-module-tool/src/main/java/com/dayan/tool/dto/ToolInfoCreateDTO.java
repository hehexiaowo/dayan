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

    /** 工具类型（pension/gap/aiartist/aichat） */
    @NotBlank(message = "工具类型不能为空")
    @Size(max = 32)
    private String toolType;

    @Size(max = 500)
    private String toolDesc;

    /** 工具配置 JSON */
    private String configJson;

    /** 状态：0=禁用 / 1=启用，默认 1 */
    private Integer status;

    @Size(max = 500)
    private String remark;
}
