package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 park_service_item 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_service_item")
public class ParkServiceItem extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 服务编码 */
    private String serviceCode;

    /** 服务名称 */
    private String serviceName;

    /** 服务类别 */
    private Integer serviceCategory;

    /** 服务详细描述 */
    private String serviceDescription;

    /** 服务频次 */
    private String serviceFrequency;

    /** 服务时长 */
    private String serviceDuration;

    /** 服务图片URL */
    private String coverImage;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
