package com.dayan.tool.model;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;

import java.util.Set;

/**
 * 工具实例类型（tool_info.tool_type）。
 */
public final class ToolType {

    public static final String PENSION = "pension";
    public static final String GAP = "gap";
    public static final String AI_CREATOR = "ai_creator";
    public static final String AI_QA = "ai_qa";

    private static final Set<String> ALL = Set.of(PENSION, GAP, AI_CREATOR, AI_QA);

    private ToolType() {
    }

    public static void requireValid(String toolType) {
        if (toolType == null || !ALL.contains(toolType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工具类型仅支持 pension/gap/ai_creator/ai_qa");
        }
    }

    public static boolean isValid(String toolType) {
        return toolType != null && ALL.contains(toolType);
    }
}
