package com.dayan.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 供应商开放平台配置创建入参。
 *
 * <p>{@code appSecret} / {@code webhookSecret} 以明文传入，由 Service 层用 AES-256-GCM
 * 加密后存储。
 */
@Data
public class SupplierOpenPlatformCreateDTO {

    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    @NotBlank(message = "平台名称不能为空")
    private String platformName;

    private String apiBaseUrl;
    private String appKey;

    /** 明文密钥，存储前加密 */
    private String appSecret;

    private String callbackUrl;

    /** 明文 Webhook 密钥，存储前加密 */
    private String webhookSecret;

    /** 协议类型 */
    private Integer protocolType;
    /** 认证方式 */
    private Integer authType;
    /** 数据格式 */
    private Integer dataFormat;
    private String apiVersion;
    private Integer rateLimit;
    private Integer timeout;
    private Integer retryCount;
    private String extraConfig;
    private Integer status;
}
