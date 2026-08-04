package com.dayan.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 表 service_equity_followup 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_equity_followup")
public class ServiceEquityFollowup extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 服务会话编码 */
    private String sessionCode;

    /** 关联安排编码 */
    private String arrangeCode;

    /** 客户编码 */
    private String clientCode;

    /** 回访管家编码 */
    private String butlerCode;

    /** 回访编码 */
    private String followupCode;

    /** 回访类型 */
    private Integer followupType;

    /** 回访方式 */
    private Integer followupMethod;

    /** 回访日期 */
    private LocalDate followupDate;

    /** 回访时间 */
    private LocalDateTime followupTime;

    /** 服务满意度 */
    private Integer serviceSatisfaction;

    /** 机构满意度 */
    private Integer parkSatisfaction;

    /** 管家满意度 */
    private Integer butlerSatisfaction;

    /** 综合满意度 */
    private Integer overallSatisfaction;

    /** 服务评价内容 */
    private String serviceEvaluation;

    /** 改进建议 */
    private String improvementSuggestions;

    /** 投诉内容 */
    private String complaints;

    /** 投诉处理情况 */
    private String complaintHandle;

    /** 是否需要后续跟进 */
    private Integer isFollowupNeeded;

    /** 后续跟进计划 */
    private String followupPlan;

    /** 下次回访日期 */
    private LocalDate nextFollowupDate;

    /** 问题是否已解决 */
    private Integer isResolved;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
