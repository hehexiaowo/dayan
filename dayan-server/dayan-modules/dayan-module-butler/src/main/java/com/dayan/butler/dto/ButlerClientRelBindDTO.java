package com.dayan.butler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管家-客户绑定入参。
 *
 * <p>一客户一管家约束：绑定时校验同 clientCode 是否已有 status=1 的有效绑定，
 * 若有则拒绝（需先解绑）。
 */
@Data
public class ButlerClientRelBindDTO {

    @NotBlank(message = "管家编码不能为空")
    private String butlerCode;

    @NotBlank(message = "客户编码不能为空")
    private String clientCode;
}
