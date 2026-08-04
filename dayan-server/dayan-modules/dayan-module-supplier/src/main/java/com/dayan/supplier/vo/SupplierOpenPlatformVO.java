package com.dayan.supplier.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商开放平台配置视图对象。
 *
 * <p>密钥类字段（appSecret / webhookSecret）出参脱敏为 {@code ***}，明文不回传。
 */
@Data
public class SupplierOpenPlatformVO {

    private Long id;
    private String supplierCode;
    private String platformName;
    private String apiBaseUrl;
    private String appKey;
    /** 脱敏占位，明文不回传 */
    private String appSecret;
    private String callbackUrl;
    /** 脱敏占位，明文不回传 */
    private String webhookSecret;
    private Integer protocolType;
    private Integer authType;
    private Integer dataFormat;
    private String apiVersion;
    private Integer rateLimit;
    private Integer timeout;
    private Integer retryCount;
    private String extraConfig;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
