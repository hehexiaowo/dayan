package com.dayan.tool.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工具 VO。
 */
@Data
public class ToolInfoVO {

    private Long id;
    /** 工具编码（TL 前缀） */
    private String toolCode;
    /** 工具名称 */
    private String toolName;
    /** 工具类型（pension/gap/aiartist/aichat） */
    private String toolType;
    /** 工具简介 */
    private String toolDesc;
    /** 工具配置 JSON */
    private String configJson;
    /** 状态：0=禁用 / 1=启用 */
    private Integer status;
    /** 备注 */
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
