package com.dayan.channel.dto;

import lombok.Data;

/**
 * 渠道信息查询入参（平铺列表 + 按 parentCode 构建树）。
 */
@Data
public class ChannelInfoQueryDTO {

    /** 父渠道编码：传空查全部，传 "0" 查顶级 */
    private String parentCode;
    private String channelCode;
    private String fullName;
    private Integer channelType;
    private Integer status;
    private Integer auditStatus;
    /** 按所属分销商筛选 */
    private String distributorCode;
}
