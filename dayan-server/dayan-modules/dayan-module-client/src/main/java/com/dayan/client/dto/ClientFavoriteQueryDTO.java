package com.dayan.client.dto;

import lombok.Data;

/**
 * 客户收藏查询入参。
 */
@Data
public class ClientFavoriteQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String clientCode;
    private Integer targetType;
    private String targetCode;
}
