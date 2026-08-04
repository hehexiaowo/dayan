package com.dayan.service.dto;

import lombok.Data;

/**
 * 探访记录（service_visit_record）查询入参（按 butlerCode/parkCode/visitDate 过滤）。
 */
@Data
public class ServiceVisitRecordQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String butlerCode;
    private String parkCode;
    private Integer visitPurpose;
    private Integer status;
}
