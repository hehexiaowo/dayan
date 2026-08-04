package com.dayan.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代理人收藏 VO。
 */
@Data
public class AgentFavoriteVO {

    private Long id;
    private String agentCode;
    private Integer targetType;
    private String targetCode;
    private LocalDateTime createdAt;
}
