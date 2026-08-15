package com.dayan.common.aliyun.bailian;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 阿里云 OpenAPI ROA 风格签名客户端（V1 签名：HmacSHA1 + canonical headers/resource）。
 *
 * <p>百炼 2023-12-29 知识库索引接口（/{workspaceId}/index/*）为 ROA 风格 POST；
 * 官方 SDK 2.0.8 的参数名（DocumentIds）已与当前服务端（file_ids）脱节，
 * 故 CreateIndex 等索引接口改由此客户端直连（签名算法已实测通过服务端校验）。
 *
 * <p>签名规则（阿里云 ROA V1）：
 * <pre>
 * StringToSign = Method + "\n" + Accept + "\n" + Content-MD5 + "\n"
 *              + Content-Type + "\n" + Date + "\n"
 *              + CanonicalizedHeaders + CanonicalizedResource
 * </pre>
 * 其中 CanonicalizedHeaders = 排序后的 x-acs-* 头（小写 key:value\n）；
 * CanonicalizedResource = 路径 + "?" + 排序后的 query（value 原样不编码）。
 * 发送时 query value 需 URL 编码（签名用原始值，服务端解码后一致）。
 */
public class BailianRoaClient {

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String endpoint;

    public BailianRoaClient(String accessKeyId, String accessKeySecret, String region) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.endpoint = "bailian." + (region == null || region.isBlank() ? "cn-beijing" : region) + ".aliyuncs.com";
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    /**
     * ROA POST 调用。
     *
     * @param action  接口名（如 CreateIndex）
     * @param version API 版本（2023-12-29）
     * @param path    路径（含 workspaceId，如 /{ws}/index/create）
     * @param query   业务参数（key 驼峰；value 原始字符串，发送时自动 URL 编码）
     * @return 响应体 JSON 字符串（HTTP 2xx 时）
     */
    public String post(String action, String version, String path, Map<String, String> query) {
        TreeMap<String, String> sorted = new TreeMap<>(query);
        String canonicalResource = path + (sorted.isEmpty() ? "" : "?" + joinQuery(sorted));
        String nonce = UUID.randomUUID().toString();
        String timestamp = TS_FORMAT.format(Instant.now());

        TreeMap<String, String> acsHeaders = new TreeMap<>();
        acsHeaders.put("x-acs-action", action);
        acsHeaders.put("x-acs-date", timestamp);
        acsHeaders.put("x-acs-signature-method", "HMAC-SHA1");
        acsHeaders.put("x-acs-signature-nonce", nonce);
        acsHeaders.put("x-acs-signature-version", "1.0");
        acsHeaders.put("x-acs-version", version);

        StringBuilder canonicalHeaders = new StringBuilder();
        acsHeaders.forEach((k, v) -> canonicalHeaders.append(k).append(':').append(v).append('\n'));

        // Date 行：服务端要求 Date header 实际值参与签名（与 x-acs-date 同格式）
        String stringToSign = "POST\napplication/json\n\napplication/json\n" + timestamp + "\n"
                + canonicalHeaders + canonicalResource;
        String signature = sign(stringToSign, accessKeySecret);

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + endpoint + path + (sorted.isEmpty() ? "" : "?" + encodeQuery(sorted))))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Date", timestamp)
                    .header("Authorization", "acs " + accessKeyId + ":" + signature);
            acsHeaders.forEach(builder::header);
            HttpRequest request = builder.POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = resp.body();
            if (resp.statusCode() >= 400) {
                throw new BusinessException(ErrorCode.BUSINESS,
                        "百炼接口调用失败（HTTP " + resp.statusCode() + "）: " + truncate(body));
            }
            return body;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "百炼接口调用异常: " + e.getMessage(), e);
        }
    }

    /**
     * ROA GET 调用（无请求体，如 GetIndexJobStatus / ListIndexDocuments）。
     *
     * @param action  接口名
     * @param version API 版本
     * @param path    路径（含 workspaceId）
     * @param query   业务参数（key 驼峰；value 原始字符串，发送时自动 URL 编码）
     * @return 响应体 JSON 字符串（HTTP 2xx 时）
     */
    public String get(String action, String version, String path, Map<String, String> query) {
        TreeMap<String, String> sorted = new TreeMap<>(query);
        String canonicalResource = path + (sorted.isEmpty() ? "" : "?" + joinQuery(sorted));
        String nonce = UUID.randomUUID().toString();
        String timestamp = TS_FORMAT.format(Instant.now());

        TreeMap<String, String> acsHeaders = new TreeMap<>();
        acsHeaders.put("x-acs-action", action);
        acsHeaders.put("x-acs-date", timestamp);
        acsHeaders.put("x-acs-signature-method", "HMAC-SHA1");
        acsHeaders.put("x-acs-signature-nonce", nonce);
        acsHeaders.put("x-acs-signature-version", "1.0");
        acsHeaders.put("x-acs-version", version);

        StringBuilder canonicalHeaders = new StringBuilder();
        acsHeaders.forEach((k, v) -> canonicalHeaders.append(k).append(':').append(v).append('\n'));

        // GET 无请求体：Content-Type 行为空（Method\nAccept\nMD5\nCT\nDate 五段）
        String stringToSign = "GET\napplication/json\n\n\n" + timestamp + "\n"
                + canonicalHeaders + canonicalResource;
        String signature = sign(stringToSign, accessKeySecret);

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + endpoint + path + (sorted.isEmpty() ? "" : "?" + encodeQuery(sorted))))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Date", timestamp)
                    .header("Authorization", "acs " + accessKeyId + ":" + signature);
            acsHeaders.forEach(builder::header);
            HttpRequest request = builder.GET().build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = resp.body();
            if (resp.statusCode() >= 400) {
                throw new BusinessException(ErrorCode.BUSINESS,
                        "百炼接口调用失败（HTTP " + resp.statusCode() + "）: " + truncate(body));
            }
            return body;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "百炼接口调用异常: " + e.getMessage(), e);
        }
    }

    /**
     * ROA POST 调用（formData 请求体，如 ApplyFileUploadLease）。
     *
     * @param action  接口名
     * @param version API 版本
     * @param path    路径（含 workspaceId）
     * @param form    form 字段（key 驼峰；value 原始字符串，发送时自动 URL 编码）
     * @return 响应体 JSON 字符串（HTTP 2xx 时）
     */
    public String postForm(String action, String version, String path, Map<String, String> form) {
        TreeMap<String, String> sorted = new TreeMap<>(form);
        String canonicalResource = path;
        String nonce = UUID.randomUUID().toString();
        String timestamp = TS_FORMAT.format(Instant.now());
        String contentType = "application/x-www-form-urlencoded";

        TreeMap<String, String> acsHeaders = new TreeMap<>();
        acsHeaders.put("x-acs-action", action);
        acsHeaders.put("x-acs-date", timestamp);
        acsHeaders.put("x-acs-signature-method", "HMAC-SHA1");
        acsHeaders.put("x-acs-signature-nonce", nonce);
        acsHeaders.put("x-acs-signature-version", "1.0");
        acsHeaders.put("x-acs-version", version);

        StringBuilder canonicalHeaders = new StringBuilder();
        acsHeaders.forEach((k, v) -> canonicalHeaders.append(k).append(':').append(v).append('\n'));

        // Date 行：服务端要求 Date header 实际值参与签名（与 x-acs-date 同格式）
        String stringToSign = "POST\napplication/json\n\n" + contentType + "\n" + timestamp + "\n"
                + canonicalHeaders + canonicalResource;
        String signature = sign(stringToSign, accessKeySecret);
        String formBody = encodeQuery(sorted);

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + endpoint + path))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Content-Type", contentType)
                    .header("Date", timestamp)
                    .header("Authorization", "acs " + accessKeyId + ":" + signature);
            acsHeaders.forEach(builder::header);
            HttpRequest request = builder
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = resp.body();
            if (resp.statusCode() >= 400) {
                throw new BusinessException(ErrorCode.BUSINESS,
                        "百炼接口调用失败（HTTP " + resp.statusCode() + "）: " + truncate(body));
            }
            return body;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS, "百炼接口调用异常: " + e.getMessage(), e);
        }
    }

    /** 签名用 query 拼接（value 原样不编码，& 分隔） */
    private String joinQuery(TreeMap<String, String> sorted) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    /** 发送用 query 拼接（value 做 RFC3986 URL 编码） */
    private String encodeQuery(TreeMap<String, String> sorted) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
        }
        return sb.toString();
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
                .replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    /** HmacSHA1 签名（ROA V1：key 直接用 AccessKeySecret，RPC 风格才拼 "&"） */
    private String sign(String stringToSign, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return java.util.Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "签名计算失败: " + e.getMessage());
        }
    }

    private String truncate(String s) {
        return s == null ? "" : (s.length() > 500 ? s.substring(0, 500) : s);
    }
}
