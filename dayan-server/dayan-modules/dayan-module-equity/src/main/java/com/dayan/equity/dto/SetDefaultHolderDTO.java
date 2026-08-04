package com.dayan.equity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设置默认权益人入参。
 *
 * <p>将指定使用人置为默认（is_default_holder=1），同 equity_code 下其他使用人置 0。
 */
@Data
public class SetDefaultHolderDTO {

    @NotNull(message = "使用人 id 不能为空")
    private Long id;

    @NotBlank(message = "权益编码不能为空")
    private String equityCode;
}
