package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 park_media_file 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_media_file")
public class ParkMediaFile extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 文件URL */
    private String fileUrl;

    /** 文件名称 */
    private String fileName;

    /** 文件类型 */
    private Integer fileType;

    /** 文件格式 */
    private String fileFormat;

    /** 文件大小(KB) */
    private Integer fileSize;

    /** 文件描述 */
    private String fileDescription;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
