package com.dayan.equity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量入库（stockIn）入参。
 *
 * <p>对指定批次按 quantity 循环生成权益记录：
 * <ul>
 *   <li>权益卡（carrierType=1）：生成 activateCode（DY-8位）</li>
 *   <li>权益函（carrierType=2）：生成 bindCode（BF-12位）</li>
 * </ul>
 * 联动 batch.producedCount += quantity、remainCount += quantity；首次入库推进 batch_status=1，全部入库推进 2。
 */
@Data
public class StockInDTO {

    @NotBlank(message = "批次编码不能为空")
    private String batchCode;

    @NotNull(message = "入库数量不能为空")
    @Min(value = 1, message = "入库数量必须大于 0")
    private Integer quantity;

    /** 载体类型：1=权益卡 / 2=权益函 */
    @NotNull(message = "载体类型不能为空")
    private Integer carrierType;

    /** 分配渠道编码（可空，写入 equity_depot.channel_code 冗余） */
    private String channelCode;
}
