package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 park_periphery 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_periphery")
public class ParkPeriphery extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 周边类型 */
    private Integer peripheryType;

    /** 地点名称 */
    private String placeName;

    /** 详细地址 */
    private String placeAddress;

    /** 距离描述 */
    private String distance;

    /** 详细描述 */
    private String detailDescription;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
