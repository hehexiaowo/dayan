package com.dayan.scene.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场景项目 VO。
 */
@Data
public class SceneItemVO {

    private Long id;
    private String sceneCode;
    private String itemCode;
    private String itemName;
    private Integer itemType;
    private String itemDescription;
    private Integer durationMinutes;
    private Integer sortOrder;
    private Integer isRequired;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
