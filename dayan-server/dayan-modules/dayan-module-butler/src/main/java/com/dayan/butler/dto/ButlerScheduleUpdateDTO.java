package com.dayan.butler.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 管家排班修改入参（非空字段才更新）。
 *
 * <p>修改 scheduleDate/startTime/endTime 时同样会做重叠检测。
 */
@Data
public class ButlerScheduleUpdateDTO {

    private LocalDate scheduleDate;
    private Integer scheduleType;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer status;
}
