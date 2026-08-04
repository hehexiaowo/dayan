package com.dayan.scene.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 场景项目创建入参。
 *
 * <p>按 {@code sceneCode} 维度 CRUD，{@code itemCode} 同场景内唯一（应用层校验）。
 */
@Data
public class SceneItemCreateDTO {

    @NotBlank(message = "场景编码不能为空")
    private String sceneCode;

    @NotBlank(message = "项目编码不能为空")
    private String itemCode;

    @NotBlank(message = "项目名称不能为空")
    private String itemName;

    /** 项目类型 */
    private Integer itemType;

    private String itemDescription;
    private Integer durationMinutes;
    private Integer sortOrder;
    private Integer isRequired;
    private Integer status;
}
