package com.dayan.service.dto;

import lombok.Data;

/**
 * 服务评价（service_evaluation）查询入参（按 sessionCode/clientCode/butlerCode 过滤）。
 */
@Data
public class ServiceEvaluationQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sessionCode;
    private String clientCode;
    private String butlerCode;
    private String parkCode;
    private Integer isAnonymous;
    private Integer status;
}
