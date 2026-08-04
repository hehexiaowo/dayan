package com.dayan.butler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 管家排班创建入参。
 *
 * <p>同 butlerCode + 同 scheduleDate 下，新排班的 [startTime, endTime] 与已有 status=1 排班
 * 不可重叠（应用层校验，重叠抛 BusinessException）。
 */
@Data
public class ButlerScheduleCreateDTO {

    @NotBlank(message = "管家编码不能为空")
    private String butlerCode;

    @NotNull(message = "排班日期不能为空")
    private LocalDate scheduleDate;

    /** 排班类型：1=上班 / 2=休假 / 3=外勤 / 4=培训 */
    private Integer scheduleType;

    @NotNull(message = "上班时间不能为空")
    private LocalTime startTime;

    @NotNull(message = "下班时间不能为空")
    private LocalTime endTime;

    /** 状态，默认 1 */
    private Integer status;
}
