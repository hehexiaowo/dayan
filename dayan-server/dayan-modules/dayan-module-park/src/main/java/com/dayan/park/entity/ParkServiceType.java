package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 park_service_type 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_service_type")
public class ParkServiceType extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 服务类型编码 */
    private String serviceTypeCode;

    /** 服务类型名称 */
    private String serviceTypeName;

    /** 服务类别 */
    private Integer serviceTypeCategory;

    /** 服务详细描述 */
    private String serviceTypeDescription;

    /** 服务频次 */
    private String serviceTypeFrequency;

    /** 服务时长 */
    private String serviceTypeDuration;

    /** 服务图片URL */
    private String coverImage;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
