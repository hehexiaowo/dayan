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
    /** 渠道隔离用：本渠道已配置的内容编码集合 */
    private java.util.List<String> contentCodes;
}
