package com.dayan.system.controller.client;

import cn.hutool.core.util.StrUtil;
import com.dayan.common.oss.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Client 端文件预览 Controller（只读代理下载，不含上传）。
 *
 * <p>客户端详情页需要展示机构图片（房型图/封面/banner 等），这些图片以 OSS key
 * 形式存 DB，前端通过此端点代理下载（同源零 CORS）。
 *
 * <p>逻辑与 {@link com.dayan.system.controller.agent.FileAgentController#preview} 一致，
 * 仅保留预览（client 端无上传场景）。
 *
 * <p>GET /client-api/v1/files/preview/{key}
 */
@Tag(name = "Client 端-文件预览")
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileClientController {

    private final StorageService storageService;

    /** 合法 key 字符集（字母/数字/_-/. /），防止日志注入 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_/\\-.]+$");

    /** preview 允许内联渲染的 contentType（其余强制下载） */
    private static final Set<String> INLINE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm",
            "application/pdf");

    @Operation(summary = "预览/下载文件（代理下载）")
    @GetMapping("/preview/**")
    public void preview(HttpServletRequest request, HttpServletResponse response) {
        // key 含斜杠（如 park/migration/2026/08/08/abc.png），提取 preview/ 之后的部分
        // 兼容不同 context-path（/client-api/ 等）
        String uri = request.getRequestURI();
        int idx = uri.indexOf("/preview/");
        if (idx < 0) {
            response.setStatus(404);
            return;
        }
        String key = uri.substring(idx + "/preview/".length());

        if (StrUtil.isBlank(key) || !KEY_PATTERN.matcher(key).matches()) {
            response.setStatus(404);
            return;
        }
        if (!storageService.exists(key)) {
            response.setStatus(404);
            return;
        }

        String contentType = storageService.contentType(key);
        if (!INLINE_CONTENT_TYPES.contains(contentType)) {
            contentType = "application/octet-stream";
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + key.substring(key.lastIndexOf('/') + 1) + "\"");
        }
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=86400");
        try (InputStream is = storageService.download(key);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        } catch (Exception e) {
            log.error("Client 文件预览失败 key={}", key, e);
            response.setStatus(500);
        }
    }
}
