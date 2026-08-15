package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 system_asset 对应实体——系统素材库。
 *
 * 统一管理整个系统的文件与外链资源（本地 OSS 对象或外部存储链接），
 * 通过 source_type + source_ref_code 追踪来源，供各业务模块统一调配。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_asset")
public class SystemAsset extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属机构编码（NULL=平台素材） */
    private String parkCode;

    /** 素材类型（1=图片 2=视频 3=文件 4=VR） */
    private Integer assetType;

    /** 存储方式（1=本地OSS 2=外链） */
    private Integer storageType;

    /** 资源地址：storage_type=1 存 OSS key；=2 存完整 http(s) 外链 */
    private String assetUrl;

    /** 文件名称 */
    private String assetName;

    /** 业务分类（图片:1-11 视频:1-3 文件:1-5 VR:1-3） */
    private Integer assetCategory;

    /** 描述 */
    private String description;

    /** 文件大小（字节，外链未知时可空） */
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

    /** 来源（media_mgmt/room_type/food_type/facility_type/service_type/display_block/adviser/park_info） */
    private String sourceType;

    /** 来源编码（media_mgmt 时为 NULL） */
    private String sourceRefCode;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=隐藏 1=显示） */
    private Integer status;
}
