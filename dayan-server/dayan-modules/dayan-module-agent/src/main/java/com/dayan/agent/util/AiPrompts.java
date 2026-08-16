package com.dayan.agent.util;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** classpath:ai-prompts/ 提示词加载与 {{变量}} 渲染 */
public final class AiPrompts {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    /** @param name 不含扩展名，如 "strategy"、"purpose/product" */
    public static String load(String name) {
        return CACHE.computeIfAbsent(name, n -> {
            try (InputStream in = AiPrompts.class.getResourceAsStream("/ai-prompts/" + n + ".md")) {
                if (in == null) {
                    throw new BusinessException(ErrorCode.BUSINESS, "AI 提示词缺失: " + n);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.BUSINESS, "AI 提示词读取失败: " + n);
            }
        });
    }

    public static String render(String template, Map<String, String> vars) {
        String out = template;
        for (Map.Entry<String, String> en : vars.entrySet()) {
            out = out.replace("{{" + en.getKey() + "}}", en.getValue() == null ? "" : en.getValue());
        }
        return out;
    }

    private AiPrompts() {}
}
