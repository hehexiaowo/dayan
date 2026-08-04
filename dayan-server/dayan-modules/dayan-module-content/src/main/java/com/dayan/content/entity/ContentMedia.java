package com.dayan.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 content_media 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_media")
public class ContentMedia extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 内容编码 */
    private String contentCode;

    /** 媒体类型 */
    private Integer mediaType;

    /** 资源URL */
    private String mediaUrl;

    /** 缩略图URL */
    private String thumbnailUrl;

    /** 资源名称 */
    private String mediaName;

    /** 文件格式 */
    private String fileFormat;

    /** 文件大小(KB) */
    private Integer fileSize;

    /** 宽度(像素) */
    private Integer width;

    /** 高度(像素) */
    private Integer height;

    /** 时长(秒) */
    private Integer duration;

    /** 资源描述 */
    private String mediaDescription;

    /** 是否在正文中 */
    private Integer isInBody;

    /** 排序号 */
    private Integer sortOrder;
}
