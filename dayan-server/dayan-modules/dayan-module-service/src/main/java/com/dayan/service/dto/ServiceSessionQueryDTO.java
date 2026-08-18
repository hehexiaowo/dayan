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
    /** 服务项目编码（服务类型=服务项目维度筛选） */
    private String itemCode;
    private String parkCode;
    private String agentCode;
    private String channelCode;
    private Integer sessionStatus;
    private String subStatus;
    private Integer sourceType;
}
