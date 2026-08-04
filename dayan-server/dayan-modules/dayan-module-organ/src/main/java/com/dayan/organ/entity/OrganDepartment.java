package com.dayan.organ.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 organ_department 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("organ_department")
public class OrganDepartment extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 所属组织编码 */
    private String organCode;

    /** 部门编码 */
    private String deptCode;

    /** 部门名称 */
    private String deptName;

    /** 父部门编码 */
    private String parentCode;

    /** 祖级列表 */
    private String ancestors;

    /** 部门类型 */
    private Integer deptType;

    /** 负责人姓名 */
    private String leaderName;

    /** 负责人电话 */
    private String leaderPhone;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
