package com.dayan.butler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管家账号创建入参。
 *
 * <p>{@code password} 明文传入，由服务层经 BCrypt 哈希后存储。
 */
@Data
public class ButlerAccountCreateDTO {

    @NotBlank(message = "管家编码不能为空")
    @Size(max = 50)
    private String butlerCode;

    @NotBlank(message = "登录用户名不能为空")
    @Size(max = 100)
    private String username;

    /** 明文密码，为空时使用默认初始密码 */
    private String password;

    @Size(max = 50)
    private String phone;

    private String openId;
    private String unionId;

    /** 账号状态：默认 1=启用 */
    private Integer accountStatus;
}
