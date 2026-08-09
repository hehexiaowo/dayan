package com.dayan.equity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 权益批次创建入参。
 *
 * <p>{@code batchCode} 由系统生成（BC+8 位，{@code SequenceProvider}）。
 * 统计字段（produced/outbound 等）初始为 0，由 depot 链路联动维护，创建时不允许指定。
 */
@Data
public class EquityBatchCreateDTO {

    @NotBlank(message = "批次名称不能为空")
    private String batchName;

    /** 商品编码（须存在且对应 goods_equity 已配置） */
    @NotBlank(message = "商品编码不能为空")
    private String goodsCode;

    /** 分配渠道编码（可空） */
    private String channelCode;

    @NotNull(message = "总数量不能为空")
    @Positive(message = "总数量必须大于 0")
    private Integer totalQuantity;

    private BigDecimal unitCost;
    private BigDecimal totalCost;

    private LocalDate produceDate;
    private LocalDate expireDate;

    /** 批次状态：0=待生产/1=生产中/2=已完成/3=已出库/4=已关闭（默认 0 待生产） */
    private Integer batchStatus;

    private String remark;
}
