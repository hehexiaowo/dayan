package com.dayan.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 表 service_equity_demand 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_equity_demand")
public class ServiceEquityDemand extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 需求编码 */
    private String demandCode;

    /** 服务会话编码 */
    private String sessionCode;

    /** 客户编码 */
    private String clientCode;

    /** 管家编码 */
    private String butlerCode;

    /** 需求类型 */
    private Integer demandType;

    /** 使用人姓名 */
    private String usePersonName;

    /** 使用人年龄 */
    private Integer usePersonAge;

    /** 使用人性别 */
    private Integer usePersonGender;

    /** 健康状况概述 */
    private String healthSummary;

    /** 所需照护等级 */
    private Integer careLevelNeed;

    /** 城市偏好 */
    private String cityPreference;

    /** 区域偏好 */
    private String areaPreference;

    /** 预算下限 */
    private BigDecimal budgetMin;

    /** 预算上限 */
    private BigDecimal budgetMax;

    /** 房间偏好 */
    private String roomPreference;

    /** 饮食偏好 */
    private String foodPreference;

    /** 特殊需求 */
    private String specialNeeds;

    /** 期望服务时间 */
    private LocalDate expectedTime;

    /** 联系偏好 */
    private Integer contactPreference;

    /** 收集方式 */
    private Integer collectMethod;

    /** 收集时间 */
    private LocalDateTime collectTime;

    /** 需求总结 */
    private String demandSummary;

    /** 需求相关资料图片 */
    private String demandImages;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
