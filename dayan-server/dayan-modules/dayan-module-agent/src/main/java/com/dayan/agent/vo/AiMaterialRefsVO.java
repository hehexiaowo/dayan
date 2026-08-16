package com.dayan.agent.vo;

import lombok.Data;

import java.util.List;

/** 素材引用（material_refs JSON 结构） */
@Data
public class AiMaterialRefsVO {
    private String refContentCode;
    private List<String> kbFileIds;
    private List<String> goodsCodes;
    private List<String> parkCodes;
}
