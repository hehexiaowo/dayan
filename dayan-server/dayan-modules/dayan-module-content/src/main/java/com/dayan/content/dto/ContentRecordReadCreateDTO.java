package com.dayan.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 阅读记录创建入参（前端上报阅读事件）。
 *
 * <p>id 由 ASSIGN_ID 自动生成；{@code readerCode} 用于 UV 去重统计。
 */
@Data
public class ContentRecordReadCreateDTO {

    @NotBlank(message = "内容编码不能为空")
    private String contentCode;

    private String readerType;
    private String readerCode;

    /** 阅读时长(秒) */
    private Integer readDuration;
    /** 阅读进度(%) */
    private BigDecimal readProgress;
    /** 阅读来源 */
    private Integer readSource;
    private String ipAddress;
    private String deviceType;
    private LocalDateTime readTime;
}
