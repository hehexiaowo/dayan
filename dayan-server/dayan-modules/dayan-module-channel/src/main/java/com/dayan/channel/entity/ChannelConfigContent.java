package com.dayan.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 channel_config_content 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("channel_config_content")
public class ChannelConfigContent extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 渠道编码 */
    private String channelCode;

    /** 内容编码 */
    private String contentCode;

    /** 内容类型 */
    private Integer contentType;

    /** 展示端类型 */
    private String appType;

    /** 展示位置 */
    private String position;

    /** 排序号 */
    private Integer sortOrder;

    /** 是否置顶 */
    private Integer isTop;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 失效时间 */
    private LocalDateTime expireTime;

    /** 状态 */
    private Integer status;
}
