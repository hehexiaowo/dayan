package com.dayan.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代理人-客户绑定关系 VO。
 */
@Data
public class AgentClientRelVO {

    private Long id;
    private String agentCode;
    private String clientCode;
    private Integer bindType;
    private LocalDateTime bindTime;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
