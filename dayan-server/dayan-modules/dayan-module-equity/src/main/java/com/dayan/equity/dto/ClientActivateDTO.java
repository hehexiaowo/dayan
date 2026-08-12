package com.dayan.equity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** client 端权益激活入参（clientCode 由登录态注入，不接受前端传入）。 */
@Data
public class ClientActivateDTO {

    @NotBlank(message = "激活码不能为空")
    @Pattern(regexp = "DY[A-Z0-9]{8}", message = "激活码格式错误（DY加8位大写字母或数字）")
    private String activateCode;
}
