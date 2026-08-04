package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 park_adviser 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_adviser")
public class ParkAdviser extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 顾问姓名 */
    private String adviserName;

    /** 顾问头衔 */
    private String adviserTitle;

    /** 顾问照片URL */
    private String adviserImage;

    /** 顾问介绍 */
    private String adviserContent;

    /** 顾问联系电话 */
    private String contactPhone;

    /** 是否首席顾问 */
    private Integer isPrimary;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
