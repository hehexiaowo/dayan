package com.dayan.content.dto;

import lombok.Data;

/**
 * 多媒体资源查询入参。
 */
@Data
public class ContentMediaQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String contentCode;
    private Integer mediaType;
}
