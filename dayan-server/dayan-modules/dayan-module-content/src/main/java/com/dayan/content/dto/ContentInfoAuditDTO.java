package com.dayan.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 内容审核入参。
 *
 * <p>审核动作：待审核（contentStatus=1）→ 通过（2） / 拒绝（3）。
 */
@Data
public class ContentInfoAuditDTO {

    @NotBlank(message = "内容编码不能为空")
    private String contentCode;

    /** 审核状态：2=通过 / 3=拒绝 */
    private Integer auditStatus;

    private String auditRemark;
}
