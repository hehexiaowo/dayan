package com.dayan.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代理人 AI 生成内容 VO。
 */
@Data
public class AgentContentVO {

    private Long id;
    private String agentCode;
    private String channelCode;
    private String title;
    private String summary;
    private String coverImage;
    private Integer contentType;
    private String contentBody;
    private String styleCode;
    private String refContentCode;
    private String refKbFiles;
    private String refGoodsCodes;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
