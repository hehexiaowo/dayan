package com.dayan.client.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户账号 VO（不含密码）。
 */
@Data
public class ClientAccountVO {

    private Long id;
    private String clientCode;
    private String channelCode;
    private String username;
    private String phone;
    /** 真实姓名 join client_info.full_name */
    private String realName;
    private String openId;
    private String unionId;
    private String alipayId;
    private String extAccountNo;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer loginCount;
    private Integer accountStatus;
    private LocalDateTime createdAt;
}
