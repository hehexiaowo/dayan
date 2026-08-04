package com.dayan.equity.dto;

import lombok.Data;

/**
 * 权益模板查询入参（分页 + 多条件）。
 */
@Data
public class EquityTemplateQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String templateCode;
    private String templateName;
    private Integer equityType;
    private Integer equityLevel;
    private Integer status;
}
