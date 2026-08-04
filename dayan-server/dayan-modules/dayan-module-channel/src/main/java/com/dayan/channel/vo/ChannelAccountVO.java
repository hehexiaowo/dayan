package com.dayan.channel.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 渠道账号 VO（不含密码）。
 */
@Data
public class ChannelAccountVO {

    private Long id;
    private String channelCode;
    private String accountCode;
    private String username;
    private String realName;
    private String avatar;
    private String phone;
    private String openId;
    private String unionId;
    private String email;
    private String position;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer loginCount;
    private Integer accountStatus;
    private Integer isAdmin;
    private LocalDateTime createdAt;
}
