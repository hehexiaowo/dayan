package com.dayan.agent.dto;

import lombok.Data;

/**
 * 代理人账号查询入参。
 */
@Data
public class AgentAccountQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    /** 渠道编码（可显式覆盖上下文） */
    private String channelCode;
    /** 代理人编码（精确） */
    private String agentCode;
    /** 用户名/手机号（模糊） */
    private String keyword;
    /** 账号状态 */
    private Integer accountStatus;
}
