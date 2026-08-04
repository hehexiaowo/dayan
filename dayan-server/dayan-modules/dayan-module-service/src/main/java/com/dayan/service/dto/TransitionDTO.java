package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通用状态机流转入参。
 *
 * <p>调用 {@code transition(SERVICE_SESSION_SM, from, event)}，event 取值见
 * {@link com.dayan.service.enums.ServiceSessionEvent}（assign_butler/submit_demand/
 * confirm_solution/reject_solution/start_service/finish/cancel）。
 * 业务专用接口（/assign-butler 等）已封装对应事件，本接口供特殊场景显式触发。
 */
@Data
public class TransitionDTO {

    @NotBlank(message = "会话编码不能为空")
    private String sessionCode;

    @NotBlank(message = "事件不能为空")
    private String event;
}
