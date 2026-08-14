package com.dayan.butler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管家开通后台账号入参。
 *
 * <p>为已建档的管家补开 organ_account 后台账号（可登录 admin），
 * 挂靠"养老管家"部门（DEPT_BUTLER）+ 普通管家角色（ROLE_BUTLER）。
 */
@Data
public class ButlerAccountOpenDTO {

    /** 后台登录用户名（全平台唯一） */
    @NotBlank(message = "登录用户名不能为空")
    @Size(max = 50)
    private String username;

    /** 初始密码（可空，留空使用系统默认密码） */
    @Size(min = 6, max = 64)
    private String password;
}
