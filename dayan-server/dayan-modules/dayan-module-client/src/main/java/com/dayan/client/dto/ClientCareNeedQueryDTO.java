package com.dayan.client.dto;

import lombok.Data;

/**
 * 客户照护需求评估查询入参。
 */
@Data
public class ClientCareNeedQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String clientCode;
    private String butlerCode;
    private Integer careLevel;
    private Integer status;
}
