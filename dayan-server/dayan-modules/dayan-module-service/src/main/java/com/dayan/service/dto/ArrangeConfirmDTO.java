package com.dayan.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 安排确认入参。
 *
 * <p>isConfirmed 置 1 后方可触发会话 start_service(4→5)。
 */
@Data
public class ArrangeConfirmDTO {

    @NotNull(message = "安排 id 不能为空")
    private Long id;

    /** 0=否, 1=是 */
    @NotNull(message = "确认标记不能为空")
    private Integer isConfirmed;
}
