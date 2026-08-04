package com.dayan.butler.dto;

import lombok.Data;

/**
 * 管家评价查询入参。
 */
@Data
public class ButlerRatingQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String butlerCode;
    private String clientCode;
    private String serviceRecordCode;
    private Integer rating;
    private Integer status;
}
