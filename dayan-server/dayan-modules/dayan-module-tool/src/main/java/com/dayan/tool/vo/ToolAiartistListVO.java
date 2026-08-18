package com.dayan.tool.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** AI 创作列表项 */
@Data
public class ToolAiartistListVO {
    private Long id;
    private String purpose;
    private Integer contentType;
    private String topic;
    private String selectedTitle;
    private String status;
    private LocalDateTime updatedAt;
}
