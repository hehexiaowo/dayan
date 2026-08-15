package com.dayan.common.aliyun.bailian;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * 百炼模型推理客户端（OpenAI 兼容模式）。
 *
 * 直调专属网关 {host}/compatible-mode/v1/chat/completions，
 * 仅依赖 JDK HttpClient，零第三方 HTTP 依赖。凭据（API-Key / 网关域名 / 模型名）
 * 由调用方从 system_config 组装注入。
 */
public class BailianChatClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    private final HttpClient httpClient;

    public BailianChatClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * 单轮对话（RAG 问答用：系统提示词拼检索片段 + 用户问题）。
     *
     * @param apiKey  百炼 API-Key（sk- 开头）
     * @param apiHost 专属网关域名（不含协议与路径）
     * @param model   模型名（如 qwen-plus）
     * @param systemPrompt 系统提示词（可含知识库检索片段）
     * @param userPrompt   用户问题
     * @return 模型回答文本
     */
    public String chat(String apiKey, String apiHost, String model,
                       String systemPrompt, String userPrompt) {
        return chat(apiKey, apiHost, model,
                List.of(new Message("system", systemPrompt), new Message("user", userPrompt)), 0.3);
    }

    /** 单轮对话（指定采样温度：知识问答 0.3，营销创作建议 0.6） */
    public String chat(String apiKey, String apiHost, String model,
                       String systemPrompt, String userPrompt, double temperature) {
        return chat(apiKey, apiHost, model,
                List.of(new Message("system", systemPrompt), new Message("user", userPrompt)), temperature);
    }

    /** 多轮对话（messages 由调用方组装，支持 role: system/user/assistant） */
    public String chat(String apiKey, String apiHost, String model, List<Message> messages) {
        return chat(apiKey, apiHost, model, messages, 0.3);
    }

    /** 多轮对话（指定采样温度），messages 由调用方组装 */
    public String chat(String apiKey, String apiHost, String model, List<Message> messages, double temperature) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS, "百炼模型 API-Key 未配置（system_config → llm 分组）");
        }
        if (apiHost == null || apiHost.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS, "百炼专属网关域名未配置（system_config → llm 分组）");
        }
        String baseUrl = apiHost.startsWith("http") ? apiHost : "https://" + apiHost;
        String url = baseUrl.replaceAll("/+$", "") + "/compatible-mode/v1/chat/completions";

        JSONArray msgs = new JSONArray();
        for (Message m : messages) {
            msgs.add(new JSONObject().set("role", m.role).set("content", m.content));
        }
        JSONObject body = new JSONObject()
                .set("model", model == null || model.isBlank() ? "qwen-plus" : model)
                .set("messages", msgs)
                .set("temperature", temperature);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JSONObject json = JSONUtil.parseObj(resp.body());
            if (resp.statusCode() != 200) {
                String err = json.getStr("error", JSONUtil.toJsonStr(json.get("message")));
                throw new BusinessException(ErrorCode.BUSINESS, "AI 问答调用失败（HTTP " + resp.statusCode() + "）：" + err);
            }
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException(ErrorCode.BUSINESS, "AI 问答返回为空");
            }
            return choices.getJSONObject(0).getJSONObject("message").getStr("content", "");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "AI 问答调用异常: " + e.getMessage(), e);
        }
    }

    /** 对话消息 */
    public record Message(String role, String content) {
    }
}
