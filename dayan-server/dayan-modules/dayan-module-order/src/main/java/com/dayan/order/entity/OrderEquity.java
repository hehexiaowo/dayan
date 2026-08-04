package com.dayan.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 表 order_equity 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_equity")
public class OrderEquity extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 订单编号 */
    private String orderCode;

    /** 采购来源 */
    private Integer orderSource;

    /** 渠道编码 */
    private String channelCode;

    /** 渠道名称(快照) */
    private String channelFullName;

    /** 代理人编码 */
    private String agentCode;

    /** 代理人姓名(快照) */
    private String agentFullName;

    /** 分销商编码 */
    private String distributorCode;

    /** 分销商名称(快照) */
    private String distributorFullName;

    /** 商品编码 */
    private String goodsCode;

    /** 商品名称 */
    private String goodsName;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 购买数量 */
    private Integer quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 订单总额 */
    private BigDecimal totalAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 支付方式 */
    private Integer payType;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 支付流水号 */
    private String payTradeNo;

    /** 权益入库方式 */
    private Integer deliverType;

    /** 已入库数量 */
    private Integer deliverCount;

    /** 入库完成时间 */
    private LocalDateTime deliverTime;

    /** 订单过期时间 */
    private LocalDateTime expireTime;

    /** 发票状态 */
    private Integer invoiceStatus;

    /** 平台运营方编码 */
    private String organCode;

    /** 订单状态 */
    private Integer orderStatus;

    /** 取消原因 */
    private String cancelReason;

    /** 备注 */
    private String remark;
}
