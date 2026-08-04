package com.dayan.channel.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 渠道开放平台配置 VO。
 *
 * <p>{@code appSecret} 出参为脱敏占位 {@code ***}，明文不再回传。
 */
@Data
public class ChannelOpenPlatformVO {

    private Long id;
    private String channelCode;
    private String platformName;
    private Integer dockType;
    private String apiBaseUrl;
    private String appKey;
    /** 脱敏后的密钥占位（明文不回传） */
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
