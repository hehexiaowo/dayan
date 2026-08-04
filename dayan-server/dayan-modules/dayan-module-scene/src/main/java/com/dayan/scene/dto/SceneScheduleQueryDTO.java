package com.dayan.scene.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 场景日程查询入参。
 */
@Data
public class SceneScheduleQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sceneCode;
    private LocalDate scheduleDate;
    private LocalDate scheduleDateStart;
    private LocalDate scheduleDateEnd;
    private Integer status;
}
