package com.dayan.organ.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
/**
 * 表 organ_employee 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("organ_employee")
public class OrganEmployee extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 所属组织编码 */
    private String organCode;

    /** 员工编码 */
    private String employeeCode;

    /** 关联账号编码 */
    private String accountCode;

    /** 所属部门编码 */
    private String deptCode;

    /** 真实姓名 */
    private String realName;

    /** 性别 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 身份证号 */
    private String idCard;

    /** 职位 */
    private String position;

    /** 入职日期 */
    private LocalDate entryDate;

    /** 离职日期 */
    private LocalDate leaveDate;

    /** 头像URL */
    private String avatar;

    /** 员工状态 */
    private Integer employeeStatus;

    /** 备注 */
    private String remark;
}
