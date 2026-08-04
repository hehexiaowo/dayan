package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 park_media_video 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_media_video")
public class ParkMediaVideo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 视频URL */
    private String videoUrl;

    /** 封面图URL */
    private String coverUrl;

    /** 视频名称 */
    private String videoName;

    /** 视频类型 */
    private Integer videoType;

    /** 视频描述 */
    private String videoDescription;

    /** 时长(秒) */
    private Integer duration;

    /** 文件大小(KB) */
    private Integer fileSize;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
