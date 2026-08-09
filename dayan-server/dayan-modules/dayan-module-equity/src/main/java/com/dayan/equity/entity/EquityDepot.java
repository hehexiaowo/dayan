package com.dayan.equity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 表 equity_depot 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equity_depot")
public class EquityDepot extends BaseEntity {

    /** 主键（分片表，雪花ID，MyBatis-Plus 自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 权益编码 */
    private String equityCode;

    /** 权益卡号 */
    private String equityNo;

    /** 商品编码（入库时从批次快照） */
    private String goodsCode;

    /** 使用人人数快照（入库时从 goods_equity 冻结） */
    private Integer personCount;

    /** 激活后有效天数快照（入库时从 goods_equity 冻结） */
    private Integer validDays;

    /** 批次编码 */
    private String batchCode;

    /** 成本价 */
    private BigDecimal costPrice;

    /** 分配渠道编码 */
    private String channelCode;

    /** 分配代理人编码 */
    private String agentCode;

    /** 领取客户编码 */
    private String clientCode;

    /** 入库时间 */
    private LocalDateTime produceTime;

    /** 分配时间 */
    private LocalDateTime allocateTime;

    /** 出库寄送渠道编码 */
    private String outboundChannelCode;

    /** 出库寄送代理人编码 */
    private String outboundAgentCode;

    /** 出库时间 */
    private LocalDateTime outboundTime;

    /** 物流单号 */
    private String logisticsNo;

    /** 激活时间 */
    private LocalDateTime activateTime;

    /** 首次使用时间 */
    private LocalDateTime firstUseTime;

    /** 最近使用时间 */
    private LocalDateTime lastUseTime;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 库存过期时间 */
    private LocalDateTime shelfExpireTime;

    /** 卡密 */
    private String cardSecret;

    /** 载体类型 */
    private Integer carrierType;

    /** 激活码 */
    private String activateCode;

    /** 绑定码 */
    private String bindCode;

    /** 权益二维码URL */
    private String qrCodeUrl;

    /** 关联订单编码 */
    private String orderCode;

    /** 权益状态 */
    private Integer equityStatus;

    /** 作废原因 */
    private String voidReason;

    /** 备注 */
    private String remark;
}
