package com.dayan.butler.dto;

import lombok.Data;

/**
 * 管家信息修改入参（按 butlerCode 定位，非空字段才更新）。
 */
@Data
public class ButlerInfoUpdateDTO {

    private String fullName;
    private String phone;
    private String avatar;
    private String organCode;
    private Integer butlerLevel;
    private Integer status;
    private String remark;
}
