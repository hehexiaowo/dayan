package com.dayan.channel.dto;

import lombok.Data;

/**
 * 渠道账号更新入参（accountCode 不可改，密码不通过此接口修改）。
 */
@Data
public class ChannelAccountUpdateDTO {

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
