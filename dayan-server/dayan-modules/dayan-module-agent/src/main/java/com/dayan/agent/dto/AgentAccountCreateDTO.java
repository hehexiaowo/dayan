package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代理人账号创建入参。
 *
 * <p>密码明文传入，由 PasswordService BCrypt 加密后存储。
 */
@Data
public class AgentAccountCreateDTO {

    @NotBlank(message = "代理人编码不能为空")
    @Size(max = 50)
    private String agentCode;

    /** 所属渠道编码（不传则取当前登录上下文） */
    @Size(max = 50)
    private String channelCode;

    @Size(max = 50)
    private String username;

    @Size(max = 20)
    private String phone;

    /** 密码（明文，缺省使用默认密码） */
    @Size(min = 6, max = 64)
    private String password;

    @Size(max = 100)
    private String openId;

    @Size(max = 100)
    private String unionId;

    @Size(max = 100)
    private String extAccountNo;

    /** 账号状态（0=锁定, 1=正常, 2=禁用） */
    private Integer accountStatus;
}
