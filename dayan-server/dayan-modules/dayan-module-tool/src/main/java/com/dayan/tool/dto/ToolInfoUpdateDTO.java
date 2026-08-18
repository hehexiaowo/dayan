package com.dayan.tool.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工具修改入参（toolCode 不可改）。
 */
@Data
public class ToolInfoUpdateDTO {

    @Size(max = 100)
    private String toolName;

    @Size(max = 32)
    private String toolType;

    @Size(max = 500)
    private String toolDesc;

    private String configJson;

    private Integer status;

    @Size(max = 500)
    private String remark;
}
