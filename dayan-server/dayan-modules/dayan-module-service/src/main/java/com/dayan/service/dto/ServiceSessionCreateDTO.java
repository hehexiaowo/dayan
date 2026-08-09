package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 服务会话（service_session）创建入参。
 *
 * <p>初始 session_status=1（待分配）、sub_status=normal、session_code=SS+10 由服务端生成。
 * equityCode 可空（非权益触发场景）；clientCode 必填。
 */
@Data
public class ServiceSessionCreateDTO {

    /** 关联权益编码（可空，非权益触发场景） */
    private String equityCode;

    /** 服务项目编码（权益激活按 service_item 创建会话时标记） */
    private String itemCode;

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 50)
    private String clientCode;

    /** 服务类型（1=机构入住, 2=场景活动, 3=居家养老, 4=健康咨询） */
    private Integer serviceType;

    @Size(max = 200)
    private String serviceTitle;

    private String serviceDescription;

    /** 优先级（0=普通, 1=优先, 2=紧急, 3=非常紧急） */
    private Integer priority;

    /** 来源（1=权益触发, 2=客户主动, 3=代理人委托, 4=管家发起） */
    private Integer sourceType;

    private String sourceCode;
    private String parkCode;
    private String parkFullName;
    private String agentCode;
    private String channelCode;
    private String remark;
}
