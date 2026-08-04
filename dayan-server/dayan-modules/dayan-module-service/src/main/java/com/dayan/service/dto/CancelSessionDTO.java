package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 取消会话入参。
 *
 * <p>触发 cancel 事件（1→7 / 2→7 / 5→7），写 closeReason/closeTime=now。
 */
@Data
public class CancelSessionDTO {

    @NotBlank(message = "会话编码不能为空")
    private String sessionCode;

    /** 关闭原因 */
    private String closeReason;
}
