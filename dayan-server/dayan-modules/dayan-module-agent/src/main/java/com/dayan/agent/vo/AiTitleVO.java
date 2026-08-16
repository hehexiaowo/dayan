package com.dayan.agent.vo;

import lombok.Data;

/** 候选标题 */
@Data
public class AiTitleVO {
    private String title;
    /** kb_number=素材硬数据/doc_logic=资料逻辑/emotion_hook=情绪悬念 */
    private String tag;
    private Integer viralScore;
    private String reasoning;
}
