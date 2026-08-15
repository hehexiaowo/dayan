package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 system_asset 对应实体——系统素材仓库。
 *
 * 全系统文件/地址登记中心（不绑定单一业务）：只存地址与冗余分类（类型1/类型2/关联编码），
 * 真实引用关系由各业务表持有（删除保护按 AssetRefMap 反查业务表）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_asset")
public class SystemAsset extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 素材类型（1=图片 2=视频 3=文件 4=VR） */
    private Integer assetType;

    /** 类型1：业务维度（park机构/platform平台/goods商品/content内容/course课程/scene场景） */
    private String refType1;

    /** 类型2：细分分类（字典 asset_ref_type2，如 room_type房型/display_block展示板块） */
    private String refType2;

    /** 关联编码：业务实体编码（如机构编码/商品编码；平台素材为空） */
    private String refCode;

    /** 存储方式（1=本地OSS 2=外链） */
    private Integer storageType;

    /** 资源地址：storage_type=1 存 OSS key；=2 存完整 http(s) 外链 */
    private String assetUrl;

    /** 文件名称 */
    private String assetName;

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

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=隐藏 1=显示） */
    private Integer status;
}
