package com.dayan.agent.dto;

import lombok.Data;

/**
 * 代理人分享记录查询入参。
 */
@Data
public class AgentShareRecordQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String agentCode;
    private String shareCode;
    /** 分享类型 */
    private Integer shareType;
    /** 接收客户编码 */
    private String clientCode;
}
