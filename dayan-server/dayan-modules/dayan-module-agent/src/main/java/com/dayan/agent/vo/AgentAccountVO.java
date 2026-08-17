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
    /** 真实姓名（join agent_info.full_name） */
    private String realName;
    /** 代理人等级（join agent_info.agent_level，1=普通 2=银牌 3=金牌 4=钻石） */
    private Integer agentLevel;
    /** 是否认证（join agent_info.is_certified，0=否 1=是） */
    private Integer isCertified;
    private Integer accountStatus;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
