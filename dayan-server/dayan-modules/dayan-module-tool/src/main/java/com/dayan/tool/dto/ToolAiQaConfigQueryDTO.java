package com.dayan.tool.dto;

import lombok.Data;

@Data
public class ToolAiQaConfigQueryDTO {
    private String personaName;
    private Integer status;
    private Integer current = 1;
    private Integer size = 10;
}
