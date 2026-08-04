package com.dayan.service.dto;

import lombok.Data;

/**
 * 回访品控（service_equity_followup）查询入参（按 sessionCode/arrangeCode 过滤）。
 */
@Data
public class ServiceEquityFollowupQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sessionCode;
    private String followupCode;
    private String arrangeCode;
    private String clientCode;
    private String butlerCode;
    private Integer followupType;
    private Integer status;
}
