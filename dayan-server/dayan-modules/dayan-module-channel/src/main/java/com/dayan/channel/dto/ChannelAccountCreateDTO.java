package com.dayan.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 渠道账号创建入参。
 *
 * <p>{@code accountCode} 由系统生成（CA 前缀），密码经 BCrypt 哈希存储。
 */
@Data
public class ChannelAccountCreateDTO {

    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64)
    private String username;

    /** 明文密码，为空则使用默认值 */
    private String password;

    private String realName;
    private String avatar;
    private String phone;
    private String openId;
    private String unionId;
    private String email;
    private String position;
    private Integer accountStatus;
    private Integer isAdmin;
}
