package com.dayan.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 表 content_record_read 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_record_read")
public class ContentRecordRead extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 内容编码 */
    private String contentCode;

    /** 阅读者类型 */
    private String readerType;

    /** 阅读者编码 */
    private String readerCode;

    /** 阅读时长(秒) */
    private Integer readDuration;

    /** 阅读进度(%) */
    private BigDecimal readProgress;

    /** 阅读来源 */
    private Integer readSource;

    /** IP地址 */
    private String ipAddress;

    /** 设备类型 */
    private String deviceType;

    /** 阅读时间 */
    private LocalDateTime readTime;
}
