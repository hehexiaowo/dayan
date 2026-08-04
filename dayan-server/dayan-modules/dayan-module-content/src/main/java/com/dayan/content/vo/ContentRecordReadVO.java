package com.dayan.content.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 阅读记录视图对象。
 */
@Data
public class ContentRecordReadVO {

    private Long id;
    private String contentCode;
    private String readerType;
    private String readerCode;
    private Integer readDuration;
    private BigDecimal readProgress;
    private Integer readSource;
    private String ipAddress;
    private String deviceType;
    private LocalDateTime readTime;
    private LocalDateTime createdAt;
}
