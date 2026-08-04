package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账目收款/付款入参。
 *
 * <p>累加 received_amount、扣减 remain_amount、推进 account_status：
 * <ul>
 *   <li>remain > 0：0→1（部分收/付）</li>
 *   <li>remain ≤ 0：→2（已结清）</li>
 * </ul>
 * last_receive_time 更新为本笔收款时间。
 */
@Data
public class AccountReceiveDTO {

    @NotBlank(message = "账目编码不能为空")
    private String accountCode;

    /** 本笔收/付金额（正数） */
    @NotNull(message = "收/付金额不能为空")
    private BigDecimal amount;

    /** 收/付款时间（为空时取当前时间） */
    private LocalDateTime receiveTime;

    /** 备注 */
    private String remark;
}
