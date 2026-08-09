package com.dayan.goods.dto;

import lombok.Data;

/**
 * 服务项目查询入参（分页 + 多条件）。
 */
@Data
public class ServiceItemQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String itemCode;
    private String itemName;
    private Integer itemCategory;
    private Integer itemSubtype;
    private Integer status;
}
