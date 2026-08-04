package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 park_care_type 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_care_type")
public class ParkCareType extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 照护类型编码 */
    private String careTypeCode;

    /** 照护类型名称 */
    private String careTypeName;

    /** 照护等级 */
    private Integer careLevel;

    /** 适用人群描述 */
    private String careTarget;

    /** 护理项目明细 */
    private String careItems;

    /** 护理频次 */
    private String careFrequency;

    /** 护患比 */
    private String nursePatientRatio;

    /** 评估标准说明 */
    private String assessmentCriteria;

    /** 照护类型详细描述 */
    private String description;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
