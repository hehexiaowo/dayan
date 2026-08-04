package com.dayan.agent.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代理人账号更新入参。
 *
 * <p>agentCode / channelCode 不可改；密码不在此修改，走 resetPassword。
 */
@Data
public class AgentAccountUpdateDTO {

    @Size(max = 50)
    private String username;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String openId;

    @Size(max = 100)
    private String unionId;

    @Size(max = 100)
    private String extAccountNo;

    /** 账号状态（0=锁定, 1=正常, 2=禁用） */
    private Integer accountStatus;
}
