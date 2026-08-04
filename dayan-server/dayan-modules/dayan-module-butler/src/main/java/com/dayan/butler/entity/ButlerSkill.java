package com.dayan.butler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
/**
 * 表 butler_skill 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("butler_skill")
public class ButlerSkill extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 管家编码 */
    private String butlerCode;

    /** 技能编码 */
    private String skillCode;

    /** 技能名称 */
    private String skillName;

    /** 熟练度 */
    private Integer proficiency;

    /** 是否持证 */
    private Integer isCertified;

    /** 证书编号 */
    private String certificateNo;

    /** 取得日期 */
    private LocalDate obtainDate;

    /** 排序号 */
    private Integer sortOrder;
}
