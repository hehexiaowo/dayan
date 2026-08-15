package com.dayan.agent.dto;

import lombok.Data;

/**
 * 我的内容分页查询（agentCode 服务端强制注入，前端不传）。
 */
@Data
public class AgentContentQueryDTO {

    private long current = 1;
    private long size = 10;
    /** 形态筛选（1=图文 2=朋友圈 3=视频脚本） */
    private Integer contentType;
    /** 标题关键字 */
    private String keyword;
}
