package com.dayan.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 service_visit_record 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_visit_record")
public class ServiceVisitRecord extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 管家编码 */
    private String butlerCode;

    /** 机构编码 */
    private String parkCode;

    /** 探访日期 */
    private LocalDate visitDate;

    /** 探访目的 */
    private Integer visitPurpose;

    /** 设施检查情况 */
    private String facilityCheck;

    /** 服务检查情况 */
    private String serviceCheck;

    /** 卫生检查情况 */
    private String hygieneCheck;

    /** 餐饮检查情况 */
    private String foodCheck;

    /** 安全检查情况 */
    private String safetyCheck;

    /** 综合评分 */
    private BigDecimal overallScore;

    /** 发现问题 */
    private String issuesFound;

    /** 改进建议 */
    private String improvementSuggestions;

    /** 探访照片 */
    private String images;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
