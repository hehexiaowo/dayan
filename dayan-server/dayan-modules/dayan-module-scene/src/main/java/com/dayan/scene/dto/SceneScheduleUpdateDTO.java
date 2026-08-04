package com.dayan.scene.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 场景日程修改入参（非空字段才更新）。
 *
 * <p>修改 currentPerson 或 maxPerson 时会做容量校验：
 * {@code currentPerson} ≤ {@code maxPerson}，否则抛 BusinessException。
 */
@Data
public class SceneScheduleUpdateDTO {

    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPerson;
    private Integer currentPerson;
    private BigDecimal priceOverride;
    private String remark;
    private Integer status;
}
