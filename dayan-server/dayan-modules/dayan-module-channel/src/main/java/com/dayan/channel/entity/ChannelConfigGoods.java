package com.dayan.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 表 channel_config_goods 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("channel_config_goods")
public class ChannelConfigGoods extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 渠道编码 */
    private String channelCode;

    /** 商品编码 */
    private String goodsCode;

    /** 商品类型 */
    private Integer goodsType;

    /** 自定义商品名称 */
    private String customName;

    /** 自定义价格 */
    private BigDecimal customPrice;

    /** 自定义描述 */
    private String customDescription;

    /** 是否渠道专属 */
    private Integer isExclusive;

    /** 采购限制数量 */
    private Integer purchaseLimit;

    /** 排序号 */
    private Integer sortOrder;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 失效时间 */
    private LocalDateTime expireTime;

    /** 状态 */
    private Integer status;
}
