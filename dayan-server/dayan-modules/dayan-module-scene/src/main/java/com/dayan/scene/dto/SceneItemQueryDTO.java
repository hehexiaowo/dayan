package com.dayan.scene.dto;

import lombok.Data;

/**
 * 场景项目查询入参。
 */
@Data
public class SceneItemQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sceneCode;
    private String itemCode;
    private String itemName;
    private Integer itemType;
    private Integer isRequired;
    private Integer status;
}
