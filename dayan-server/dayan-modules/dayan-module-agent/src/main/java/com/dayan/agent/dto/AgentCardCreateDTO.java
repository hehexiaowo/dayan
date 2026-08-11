package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代理人名片新增入参。
 */
@Data
public class AgentCardCreateDTO {

    @NotBlank(message = "名片名称不能为空")
    @Size(max = 100, message = "名片名称不能超过100字")
    private String cardName;

    @NotBlank(message = "显示姓名不能为空")
    @Size(max = 50, message = "显示姓名不能超过50字")
    private String displayName;

    @Size(max = 100, message = "职务不能超过100字")
    private String title;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 50, message = "微信号不能超过50字")
    private String wechat;

    @Size(max = 100, message = "邮箱不能超过100字")
    private String email;

    @Size(max = 200, message = "公司名称不能超过200字")
    private String company;

    @Size(max = 300, message = "地址不能超过300字")
    private String address;

    @Size(max = 500, message = "头像路径不能超过500字")
    private String avatar;

    @Size(max = 2000, message = "个人简介不能超过2000字")
    private String intro;

    @Size(max = 500, message = "标签不能超过500字")
    private String tags;
}
