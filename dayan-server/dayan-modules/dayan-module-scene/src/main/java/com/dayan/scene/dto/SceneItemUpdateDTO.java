package com.dayan.scene.dto;

import lombok.Data;

/**
 * 场景项目修改入参（非空字段才更新）。
 */
@Data
public class SceneItemUpdateDTO {

    private String itemName;
    private Integer itemType;
    private String itemDescription;
    private Integer durationMinutes;
    private Integer sortOrder;
    private Integer isRequired;
    private Integer status;
}
