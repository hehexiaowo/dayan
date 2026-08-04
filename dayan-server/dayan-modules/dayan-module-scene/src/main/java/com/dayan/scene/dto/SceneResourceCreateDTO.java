package com.dayan.scene.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 场景资源创建入参。
 *
 * <p>按 {@code sceneCode} 维度管理资源。资源冲突检测：同 {@code sceneCode} 下，
 * 同 {@code resourceName}（资源标识）+ 同 {@code resourceType} 视为冲突（重复资源），
 * 应用层校验，冲突抛 BusinessException。
 *
 * <p>注：本表 schema 无时间字段，故"时间段重叠"维度降级为"资源标识重复"校验；
 * 若后续表结构补充 resource_code/start_time/end_time，应改为真正的区间重叠检测
 * （参考 {@code ButlerScheduleServiceImpl#checkOverlap}）。
 */
@Data
public class SceneResourceCreateDTO {

    @NotBlank(message = "场景编码不能为空")
    private String sceneCode;

    /** 资源类型 */
    private Integer resourceType;

    @NotBlank(message = "资源名称不能为空")
    private String resourceName;

    private String resourceDescription;
    private Integer quantity;
    private String unit;
    private BigDecimal unitCost;
    private Integer isProvided;
    private Integer sortOrder;
    private Integer status;
}
