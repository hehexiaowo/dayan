package com.dayan.content.dto;

import lombok.Data;

/**
 * 分享记录查询入参。
 */
@Data
public class ContentRecordShareQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String contentCode;
    private String sharerCode;
    private Integer shareChannel;
}
