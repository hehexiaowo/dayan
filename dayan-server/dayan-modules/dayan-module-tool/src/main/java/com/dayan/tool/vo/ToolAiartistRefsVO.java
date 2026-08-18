package com.dayan.tool.vo;

import lombok.Data;

import java.util.List;

/** 素材引用（material_refs JSON 结构，前端随创建提交并带展示名；旧数据 kbFileIds/goodsCodes/parkCodes 反序列化为 null 不影响流程） */
@Data
public class ToolAiartistRefsVO {
    /** 参考范文（TPL:模板码 或 内容 code） */
    private String refContentCode;
    /** 知识库文件（含文件名，保存成品 refKbFiles 用） */
    private List<KbFileRef> kbFiles;
    /** 商品（含名称） */
    private List<CodeNameRef> goods;
    /** 机构（含名称） */
    private List<CodeNameRef> parks;

    /** 知识库文件引用 */
    @Data
    public static class KbFileRef {
        private String fileId;
        private String fileName;
    }

    /** 商品/机构引用（编码+展示名） */
    @Data
    public static class CodeNameRef {
        private String code;
        private String name;
    }
}
