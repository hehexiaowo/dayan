package com.dayan.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 知识库文档 VO（实时代理百炼：ListIndexDocuments / DescribeFile）。
 */
@Data
public class SystemKnowledgeDocVO {

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

    /** 所属类目 ID（DescribeFile 返回） */
    private String categoryId;

    /** 文件标签（DescribeFile 返回，≤10） */
    private List<String> tags;

    /** 解析器（DASHSCOPE_DOCMIND/DOCMIND_DIGITAL/DOCMIND_LLM_VERSION/AUTO_SELECT） */
    private String parser;

    /** 来源仓库 ID（agent 端合并列表时填充；admin 单库列表为空） */
    private Long repoId;

    /** 来源仓库名称（agent 端合并列表时填充） */
    private String repoName;
}
