package com.dayan.butler.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管家账号 VO。
 */
@Data
public class ButlerAccountVO {

    private Long id;
    /** 管家编码 */
    private String butlerCode;
    /** 登录用户名 */
    private String username;
    /** 手机号 */
    private String phone;
    /** 微信OpenID */
    private String openId;
    /** 微信UnionID */
    private String unionId;
    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;
    /** 账号状态 */
    private Integer accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
