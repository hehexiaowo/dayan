package com.dayan.tool.dto;

import lombok.Data;

/** 我的创作列表查询 */
@Data
public class ToolAiCreatorQueryDTO {
    private long current = 1;
    private long size = 10;
    /** 阶段过滤（可空） */
    private String status;
}
