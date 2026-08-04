package com.dayan.service.dto;

import lombok.Data;

/**
 * 服务会话（service_session）查询入参。
 */
@Data
public class ServiceSessionQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sessionCode;
    private String equityCode;
    private String clientCode;
    private String butlerCode;
    private Integer serviceType;
    private String parkCode;
    private String agentCode;
    private String channelCode;
    private Integer sessionStatus;
    private String subStatus;
    private Integer sourceType;
}
