package com.dayan.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建权益采购订单（order_equity）入参。
 *
 * <p>金额校验：{@code totalAmount = unitPrice * quantity}，{@code payAmount = totalAmount - discountAmount}
 * （权益订单无优惠券字段，coupon 概念不适用，仅 discount）。
 * 快照字段（channelFullName/agentFullName 等）由 DTO 传入，不强校验存在性。
 */
@Data
public class CreateOrderEquityDTO {

    /** 采购来源：1=对公 / 2=个人 */
    @NotNull(message = "采购来源不能为空")
    private Integer orderSource;

    /** 渠道编码（可空） */
    private String channelCode;
    /** 渠道名称快照（可空，由调用方传入） */
    private String channelFullName;

    /** 代理人编码（可空） */
    private String agentCode;
    /** 代理人姓名快照（可空） */
    private String agentFullName;

    /** 分销商编码（可空） */
    private String distributorCode;
    /** 分销商名称快照（可空） */
    private String distributorFullName;

    /** 商品编码 */
    @NotBlank(message = "商品编码不能为空")
    private String goodsCode;
    /** 商品名称快照 */
    @NotBlank(message = "商品名称不能为空")
    private String goodsName;
    /** SKU编码（可空） */
    private String skuCode;
    /** SKU名称（可空） */
    private String skuName;

    /** 购买数量 */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于 0")
    private Integer quantity;

    /** 单价 */
    @NotNull(message = "单价不能为空")
    @Min(value = 0, message = "单价不能为负")
    private BigDecimal unitPrice;

    /** 优惠金额（可空，默认 0） */
    private BigDecimal discountAmount;

    /** 权益入库方式：1=批量 / 2=逐张 / 3=自动入库（可空，默认 1） */
    private Integer deliverType;

    /** 平台运营方编码（可空） */
    private String organCode;

    /** 备注（可空） */
    private String remark;

    /** 创建人编码（操作人，写入日志 operatorCode，可空默认 system） */
    private String operatorCode;
    /** 创建人姓名（写入日志 operatorName，可空） */
    private String operatorName;
    /** 操作人类型：admin/channel/agent/client/supplier/distributor（写入日志 operatorType，可空） */
    private String operatorType;
}
