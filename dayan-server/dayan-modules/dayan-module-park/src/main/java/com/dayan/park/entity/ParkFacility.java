package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 park_facility 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_facility")
public class ParkFacility extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 设施编码 */
    private String facilityCode;

    /** 设施名称 */
    private String facilityName;

    /** 设施类别 */
    private Integer facilityCategory;

    /** 所在楼栋 */
    private String buildingName;

    /** 所在楼层 */
    private String floor;

    /** 面积 */
    private BigDecimal area;

    /** 最大容纳人数 */
    private Integer capacity;

    /** 开放时间 */
    private String openTime;

    /** 设施详细描述 */
    private String facilityDescription;

    /** 封面图URL */
    private String coverImage;

    /** 设施图片URL列表 */
    private String images;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
