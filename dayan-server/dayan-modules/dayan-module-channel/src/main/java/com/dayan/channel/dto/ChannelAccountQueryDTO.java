package com.dayan.channel.dto;

import lombok.Data;

/**
 * 渠道账号查询入参。
 */
@Data
public class ChannelAccountQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String channelCode;
    /** 用户名/手机号/邮箱（模糊匹配 username 或精确匹配 phone/email） */
    private String username;
    /** 手机号（模糊匹配） */
    private String phone;
    private String realName;
    private Integer accountStatus;
}
