package com.dayan.scene.dto;

import lombok.Data;

/**
 * 场景资源查询入参。
 */
@Data
public class SceneResourceQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sceneCode;
    private Integer resourceType;
    private String resourceName;
    private Integer isProvided;
    private Integer status;
}
