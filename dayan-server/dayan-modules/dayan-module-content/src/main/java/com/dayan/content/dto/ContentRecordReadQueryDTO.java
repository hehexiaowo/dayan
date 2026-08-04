package com.dayan.content.dto;

import lombok.Data;

/**
 * 阅读记录查询入参。
 */
@Data
public class ContentRecordReadQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String contentCode;
    private String readerCode;
    private Integer readSource;
}
