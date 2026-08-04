package com.dayan.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客户账号创建入参。
 */
@Data
public class ClientAccountCreateDTO {

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 64)
    private String clientCode;

    @NotBlank(message = "渠道编码不能为空")
    @Size(max = 64)
    private String channelCode;

    @Size(max = 64)
    private String username;

    @Size(max = 32)
    private String phone;

    /** 明文密码（创建时由 PasswordService 进行 BCrypt 哈希） */
    @Size(min = 6, max = 64, message = "密码长度 6-64 位")
    private String password;

    @Size(max = 128)
    private String openId;

    @Size(max = 128)
    private String unionId;

    @Size(max = 128)
    private String alipayId;

    @Size(max = 128)
    private String extAccountNo;

    /** 账号状态：0=锁定 1=正常 2=禁用 */
    private Integer accountStatus;
}
