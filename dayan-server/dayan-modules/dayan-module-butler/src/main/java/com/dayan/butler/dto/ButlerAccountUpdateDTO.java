package com.dayan.butler.dto;

import lombok.Data;

/**
 * 管家账号修改入参（非空字段才更新）。
 */
@Data
public class ButlerAccountUpdateDTO {

    private String phone;
    private String openId;
    private String unionId;
    private Integer accountStatus;
}
