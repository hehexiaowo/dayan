package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 park_media_vr 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_media_vr")
public class ParkMediaVr extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** VR全景链接 */
    private String vrUrl;

    /** VR服务提供商 */
    private String vrProvider;

    /** VR资源名称 */
    private String vrName;

    /** VR类型 */
    private Integer vrType;

    /** 缩略图URL */
    private String thumbnailUrl;

    /** VR描述 */
    private String vrDescription;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
