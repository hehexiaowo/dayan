package com.dayan.channel.dto;

import lombok.Data;

/**
 * 渠道开放平台配置查询入参。
 */
@Data
public class ChannelOpenPlatformQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String channelCode;
    private String platformName;
    private Integer dockType;
    private Integer status;
}
