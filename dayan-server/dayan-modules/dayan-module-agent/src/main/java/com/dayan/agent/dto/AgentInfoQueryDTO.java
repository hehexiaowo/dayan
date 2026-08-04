package com.dayan.agent.dto;

import lombok.Data;

/**
 * 代理人查询入参（按 channel_code 隔离，由租户拦截器自动追加）。
 */
@Data
public class AgentInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    /** 代理人编码 */
    private String agentCode;
    /** 渠道编码（可显式覆盖上下文） */
    private String channelCode;
    /** 代理人姓名（模糊） */
    private String fullName;
    /** 手机号（模糊） */
    private String phone;
    /** 等级 */
    private Integer agentLevel;
    /** 是否认证 */
    private Integer isCertified;
    /** 状态 */
    private Integer status;
}
