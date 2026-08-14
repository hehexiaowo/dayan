package com.dayan.tool.dto;

import lombok.Data;

/**
 * 工具查询入参。
 */
@Data
public class ToolInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String toolCode;
    private String toolName;
    private Integer toolType;
    private Integer status;
}
