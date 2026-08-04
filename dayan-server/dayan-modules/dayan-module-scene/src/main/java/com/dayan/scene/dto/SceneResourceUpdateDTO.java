package com.dayan.scene.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 场景资源修改入参（非空字段才更新）。
 *
 * <p>修改 {@code resourceName}/{@code resourceType} 时会做冲突检测：
 * 同 sceneCode 下重复的资源标识抛 BusinessException。
 */
@Data
public class SceneResourceUpdateDTO {

    private Integer resourceType;
    private String resourceName;
    private String resourceDescription;
    private Integer quantity;
    private String unit;
    private BigDecimal unitCost;
    private Integer isProvided;
    private Integer sortOrder;
    private Integer status;
}
