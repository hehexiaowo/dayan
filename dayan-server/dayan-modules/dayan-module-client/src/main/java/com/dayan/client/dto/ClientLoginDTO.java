package com.dayan.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Client 客户端登录入参。
 *
 * <p>支持手机号/微信 OpenID 任一作为登录标识（identifier 字段二选一），需选定渠道。
 */
@Data
public class ClientLoginDTO {

    /** 所属渠道编码 */
    @NotBlank(message = "渠道不能为空")
    @Size(max = 64, message = "渠道编码长度不能超过 64")
    private String channelCode;

    /** 登录标识：手机号或微信 OpenID */
    @NotBlank(message = "登录标识不能为空")
    @Size(max = 100, message = "登录标识长度不能超过 100")
    private String identifier;

    /** 密码（明文，登录后由 PasswordService 进行 BCrypt 校验） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64 位")
    private String password;
}
