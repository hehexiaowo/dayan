package com.dayan.tool.vo;

import lombok.Data;

import java.util.List;

/** 渠道端问答人物（含 admin 全局库与本渠道补充库，供控制台分别展示） */
@Data
public class ToolChannelPersonaVO {

    /** 工具实例编码（TL 前缀） */
    private String toolCode;

    /** 人物名称（= tool_info.tool_name） */
    private String personaName;

    /** 工具简介 */
    private String toolDesc;

    /** admin 全局绑定的知识库 ID（config_json.repoIds，只读） */
    private List<Long> globalRepoIds;

    /** 本渠道补充的知识库 ID（tool_channel_repo_bind，可编辑） */
    private List<Long> channelRepoIds;
}
