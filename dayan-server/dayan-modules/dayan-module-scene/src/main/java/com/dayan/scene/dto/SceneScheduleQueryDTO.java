package com.dayan.scene.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 场景日程查询入参。
 */
@Data
public class SceneScheduleQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sceneCode;
    /**
     * 场景编码集合（渠道端隔离用，服务端强制注入，不接受前端传入）。
     * 与 {@link SceneInfoQueryDTO#getSceneCodes()} 风格保持一致。
     */
    private List<String> sceneCodes;
    private LocalDate scheduleDate;
    private LocalDate scheduleDateStart;
    private LocalDate scheduleDateEnd;
    private Integer status;
}
