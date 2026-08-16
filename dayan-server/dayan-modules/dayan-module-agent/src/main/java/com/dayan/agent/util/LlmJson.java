package com.dayan.agent.util;

import cn.hutool.json.JSONUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;

/** LLM JSON 输出解析：剥围栏/截取首尾花括号 + hutool toBean */
public final class LlmJson {

    /** 提取首个 { 到最后一个 } 的 JSON 文本；无花括号抛业务异常 */
    public static String extractObject(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS, "模型未返回内容");
        }
        String cleaned = raw.replaceAll("```[a-zA-Z]*", "").replaceAll("```", "").trim();
        int l = cleaned.indexOf('{');
        int r = cleaned.lastIndexOf('}');
        if (l < 0 || r <= l) {
            throw new BusinessException(ErrorCode.BUSINESS, "模型未返回合法 JSON");
        }
        return cleaned.substring(l, r + 1);
    }

    public static <T> T parse(String raw, Class<T> clazz) {
        try {
            return JSONUtil.toBean(extractObject(raw), clazz);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "模型 JSON 解析失败: " + e.getMessage());
        }
    }

    private LlmJson() {}
}
