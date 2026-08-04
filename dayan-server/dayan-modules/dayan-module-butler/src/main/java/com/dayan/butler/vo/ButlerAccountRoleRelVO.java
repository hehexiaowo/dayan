package com.dayan.butler.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管家账号-角色关联 VO。
 */
@Data
public class ButlerAccountRoleRelVO {

    private Long id;
    /** 管家账号编码 */
    private String accountCode;
    /** 管家编码 */
    private String butlerCode;
    /** 角色类型 */
    private Integer roleType;
    /** 角色描述 */
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
