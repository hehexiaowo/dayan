package com.dayan.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 验证码登录请求。
 */
@Data
public class SmsLoginDTO {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    /** 所属渠道编码 */
    @NotBlank(message = "渠道不能为空")
    private String channelCode;

    /** 短信验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
