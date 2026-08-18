package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 计算器使用记录保存入参。
 */
@Data
public class ToolCalculatorRecordCreateDTO {

    /** 所属工具实例，空则按类型回落预置编码 */
    private String toolCode;

    @NotBlank(message = "计算输入不能为空")
    private String inputJson;

    @NotBlank(message = "计算结果不能为空")
    private String resultJson;
}
