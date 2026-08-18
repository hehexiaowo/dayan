package com.dayan.tool.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ToolAichatSessionVO {
    private Long id;
    private String sessionCode;
    private String toolCode;
    private String personaName;
    private String title;
    private Integer messageCount;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
