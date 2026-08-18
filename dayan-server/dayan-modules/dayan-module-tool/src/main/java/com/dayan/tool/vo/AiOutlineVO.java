package com.dayan.tool.vo;

import lombok.Data;

import java.util.List;

/** 大纲（outline JSON 结构） */
@Data
public class AiOutlineVO {
    private AiImageSpec coverImage;
    private List<AiOutlineNode> nodes;

    @Data
    public static class AiOutlineNode {
        private String id;
        private String section;
        private List<String> corePoints;
        private List<String> arguments;
        private List<String> viralTags;
        private AiImageSpec imageInsertion;
    }

    @Data
    public static class AiImageSpec {
        /** ai_generated / user_upload */
        private String source;
        /** 1024*1024 / 1280*720 / 1080*1440 */
        private String size;
        private String prompt;
        private String imagePromptZh;
    }
}
