package com.dayan.service.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务会话 VO。
 */
@Data
public class ServiceSessionVO {

    private Long id;
    private String sessionCode;
    private String equityCode;
    /** 服务项目编码 */
    private String itemCode;
    /** 服务项目名称（join service_item.item_name；服务类型展示用） */
    private String itemName;
    private String clientCode;
    /** 客户姓名 join client_info.full_name */
    private String clientName;
    private String butlerCode;
    private String butlerFullName;
    private Integer serviceType;
    private String serviceTitle;
    private String serviceDescription;
    private Integer priority;
    private Integer sourceType;
    private String sourceCode;
    private String parkCode;
    private String parkFullName;
    private String agentCode;
    private String channelCode;
    private LocalDateTime acceptTime;
    private LocalDateTime completeTime;
    private LocalDateTime closeTime;
    private Integer totalDuration;
    private Integer touchCount;
    /** 最大使用次数（配额上限） */
    private Integer maxUseCount;
    /** 已使用次数 */
    private Integer usedCount;
    /** 配额周期（1=终身,2=年度） */
    private Integer quotaType;
    private Integer isSatisfied;
    private Integer overallRating;
    private Integer sessionStatus;
    private String subStatus;
    private String closeReason;
    private String remark;
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
