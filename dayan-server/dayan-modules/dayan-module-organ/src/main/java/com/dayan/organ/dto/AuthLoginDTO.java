package com.dayan.organ.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Admin/Channel 端登录入参。
 *
 * <p>支持用户名/手机号/邮箱任一作为登录标识（username 字段三合一）。
 */
@Data
public class AuthLoginDTO {

    /** 登录标识：用户名 / 手机号 / 邮箱 */
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 100, message = "登录账号长度不能超过 100")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64 位")
    private String password;
}
