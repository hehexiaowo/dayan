package com.dayan.scene.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场景资源 VO。
 */
@Data
public class SceneResourceVO {

    private Long id;
    private String sceneCode;
    private Integer resourceType;
    private String resourceName;
    private String resourceDescription;
    private Integer quantity;
    private String unit;
    private BigDecimal unitCost;
    private Integer isProvided;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
