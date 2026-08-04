package com.dayan.butler.dto;

import lombok.Data;

/**
 * 管家服务记录查询入参。
 */
@Data
public class ButlerServiceRecordQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String butlerCode;
    private String clientCode;
    private Integer serviceType;
    private Integer communicateWay;
    private Integer status;
}
