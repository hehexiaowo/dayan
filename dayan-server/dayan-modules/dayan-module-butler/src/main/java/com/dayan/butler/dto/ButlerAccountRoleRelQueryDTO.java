package com.dayan.butler.dto;

import lombok.Data;

/**
 * 管家账号-角色关联查询入参。
 */
@Data
public class ButlerAccountRoleRelQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String accountCode;
    private String butlerCode;
    private Integer roleType;
}
