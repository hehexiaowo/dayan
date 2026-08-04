package com.dayan.agent.dto;

import lombok.Data;

/**
 * 代理人业绩查询入参。
 */
@Data
public class AgentPerformanceQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    /** 渠道编码（可显式覆盖上下文） */
    private String channelCode;
    private String agentCode;
    /** 统计周期 */
    private Integer periodType;
    /** 周期值（如 202608） */
    private String periodValue;
}
