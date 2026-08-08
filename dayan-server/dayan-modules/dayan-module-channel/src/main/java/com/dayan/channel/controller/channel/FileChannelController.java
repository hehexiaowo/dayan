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

    @Operation(summary = "预览/下载文件（代理下载）")
    @GetMapping("/preview/**")
    public void preview(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        String prefix = "/channel-api/v1/files/preview/";
        String key = uri.substring(uri.indexOf(prefix) + prefix.length());
        if (StrUtil.isBlank(key) || !storageService.exists(key)) {
            response.setStatus(404);
            return;
        }
        response.setContentType(storageService.contentType(key));
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
