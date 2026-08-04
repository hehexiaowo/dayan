package com.dayan.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代理人账号 VO（不返回密码、盐值）。
 */
@Data
public class AgentAccountVO {

    private Long id;
    private String agentCode;
    private String channelCode;
    private String username;
    private String phone;
    private String openId;
    private String unionId;
    private String extAccountNo;
    private Integer accountStatus;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
