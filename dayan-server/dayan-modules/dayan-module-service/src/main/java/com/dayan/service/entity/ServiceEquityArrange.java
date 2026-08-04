package com.dayan.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
/**
 * 表 service_equity_arrange 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_equity_arrange")
public class ServiceEquityArrange extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 服务会话编码 */
    private String sessionCode;

    /** 关联方案编码 */
    private String solutionCode;

    /** 客户编码 */
    private String clientCode;

    /** 管家编码 */
    private String butlerCode;

    /** 安排编码 */
    private String arrangeCode;

    /** 安排类型 */
    private Integer arrangeType;

    /** 关联养老机构编码 */
    private String parkCode;

    /** 关联养老机构名称(快照) */
    private String parkFullName;

    /** 安排日期 */
    private LocalDate arrangeDate;

    /** 开始时间 */
    private LocalTime arrangeTimeStart;

    /** 结束时间 */
    private LocalTime arrangeTimeEnd;

    /** 安排地址 */
    private String arrangeAddress;

    /** 对接联系人 */
    private String contactPerson;

    /** 对接联系电话 */
    private String contactPhone;

    /** 参与人数 */
    private Integer participantCount;

    /** 准备事项 */
    private String prepareItems;

    /** 进展备注 */
    private String progressNotes;

    /** 客户确认时间 */
    private LocalDateTime confirmTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 是否已确认 */
    private Integer isConfirmed;

    /** 状态 */
    private Integer status;

    /** 取消原因 */
    private String cancelReason;

    /** 备注 */
    private String remark;
}
