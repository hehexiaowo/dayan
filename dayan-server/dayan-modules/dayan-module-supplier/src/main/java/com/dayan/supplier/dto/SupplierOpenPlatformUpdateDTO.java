package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商开放平台配置更新入参（{@code id} 由路径参数提供）。
 */
@Data
public class SupplierOpenPlatformUpdateDTO {

    private String platformName;
    private String apiBaseUrl;
    private String appKey;
    /** 明文密钥，非空才轮换（避免误清空） */
    private String appSecret;
    private String callbackUrl;
    /** 明文 Webhook 密钥，非空才轮换 */
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
}
