package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信授权登录请求。
 *
 * <p>前端通过 {@code uni.login({provider:'weixin'})} 获取 code 后提交。
 */
@Data
public class WxLoginDTO {

    /** 微信登录凭证（由 uni.login / wx.login 返回的 code） */
    @NotBlank(message = "微信授权 code 不能为空")
    private String code;

    /** 所属渠道编码 */
    @NotBlank(message = "渠道不能为空")
    private String channelCode;
}
