package com.dayan.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 supplier_evaluation 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier_evaluation")
public class SupplierEvaluation extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 供应商编码 */
    private String supplierCode;

    /** 评价周期 */
    private String evalPeriod;

    /** 评价类型 */
    private Integer evalType;

    /** 服务质量评分 */
    private BigDecimal serviceQualityScore;

    /** 设施质量评分 */
    private BigDecimal facilityQualityScore;

    /** 配合度评分 */
    private BigDecimal cooperationScore;

    /** 投诉率 */
    private BigDecimal complaintRate;

    /** 期间订单总量 */
    private Integer totalOrderCount;

    /** 期间投诉量 */
    private Integer complaintCount;

    /** 综合评分 */
    private BigDecimal totalScore;

    /** 评分等级 */
    private Integer scoreLevel;

    /** 评价内容 */
    private String evalContent;

    /** 改进建议 */
    private String improvementSuggestions;

    /** 评价人编码 */
    private String evaluatorCode;

    /** 评价人姓名 */
    private String evaluatorName;

    /** 评价日期 */
    private LocalDate evalDate;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
