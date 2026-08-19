package com.dayan.system.controller.agent;

import cn.hutool.core.util.StrUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.common.oss.config.StorageProperties;
import com.dayan.common.oss.service.StorageService;
import com.dayan.common.security.StpKit;
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
 * Agent 端文件预览 Controller（只读代理下载，不含上传）。
 *
 * <p>Agent 端详情页需要展示机构图片（房型图/封面/banner 等），
 * 这些图片以 OSS key 形式存 DB，前端通过此端点代理下载（同源零 CORS）。
 *
 * <p>GET /agent-api/v1/files/preview/{key}
 */
@Tag(name = "Agent 端-文件预览")
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileAgentController {

    private final StorageService storageService;
    private final StorageProperties storageProperties;

    /** 允许的图片后缀（agent 端仅图片场景：头像） */
    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 允许的上传模块白名单（防止任意字符进入存储 key 前缀） */
    private static final Set<String> ALLOWED_MODULES = Set.of("avatar", "common");

    /** 合法 key 字符集（字母/数字/_-/. /），防止日志注入 */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_/\\-.]+$");

    /** preview 允许内联渲染的 contentType（其余强制下载） */
    private static final Set<String> INLINE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm",
            "application/pdf");

    /** 静态资源缓存时间：24 小时 */
    private static final String CACHE_CONTROL_ONE_DAY = "max-age=86400";

    @Operation(summary = "上传文件（图片，module 默认 avatar）")
    @PostMapping("/upload")
    public R<com.dayan.common.oss.dto.FileUploadDTO> upload(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "module", required = false) String module) {
        // 显式登录校验（agent 端无注解式全局登录强制）
        if (StpKit.AGENT.getLoginIdDefaultNull() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        long size = file.getSize();
        if (size > storageProperties.getMaxSize()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "文件大小超过限制 " + (storageProperties.getMaxSize() / 1024 / 1024) + "MB");
        }
        String originalName = file.getOriginalFilename();
        String ext = cn.hutool.core.io.FileUtil.extName(originalName);
        if (StrUtil.isBlank(ext) || !ALLOWED_IMAGE_EXT.contains(ext.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持图片文件: " + ext);
        }
        String channelCode = "agent";
        try {
            String sessionChannel = (String) StpKit.AGENT.getSession().get("channelCode");
            if (StrUtil.isNotBlank(sessionChannel)) {
                channelCode = sessionChannel;
            }
        } catch (Exception ignored) {
            // 无 session 时用默认值
        }
        String mod = StrUtil.isBlank(module) ? "avatar" : module.trim();
        if (!ALLOWED_MODULES.contains(mod)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的上传模块: " + mod);
        }
        try {
            String key = storageService.upload(mod, channelCode,
                    file.getInputStream(), size, file.getContentType(), originalName);
            com.dayan.common.oss.dto.FileUploadDTO dto = new com.dayan.common.oss.dto.FileUploadDTO();
            dto.setKey(key);
            dto.setUrl("/agent-api/v1/files/preview/" + key);
            dto.setOriginalName(originalName);
            dto.setSize(size);
            return R.ok(dto);
        } catch (Exception e) {
            log.error("Agent 文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "预览/下载文件（代理下载）")
    @GetMapping("/preview/**")
    public void preview(HttpServletRequest request, HttpServletResponse response) {
        // key 含斜杠（如 park/day001/2026/08/08/abc.jpg），提取 preview/ 之后的部分
        // 兼容不同 context-path（/agent-api/ 或 /admin-api/）
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
        response.setHeader("Cache-Control", CACHE_CONTROL_ONE_DAY);
        try (InputStream is = storageService.download(key);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        } catch (Exception e) {
            log.error("Agent 文件预览失败 key={}", key, e);
            response.setStatus(500);
        }
    }
}
