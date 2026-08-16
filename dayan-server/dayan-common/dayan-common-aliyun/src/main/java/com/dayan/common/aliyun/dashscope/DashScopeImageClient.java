package com.dayan.common.aliyun.dashscope;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * DashScope 文生图（异步）：提交任务 → 轮询 → 取结果 URL → 下载字节。
 * 与百炼兼容模式共用同一 sk- API-Key。
 */
public class DashScopeImageClient {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final long POLL_INTERVAL_MS = 3000;

    /** 提交文生图任务，返回 taskId */
    public String submit(String apiKey, String apiBase, String model, String prompt, String size) {
        JSONObject body = JSONUtil.createObj()
                .set("model", model)
                .set("input", JSONUtil.createObj().set("prompt", prompt))
                .set("parameters", JSONUtil.createObj().set("size", size).set("n", 1));
        HttpRequest request = HttpRequest.newBuilder(URI.create(trimBase(apiBase) + "/api/v1/services/aigc/text2image/image-synthesis"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-DashScope-Async", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        JSONObject resp = execute(request);
        String taskId = resp.getByPath("output.task_id", String.class);
        if (taskId == null || taskId.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS, "文生图任务提交失败: " + brief(resp));
        }
        return taskId;
    }

    /** 轮询直到成功返回图片 URL；FAILED/超时抛业务异常 */
    public String pollImageUrl(String apiKey, String apiBase, String taskId, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(trimBase(apiBase) + "/api/v1/tasks/" + taskId))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .GET().build();
            JSONObject resp = execute(request);
            String status = resp.getByPath("output.task_status", String.class);
            if ("SUCCEEDED".equals(status)) {
                String url = resp.getByPath("output.results[0].url", String.class);
                if (url == null || url.isBlank()) {
                    throw new BusinessException(ErrorCode.BUSINESS, "文生图成功但未返回 URL");
                }
                return url;
            }
            if ("FAILED".equals(status) || "CANCELED".equals(status)) {
                throw new BusinessException(ErrorCode.BUSINESS, "文生图失败: " + brief(resp));
            }
            sleep();
        }
        throw new BusinessException(ErrorCode.BUSINESS, "文生图超时（taskId=" + taskId + "）");
    }

    /** 下载远程图片字节（10MB 上限） */
    public byte[] download(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).GET().build();
        try {
            HttpResponse<byte[]> resp = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                throw new BusinessException(ErrorCode.BUSINESS, "图片下载失败（HTTP " + resp.statusCode() + "）");
            }
            byte[] data = resp.body();
            if (data == null || data.length == 0 || data.length > 10 * 1024 * 1024) {
                throw new BusinessException(ErrorCode.BUSINESS, "图片内容异常（大小=" + (data == null ? 0 : data.length) + "）");
            }
            return data;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "图片下载异常: " + e.getMessage());
        }
    }

    private JSONObject execute(HttpRequest request) {
        try {
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new BusinessException(ErrorCode.BUSINESS, "DashScope 调用失败（HTTP " + resp.statusCode() + "）：" + truncate(resp.body()));
            }
            return JSONUtil.parseObj(resp.body());
        } catch (BusinessException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            throw new BusinessException(ErrorCode.BUSINESS, "DashScope 调用异常: " + e.getMessage());
        }
    }

    private static void sleep() {
        try { Thread.sleep(POLL_INTERVAL_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static String trimBase(String base) {
        String b = (base == null || base.isBlank()) ? "https://dashscope.aliyuncs.com" : base;
        return b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
    }

    private static String brief(JSONObject resp) {
        return truncate(resp.toString());
    }

    private static String truncate(String s) {
        return s == null ? "" : (s.length() > 500 ? s.substring(0, 500) : s);
    }
}
