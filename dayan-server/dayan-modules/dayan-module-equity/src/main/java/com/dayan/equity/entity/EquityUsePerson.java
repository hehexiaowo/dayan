package com.dayan.equity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
/**
 * 表 equity_use_person 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equity_use_person")
public class EquityUsePerson extends BaseEntity {

    /** 主键（分片表，雪花ID，MyBatis-Plus 自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 权益编码 */
    private String equityCode;

    /** 权益持有人编码 */
    private String clientCode;

    /** 使用人姓名 */
    private String usePersonName;

    /** 使用人性别 */
    private Integer usePersonGender;

    /** 使用人出生日期 */
    private LocalDate usePersonBirthday;

    /** 使用人年龄 */
    private Integer usePersonAge;

    /** 使用人手机号 */
    private String usePersonPhone;

    /** 使用人身份证号 */
    private String usePersonIdCard;

    /** 与持有人关系 */
    private String relationWithHolder;

    /** 健康状况简述 */
    private String healthStatus;

    /** 照护需求简述 */
    private String careNeed;

    /** 是否默认权益人 */
    private Integer isDefaultHolder;

    /** 备注 */
    private String remark;
}
