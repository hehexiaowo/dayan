package com.dayan.tool.dto;

import lombok.Data;

/** AI 创作成品保存指令（port 传输，字段对应 AgentContentCreateDTO） */
@Data
public class ToolAiartistContentCmd {
    private String title;
    private Integer contentType;
    private String contentBody;
    private String styleCode;
    private String audience;
    private String purpose;
    private String refContentCode;
    /** refKbFiles JSON（[{fileId,fileName}]） */
    private String refKbFiles;
    /** refGoodsCodes JSON（[code,...]） */
    private String refGoodsCodes;
    private String coverImage;
}
