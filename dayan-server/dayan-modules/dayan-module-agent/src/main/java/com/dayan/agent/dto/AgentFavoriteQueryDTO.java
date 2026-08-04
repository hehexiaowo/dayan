package com.dayan.agent.dto;

import lombok.Data;

/**
 * 代理人收藏查询入参。
 */
@Data
public class AgentFavoriteQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String agentCode;
    private Integer targetType;
    private String targetCode;
}
