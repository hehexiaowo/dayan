package com.dayan.butler.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管家排班查询入参。
 */
@Data
public class ButlerScheduleQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String butlerCode;
    private LocalDate scheduleDate;
    private LocalDate scheduleDateStart;
    private LocalDate scheduleDateEnd;
    private Integer scheduleType;
    private Integer status;
}
