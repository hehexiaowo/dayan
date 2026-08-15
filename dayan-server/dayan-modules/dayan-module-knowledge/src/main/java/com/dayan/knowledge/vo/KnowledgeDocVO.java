package com.dayan.knowledge.vo;

import lombok.Data;

/**
 * 知识库文档 VO（实时代理百炼：ListIndexDocuments / DescribeFile）。
 */
@Data
public class KnowledgeDocVO {

    /** 百炼文件/文档 ID */
    private String fileId;

    /** 文件名 */
    private String fileName;

    /** 解析状态（INIT/PARSING/PARSE_SUCCESS/PARSE_FAILED，来自 DescribeFile） */
    private String parseStatus;

    /** 索引内文档状态（INSERT_ERROR/RUNNING/FINISH/DELETED 等，来自 ListIndexDocuments） */
    private String indexStatus;

    /** 文件大小（字节） */
    private Long sizeInBytes;

    /** 更新时间（毫秒时间戳，来自百炼） */
    private Long gmtModified;

    /** 文档类型 */
    private String documentType;
}
