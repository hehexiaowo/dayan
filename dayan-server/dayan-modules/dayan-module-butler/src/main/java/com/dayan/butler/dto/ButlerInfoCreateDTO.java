package com.dayan.butler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管家信息创建入参。
 *
 * <p>{@code butlerCode} 由系统生成（BT 前缀 + 5 位序列）。
 */
@Data
public class ButlerInfoCreateDTO {

    @NotBlank(message = "管家姓名不能为空")
    @Size(max = 100)
    private String fullName;

    @Size(max = 50)
    private String phone;

    @Size(max = 500)
    private String avatar;

    /** 所属组织编码 */
    private String organCode;

    /** 管家等级 */
    private Integer butlerLevel;

    /** 状态：0=停用 / 1=启用，默认 1 */
    private Integer status;

    /**
     * 后台登录用户名（可空）。
     *
     * <p>填写即在创建管家的同时开通 organ_account 后台账号（可登录 admin），
     * 并挂靠"养老管家"部门（DEPT_BUTLER）+ 普通管家角色（ROLE_BUTLER）；
     * 留空则只建档不开通，后续可走"开通后台账号"接口补办。
     */
    @Size(max = 50)
    private String username;

    /** 后台账号初始密码（可空，留空使用系统默认密码） */
    @Size(min = 6, max = 64)
    private String password;

    @Size(max = 500)
    private String remark;
}
