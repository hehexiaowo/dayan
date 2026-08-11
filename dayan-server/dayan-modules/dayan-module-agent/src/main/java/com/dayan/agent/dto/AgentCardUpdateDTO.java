package com.dayan.agent.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代理人名片更新入参（选择性更新，null 字段不覆盖）。
 */
@Data
public class AgentCardUpdateDTO {

    @Size(max = 100, message = "名片名称不能超过100字")
    private String cardName;

    @Size(max = 50, message = "显示姓名不能超过50字")
    private String displayName;

    @Size(max = 100, message = "职务不能超过100字")
    private String title;

    @Size(max = 20, message = "手机号不能超过20字")
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

    /** 排序值 */
    private Integer sortOrder;

    /** 状态：1=启用 0=停用 */
    private Integer status;
}
