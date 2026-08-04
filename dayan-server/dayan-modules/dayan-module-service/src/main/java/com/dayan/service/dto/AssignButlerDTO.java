package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分配管家入参。
 *
 * <p>触发 assign_butler(1→2) 事件，写 butlerCode/butlerFullName(快照)/acceptTime=now。
 */
@Data
public class AssignButlerDTO {

    @NotBlank(message = "会话编码不能为空")
    private String sessionCode;

    @NotBlank(message = "管家编码不能为空")
    private String butlerCode;
}
