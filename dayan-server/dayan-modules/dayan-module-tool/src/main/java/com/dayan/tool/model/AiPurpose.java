package com.dayan.tool.model;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;

/** 文章目的（决定素材必填项与 prompt 配比规则） */
public final class AiPurpose {
    public static final String PRODUCT = "product";
    public static final String PARK = "park";
    public static final String SCIENCE = "science";

    public static String requireValid(String purpose) {
        if (PRODUCT.equals(purpose) || PARK.equals(purpose) || SCIENCE.equals(purpose)) {
            return purpose;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文章目的: " + purpose);
    }
    private AiPurpose() {}
}
