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
 * 表 order_scene 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_scene")
public class OrderScene extends BaseEntity {

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

    /** 场景编码 */
    private String sceneCode;

    /** 场景名称 */
    private String sceneName;

    /** SKU编码 */
    private String skuCode;

    /** 排期编码 */
    private String scheduleCode;

    /** 活动日期 */
    private LocalDate activityDate;

    /** 参与人数 */
    private Integer participantCount;

    /** 参与人姓名 */
    private String participantNames;

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

    /** 优惠券抵扣 */
    private BigDecimal couponAmount;

    /** 支付方式 */
    private Integer payType;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 使用的权益编码 */
    private String equityCode;

    /** 联系人姓名 */
    private String contactName;

    /** 联系人电话 */
    private String contactPhone;

    /** 备注 */
    private String remark;

    /** 状态 */
    private Integer orderStatus;

    /** 取消原因 */
    private String cancelReason;
}
