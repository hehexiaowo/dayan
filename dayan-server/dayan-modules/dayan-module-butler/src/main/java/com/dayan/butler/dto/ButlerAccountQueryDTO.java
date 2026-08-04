package com.dayan.butler.dto;

import lombok.Data;

/**
 * 管家账号查询入参。
 */
@Data
public class ButlerAccountQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String butlerCode;
    /** 用户名/手机号模糊匹配 */
    private String username;
    private String phone;
    private Integer accountStatus;
}
