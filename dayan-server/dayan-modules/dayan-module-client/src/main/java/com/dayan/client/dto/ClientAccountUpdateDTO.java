package com.dayan.client.dto;

import lombok.Data;

/**
 * 客户账号更新入参（clientCode/channelCode 不可改；密码走 resetPassword）。
 */
@Data
public class ClientAccountUpdateDTO {

    private String username;
    private String phone;
    private String openId;
    private String unionId;
    private String alipayId;
    private String extAccountNo;
    private Integer accountStatus;
}
