package com.dayan.butler.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管家信息 VO。
 */
@Data
public class ButlerInfoVO {

    private Long id;
    /** 管家编码（BT 前缀） */
    private String butlerCode;
    /** 管家姓名 */
    private String fullName;
    /** 手机号 */
    private String phone;
    /** 头像URL */
    private String avatar;
    /** 所属组织编码 */
    private String organCode;
    /** 关联后台账号编码（organ_account.account_code，未开通为 null） */
    private String accountCode;
    /** 管家等级 */
    private Integer butlerLevel;
    /** 状态：0=停用 / 1=启用 */
    private Integer status;
    /** 备注 */
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
