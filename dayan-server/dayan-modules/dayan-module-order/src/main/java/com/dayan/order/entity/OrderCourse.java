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
 * 表 order_course 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_course")
public class OrderCourse extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
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

    /** 课程编码 */
    private String courseCode;

    /** 课程名称 */
    private String courseName;

    /** SKU编码 */
    private String skuCode;

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

    /** 优惠券编码 */
    private String couponCode;

    /** 支付方式 */
    private Integer payType;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 使用的权益编码 */
    private String equityCode;

    /** 状态 */
    private Integer orderStatus;

    /** 取消原因 */
    private String cancelReason;

    /** 备注 */
    private String remark;
}
