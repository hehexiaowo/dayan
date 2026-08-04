package com.dayan.scene.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 场景审核入参。
 *
 * <p>审核动作：置 {@code audit_status} 为 1=通过 / 2=驳回。
 */
@Data
public class SceneInfoAuditDTO {

    @NotBlank(message = "场景编码不能为空")
    private String sceneCode;

    /** 审核状态：1=通过 / 2=驳回 */
    private Integer auditStatus;

    private String auditRemark;
}
