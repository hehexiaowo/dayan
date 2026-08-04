package com.dayan.service.dto;

import lombok.Data;

/**
 * 服务会话（service_session）更新入参（仅普通字段，状态/子状态走专用接口）。
 */
@Data
public class ServiceSessionUpdateDTO {

    private String serviceTitle;
    private String serviceDescription;
    private Integer priority;
    private String parkCode;
    private String parkFullName;
    private String agentCode;
    private String channelCode;
    private Integer isSatisfied;
    private Integer overallRating;
    private String remark;
}
