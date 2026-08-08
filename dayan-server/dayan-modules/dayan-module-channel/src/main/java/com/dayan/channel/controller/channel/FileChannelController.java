package com.dayan.channel.controller.channel;

import cn.hutool.core.util.StrUtil;
import com.dayan.common.oss.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 文件预览 Controller（channel 端，只读）。
 * channel 端本轮不提供上传，只复用同一 MinIO 做代理下载显示。
 */
@Tag(name = "文件预览")
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileChannelController {

    private final StorageService storageService;

    /** 合法 key 字符集（字母/数字/_-/. /），防止日志注入与异常输入 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_/\\-.]+$");

    /** preview 允许内联渲染的 contentType（其余强制下载，避免 XSS） */
    private static final Set<String> INLINE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm",
            "application/pdf");

    @Operation(summary = "预览/下载文件（代理下载）")
    @GetMapping("/preview/**")
    public void preview(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        String prefix = "/channel-api/v1/files/preview/";
        String key = uri.substring(uri.indexOf(prefix) + prefix.length());
        if (StrUtil.isBlank(key) || !KEY_PATTERN.matcher(key).matches() || !storageService.exists(key)) {
            response.setStatus(404);
            return;
        }
        // 非图片/视频/PDF 类型强制下载，防止同源 XSS（key 后缀可伪造）
        String contentType = storageService.contentType(key);
        if (!INLINE_CONTENT_TYPES.contains(contentType)) {
            contentType = "application/octet-stream";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + key.substring(key.lastIndexOf('/') + 1) + "\"");
        }
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=86400");
        try (InputStream is = storageService.download(key);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        } catch (Exception e) {
            log.error("文件预览失败 key={}", key, e);
            response.setStatus(500);
        }
    }
}
