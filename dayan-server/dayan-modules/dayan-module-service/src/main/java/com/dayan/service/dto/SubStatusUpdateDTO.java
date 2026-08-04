package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会话子状态更新入参。
 *
 * <p>sub_status 独立于状态机，由应用层直接 UPDATE。终态校验：
 * session_status=6(完成)/7(取消) 且 sub_status=refund_done 时拒绝再转。
 * 取值见 {@link com.dayan.service.enums.ServiceSessionEvent} SUB_* 常量。
 */
@Data
public class SubStatusUpdateDTO {

    @NotBlank(message = "会话编码不能为空")
    private String sessionCode;

    @NotBlank(message = "子状态不能为空")
    private String subStatus;
}
