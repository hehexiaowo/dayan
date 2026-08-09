package com.dayan.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 service_session 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_session")
public class ServiceSession extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 会话编码 */
    private String sessionCode;

    /** 关联权益编码 */
    private String equityCode;

    /** 服务项目编码（权益激活按 service_item 创建会话时标记） */
    private String itemCode;

    /** 客户编码 */
    private String clientCode;

    /** 服务管家编码 */
    private String butlerCode;

    /** 服务管家姓名(快照) */
    private String butlerFullName;

    /** 服务类型 */
    private Integer serviceType;

    /** 服务标题 */
    private String serviceTitle;

    /** 服务描述 */
    private String serviceDescription;

    /** 优先级 */
    private Integer priority;

    /** 来源 */
    private Integer sourceType;

    /** 来源编码 */
    private String sourceCode;

    /** 关联养老机构编码 */
    private String parkCode;

    /** 关联养老机构名称(快照) */
    private String parkFullName;

    /** 关联代理人编码 */
    private String agentCode;

    /** 关联渠道编码 */
    private String channelCode;

    /** 受理时间 */
    private LocalDateTime acceptTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 关闭时间 */
    private LocalDateTime closeTime;

    /** 总服务时长(小时) */
    private Integer totalDuration;

    /** 服务接触次数 */
    private Integer touchCount;

    /** 是否满意 */
    private Integer isSatisfied;

    /** 综合评分 */
    private Integer overallRating;

    /** 会话状态 */
    private Integer sessionStatus;

    /** 子状态 */
    private String subStatus;

    /** 关闭原因 */
    private String closeReason;

    /** 备注 */
    private String remark;
}
