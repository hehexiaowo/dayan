package com.dayan.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 表 order_sojourn 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_sojourn")
public class OrderSojourn extends BaseEntity {

    /** 主键（分片表，雪花ID，MyBatis-Plus 自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 订单编号 */
    private String orderCode;

    /** 订单类型 */
    private Integer orderType;

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

    /** 客户编码 */
    private String clientCode;

    /** 客户姓名(快照) */
    private String clientFullName;

    /** 商品编码 */
    private String goodsCode;

    /** 商品名称(快照) */
    private String goodsName;

    /** 机构编码 */
    private String parkCode;

    /** 机构名称(快照) */
    private String parkFullName;

    /** 房间类型编码 */
    private String roomTypeCode;

    /** SKU编码 */
    private String skuCode;

    /** 规格名称(快照) */
    private String skuName;

    /** 入住日期 */
    private LocalDate checkinDate;

    /** 退房日期 */
    private LocalDate checkoutDate;

    /** 入住天数 */
    private Integer stayDays;

    /** 入住人数 */
    private Integer residentCount;

    /** 入住人姓名 */
    private String residentNames;

    /** 照护类型编码 */
    private String careTypeCode;

    /** 餐饮类型编码 */
    private String foodTypeCode;

    /** 房间费用 */
    private BigDecimal roomFee;

    /** 照护费用 */
    private BigDecimal careFee;

    /** 餐饮费用 */
    private BigDecimal foodFee;

    /** 其他费用 */
    private BigDecimal otherFee;

    /** 订单总额 */
    private BigDecimal totalAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 优惠券编码 */
    private String couponCode;

    /** 支付方式 */
    private Integer payType;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 押金金额 */
    private BigDecimal depositAmount;

    /** 使用的权益编码 */
    private String equityCode;

    /** 联系人姓名 */
    private String contactName;

    /** 联系人电话 */
    private String contactPhone;

    /** 特殊需求 */
    private String specialNeeds;

    /** 状态 */
    private Integer orderStatus;

    /** 取消原因 */
    private String cancelReason;

    /** 备注 */
    private String remark;
}
