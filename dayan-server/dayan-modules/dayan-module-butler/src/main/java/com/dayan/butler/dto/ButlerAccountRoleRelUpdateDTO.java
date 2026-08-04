package com.dayan.butler.dto;

import lombok.Data;

/**
 * 管家账号-角色关联修改入参（非空字段才更新）。
 */
@Data
public class ButlerAccountRoleRelUpdateDTO {

    private Integer roleType;
    private String description;
}
