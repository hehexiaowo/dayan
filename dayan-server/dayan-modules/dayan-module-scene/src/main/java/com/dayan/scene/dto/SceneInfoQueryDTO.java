package com.dayan.scene.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 场景信息查询入参。
 */
@Data
public class SceneInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sceneCode;
    private String sceneName;
    private Integer sceneType;
    private String parkCode;
    private Integer sceneStatus;
    private Integer auditStatus;
}
