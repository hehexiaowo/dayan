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

    @Size(max = 500)
    private String remark;
}
