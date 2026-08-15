package com.dayan.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 选题灵感请求（基于勾选素材 + 时节出选题方向）。
 */
@Data
public class AiTopicsDTO {

    /** 勾选知识库文档 fileId（可空） */
    private List<String> kbFileIds;

    /** 勾选商品 goodsCode（可空） */
    private List<String> goodsCodes;
}
