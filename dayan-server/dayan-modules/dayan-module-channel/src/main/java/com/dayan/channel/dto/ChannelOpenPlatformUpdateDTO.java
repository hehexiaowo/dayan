package com.dayan.channel.dto;

import lombok.Data;

/**
 * 渠道开放平台配置更新入参。
 *
 * <p>{@code appSecret} 非空时视为需要轮换密钥，加密后覆盖存储。
 */
@Data
public class ChannelOpenPlatformUpdateDTO {

    private String platformName;
    private Integer dockType;
    private String apiBaseUrl;
    private String appKey;
    /** 传入新明文则加密覆盖；为空则保持原密钥不变 */
    private String appSecret;
    private String callbackUrl;
    private String h5Domain;
    private String h5Theme;
    private Integer authType;
    private String ipWhitelist;
    private Integer rateLimit;
    private Integer timeout;
    private String extraConfig;
    private Integer status;
}
