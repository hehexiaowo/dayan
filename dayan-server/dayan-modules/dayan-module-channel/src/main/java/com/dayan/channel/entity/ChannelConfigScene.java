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
 * 表 channel_config_scene 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("channel_config_scene")
public class ChannelConfigScene extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 渠道编码 */
    private String channelCode;

    /** 场景编码 */
    private String sceneCode;

    /** 是否渠道专属 */
    private Integer isExclusive;

    /** 自定义场景名称 */
    private String customName;

    /** 自定义价格 */
    private BigDecimal customPrice;

    /** 排序号 */
    private Integer sortOrder;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 失效时间 */
    private LocalDateTime expireTime;

    /** 状态 */
    private Integer status;
}
