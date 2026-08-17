package com.dayan.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代理人分享记录 VO。
 */
@Data
public class AgentShareRecordVO {

    private Long id;
    private String shareCode;
    private String agentCode;
    private Integer shareType;
    private String bizCode;
    private Integer shareChannel;
    private String clientCode;
    /** 客户姓名 join client_info.full_name */
    private String clientName;
    private Integer viewCount;
    private LocalDateTime shareTime;
    private LocalDateTime createdAt;
}
