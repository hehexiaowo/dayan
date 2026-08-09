package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 方案接受/拒绝标记入参。
 *
 * <p>isAccepted: 0=否, 1=是, 2=需调整。会话确认方案（confirm_solution）前须存在 isAccepted=1 的方案。
 */
@Data
public class SolutionAcceptDTO {

    @NotBlank(message = "方案编码不能为空")
    private String solutionCode;

    /** 0=否, 1=是, 2=需调整 */
    @NotNull(message = "接受标记不能为空")
    private Integer isAccepted;

    private String clientFeedback;
}
