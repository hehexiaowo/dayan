package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 park_media_image 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_media_image")
public class ParkMediaImage extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 图片URL */
    private String imageUrl;

    /** 图片名称 */
    private String imageName;

    /** 图片类型 */
    private Integer imageType;

    /** 图片描述 */
    private String imageDescription;

    /** 图片宽度 */
    private Integer width;

    /** 图片高度 */
    private Integer height;

    /** 文件大小(KB) */
    private Integer fileSize;

    /** 排序号 */
    private Integer sortOrder;

    /** 是否封面 */
    private Integer isCover;

    /** 状态 */
    private Integer status;
}
