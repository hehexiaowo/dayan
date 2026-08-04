package com.dayan.organ.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录成功响应。
 */
@Data
@Builder
public class AuthLoginVO {

    /** Sa-Token 签发的 Token 值 */
    private String token;
    /** Token 请求头名称（Admin-Token / Channel-Token） */
    private String tokenName;
    /** 账号编码 */
    private String accountCode;
    /** 真实姓名 */
    private String realName;
    /** 头像 */
    private String avatar;
    /** 是否超级管理员 */
    private Boolean isAdmin;
}
