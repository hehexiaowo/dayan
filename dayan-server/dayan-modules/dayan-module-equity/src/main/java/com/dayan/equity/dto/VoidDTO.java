package com.dayan.equity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 权益作废（void）入参。
 *
 * <p>校验 equity_status ∈ {0,1}（库存中/已出库可作废，状态机不允许 2 已激活直接作废）。
 * 经状态机 void:0→6 或 1→6，写 void_reason；联动 batch.voidedCount += 1、remainCount -= 1（仅 from=0 时）。
 */
@Data
public class VoidDTO {

    @NotBlank(message = "权益编码不能为空")
    private String equityCode;

    @NotBlank(message = "作废原因不能为空")
    private String voidReason;
}
