package com.dayan.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 content_record_share 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_record_share")
public class ContentRecordShare extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 内容编码 */
    private String contentCode;

    /** 分享者类型 */
    private String sharerType;

    /** 分享者编码 */
    private String sharerCode;

    /** 分享渠道 */
    private Integer shareChannel;

    /** 分享链接 */
    private String shareUrl;

    /** 分享标题 */
    private String shareTitle;

    /** 分享描述 */
    private String shareDescription;

    /** 分享缩略图 */
    private String shareImage;

    /** 点击次数 */
    private Integer clickCount;

    /** 转化次数 */
    private Integer convertCount;

    /** 分享时间 */
    private LocalDateTime shareTime;
}
