package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 park_asset 对应实体——机构素材库。
 *
 * 统一管理所有来源的图片/视频/文件/VR，通过 source_type + source_ref_code 追踪来源。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_asset")
public class ParkAsset extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机构编码 */
    private String parkCode;

    /** 素材类型（1=图片 2=视频 3=文件 4=VR） */
    private Integer assetType;

    /** 文件 OSS key（存 key 非完整 URL） */
    private String assetUrl;

    /** 文件名称 */
    private String assetName;

    /** 业务分类（图片:1-11 视频:1-3 文件:1-5 VR:1-3） */
    private Integer assetCategory;

    /** 描述 */
    private String description;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 图片宽度px（图片专属） */
    private Integer width;

    /** 图片高度px（图片专属） */
    private Integer height;

    /** 是否封面（图片专属 0=否 1=是） */
    private Integer isCover;

    /** 封面图key（视频专属） */
    private String coverUrl;

    /** 时长秒（视频专属） */
    private Integer duration;

    /** 文件格式（文件专属 pdf/doc/xls等） */
    private String fileFormat;

    /** VR服务提供商（VR专属） */
    private String vrProvider;

    /** 缩略图key（VR专属） */
    private String thumbnailUrl;

    /** 来源（media_mgmt/room_type/food_type/facility/service_item/display_block/adviser/park_info） */
    private String sourceType;

    /** 来源编码（media_mgmt 时为 NULL） */
    private String sourceRefCode;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=隐藏 1=显示） */
    private Integer status;
}
