package com.dayan.channel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 渠道开放平台配置创建入参。
 *
 * <p>{@code appSecret} 以明文传入，由 Service 层用 AES-GCM 加密后存储。
 */
@Data
public class ChannelOpenPlatformCreateDTO {

    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

    @NotBlank(message = "平台名称不能为空")
    private String platformName;

    /** 对接类型：1=API, 2=H5, 3=小程序 等 */
    private Integer dockType;

    private String apiBaseUrl;
    private String appKey;

    /** 明文密钥，存储前加密 */
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
