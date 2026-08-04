package com.dayan.agent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 代理人业绩 VO。
 */
@Data
public class AgentPerformanceVO {

    private Long id;
    private String agentCode;
    private String channelCode;
    private Integer periodType;
    private String periodValue;
    private Integer equityGrantCount;
    private BigDecimal equityGrantAmount;
    private Integer sceneOrderCount;
    private BigDecimal sceneOrderAmount;
    private Integer courseOrderCount;
    private BigDecimal courseOrderAmount;
    private LocalDateTime createdAt;
}
