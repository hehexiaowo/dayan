package com.dayan.tool.vo;

import lombok.Data;

import java.util.List;

@Data
public class ToolAichatMessageVO {
    private Long id;
    private String sessionCode;
    private String role;
    private String content;
    private List<ToolAichatChatResultVO.Citation> citations;
}
